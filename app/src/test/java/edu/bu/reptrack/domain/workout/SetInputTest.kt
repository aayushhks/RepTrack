package edu.bu.reptrack.domain.workout

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SetInputTest {

    @Test
    fun cleanWeight_keepsOneDecimalPoint() {
        assertEquals("72.5", SetInput.cleanWeight("72,5 kg"))
    }

    @Test
    fun cleanReps_keepsUpToThreeDigits() {
        assertEquals("12", SetInput.cleanReps("1x2"))
        assertEquals("100", SetInput.cleanReps("1000"))
    }

    @Test
    fun emptyWeight_isFine_butBadWeightsAreNot() {
        assertFalse(SetInput.isWeightBad(""))
        assertFalse(SetInput.isWeightBad("500"))
        assertTrue(SetInput.isWeightBad("500.5"))
        assertTrue(SetInput.isWeightBad("."))
    }

    @Test
    fun repsMustBeOneToHundred() {
        assertTrue(SetInput.isRepsBad(""))
        assertTrue(SetInput.isRepsBad("0"))
        assertTrue(SetInput.isRepsBad("101"))
        assertFalse(SetInput.isRepsBad("1"))
        assertFalse(SetInput.isRepsBad("100"))
    }

    @Test
    fun parse_emptyWeight_meansBodyweight() {
        assertEquals(ParsedSet(weightKg = 0f, reps = 15), SetInput.parse("", "15"))
    }

    @Test
    fun parse_validSet() {
        assertEquals(ParsedSet(weightKg = 62.5f, reps = 8), SetInput.parse("62.5", "8"))
    }

    @Test
    fun parse_returnsNull_whenEitherBoxIsWrong() {
        assertNull(SetInput.parse("60", ""))
        assertNull(SetInput.parse("900", "8"))
    }
}
