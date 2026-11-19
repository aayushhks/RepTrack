package edu.bu.reptrack.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.bu.reptrack.ui.theme.OverlineStyle
import edu.bu.reptrack.ui.theme.statStyle
import java.util.Locale

// Small building blocks of the RepTrack "Volt" look, shared by the screens.

/** A card on the dark background: one step lighter, round corners, no border. */
@Composable
fun VoltCard(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surfaceContainerLowest,
    contentPadding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = color,
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

/** Small all-caps label, like "DISTANCE" above a number. */
@Composable
fun Overline(text: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Text(
        text = text.uppercase(Locale.getDefault()),
        style = OverlineStyle,
        color = color,
        modifier = modifier,
    )
}

/** A square tile with an icon, at the start of a list row. The icon is decorative. */
@Composable
fun IconTile(
    @DrawableRes icon: Int,
    tint: Color,
    container: Color,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
) {
    Surface(modifier = modifier.size(size), shape = MaterialTheme.shapes.small, color = container) {
        Box(contentAlignment = Alignment.Center) {
            Icon(painterResource(icon), contentDescription = null, tint = tint, modifier = Modifier.size(size / 2))
        }
    }
}

/** A label over a big condensed number, like the stat tiles on a run or the weekly totals. */
@Composable
fun StatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    valueSize: TextUnit = 30.sp,
    container: Color = MaterialTheme.colorScheme.surfaceContainerLowest,
    valueModifier: Modifier = Modifier,
) {
    Surface(modifier = modifier, shape = MaterialTheme.shapes.small, color = container) {
        // One item for TalkBack: "Pace, 5:13 /km".
        Column(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 12.dp)
                .semantics(mergeDescendants = true) {},
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Overline(label)
            Text(text = value, style = statStyle(valueSize), color = valueColor, modifier = valueModifier)
        }
    }
}

/** Ring that fills up toward a goal, with the percentage in the middle. */
@Composable
fun GoalRing(
    progress: Float,
    description: String,
    modifier: Modifier = Modifier,
    size: Dp = 76.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            // One sentence for TalkBack instead of a progress bar plus a number.
            .clearAndSetSemantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            progress = { progress },
            modifier = Modifier.size(size),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            strokeWidth = 8.dp,
            strokeCap = StrokeCap.Round,
        )
        Text(
            text = "${(progress * 100).toInt()}%",
            style = statStyle(20.sp),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/**
 * Something went wrong, said in plain words, with the one action that helps (Try again or
 * Dismiss). TalkBack reads it out as soon as it appears.
 */
@Composable
fun ErrorCard(
    message: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .weight(1f)
                    .semantics { liveRegion = LiveRegionMode.Polite },
            )
            TextButton(
                onClick = onAction,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onErrorContainer),
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text(actionLabel, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

/** Keeps content to a comfortable reading width on tablets and in landscape, centered. */
fun Modifier.readableWidth(): Modifier = this
    .fillMaxWidth()
    .wrapContentWidth(Alignment.CenterHorizontally)
    .widthIn(max = 640.dp)
