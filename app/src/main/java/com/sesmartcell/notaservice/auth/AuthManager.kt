package com.sesmartcell.notaservice.auth

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.Timestamp
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.firestore
import com.sesmartcell.notaservice.R
import com.sesmartcell.notaservice.data.database.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.security.MessageDigest

data class AppUser(
    val uid: String,
    val customUserId: String,
    val customPassword: String,
    val displayName: String,
    val email: String?,
    val loginMethod: String,
    val registrationComplete: Boolean = false
)

/**
 * Alur autentikasi yang DIPAKSAKAN (wajib berurutan):
 *  1. Daftar & verifikasi lewat akun Google      -> [registerWithGoogle]
 *  2. Membuat ID Login & Password sendiri        -> [completeRegistration]
 *  3. Baru bisa login dengan ID & Password       -> [loginWithCredentials]
 *
 * - Kredensial akun tersimpan di Firestore `users/{uid}` sesuai firestore.rules:
 *   hanya `passwordHash` (SHA-256), TIDAK ADA plaintext password di cloud.
 * - Akun yang pernah mendaftar di perangkat ini disimpan di penyimpanan lokal
 *   khusus (outlived logout), sehingga login ID & password tetap bisa
 *   diverifikasi meski offline.
 * - Tidak ada operasi auth yang menunggu respons server secara blocking,
 *   sehingga tidak lagi muncul "Failed to get document because the client is
 *   offline" saat perangkat sedang tidak ada koneksi.
 */
object AuthManager {

    internal const val PREFS_NAME = "se_smart_cell_auth_prefs"
    private const val ACCOUNTS_PREFS = "se_smart_cell_registered_accounts"
    private const val KEY_LOGGED_IN = "key_is_logged_in"
    private const val KEY_REGISTRATION_COMPLETE = "key_registration_complete"
    internal const val KEY_UID = "key_user_uid"
    private const val KEY_CUSTOM_USER_ID = "key_custom_user_id"
    private const val KEY_CUSTOM_PASSWORD = "key_custom_password"
    private const val KEY_NAME = "key_user_name"
    private const val KEY_EMAIL = "key_user_email"

    val auth: FirebaseAuth
        get() = Firebase.auth

    val currentUser: FirebaseUser?
        get() = try {
            Firebase.auth.currentUser
        } catch (e: Exception) {
            null
        }

    /**
     * Instance Firestore mengikuti `firestore_database_id` (satu sumber
     * kebenaran yang sama dengan FirestoreSyncManager) supaya dokumen akun
     * dan data sync selalu berada di database yang sama.
     */
    private fun firestore(context: Context): FirebaseFirestore {
        val dbId = try {
            context.getString(R.string.firestore_database_id)
        } catch (_: Exception) {
            ""
        }.trim()
        return if (dbId.isNotBlank() && dbId != "(default)") {
            FirebaseFirestore.getInstance(dbId)
        } else {
            Firebase.firestore
        }
    }

    private fun sessionPrefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Penyimpanan akun terdaftar di perangkat — TIDAK dihapus saat logout. */
    private fun accountsStore(context: Context) =
        context.getSharedPreferences(ACCOUNTS_PREFS, Context.MODE_PRIVATE)

    private fun accountKey(uid: String) = "acct_$uid"

    private fun hashPassword(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest("NotaService:$password".toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }

    private fun fallbackEmail(uid: String) = "user_${uid.take(10)}@notaservice.local"

    private fun ensureFirebaseInitialized(context: Context) {
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
        } catch (e: Exception) {
            Log.w("AuthManager", "Firebase init skipped: ${e.message}")
        }
    }

    private fun resolveGoogleServerClientId(context: Context): String {
        val resourceId = try {
            context.getString(R.string.default_web_client_id)
        } catch (_: Exception) {
            ""
        }.trim()

        if (resourceId.isNotBlank() && !resourceId.startsWith("YOUR_") && resourceId.contains(".apps.googleusercontent.com")) {
            return resourceId
        }

        throw IllegalStateException(
            "Google Web Client ID belum dikonfigurasi. Pastikan google-services.json sudah ada dan project Firebase benar."
        )
    }

