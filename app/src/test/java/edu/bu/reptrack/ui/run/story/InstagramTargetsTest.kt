package edu.bu.reptrack.ui.run.story

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InstagramTargetsTest {

    @Test
    fun picksTheStoryScreen() {
        val screens = listOf(
            "com.instagram.share.handleractivity.ShareHandlerActivity",
            "com.instagram.share.handleractivity.StoryShareHandlerActivity",
            "com.instagram.direct.share.handler.DirectShareHandlerActivity",
        )
        assertEquals("com.instagram.share.handleractivity.StoryShareHandlerActivity", InstagramTargets.pickStoryScreen(screens))
    }

    @Test
    fun noStoryScreen_isNull_soInstagramOffersItsOwnChoice() {
        assertNull(InstagramTargets.pickStoryScreen(listOf("com.instagram.share.handleractivity.ShareHandlerActivity")))
        assertNull(InstagramTargets.pickStoryScreen(emptyList()))
    }
}
