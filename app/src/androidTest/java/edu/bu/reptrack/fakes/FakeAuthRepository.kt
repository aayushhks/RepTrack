package edu.bu.reptrack.fakes

import edu.bu.reptrack.data.auth.AuthRepository
import edu.bu.reptrack.data.auth.AuthResult
import edu.bu.reptrack.data.auth.AuthUser
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * In-memory [AuthRepository] for tests, so nothing ever talks to real Firebase.
 * Set [nextResult] to choose what the next sign-in or account creation returns.
 * The calls are recorded so tests can check what was (or was not) sent.
 */
class FakeAuthRepository(initialUser: AuthUser? = null) : AuthRepository {
    private val userFlow = MutableStateFlow(initialUser)

    @Volatile
    var nextResult: AuthResult = AuthResult.Success

    val signInCalls = CopyOnWriteArrayList<Pair<String, String>>()
    val createAccountCalls = CopyOnWriteArrayList<Pair<String, String>>()

    override val currentUser: Flow<AuthUser?> = userFlow

    override suspend fun signIn(email: String, password: String): AuthResult {
        signInCalls += email to password
        return finish(email)
    }

    override suspend fun createAccount(email: String, password: String): AuthResult {
        createAccountCalls += email to password
        return finish(email)
    }

    override suspend fun signOut() {
        userFlow.value = null
    }

    /** A successful call signs the fake user in, like the real thing. */
    private fun finish(email: String): AuthResult {
        val result = nextResult
        if (result is AuthResult.Success) userFlow.value = AuthUser(uid = "fake-uid", email = email)
        return result
    }
}
