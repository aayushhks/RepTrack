package edu.bu.reptrack.domain.auth

import org.junit.Assert.assertEquals
import org.junit.Test

class FirstScreenTest {

    @Test
    fun signedOut_andNotSkipped_opensSignIn() {
        assertEquals(FirstScreen.SIGN_IN, FirstScreen.decide(signedIn = false, skippedSignIn = false, setupDone = false))
        assertEquals(FirstScreen.SIGN_IN, FirstScreen.decide(signedIn = false, skippedSignIn = false, setupDone = true))
    }

    @Test
    fun signedIn_withoutSetup_opensGoalSetup() {
        assertEquals(FirstScreen.GOAL_SETUP, FirstScreen.decide(signedIn = true, skippedSignIn = false, setupDone = false))
    }

    @Test
    fun skipped_withoutSetup_opensGoalSetup() {
        assertEquals(FirstScreen.GOAL_SETUP, FirstScreen.decide(signedIn = false, skippedSignIn = true, setupDone = false))
    }

    @Test
    fun signedInOrSkipped_withSetupDone_opensTheTabs() {
        assertEquals(FirstScreen.TABS, FirstScreen.decide(signedIn = true, skippedSignIn = false, setupDone = true))
        assertEquals(FirstScreen.TABS, FirstScreen.decide(signedIn = false, skippedSignIn = true, setupDone = true))
    }
}
