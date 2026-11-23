package edu.bu.reptrack.ui.howto

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import edu.bu.reptrack.R
import edu.bu.reptrack.domain.exercise.ExerciseDemo

/**
 * The two how-to photos, the start and the end of the movement. They take turns about once a
 * second with a short fade, so the exercise moves like a GIF. With animations turned off in the
 * phone's settings, the two photos sit side by side instead. TalkBack reads one description.
 */
@Composable
fun DemoPhotos(demo: ExerciseDemo, exerciseName: String, modifier: Modifier = Modifier) {
    val photos = demo.imageUrls.take(2)
    if (photos.isEmpty()) return
    val description = stringResource(R.string.howto_photos_description, exerciseName)
    val context = LocalContext.current
    val animate = remember(context) { animationsOn(context) } && photos.size == 2

    if (!animate) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .testTag("demo_photos")
                .clearAndSetSemantics { contentDescription = description },
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            photos.forEachIndexed { index, url ->
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Photo(url = url, modifier = Modifier.fillMaxWidth())
                    Text(
                        text = stringResource(if (index == 0) R.string.howto_start else R.string.howto_end),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        return
    }

    // In UI tests infinite animations do not run, so the start photo simply stays.
    val phase = rememberInfiniteTransition(label = "demo").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = FLIP_MS * 2, easing = LinearEasing)),
        label = "demo_phase",
    )
    // Recomposes only when the label flips, not on every frame.
    val showingEnd by remember { derivedStateOf { secondPhotoAlpha(phase.value) >= 0.5f } }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .testTag("demo_photos")
            .clearAndSetSemantics { contentDescription = description },
    ) {
        Photo(url = photos[0], modifier = Modifier.fillMaxWidth())
        // Read inside graphicsLayer, so each frame only redraws instead of recomposing.
        Photo(url = photos[1], modifier = Modifier.fillMaxWidth().graphicsLayer { alpha = secondPhotoAlpha(phase.value) })
        Surface(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(10.dp),
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surface,
        ) {
            Text(
                text = stringResource(if (showingEnd) R.string.howto_end else R.string.howto_start),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun Photo(url: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .aspectRatio(PHOTO_RATIO)
            .clip(MaterialTheme.shapes.medium)
            // The photos have white backgrounds, so they sit on white in both themes.
            .background(Color.White),
    ) {
        AsyncImage(
            model = url,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

/**
 * How visible the end photo is, over one turn of [phase] from 0 to 1: hidden, a quick fade in,
 * shown, a quick fade out. Plain math, so it is easy to test.
 */
internal fun secondPhotoAlpha(phase: Float): Float = when {
    phase < 0.42f -> 0f
    phase < 0.5f -> (phase - 0.42f) / 0.08f
    phase < 0.92f -> 1f
    else -> (1f - (phase - 0.92f) / 0.08f).coerceIn(0f, 1f)
}

/** False when the user turned animations off (Settings, Accessibility, Remove animations). */
private fun animationsOn(context: Context): Boolean =
    Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f

/** Each photo shows for about a second. */
private const val FLIP_MS = 1_000

/** The photos are a bit wider than tall. */
private const val PHOTO_RATIO = 4f / 3f
