package edu.bu.reptrack.domain.auth

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CredentialsValidatorTest {

    @Test
    fun email_needsAnAtSignWithTextOnBothSides() {
        assertTrue(CredentialsValidator.isEmailValid("runner@bu.edu"))
        assertFalse(CredentialsValidator.isEmailValid("runner"))
        assertFalse(CredentialsValidator.isEmailValid("@bu.edu"))
        assertFalse(CredentialsValidator.isEmailValid("runner@"))
        assertFalse(CredentialsValidator.isEmailValid(""))
    }

    @Test
    fun email_ignoresSpacesAtTheEnds() {
        assertTrue(CredentialsValidator.isEmailValid("  runner@bu.edu  "))
        assertFalse(CredentialsValidator.isEmailValid("   "))
    }

    @Test
    fun password_needsAtLeastSixCharacters() {
        assertTrue(CredentialsValidator.isPasswordValid("123456"))
        assertFalse(CredentialsValidator.isPasswordValid("12345"))
        assertFalse(CredentialsValidator.isPasswordValid(""))
    }

    @Test
    fun password_isNotTrimmed() {
        assertTrue(CredentialsValidator.isPasswordValid("ab cd "))
    }
}
