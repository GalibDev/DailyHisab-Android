package com.dailyhisab.android.feature.auth

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AuthUser(
    val uid: String,
    val displayName: String,
    val email: String,
    val hasPassword: Boolean,
    val isGoogleUser: Boolean,
)

data class AuthUiState(
    val user: AuthUser? = null,
    val busy: Boolean = false,
    val error: String? = null,
    val message: String? = null,
)

class AuthViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val mutableState = MutableStateFlow(AuthUiState(user = auth.currentUser?.toAuthUser()))
    val state: StateFlow<AuthUiState> = mutableState.asStateFlow()
    private val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        mutableState.value = mutableState.value.copy(user = firebaseAuth.currentUser?.toAuthUser(), busy = false)
    }

    init { auth.addAuthStateListener(listener) }

    fun register(name: String, email: String, password: String) = runAuthTask {
        auth.createUserWithEmailAndPassword(email.trim(), password).addOnCompleteListener { task ->
            if (!task.isSuccessful) finishError(task.exception)
            else {
                val request = UserProfileChangeRequest.Builder().setDisplayName(name.trim()).build()
                task.result.user?.updateProfile(request)?.addOnCompleteListener { update ->
                    if (update.isSuccessful) {
                        mutableState.value = AuthUiState(task.result.user?.toAuthUser(), message = "Account created")
                    } else finishError(update.exception)
                }
            }
        }
    }

    fun signIn(email: String, password: String) = runAuthTask {
        auth.signInWithEmailAndPassword(email.trim(), password).addOnCompleteListener { task ->
            if (task.isSuccessful) mutableState.value = AuthUiState(task.result.user?.toAuthUser(), message = "Signed in")
            else finishError(task.exception)
        }
    }

    fun signInWithGoogle(idToken: String) = runAuthTask {
        auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).addOnCompleteListener { task ->
            if (task.isSuccessful) mutableState.value = AuthUiState(task.result.user?.toAuthUser(), message = "Signed in with Google")
            else finishError(task.exception)
        }
    }

    fun sendPasswordReset(email: String) = runAuthTask {
        if (email.isBlank()) return@runAuthTask finishError(IllegalArgumentException("Enter your email first"))
        auth.sendPasswordResetEmail(email.trim()).addOnCompleteListener { task ->
            if (task.isSuccessful) mutableState.value = mutableState.value.copy(busy = false, message = "Password reset email sent", error = null)
            else finishError(task.exception)
        }
    }

    fun changePassword(password: String) = runAuthTask {
        if (password.length < 6) return@runAuthTask finishError(IllegalArgumentException("Password must be at least 6 characters"))
        auth.currentUser?.updatePassword(password)?.addOnCompleteListener { task ->
            if (task.isSuccessful) mutableState.value = mutableState.value.copy(busy = false, message = "Password changed", error = null)
            else finishError(task.exception)
        } ?: finishError(IllegalStateException("Sign in first"))
    }

    fun createPassword(password: String) = runAuthTask {
        val user = auth.currentUser ?: return@runAuthTask finishError(IllegalStateException("Sign in first"))
        val email = user.email ?: return@runAuthTask finishError(IllegalStateException("This account has no email"))
        if (password.length < 6) return@runAuthTask finishError(IllegalArgumentException("Password must be at least 6 characters"))
        user.linkWithCredential(EmailAuthProvider.getCredential(email, password)).addOnCompleteListener { task ->
            if (task.isSuccessful) mutableState.value = AuthUiState(task.result.user?.toAuthUser(), message = "Password created")
            else finishError(task.exception)
        }
    }

    fun signOut() {
        auth.signOut()
        mutableState.value = AuthUiState(message = "Signed out")
    }

    fun reportError(error: Throwable) = finishError(error)
    fun clearNotice() { mutableState.value = mutableState.value.copy(error = null, message = null) }

    private fun runAuthTask(block: () -> Unit) {
        mutableState.value = mutableState.value.copy(busy = true, error = null, message = null)
        block()
    }

    private fun finishError(error: Throwable?) {
        mutableState.value = mutableState.value.copy(busy = false, error = error?.localizedMessage ?: "Authentication failed", message = null)
    }

    override fun onCleared() {
        auth.removeAuthStateListener(listener)
        super.onCleared()
    }
}

private fun FirebaseUser.toAuthUser() = AuthUser(
    uid = uid,
    displayName = displayName.orEmpty(),
    email = email.orEmpty(),
    hasPassword = providerData.any { it.providerId == EmailAuthProvider.PROVIDER_ID },
    isGoogleUser = providerData.any { it.providerId == GoogleAuthProvider.PROVIDER_ID },
)