    // ==== Status sesi ====

    /**
     * Pengguna dianggap login hanya jika pendaftaran (Google + buat ID &
     * password) sudah selesai. Sesi Google saja belum menyelesaikan
     * pendaftaran tidak dianggap login.
     */
    fun getAppUser(context: Context): AppUser? {
        val prefs = sessionPrefs(context)
        val isLoggedIn = prefs.getBoolean(KEY_LOGGED_IN, false) &&
            prefs.getBoolean(KEY_REGISTRATION_COMPLETE, false)
        if (!isLoggedIn) return null

        val uid = prefs.getString(KEY_UID, null) ?: return null

        return AppUser(
            uid = uid,
            customUserId = prefs.getString(KEY_CUSTOM_USER_ID, "") ?: "",
            customPassword = prefs.getString(KEY_CUSTOM_PASSWORD, "") ?: "",
            displayName = prefs.getString(KEY_NAME, "Operator Toko") ?: "Operator Toko",
            email = prefs.getString(KEY_EMAIL, ""),
            loginMethod = "GOOGLE",
            registrationComplete = true
        )
    }

    // ==== Langkah 1: daftar & verifikasi via akun Google ====

    /**
     * Verifikasi akun Google (wajib, langkah pertama pendaftaran).
     * - Jika akun SUDAH pernah menyelesaikan pendaftaran (dokumen Firestore
     *   `users/{uid}` punya registrationComplete=true, dicek server lalu cache
     *   lokal), sesi langsung dipulihkan -> pengguna otomatis masuk.
     * - Jika BELUM, AppScreen menampilkan dialog untuk membuat ID Login &
     *   Password (langkah 2).
     * Tidak pernah blocking menunggu server — aman saat offline.
     */
    suspend fun registerWithGoogle(context: Context): Result<AppUser> {
        return try {
            ensureFirebaseInitialized(context)
            val serverClientId = resolveGoogleServerClientId(context)
            val googleOption = GetSignInWithGoogleOption.Builder(serverClientId).build()
            val request = GetCredentialRequest.Builder().addCredentialOption(googleOption).build()

            val credentialManager = CredentialManager.create(context)
            val result = credentialManager.getCredential(context, request)
            val credential = result.credential

            if (credential !is CustomCredential ||
                credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                return Result.failure(IllegalStateException("Kredensial Google tidak valid atau pendaftaran dibatalkan."))
            }

            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val firebaseCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
            val authResult = auth.signInWithCredential(firebaseCredential).await()
            val firebaseUser = authResult.user
                ?: return Result.failure(IllegalStateException("Gagal masuk ke Firebase dengan akun Google."))

            val uid = firebaseUser.uid
            val displayName = firebaseUser.displayName ?: "Pemilik Toko"
            val email = firebaseUser.email ?: fallbackEmail(uid)

            // Simpan identitas Google ke prefs sesi (BELUM dianggap login).
            sessionPrefs(context).edit()
                .putBoolean(KEY_LOGGED_IN, false)
                .putBoolean(KEY_REGISTRATION_COMPLETE, false)
                .putString(KEY_UID, uid)
                .putString(KEY_CUSTOM_USER_ID, "")
                .putString(KEY_CUSTOM_PASSWORD, "")
                .putString(KEY_NAME, displayName)
                .putString(KEY_EMAIL, email)
                .apply()

            // Cek status pendaftaran: server dulu (diizinkan rules utk pemilik),
            // lalu cache lokal — tidak blocking saat offline.
            val userDocRef = firestore(context).collection("users").document(uid)
            val snapshot = try {
                userDocRef.get(Source.SERVER).await()
            } catch (_: Exception) {
                try {
                    userDocRef.get(Source.CACHE).await()
                } catch (_: Exception) {
                    null
                }
            }

            if (snapshot?.getBoolean("registrationComplete") == true) {
                // Akun sudah terdaftar -> pulihkan sesi langsung.
                val customUserId = snapshot.getString("customUserId") ?: ""
                val storedHash = snapshot.getString("passwordHash") ?: ""
                val storedName = snapshot.getString("displayName") ?: displayName
                val storedEmail = snapshot.getString("email") ?: email

                saveRegisteredAccount(context, uid, customUserId, storedHash, storedName, storedEmail)

                sessionPrefs(context).edit()
                    .putBoolean(KEY_LOGGED_IN, true)
                    .putBoolean(KEY_REGISTRATION_COMPLETE, true)
                    .putString(KEY_UID, uid)
                    .putString(KEY_CUSTOM_USER_ID, customUserId)
                    .putString(KEY_CUSTOM_PASSWORD, "")
                    .putString(KEY_NAME, storedName)
                    .putString(KEY_EMAIL, storedEmail)
                    .apply()

                Result.success(AppUser(uid, customUserId, "", storedName, storedEmail, "GOOGLE", true))
            } else {
                // Belum terdaftar -> lanjut langkah 2: buat ID & Password.
                Result.success(AppUser(uid, "", "", displayName, email, "GOOGLE", false))
            }
        } catch (e: Exception) {
            Log.e("AuthManager", "Google Sign-In error", e)
            Result.failure(e)
        }
    }

