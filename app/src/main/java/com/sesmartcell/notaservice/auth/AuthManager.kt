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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import com.sesmartcell.notaservice.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

data class AppUser(
    val uid: String,
    val customUserId: String,
    val customPassword: String,
    val displayName: String,
    val email: String?,
    val loginMethod: String
)

/** Hasil verifikasi Google: user sudah terdaftar atau masih harus membuat ID & password. */
data class GoogleAuthResult(
    val user: AppUser,
    val alreadyRegistered: Boolean
)

object AuthManager {

    const val PREFS_NAME = "se_smart_cell_auth_prefs"
    const val KEY_UID = "key_user_uid"
    private const val KEY_LOGGED_IN = "key_is_logged_in"
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

    /**
     * User dianggap masuk HANYA jika pendaftaran (pembuatan ID & password) sudah
     * selesai di perangkat ini. Sesi Firebase saja tidak cukup — mencegah
     * navigasi ke aplikasi sebelum langkah pendaftaran selesai.
     */
    fun getAppUser(context: Context): AppUser? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isLoggedIn = prefs.getBoolean(KEY_LOGGED_IN, false)

        if (!isLoggedIn) return null

        val fbUser = currentUser
        val uid = prefs.getString(KEY_UID, null) ?: fbUser?.uid ?: return null
        val name = prefs.getString(KEY_NAME, null) ?: fbUser?.displayName ?: "Operator Toko"
        val email = prefs.getString(KEY_EMAIL, null) ?: fbUser?.email ?: ""
        val customUserId = prefs.getString(KEY_CUSTOM_USER_ID, null) ?: return null
        val customPassword = prefs.getString(KEY_CUSTOM_PASSWORD, null) ?: return null

