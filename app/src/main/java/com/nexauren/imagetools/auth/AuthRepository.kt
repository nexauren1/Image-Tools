package com.nexauren.imagetools.auth

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

class AuthRepository(private val context: Context) {
    private val auth = FirebaseAuth.getInstance()
    private val credentialManager = CredentialManager.create(context)

    var currentUser by mutableStateOf(auth.currentUser)
        private set

    init {
        auth.addAuthStateListener { firebaseAuth ->
            currentUser = firebaseAuth.currentUser
        }
    }

    suspend fun signInEmail(email: String, password: String): Result<Unit> = runCatching {
        auth.signInWithEmailAndPassword(email.trim(), password).await()
    }

    suspend fun registerEmail(email: String, password: String): Result<Unit> = runCatching {
        val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
        result.user?.sendEmailVerification()?.await()
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> = runCatching {
        auth.sendPasswordResetEmail(email.trim()).await()
    }

    suspend fun sendEmailVerification(): Result<Unit> = runCatching {
        auth.currentUser?.sendEmailVerification()?.await()
            ?: error("No signed-in user.")
    }

    suspend fun reloadCurrentUser(): Result<Unit> = runCatching {
        auth.currentUser?.reload()?.await()
        currentUser = auth.currentUser
    }

    suspend fun updateDisplayName(name: String): Result<Unit> = runCatching {
        val user = auth.currentUser ?: error("No signed-in user.")
        val updates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
            .setDisplayName(name.trim().ifBlank { null })
            .build()
        user.updateProfile(updates).await()
        currentUser = auth.currentUser
    }

    suspend fun signInGoogle(serverClientId: String): Result<Unit> = runCatching {
        require(serverClientId.isNotBlank()) { "Google sign-in needs GOOGLE_WEB_CLIENT_ID." }
        val option = GetGoogleIdOption.Builder()
            .setServerClientId(serverClientId)
            .setFilterByAuthorizedAccounts(false)
            .build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        val result = credentialManager.getCredential(context, request)
        val credential = result.credential
        require(credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            "Google credential was not returned."
        }
        val google = GoogleIdTokenCredential.createFrom(credential.data)
        auth.signInWithCredential(GoogleAuthProvider.getCredential(google.idToken, null)).await()
    }

    fun signOut() = auth.signOut()

    suspend fun idToken(forceRefresh: Boolean = false): String? = runCatching {
        auth.currentUser?.getIdToken(forceRefresh)?.await()?.token
    }.getOrNull()
}