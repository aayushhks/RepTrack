package edu.bu.reptrack.ui.theme

import androidx.compose.ui.graphics.Color

// RepTrack "Volt" palette: near-black surfaces, one lime accent for anything you act on or
// achieve, and orange for cardio and the goal line. Dark is the default look; the light
// theme keeps the same accents but uses ink for text and buttons, since lime is too light
// to read as text on white.

// Shared accents
val Lime = Color(0xFFC8F04A) // main accent: Start, Log, progress
val LimeDeep = Color(0xFF2B3510) // lime tint on dark, for chips
val LimeMist = Color(0xFFE9F7BF) // lime tint on light
val Ink = Color(0xFF0B0D10) // text on lime, buttons on light
val Ember = Color(0xFFFF7A3D) // cardio and the goal line on dark
val EmberDeep = Color(0xFFC2410C) // the same orange, dark enough to read on white
val EmberNight = Color(0xFF3A1E10)
val EmberMist = Color(0xFFFFE1D2)
val EmberInk = Color(0xFF5A1A00)

// Dark theme (default)
val Night = Color(0xFF0B0D10) // app background
val NightCard = Color(0xFF15181D) // cards, one step up
val NightBar = Color(0xFF111419) // bottom bar
val NightHigh = Color(0xFF1C2027) // tonal buttons, tiles inside cards
val NightHighest = Color(0xFF262B33) // tracks of rings and bars
val NightOutline = Color(0xFF3A414B)
val NightHairline = Color(0xFF232830)
val Chalk = Color(0xFFF2F4F7) // text on dark
val Fog = Color(0xFF9AA3AF) // secondary text on dark (7:1 on Night)

// Light theme
val Paper = Color(0xFFF5F6F3) // app background
val PaperCard = Color(0xFFFFFFFF)
val PaperHigh = Color(0xFFE9EBE6)
val PaperHighest = Color(0xFFDDE0D9)
val PaperOutline = Color(0xFFBFC3BA)
val PaperHairline = Color(0xFFE2E4DF)
val Graphite = Color(0xFF5B6370) // secondary text on light (5.6:1 on Paper)

// Errors
val Coral = Color(0xFFFF8A80)
val CoralNight = Color(0xFF4A1512)
val Brick = Color(0xFFB3261E)
val BrickMist = Color(0xFFF9DEDC)
