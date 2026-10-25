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
import com.sesmartcell.notaservice.data.database.AppDatabase
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

object AuthManager {

    internal const val PREFS_NAME = "se_smart_cell_auth_prefs"
    private const val KEY_LOGGED_IN = "key_is_logged_in"
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

    fun getAppUser(context: Context): AppUser? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isLoggedIn = prefs.getBoolean(KEY_LOGGED_IN, false) || currentUser != null

        if (isLoggedIn) {
            val fbUser = currentUser
            val uid = fbUser?.uid ?: prefs.getString(KEY_UID, "user_guest") ?: "user_guest"
            val name = fbUser?.displayName ?: prefs.getString(KEY_NAME, "Operator Toko") ?: "Operator Toko"
            val email = fbUser?.email ?: prefs.getString(KEY_EMAIL, "")
            val customUserId = prefs.getString(KEY_CUSTOM_USER_ID, generateUserId(uid)) ?: generateUserId(uid)
            val customPassword = prefs.getString(KEY_CUSTOM_PASSWORD, generatePassword()) ?: generatePassword()

            return AppUser(
                uid = uid,
                customUserId = customUserId,
                customPassword = customPassword,
                displayName = name,
                email = email,
                loginMethod = "GOOGLE"
            )
        }

        return null
    }

    private fun generateUserId(uid: String): String {
        val hash = kotlin.math.abs(uid.hashCode()) % 900000 + 100000
        return "USER_$hash"
    }

    private fun generatePassword(): String {
        val passNum = (100000..999999).random()
        return "PASS_$passNum"
    }

    private suspend fun upsertUserProfile(uid: String, name: String, email: String?): AppUser {
        val db = Firebase.firestore
        val userRef = db.collection("users").document(uid)
        val snapshot = userRef.get().await()

        val customUserId = if (snapshot.exists()) {
            snapshot.getString("customUserId") ?: generateUserId(uid)
        } else {
            generateUserId(uid)
        }

        val customPassword = if (snapshot.exists()) {
            snapshot.getString("customPassword") ?: generatePassword()
        } else {
            generatePassword()
        }

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

    suspend fun signInWithGoogle(context: Context): Result<AppUser> {
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

                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit()
                    .putBoolean(KEY_LOGGED_IN, true)
                    .putString(KEY_UID, firebaseUser.uid)
                    .putString(KEY_CUSTOM_USER_ID, appUser.customUserId)
                    .putString(KEY_CUSTOM_PASSWORD, appUser.customPassword)
                    .putString(KEY_NAME, appUser.displayName)
                    .putString(KEY_EMAIL, appUser.email ?: "")
                    .apply()

                Result.success(appUser)
            } else {
                Result.failure(IllegalStateException("Kredensial Google tidak valid atau login dibatalkan."))
            }
        } catch (e: Exception) {
            Log.e("AuthManager", "Google Sign-In error", e)
            Result.failure(e)
        }
    }

    fun updateCustomPassword(context: Context, newPassword: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val uid = prefs.getString(KEY_UID, currentUser?.uid) ?: return false
        val trimmed = newPassword.trim()
        if (trimmed.length < 6) return false

        prefs.edit().putString(KEY_CUSTOM_PASSWORD, trimmed).apply()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = Firebase.firestore
                db.collection("users").document(uid).update("customPassword", trimmed).await()
            } catch (e: Exception) {
                Log.e("AuthManager", "Failed updating password in Firestore", e)
            }
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

    /**
     * Menghapus akun dan seluruh data lokal milik akun yang sedang login.
     * Database lokal per-user (lihat AppDatabase) ditutup dan dihapus,
     * session Firebase dibersihkan, dan (best-effort) akun Firebase dihapus.
     */
    fun deleteAccountAndResetLocalData(
        context: Context,
        scope: CoroutineScope,
        onComplete: () -> Unit
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val uid = prefs.getString(KEY_UID, null) ?: currentUser?.uid

        scope.launch(Dispatchers.IO) {
            try {
                AppDatabase.deleteDatabaseForUser(context, uid)
            } catch (e: Exception) {
                Log.e("Auth", "Gagal menghapus database lokal", e)
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

            prefs.edit().clear().apply()

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

