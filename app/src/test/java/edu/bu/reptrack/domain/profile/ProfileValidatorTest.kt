package edu.bu.reptrack.domain.profile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProfileValidatorTest {

    private val height = ProfileValidator.HEIGHT_CM_RANGE

    @Test
    fun emptyText_isEmptyError() {
        assertEquals(FieldError.EMPTY, ProfileValidator.check("", height))
        assertEquals(FieldError.EMPTY, ProfileValidator.check("   ", height))
    }

    @Test
    fun text_thatIsNotANumber_isNotANumberError() {
        assertEquals(FieldError.NOT_A_NUMBER, ProfileValidator.check("abc", height))
        assertEquals(FieldError.NOT_A_NUMBER, ProfileValidator.check(".", height))
    }

    @Test
    fun rangeLimits_areIncluded() {
        assertNull(ProfileValidator.check("100", height))
        assertNull(ProfileValidator.check("250", height))
        assertEquals(FieldError.OUT_OF_RANGE, ProfileValidator.check("99.9", height))
        assertEquals(FieldError.OUT_OF_RANGE, ProfileValidator.check("250.1", height))
    }

    @Test
    fun cleanNumber_keepsDigitsAndOneDot() {
        assertEquals("72.5", ProfileValidator.cleanNumber("72,5 kg"))
        assertEquals("1.23", ProfileValidator.cleanNumber("1.2.3"))
        assertEquals("", ProfileValidator.cleanNumber("-"))
    }

    @Test
    fun cleanNumber_capsLength() {
        assertEquals("123456", ProfileValidator.cleanNumber("123456789"))
    }

    @Test
    fun toProfile_withValidFields_buildsProfile() {
        val profile = ProfileValidator.toProfile("178", "82.5", "76", FitnessGoal.BUILD_MUSCLE, TrainingPlace.HOME)

        assertEquals(
            UserProfile(178f, 82.5f, 76f, FitnessGoal.BUILD_MUSCLE, TrainingPlace.HOME),
            profile,
        )
    }

    @Test
    fun toProfile_withAnyBadField_returnsNull() {
        assertNull(ProfileValidator.toProfile("", "82", "76", FitnessGoal.STAY_FIT, TrainingPlace.GYM))
        assertNull(ProfileValidator.toProfile("178", "20", "76", FitnessGoal.STAY_FIT, TrainingPlace.GYM))
        assertNull(ProfileValidator.toProfile("178", "82", "400", FitnessGoal.STAY_FIT, TrainingPlace.GYM))
    }
}
