package edu.bu.reptrack.ui.howto

import org.junit.Assert.assertEquals
import org.junit.Test

class DemoPhotosTest {

    @Test
    fun startPhoto_thenFadeToTheEndPhoto_thenFadeBack() {
        assertEquals(0f, secondPhotoAlpha(0f), 0.001f)
        assertEquals(0f, secondPhotoAlpha(0.4f), 0.001f)
        assertEquals(0.5f, secondPhotoAlpha(0.46f), 0.001f)
        assertEquals(1f, secondPhotoAlpha(0.5f), 0.001f)
        assertEquals(1f, secondPhotoAlpha(0.9f), 0.001f)
        assertEquals(0.5f, secondPhotoAlpha(0.96f), 0.001f)
        assertEquals(0f, secondPhotoAlpha(1f), 0.001f)
    }
}
