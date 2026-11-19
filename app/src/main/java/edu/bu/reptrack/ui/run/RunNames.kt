package edu.bu.reptrack.ui.run

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import edu.bu.reptrack.R
import edu.bu.reptrack.domain.run.DayPart
import edu.bu.reptrack.ui.progress.formatWorkoutDate
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@StringRes
private fun DayPart.labelRes(): Int = when (this) {
    DayPart.MORNING -> R.string.day_part_morning
    DayPart.LUNCH -> R.string.day_part_lunch
    DayPart.AFTERNOON -> R.string.day_part_afternoon
    DayPart.EVENING -> R.string.day_part_evening
    DayPart.NIGHT -> R.string.day_part_night
}

@StringRes
private fun nameRes(isWalk: Boolean): Int = if (isWalk) R.string.run_name_walk else R.string.run_name_run

/** "Morning run", "Lunch walk": named by when it started, like Strava. */
@Composable
fun runName(dayPart: DayPart, isWalk: Boolean): String =
    stringResource(nameRes(isWalk), stringResource(dayPart.labelRes()))

/** The same name, for drawing outside Compose (the story picture). */
fun Context.runName(dayPart: DayPart, isWalk: Boolean): String =
    getString(nameRes(isWalk), getString(dayPart.labelRes()))

/** When a run started, like "Thu, Nov 19 · 7:42 AM". */
fun formatRunStart(
    epochMillis: Long,
    zone: ZoneId = ZoneId.systemDefault(),
    locale: Locale = Locale.getDefault(),
): String {
    val time = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
        .withLocale(locale)
        .format(Instant.ofEpochMilli(epochMillis).atZone(zone))
    return "${formatWorkoutDate(epochMillis, zone, locale)} · $time"
}
