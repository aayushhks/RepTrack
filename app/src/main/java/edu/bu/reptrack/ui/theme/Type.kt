package edu.bu.reptrack.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import edu.bu.reptrack.R

// Barlow Condensed for headlines and the big numbers, Archivo for everything else.
// Both are under the SIL Open Font License, see licenses/.
// Condensed numbers fit a lot of digits in a small width, so "12:34" or "82.4" can be
// read at a glance between sets or while running.

val BarlowCondensed = FontFamily(
    Font(R.font.barlow_condensed_semibold, FontWeight.SemiBold),
    Font(R.font.barlow_condensed_bold, FontWeight.Bold),
)

val Archivo = FontFamily(
    Font(R.font.archivo_regular, FontWeight.Normal),
    Font(R.font.archivo_medium, FontWeight.Medium),
    Font(R.font.archivo_semibold, FontWeight.SemiBold),
    Font(R.font.archivo_bold, FontWeight.Bold),
)

// Big condensed type has a lot of empty space above the digits; trimming it lines numbers
// up with the labels next to them.
private val TightLines = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.Both,
)

private fun headline(size: Int, lineHeight: Int, spacing: Double = 0.0) = TextStyle(
    fontFamily = BarlowCondensed,
    fontWeight = FontWeight.Bold,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = spacing.sp,
    lineHeightStyle = TightLines,
)

private fun text(size: Int, lineHeight: Int, weight: FontWeight, spacing: Double = 0.0) = TextStyle(
    fontFamily = Archivo,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = spacing.sp,
)

val RepTrackTypography = Typography(
    displayLarge = headline(96, 96, -1.0),
    displayMedium = headline(64, 64, -0.5),
    displaySmall = headline(48, 50, -0.25),
    headlineLarge = headline(40, 44),
    headlineMedium = headline(34, 38, 0.25),
    headlineSmall = headline(28, 32, 0.25),
    titleLarge = text(20, 26, FontWeight.Bold),
    titleMedium = text(16, 22, FontWeight.SemiBold, 0.1),
    titleSmall = text(14, 20, FontWeight.SemiBold, 0.1),
    bodyLarge = text(16, 24, FontWeight.Normal),
    bodyMedium = text(14, 21, FontWeight.Normal),
    bodySmall = text(12, 17, FontWeight.Normal, 0.1),
    labelLarge = text(14, 20, FontWeight.SemiBold, 0.1),
    labelMedium = text(12, 16, FontWeight.SemiBold, 0.4),
    labelSmall = text(11, 16, FontWeight.Medium, 0.4),
)

/** Big condensed numbers in any size, for stats like distance, weight and the rest countdown. */
fun statStyle(size: TextUnit): TextStyle = TextStyle(
    fontFamily = BarlowCondensed,
    fontWeight = FontWeight.Bold,
    fontSize = size,
    lineHeight = size,
    lineHeightStyle = TightLines,
    fontFeatureSettings = "tnum",
)

/** Small all-caps label above a stat ("DISTANCE", "PACE"). Pass the text already in capitals. */
val OverlineStyle = TextStyle(
    fontFamily = Archivo,
    fontWeight = FontWeight.SemiBold,
    fontSize = 11.sp,
    lineHeight = 16.sp,
    letterSpacing = 1.2.sp,
)
