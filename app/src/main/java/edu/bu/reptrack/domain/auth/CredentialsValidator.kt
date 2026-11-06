package edu.bu.reptrack.domain.auth

/**
 * Quick checks before we call Firebase, so an obvious typo does not need a network round trip.
 * Pure Kotlin so it is easy to unit test. Firebase still has the final say.
 */
object CredentialsValidator {
    const val MIN_PASSWORD_LENGTH = 6

    /** An "@" with something before it and something after it. */
    fun isEmailValid(email: String): Boolean {
        val trimmed = email.trim()
        val at = trimmed.indexOf('@')
        return at > 0 && at < trimmed.length - 1
    }

    /** Passwords are not trimmed: spaces can be part of one. */
    fun isPasswordValid(password: String): Boolean = password.length >= MIN_PASSWORD_LENGTH
}
