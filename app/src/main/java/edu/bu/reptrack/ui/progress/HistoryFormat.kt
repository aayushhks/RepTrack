package edu.bu.reptrack.ui.progress

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** The date a workout finished, like "Sat, Nov 7". Plain Kotlin so it is easy to test. */
fun formatWorkoutDate(
    epochMillis: Long,
    zone: ZoneId = ZoneId.systemDefault(),
    locale: Locale = Locale.getDefault(),
): String = DateTimeFormatter.ofPattern("EEE, MMM d", locale)
    .format(Instant.ofEpochMilli(epochMillis).atZone(zone))

/** Splits whole minutes into hours and minutes: 75 -> (1, 15). */
fun splitHoursMinutes(totalMinutes: Long): Pair<Long, Long> = totalMinutes / 60 to totalMinutes % 60
