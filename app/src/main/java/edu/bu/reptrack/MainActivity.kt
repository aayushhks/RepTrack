package edu.bu.reptrack

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import edu.bu.reptrack.ui.AppViewModel
import edu.bu.reptrack.ui.RepTrackApp
import edu.bu.reptrack.ui.theme.RepTrackTheme
import edu.bu.reptrack.ui.theme.isDark

/** The only activity. All screens are composables inside [RepTrackApp]. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val appViewModel: AppViewModel = viewModel(factory = AppViewModel.Factory)
            val themeMode by appViewModel.themeMode.collectAsStateWithLifecycle()
            val dark = themeMode.isDark()

            // The clock and battery icons follow the app's theme, not the phone's, so they
            // stay readable when the app is dark and the phone is light (or the other way round).
            DisposableEffect(dark) {
                val style = if (dark) {
                    SystemBarStyle.dark(Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose {}
            }

            RepTrackTheme(darkTheme = dark) {
                RepTrackApp(appViewModel = appViewModel)
            }
        }
    }
}