    // ==== Langkah 2: membuat ID Login & Password setelah verifikasi Google ====

    /**
     * Menyimpan ID Login & Password pilihan pengguna:
     * - Firestore `users/{uid}` (passwordHash SHA-256, sesuai rules — tanpa
     *   plaintext). Write bersifat fire-and-forget: Firestore menyimpannya di
     *   cache lokal dan tersinkron otomatis saat online.
     * - Menautkan kredensial email/password ke akun Firebase agar login
     *   ID & password tetap membuka sesi cloud (best effort).
     * - Prefs sesi + penyimpanan akun lokal (hash), lalu langsung login.
     */
    suspend fun completeRegistration(
        context: Context,
        googleUser: AppUser,
        customUserId: String,
        customPassword: String
    ): Result<AppUser> {
        val id = customUserId.trim()
        val pass = customPassword.trim()

        if (id.length < 4) {
            return Result.failure(IllegalArgumentException("ID Login minimal 4 karakter."))
        }
        if (!id.matches(Regex("^[A-Za-z0-9_.-]+$"))) {
            return Result.failure(IllegalArgumentException("ID Login hanya boleh huruf, angka, titik, garis bawah, dan strip."))
        }
        if (pass.length < 6) {
            return Result.failure(IllegalArgumentException("Password minimal 6 karakter."))
        }

        val uid = googleUser.uid
        val displayName = googleUser.displayName
        val email = googleUser.email ?: fallbackEmail(uid)
        val passHash = hashPassword(pass)

        // AKUN BARU = DATA BERSIH: hapus database lokal uid ini (jika pernah
        // ada sisa) sehingga pendaftaran baru selalu mulai dari nol dan tidak
        // mewarisi data akun lain. Database akun lain tidak disentuh.
        try {
            AppDatabase.deleteDatabaseForUser(context, uid)
        } catch (e: Exception) {
            Log.w("AuthManager", "Gagal membersihkan database untuk akun baru: ${e.message}")
        }

        try {
            // Dokumen penuh (bukan merge) agar field lama yang melanggar rules
            // (mis. customPassword plaintext) ikut terganti.
            firestore(context).collection("users").document(uid)
                .set(
                    mapOf(
                        "uid" to uid,
                        "customUserId" to id,
                        "passwordHash" to passHash,
                        "displayName" to displayName,
                        "email" to email,
                        "loginMethod" to "GOOGLE",
                        "registrationComplete" to true,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .addOnSuccessListener { Log.d("AuthManager", "Kredensial akun tersinkron ke Firestore") }
                .addOnFailureListener { Log.e("AuthManager", "Gagal menyimpan kredensial ke Firestore", it) }
        } catch (e: Exception) {
            Log.e("AuthManager", "Gagal menulis ke Firestore", e)
        }

        // Tautkan kredensial email/password ke akun Firebase (best effort,
        // butuh koneksi — kegagalan tidak menghalangi pendaftaran).
        try {
            val fbUser = currentUser
            if (fbUser != null) {
                fbUser.linkWithCredential(EmailAuthProvider.getCredential(email, pass))
                    .addOnSuccessListener { Log.d("AuthManager", "Kredensial email/password berhasil ditautkan") }
                    .addOnFailureListener { Log.w("AuthManager", "Tautan email/password dilewati: ${it.message}") }
            }
        } catch (e: Exception) {
            Log.w("AuthManager", "Link email/password error", e)
        }

        saveRegisteredAccount(context, uid, id, passHash, displayName, email)

        sessionPrefs(context).edit()
            .putBoolean(KEY_LOGGED_IN, true)
            .putBoolean(KEY_REGISTRATION_COMPLETE, true)
            .putString(KEY_UID, uid)
            .putString(KEY_CUSTOM_USER_ID, id)
            .putString(KEY_CUSTOM_PASSWORD, "")
            .putString(KEY_NAME, displayName)
            .putString(KEY_EMAIL, email)
            .apply()

        return Result.success(AppUser(uid, id, "", displayName, email, "GOOGLE", true))
    }

    // ==== Langkah 3: login dengan ID & Password ====

    /**
     * Login memakai ID Login & Password yang dibuat saat pendaftaran.
     * Verifikasi memakai hash di penyimpanan akun lokal (bisa offline).
     * Saat online, login Firebase email/password dicoba agar sinkronisasi
     * cloud aktif (best effort).
     */
    suspend fun loginWithCredentials(
        context: Context,
        customUserId: String,
        password: String
    ): Result<AppUser> {
        val id = customUserId.trim()
        val pass = password.trim()

        if (id.isEmpty() || pass.isEmpty()) {
            return Result.failure(IllegalArgumentException("Mohon isi ID Login dan Password."))
        }

        val account = findRegisteredAccount(context, id)
            ?: return Result.failure(
                IllegalStateException(
                    "User ID \"$id\" belum terdaftar di perangkat ini.\n" +
                        "Silakan daftar terlebih dahulu lewat akun Google (tombol di bawah)."
                )
            )

        if (account.passwordHash.isBlank() || hashPassword(pass) != account.passwordHash) {
            // Hash lokal tidak cocok — mungkin sandi baru saja diubah lewat email
            // reset. Verifikasi ke Firebase Auth (butuh online); jika cocok,
            // perbarui hash lokal agar login offline berikutnya tetap jalan.
            val verifiedByFirebase = try {
                auth.signInWithEmailAndPassword(account.email, pass).await()
                true
            } catch (e: Exception) {
                Log.w("AuthManager", "Verifikasi Firebase email/password gagal: ${e.message}")
                false
            }
            if (!verifiedByFirebase) {
                return Result.failure(IllegalArgumentException("Password salah. Silakan coba lagi."))
            }
            saveRegisteredAccount(context, account.uid, account.customUserId, hashPassword(pass), account.displayName, account.email)
        }

        // Login Firebase email/password (best effort) supaya sinkron cloud aktif.
        try {
            auth.signInWithEmailAndPassword(account.email, pass).await()
        } catch (e: Exception) {
            Log.w("AuthManager", "Login Firebase email/password dilewati: ${e.message}")
        }

        sessionPrefs(context).edit()
            .putBoolean(KEY_LOGGED_IN, true)
            .putBoolean(KEY_REGISTRATION_COMPLETE, true)
            .putString(KEY_UID, account.uid)
            .putString(KEY_CUSTOM_USER_ID, account.customUserId)
            .putString(KEY_CUSTOM_PASSWORD, "")
            .putString(KEY_NAME, account.displayName)
            .putString(KEY_EMAIL, account.email)
            .apply()

        return Result.success(
            AppUser(account.uid, account.customUserId, "", account.displayName, account.email, "GOOGLE", true)
        )
    }

    /** Untuk dialog "Lupa Sandi / User ID": cari akun lokal berdasarkan ID atau email. */
    fun findLocalAccount(context: Context, identifier: String): AppUser? {
        val account = findRegisteredAccount(context, identifier.trim())
            ?: return null
        return AppUser(
            uid = account.uid,
            customUserId = account.customUserId,
            customPassword = "",
            displayName = account.displayName,
            email = account.email,
            loginMethod = "GOOGLE",
            registrationComplete = true
        )
    }

    /**
     * Kirim email reset password (Firebase Auth) ke email Google akun yang
     * ditemukan — dipakai dialog "Lupa Sandi / User ID" di halaman login.
     * Setelah user menyetel sandi baru lewat tautan di email, dia login dengan
     * sandi baru itu; [loginWithCredentials] mengenali sandi baru dari Firebase
     * dan otomatis memperbarui hash lokal.
     */
    suspend fun sendPasswordResetEmail(context: Context, identifier: String): Result<String> {
        val account = findRegisteredAccount(context, identifier)
            ?: return Result.failure(IllegalStateException("Akun tidak ditemukan di perangkat ini."))
        if (account.email.isBlank() || account.email.endsWith("@notaservice.local")) {
            return Result.failure(
                IllegalStateException(
                    "Akun ini tidak punya email Google aktif. Masuk lewat akun Google, " +
                        "lalu gunakan Reset / Ubah Sandi di Pengaturan."
                )
            )
        }
        return try {
            auth.sendPasswordResetEmail(account.email).await()
            Result.success(account.email)
        } catch (e: Exception) {
            Log.e("AuthManager", "Gagal kirim email reset: ${e.message}", e)
            Result.failure(e)
        }
    }

    // ==== Akun lokal ====

    private data class LocalAccount(
        val uid: String,
        val customUserId: String,
        val passwordHash: String,
        val displayName: String,
        val email: String
    )

    private fun saveRegisteredAccount(
        context: Context,
        uid: String,
        customUserId: String,
        passwordHash: String,
        displayName: String,
        email: String
    ) {
        val entry = JSONObject().apply {
            put("customUserId", customUserId)
            put("passwordHash", passwordHash)
            put("displayName", displayName)
            put("email", email)
        }
        accountsStore(context).edit().putString(accountKey(uid), entry.toString()).apply()
    }

    private fun findRegisteredAccount(context: Context, identifier: String): LocalAccount? {
        if (identifier.isBlank()) return null
        val target = identifier.trim()

        for ((key, raw) in accountsStore(context).all) {
            if (!key.startsWith("acct_")) continue
            try {
                val obj = JSONObject(raw.toString())
                val id = obj.optString("customUserId")
                val email = obj.optString("email")
                if (id.equals(target, ignoreCase = true) || email.equals(target, ignoreCase = true)) {
                    return LocalAccount(
                        uid = key.removePrefix("acct_"),
                        customUserId = id,
                        passwordHash = obj.optString("passwordHash"),
                        displayName = obj.optString("displayName").ifBlank { "Operator Toko" },
                        email = email.ifBlank { fallbackEmail(key.removePrefix("acct_")) }
                    )
                }
            } catch (_: Exception) {
                // entri rusak -> lewati
            }
        }
        return null
    }

    // ==== Ubah sandi ====

    /**
     * Reset / ubah sandi dari halaman Pengaturan. Memperbarui hash di
     * penyimpanan akun lokal dan Firestore (sesuai rules), best effort
     * ke Firebase Auth.
     */
    fun updateCustomPassword(context: Context, newPassword: String): Boolean {
        val prefs = sessionPrefs(context)
        val uid = prefs.getString(KEY_UID, null) ?: currentUser?.uid ?: return false
        val trimmed = newPassword.trim()
        if (trimmed.length < 6) return false

        val customUserId = prefs.getString(KEY_CUSTOM_USER_ID, "") ?: ""
        val displayName = prefs.getString(KEY_NAME, "Pemilik Toko") ?: "Pemilik Toko"
        val email = prefs.getString(KEY_EMAIL, "")?.ifBlank { fallbackEmail(uid) } ?: fallbackEmail(uid)
        val newHash = hashPassword(trimmed)

        saveRegisteredAccount(context, uid, customUserId, newHash, displayName, email)

        try {
            firestore(context).collection("users").document(uid)
                .set(
                    mapOf(
                        "uid" to uid,
                        "customUserId" to customUserId,
                        "passwordHash" to newHash,
                        "displayName" to displayName,
                        "email" to email,
                        "loginMethod" to "GOOGLE",
                        "registrationComplete" to true,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .addOnSuccessListener { Log.d("AuthManager", "Sandi baru tersinkron ke Firestore") }
                .addOnFailureListener { Log.e("AuthManager", "Gagal update sandi di Firestore", it) }
        } catch (e: Exception) {
            Log.e("AuthManager", "Gagal menulis sandi ke Firestore", e)
        }

        try {
            currentUser?.updatePassword(trimmed)
                ?.addOnFailureListener { Log.w("AuthManager", "updatePassword auth gagal: ${it.message}") }
        } catch (_: Exception) {
        }

        return true
    }

    // ==== Keluar & hapus akun ====

    fun signOut(
        context: Context,
        scope: CoroutineScope,
        onComplete: () -> Unit
    ) {
        try {
            auth.signOut()
        } catch (e: Exception) {
            Log.e("Auth", "Error Firebase signOut", e)
        }

        sessionPrefs(context).edit().clear().apply()

        scope.launch(Dispatchers.IO) {
            try {
                CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.e("Auth", "Gagal clear credential state", e)
            } finally {
                withContext(Dispatchers.Main) {
                    onComplete()
                }
            }
        }
    }

    /**
     * Menghapus akun dan seluruh data lokal milik akun yang sedang login.
     * Database lokal per-user (lihat AppDatabase) ditutup dan dihapus,
     * kredensial lokal dihapus, session Firebase dibersihkan, dan
     * (best-effort) akun Firebase dihapus.
     */
    fun deleteAccountAndResetLocalData(
        context: Context,
        scope: CoroutineScope,
        onComplete: () -> Unit
    ) {
        val uid = sessionPrefs(context).getString(KEY_UID, null) ?: currentUser?.uid

        scope.launch(Dispatchers.IO) {
            try {
                if (uid != null) {
                    AppDatabase.deleteDatabaseForUser(context, uid)
                } else {
                    Log.w("Auth", "Hapus akun: uid tidak diketahui, database lokal dibiarkan")
                }
            } catch (e: Exception) {
                Log.e("Auth", "Gagal menghapus database lokal", e)
            }

            if (uid != null) {
                try {
                    accountsStore(context).edit().remove(accountKey(uid)).apply()
                } catch (e: Exception) {
                    Log.e("Auth", "Gagal menghapus kredensial lokal", e)
                }
            }

            try {
                currentUser?.delete()?.await()
            } catch (e: Exception) {
                Log.e("Auth", "Gagal menghapus akun Firebase", e)
            }

            try {
                auth.signOut()
            } catch (_: Exception) {
            }

            sessionPrefs(context).edit().clear().apply()

            try {
                CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.e("Auth", "Gagal clear credential state", e)
            } finally {
                withContext(Dispatchers.Main) {
                    onComplete()
                }
            }
        }
    }
}
