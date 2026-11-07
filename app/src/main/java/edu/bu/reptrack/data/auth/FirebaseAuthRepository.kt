package edu.bu.reptrack.data.auth

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.tasks.await

/** [AuthRepository] backed by Firebase Authentication (email and password). */
class FirebaseAuthRepository(
    private val auth: FirebaseAuth,
) : AuthRepository {

    // Firebase calls the listener right away with the current user, and again on every
    // sign-in or sign-out. The listener is removed when nobody collects the flow any more.
    override val currentUser: Flow<AuthUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser?.toAuthUser())
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }.distinctUntilChanged()

    override suspend fun signIn(email: String, password: String): AuthResult =
        attempt { auth.signInWithEmailAndPassword(email, password).await() }

    override suspend fun createAccount(email: String, password: String): AuthResult =
        attempt { auth.createUserWithEmailAndPassword(email, password).await() }

    override suspend fun signOut() {
        auth.signOut()
    }

    /** Runs a Firebase call and turns any failure into an [AuthResult]. */
    private suspend fun attempt(call: suspend () -> Unit): AuthResult =
        try {
            call()
            AuthResult.Success
        } catch (cancelled: CancellationException) {
            throw cancelled // never swallow cancellation
        } catch (error: Exception) {
            AuthResult.Failure(error.toAuthError())
        }
}

private fun FirebaseUser.toAuthUser() = AuthUser(uid = uid, email = email)

/**
 * Maps a Firebase exception to our own error. The order matters: the weak password exception
 * is a kind of invalid credentials exception, so it has to be checked first.
 *
 * New Firebase projects do not say whether the email or the password was wrong, so both
 * map to [AuthError.WRONG_EMAIL_OR_PASSWORD]. A badly formatted email has its own error code.
 */
internal fun Exception.toAuthError(): AuthError = when (this) {
    is FirebaseAuthWeakPasswordException -> AuthError.WEAK_PASSWORD
    is FirebaseAuthInvalidCredentialsException ->
        if (errorCode == "ERROR_INVALID_EMAIL") AuthError.INVALID_EMAIL else AuthError.WRONG_EMAIL_OR_PASSWORD
    is FirebaseAuthInvalidUserException -> AuthError.WRONG_EMAIL_OR_PASSWORD
    is FirebaseAuthUserCollisionException -> AuthError.EMAIL_IN_USE
    is FirebaseNetworkException -> AuthError.NETWORK
    else -> AuthError.UNKNOWN
}
