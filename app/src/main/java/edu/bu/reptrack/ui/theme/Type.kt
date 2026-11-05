package edu.bu.reptrack.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import edu.bu.reptrack.R

// Archivo (SIL Open Font License, see licenses/archivo-OFL.txt).
// The expanded width is used for headlines, the normal width for everything else.

val Archivo = FontFamily(
    Font(R.font.archivo_regular, FontWeight.Normal),
    Font(R.font.archivo_medium, FontWeight.Medium),
    Font(R.font.archivo_semibold, FontWeight.SemiBold),
    Font(R.font.archivo_bold, FontWeight.Bold),
)

val ArchivoExpanded = FontFamily(
    Font(R.font.archivo_expanded_semibold, FontWeight.SemiBold),
    Font(R.font.archivo_expanded_bold, FontWeight.Bold),
)

private fun headline(size: Int, lineHeight: Int, spacing: Double, weight: FontWeight = FontWeight.Bold) = TextStyle(
    fontFamily = ArchivoExpanded,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = spacing.sp,
)

private fun text(size: Int, lineHeight: Int, weight: FontWeight, spacing: Double = 0.0) = TextStyle(
    fontFamily = Archivo,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = spacing.sp,
)

val RepTrackTypography = Typography(
    displayLarge = headline(52, 58, -1.0),
    displayMedium = headline(44, 50, -0.75),
    displaySmall = headline(36, 42, -0.5),
    headlineLarge = headline(32, 38, -0.5),
    headlineMedium = headline(28, 34, -0.25),
    headlineSmall = headline(24, 30, -0.25, FontWeight.SemiBold),
    titleLarge = text(22, 28, FontWeight.SemiBold),
    titleMedium = text(17, 24, FontWeight.SemiBold, 0.1),
    titleSmall = text(14, 20, FontWeight.Medium, 0.1),
    bodyLarge = text(16, 24, FontWeight.Normal),
    bodyMedium = text(14, 21, FontWeight.Normal),
    bodySmall = text(12, 17, FontWeight.Normal, 0.1),
    labelLarge = text(14, 20, FontWeight.SemiBold, 0.1),
    labelMedium = text(12, 16, FontWeight.Medium, 0.3),
    labelSmall = text(11, 16, FontWeight.Medium, 0.3),
)
