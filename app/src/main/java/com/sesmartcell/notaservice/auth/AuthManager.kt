package com.sesmartcell.notaservice.auth

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
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

    private const val PREFS_NAME = "se_smart_cell_auth_prefs"
    private const val KEY_LOGGED_IN = "key_is_logged_in"
    private const val KEY_UID = "key_user_uid"
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
        val hash = Math.abs(uid.hashCode()) % 900000 + 100000
        return "USER_$hash"
    }

    private fun generatePassword(): String {
        val passNum = (100000..999999).random()
        return "PASS_$passNum"
    }

    suspend fun signInWithGoogle(context: Context): Result<AppUser> {
        return try {
            val serverClientId = context.getString(com.sesmartcell.notaservice.R.string.default_web_client_id)
            val googleOption = GetSignInWithGoogleOption.Builder(serverClientId)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleOption)
                .build()

            val credentialManager = CredentialManager.create(context)
            val result = credentialManager.getCredential(context, request)
            val credential = result.credential

            if (credential is androidx.credentials.CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val googleToken = googleIdTokenCredential.idToken
                val firebaseCredential = GoogleAuthProvider.getCredential(googleToken, null)

                val authResult = auth.signInWithCredential(firebaseCredential).await()
                val firebaseUser = authResult.user ?: throw IllegalStateException("Gagal login Firebase")

                val uid = firebaseUser.uid
                val name = firebaseUser.displayName ?: "Pemilik Toko"
                val email = firebaseUser.email

                val db = Firebase.firestore
                val userRef = db.collection("users").document(uid)
                val snapshot = userRef.get().await()

                val customUserId: String
                val customPassword: String

                if (snapshot.exists()) {
                    customUserId = snapshot.getString("customUserId") ?: generateUserId(uid)
                    customPassword = snapshot.getString("customPassword") ?: generatePassword()
                } else {
                    customUserId = generateUserId(uid)
                    customPassword = generatePassword()
                    val userData = mapOf(
                        "uid" to uid,
                        "customUserId" to customUserId,
                        "customPassword" to customPassword,
                        "displayName" to name,
                        "email" to (email ?: ""),
                        "createdAt" to com.google.firebase.Timestamp.now()
                    )
                    userRef.set(userData).await()
                }

                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit()
                    .putBoolean(KEY_LOGGED_IN, true)
                    .putString(KEY_UID, uid)
                    .putString(KEY_CUSTOM_USER_ID, customUserId)
                    .putString(KEY_CUSTOM_PASSWORD, customPassword)
                    .putString(KEY_NAME, name)
                    .putString(KEY_EMAIL, email ?: "")
                    .apply()

                Result.success(
                    AppUser(
                        uid = uid,
                        customUserId = customUserId,
                        customPassword = customPassword,
                        displayName = name,
                        email = email,
                        loginMethod = "GOOGLE"
                    )
                )
            } else {
                Result.failure(IllegalStateException("Kredensial Google tidak valid"))
            }
        } catch (e: Exception) {
            Log.e("AuthManager", "Google Sign-In error", e)
            Result.failure(e)
        }
    }

    fun updateCustomPassword(context: Context, newPassword: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val uid = prefs.getString(KEY_UID, currentUser?.uid) ?: return false
        prefs.edit().putString(KEY_CUSTOM_PASSWORD, newPassword).apply()

        try {
            val db = Firebase.firestore
            db.collection("users").document(uid).update("customPassword", newPassword)
        } catch (e: Exception) {
            Log.e("AuthManager", "Failed updating password in Firestore", e)
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

        val credentialManager = CredentialManager.create(context)
        scope.launch(Dispatchers.IO) {
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
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
