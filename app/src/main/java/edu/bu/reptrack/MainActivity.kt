package edu.bu.reptrack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import edu.bu.reptrack.ui.RepTrackApp
import edu.bu.reptrack.ui.theme.RepTrackTheme

/** The only activity. All screens are composables inside [RepTrackApp]. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RepTrackTheme {
                RepTrackApp()
            }
        }
    }
}
