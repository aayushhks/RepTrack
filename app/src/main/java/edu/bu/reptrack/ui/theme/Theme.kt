package edu.bu.reptrack.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = InkNavy,
    onPrimary = Chalk,
    primaryContainer = NavyMist,
    onPrimaryContainer = InkNavy,
    secondary = SlateDark,
    onSecondary = ChalkWhite,
    secondaryContainer = Stone,
    onSecondaryContainer = InkNavy,
    tertiary = Olive,
    onTertiary = ChalkWhite,
    tertiaryContainer = OliveMist,
    onTertiaryContainer = OliveDeep,
    error = Brick,
    onError = ChalkWhite,
    errorContainer = BrickMist,
    onErrorContainer = Brick,
    background = Chalk,
    onBackground = InkNavy,
    surface = Chalk,
    onSurface = InkNavy,
    surfaceVariant = Stone,
    onSurfaceVariant = Slate,
    // Same as the surface, so cards and sheets never get a colored tint.
    surfaceTint = Chalk,
    outline = Pebble,
    outlineVariant = PebbleLight,
    surfaceContainerLowest = ChalkWhite,
    surfaceContainerLow = Chalk,
    // Bottom bar is white, so the stone tab indicator reads clearly.
    surfaceContainer = ChalkWhite,
    surfaceContainerHigh = Stone,
    surfaceContainerHighest = StoneDeep,
)

private val DarkColors = darkColorScheme(
    // On dark, the main color flips to chalk, so main buttons are light pills.
    primary = ChalkDim,
    onPrimary = Night,
    primaryContainer = NavyDeep,
    onPrimaryContainer = ChalkDim,
    secondary = SlateMist,
    onSecondary = NightLow,
    secondaryContainer = NightHighest,
    onSecondaryContainer = ChalkDim,
    tertiary = OliveLight,
    onTertiary = OliveInk,
    tertiaryContainer = OliveNight,
    onTertiaryContainer = OliveMist,
    error = BrickLight,
    onError = BrickNight,
    errorContainer = BrickNight,
    onErrorContainer = BrickMist,
    background = Night,
    onBackground = ChalkDim,
    surface = Night,
    onSurface = ChalkDim,
    surfaceVariant = NightHigh,
    onSurfaceVariant = SlateLight,
    surfaceTint = Night,
    outline = NightOutline,
    outlineVariant = NightHairline,
    // Our quiet cards use the lowest container. On dark we lift it a step so cards do not look sunken.
    surfaceContainerLowest = NightCard,
    surfaceContainerLow = NightLow,
    surfaceContainer = NightMid,
    surfaceContainerHigh = NightHigh,
    surfaceContainerHighest = NightHighest,
)

// Rounder as things get bigger: text fields 12, chips and small items 16 (a pill at chip height),
// cards 20, the main plan card 28. Buttons are full pills by Material default.
private val RepTrackShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

/** App theme. Dynamic color is off so the brand colors look the same on every phone. */
@Composable
fun RepTrackTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = RepTrackTypography,
        shapes = RepTrackShapes,
        content = content,
    )
}
