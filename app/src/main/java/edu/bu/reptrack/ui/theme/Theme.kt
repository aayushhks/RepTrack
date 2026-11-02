package edu.bu.reptrack.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Teal,
    onPrimary = Color.White,
    primaryContainer = TealTint,
    onPrimaryContainer = TealDark,
    secondary = Muted,
    onSecondary = Color.White,
    tertiary = Orange,
    onTertiary = Color.White,
    tertiaryContainer = OrangeTint,
    onTertiaryContainer = Orange,
    background = AppBackground,
    onBackground = Ink,
    surface = AppBackground,
    onSurface = Ink,
    onSurfaceVariant = Muted,
)

private val DarkColors = darkColorScheme(
    primary = TealLight,
    onPrimary = TealOnDark,
    primaryContainer = TealContainerDark,
    onPrimaryContainer = TealTintDark,
    tertiary = OrangeLight,
    background = SurfaceDark,
    surface = SurfaceDark,
)

/** App theme. Dynamic color is off so the brand teal looks the same on every phone. */
@Composable
fun RepTrackTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        content = content,
    )
}
