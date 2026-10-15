package com.example.arkin.data.firebase

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.concurrent.CancellationException

object FirebaseAuthHelper {

    val auth: FirebaseAuth
        get() = FirebaseAuth.getInstance()

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    val currentUserId: String?
        get() = auth.currentUser?.uid

    val isUserLoggedIn: Boolean
        get() = auth.currentUser != null

    /**
     * Flow that emits whenever the Firebase auth state changes.
     */
    val authStateFlow: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser)
        }
        auth.addAuthStateListener(listener)
        awaitClose {
            auth.removeAuthStateListener(listener)
        }
    }

    /**
     * Register a new student account using Email and Password.
     */
    suspend fun signUpWithEmail(
        email: String,
        password: String,
        displayName: String
    ): Result<FirebaseUser> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
            val user = result.user ?: throw IllegalStateException("Firebase user was null after sign up")

            // Update user profile with display name
            if (displayName.isNotBlank()) {
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(displayName.trim())
                    .build()
                user.updateProfile(profileUpdates).await()
            }

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(mapAuthException(e))
        }
    }

    /**
     * Sign in existing student account using Email and Password.
     */
    suspend fun signInWithEmail(
        email: String,
        password: String
    ): Result<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email.trim(), password).await()
            val user = result.user ?: throw IllegalStateException("Firebase user was null after sign in")
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(mapAuthException(e))
        }
    }

    /**
     * Send password reset email.
     */
    suspend fun sendPasswordReset(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email.trim()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(mapAuthException(e))
        }
    }

    private fun Context.findActivity(): Activity? {
        var ctx = this
        while (ctx is ContextWrapper) {
            if (ctx is Activity) return ctx
            ctx = ctx.baseContext
        }
        return null
    }

    /**
     * Sign in with Google using Android Credential Manager and Firebase Auth.
     */
    suspend fun signInWithGoogle(
        context: Context,
        webClientIdOverride: String? = null
    ): Result<FirebaseUser> {
        val clientId = when {
            !webClientIdOverride.isNullOrBlank() -> webClientIdOverride.trim()
            else -> try {
                context.getString(R.string.default_web_client_id).trim()
            } catch (e: Exception) {
                ""
            }
        }

        if (clientId.isBlank() || clientId.contains("placeholder")) {
            return Result.failure(
                IllegalStateException(
                    "Google Sign-In requires an OAuth 2.0 Web Client ID from Firebase Console. " +
                            "Please configure 'default_web_client_id' in strings.xml or .env file."
                )
            )
        }

        return try {
            val targetContext = context.findActivity() ?: context
            val credentialManager = CredentialManager.create(targetContext)

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(clientId)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val response = credentialManager.getCredential(
                context = targetContext,
                request = request
            )

            val credential = response.credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken
                val authCredential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = auth.signInWithCredential(authCredential).await()
                val user = authResult.user ?: throw IllegalStateException("Firebase user was null after Google sign in")
                Result.success(user)
            } else {
                Result.failure(IllegalStateException("Unexpected credential type received: ${credential.type}"))
            }
        } catch (e: GetCredentialCancellationException) {
            // User intentionally dismissed Google Sign-In prompt
            Result.failure(CancellationException("Google Sign-In was cancelled."))
        } catch (e: NoCredentialException) {
            Result.failure(IllegalStateException("No Google account found on device. Please add a Google account in Android Settings."))
        } catch (e: GetCredentialException) {
            Result.failure(
                IllegalStateException(
                    "Google Sign-In failed: ${e.message}. " +
                            "Make sure Google Sign-In is enabled in Firebase Console (arkin-64543) and the SHA-1 fingerprint matches."
                )
            )
        } catch (e: Exception) {
            Result.failure(mapAuthException(e))
        }
    }

    /**
     * Sign out current user from Firebase.
     */
    fun signOut() {
        auth.signOut()
    }

    private fun mapAuthException(e: Exception): Exception {
        val msg = e.message ?: ""
        return when {
            msg.contains("The email address is already in use", ignoreCase = true) ->
                Exception("An account already exists with this email address. Please log in instead.")
            msg.contains("There is no user record", ignoreCase = true) ||
            msg.contains("INVALID_LOGIN_CREDENTIALS", ignoreCase = true) ||
            msg.contains("wrong password", ignoreCase = true) ->
                Exception("Invalid email or password. Please verify your credentials.")
            msg.contains("The email address is badly formatted", ignoreCase = true) ->
                Exception("The email address is invalid. Please check your spelling.")
            msg.contains("Password should be at least", ignoreCase = true) ->
                Exception("Password is too weak. Please use at least 6 characters.")
            msg.contains("network error", ignoreCase = true) ->
                Exception("Network error. Please check your internet connection.")
            else -> e
        }
    }
}