        return AppUser(
            uid = uid,
            customUserId = customUserId,
            customPassword = customPassword,
            displayName = name,
            email = email,
            loginMethod = "GOOGLE"
        )
    }

    private fun generateUserId(uid: String): String {
        val hash = kotlin.math.abs(uid.hashCode()) % 900000 + 100000
        return "USER_$hash"
    }

    private fun generatePassword(): String {
        val passNum = (100000..999999).random()
        return "PASS_$passNum"
    }

    private fun prefsWrite(context: Context, user: AppUser, loggedIn: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_LOGGED_IN, loggedIn)
            .putString(KEY_UID, user.uid)
            .putString(KEY_CUSTOM_USER_ID, user.customUserId)
            .putString(KEY_CUSTOM_PASSWORD, user.customPassword)
            .putString(KEY_NAME, user.displayName)
            .putString(KEY_EMAIL, user.email ?: "")
            .apply()
    }

    /**
     * Langkah 1 alur baru: daftar/verifikasi via akun Google.
     * TIDAK menandai user sebagai masuk — pemanggil harus melanjutkan ke
     * pembuatan ID & password (user baru) atau markLoggedIn (sudah terdaftar).
     */
    suspend fun signInWithGoogle(context: Context): Result<GoogleAuthResult> {
        return try {
            ensureFirebaseInitialized(context)
            val serverClientId = resolveGoogleServerClientId(context)
            val googleOption = GetSignInWithGoogleOption.Builder(serverClientId).build()
            val request = GetCredentialRequest.Builder().addCredentialOption(googleOption).build()

            val credentialManager = CredentialManager.create(context)
            val result = credentialManager.getCredential(context, request)
            val credential = result.credential

            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val googleToken = googleIdTokenCredential.idToken
                val firebaseCredential = GoogleAuthProvider.getCredential(googleToken, null)

                val authResult = auth.signInWithCredential(firebaseCredential).await()
                val firebaseUser = authResult.user ?: throw IllegalStateException("Gagal login Firebase")

                val appUser = upsertUserProfile(
                    uid = firebaseUser.uid,
                    name = firebaseUser.displayName ?: "Pemilik Toko",
                    email = firebaseUser.email
                )

                prefsWrite(context, appUser, loggedIn = false)

                val registered = appUser.customUserId.isNotBlank() && appUser.customPassword.isNotBlank()
                Result.success(GoogleAuthResult(user = appUser, alreadyRegistered = registered))
            } else {
                Result.failure(IllegalStateException("Kredensial Google tidak valid atau login dibatalkan."))
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("AuthManager", "Google Sign-In error", e)
            Result.failure(e)
        }
    }

    private suspend fun upsertUserProfile(uid: String, name: String, email: String?): AppUser {
        val db = Firebase.firestore
        val userRef = db.collection("users").document(uid)
        val snapshot = userRef.get().await()

        val customUserId = snapshot.getString("customUserId") ?: generateUserId(uid)
        val customPassword = snapshot.getString("customPassword") ?: generatePassword()

        val userData = mapOf(
            "uid" to uid,
            "customUserId" to customUserId,
            "customPassword" to customPassword,
            "displayName" to name,
            "email" to (email ?: ""),
            "loginMethod" to "GOOGLE",
            "updatedAt" to Timestamp.now(),
            "createdAt" to (snapshot.getTimestamp("createdAt") ?: Timestamp.now())
        )

        userRef.set(userData, SetOptions.merge()).await()

        return AppUser(
            uid = uid,
            customUserId = customUserId,
            customPassword = customPassword,
            displayName = name,
            email = email,
            loginMethod = "GOOGLE"
        )
    }

    /**
     * Langkah 2 alur baru: setelah verifikasi Google, user WAJIB membuat
     * ID login & password unik. Menyimpan ke Firestore (users/{uid}) dan
     * menandai user sebagai terdaftar & masuk di perangkat ini.
     */
    suspend fun completeRegistration(
        context: Context,
        uid: String,
        customUserId: String,
        customPassword: String,
        displayName: String,
        email: String?
    ): Result<AppUser> {
        val id = customUserId.trim()
        val pass = customPassword.trim()

        if (id.length < 3 || id.length > 100 || !id.matches(Regex("^[A-Za-z0-9._-]+$"))) {
            return Result.failure(IllegalStateException("ID Login harus 3-100 karakter (huruf, angka, titik, garis bawah, tanda hubung)."))
        }
        if (pass.length < 6 || pass.length > 200) {
            return Result.failure(IllegalStateException("Password harus 6-200 karakter."))
        }

        return try {
            ensureFirebaseInitialized(context)
            val db = Firebase.firestore

            // Cek keunikan ID antar akun (best-effort: jika rules menolak query
            // koleksi, lanjutkan dengan validasi lokal saja).
            try {
                val query = db.collection("users")
                    .whereEqualTo("customUserId", id)
                    .get()
                    .await()
                val taken = query.documents.any { it.id != uid }
                if (taken) {
                    return Result.failure(IllegalStateException("ID Login \"$id\" sudah digunakan akun lain. Pilih ID lain."))
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("AuthManager", "Cek keunikan ID dilewati: ${e.message}")
            }

            val user = AppUser(
                uid = uid,
                customUserId = id,
                customPassword = pass,
                displayName = displayName,
                email = email,
                loginMethod = "GOOGLE"
            )

            val userData = mapOf(
                "uid" to uid,
                "customUserId" to id,
                "customPassword" to pass,
                "displayName" to displayName,
                "email" to (email ?: ""),
                "loginMethod" to "GOOGLE",
                "registeredAt" to Timestamp.now(),
                "updatedAt" to Timestamp.now()
            )
            db.collection("users").document(uid).set(userData, SetOptions.merge()).await()

            prefsWrite(context, user, loggedIn = true)
            Result.success(user)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("AuthManager", "Registrasi gagal", e)
            Result.failure(e)
        }
    }

    /** Masuk kembali dengan ID & password yang dibuat saat mendaftar (terverifikasi lokal). */
    fun loginWithCredentials(context: Context, customUserId: String, password: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedId = prefs.getString(KEY_CUSTOM_USER_ID, null) ?: return false
        val savedPass = prefs.getString(KEY_CUSTOM_PASSWORD, null) ?: return false
        val uid = prefs.getString(KEY_UID, null) ?: return false

        if (customUserId.trim() != savedId || password != savedPass) return false

        prefs.edit().putBoolean(KEY_LOGGED_IN, true).apply()
        Log.d("AuthManager", "Login via ID & password untuk uid=$uid")
        return true
    }

    /** User Google yang sudah terdaftar (dokumen Firestore lengkap) langsung dianggap masuk. */
    fun markLoggedIn(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_LOGGED_IN, true).apply()
    }

    fun updateCustomPassword(context: Context, newPassword: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val uid = prefs.getString(KEY_UID, currentUser?.uid) ?: return false
        val trimmed = newPassword.trim()
        if (trimmed.length < 6) return false

        prefs.edit().putString(KEY_CUSTOM_PASSWORD, trimmed).apply()

        try {
            val db = Firebase.firestore
            db.collection("users").document(uid).update("customPassword", trimmed)
        } catch (e: Exception) {
            Log.e("AuthManager", "Failed updating password in Firestore", e)
            return false
        }
        return true
    }

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

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()

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
}
