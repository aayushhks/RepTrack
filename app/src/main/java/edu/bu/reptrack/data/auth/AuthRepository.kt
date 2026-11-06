package edu.bu.reptrack.data.auth

import kotlinx.coroutines.flow.Flow

/** The signed-in user. Only what the app needs: an id and an email. */
data class AuthUser(val uid: String, val email: String?)

/** Why sign-in or account creation failed. Screens turn these into messages. */
enum class AuthError { INVALID_EMAIL, WRONG_EMAIL_OR_PASSWORD, EMAIL_IN_USE, WEAK_PASSWORD, NETWORK, UNKNOWN }

sealed interface AuthResult {
    data object Success : AuthResult
    data class Failure(val error: AuthError) : AuthResult
}

/** Accounts. Screens talk to this, not to Firebase directly. */
interface AuthRepository {
    /** The signed-in user, or null when signed out. Updates live. */
    val currentUser: Flow<AuthUser?>

    suspend fun signIn(email: String, password: String): AuthResult

    suspend fun createAccount(email: String, password: String): AuthResult

    suspend fun signOut()
}
