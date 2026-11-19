package edu.bu.reptrack.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import edu.bu.reptrack.domain.settings.ThemeMode

// How the roles are used across the app:
// primary: the main action color. Lime on dark, ink on light, so it always reads on the background.
// primaryContainer: the lime hero cards (today's workout, the run screen). Lime in both themes.
// tertiary: orange, for cardio and the goal line.
// surfaceContainerLowest: cards. surfaceContainerHigh: tiles and tonal buttons inside cards.

private val DarkColors = darkColorScheme(
    primary = Lime,
    onPrimary = Ink,
    primaryContainer = Lime,
    onPrimaryContainer = Ink,
    inversePrimary = Ink,
    secondary = Chalk,
    onSecondary = Ink,
    secondaryContainer = NightHigh,
    onSecondaryContainer = Chalk,
    tertiary = Ember,
    onTertiary = Ink,
    tertiaryContainer = EmberNight,
    onTertiaryContainer = EmberMist,
    error = Coral,
    onError = CoralNight,
    errorContainer = CoralNight,
    onErrorContainer = BrickMist,
    background = Night,
    onBackground = Chalk,
    surface = Night,
    onSurface = Chalk,
    surfaceVariant = NightHigh,
    onSurfaceVariant = Fog,
    // Same as the surface, so cards and sheets never get a colored tint.
    surfaceTint = Night,
    outline = NightOutline,
    outlineVariant = NightHairline,
    surfaceContainerLowest = NightCard,
    surfaceContainerLow = NightCard,
    surfaceContainer = NightBar,
    surfaceContainerHigh = NightHigh,
    surfaceContainerHighest = NightHighest,
    inverseSurface = Chalk,
    inverseOnSurface = Ink,
    scrim = Ink,
)

private val LightColors = lightColorScheme(
    // Ink buttons with lime text: the same Volt pair, flipped for a light background.
    primary = Ink,
    onPrimary = Lime,
    primaryContainer = Lime,
    onPrimaryContainer = Ink,
    inversePrimary = Lime,
    secondary = Ink,
    onSecondary = PaperCard,
    secondaryContainer = PaperHigh,
    onSecondaryContainer = Ink,
    tertiary = EmberDeep,
    onTertiary = PaperCard,
    tertiaryContainer = EmberMist,
    onTertiaryContainer = EmberInk,
    error = Brick,
    onError = PaperCard,
    errorContainer = BrickMist,
    onErrorContainer = Brick,
    background = Paper,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = PaperHigh,
    onSurfaceVariant = Graphite,
    surfaceTint = Paper,
    outline = PaperOutline,
    outlineVariant = PaperHairline,
    surfaceContainerLowest = PaperCard,
    surfaceContainerLow = PaperCard,
    surfaceContainer = PaperCard,
    surfaceContainerHigh = PaperHigh,
    surfaceContainerHighest = PaperHighest,
    inverseSurface = Ink,
    inverseOnSurface = Chalk,
)

// Soft, round shapes: text fields 14, chips and tiles 16, cards 22, hero cards 28.
// Buttons are full pills by Material default.
private val RepTrackShapes = Shapes(
    extraSmall = RoundedCornerShape(14.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

/** True when [mode] means the dark colors. Dark is the app's default look. */
@Composable
fun ThemeMode.isDark(): Boolean = when (this) {
    ThemeMode.DARK -> true
    ThemeMode.LIGHT -> false
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
}

/** App theme. Dynamic color is off so the brand colors look the same on every phone. */
@Composable
fun RepTrackTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = RepTrackTypography,
        shapes = RepTrackShapes,
        content = content,
    )
}
