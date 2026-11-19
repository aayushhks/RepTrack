package edu.bu.reptrack.ui.run.story

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import edu.bu.reptrack.R
import edu.bu.reptrack.domain.run.RunFormat
import edu.bu.reptrack.domain.run.RunSummary
import edu.bu.reptrack.ui.components.ErrorCard
import edu.bu.reptrack.ui.components.readableWidth
import edu.bu.reptrack.ui.run.runName
import edu.bu.reptrack.ui.theme.RepTrackTheme
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Connects the share screen to its ViewModel, and hands the finished picture to other apps. */
@Composable
fun RunStoryRoute(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RunStoryViewModel = viewModel(factory = RunStoryViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val instagramAvailable = remember(context) { context.findInstagram() != null }
    val chooserTitle = stringResource(R.string.story_share_chooser)

    RunStoryScreen(
        state = state,
        instagramAvailable = instagramAvailable,
        onClose = onClose,
        onStyleSelected = viewModel::selectStyle,
        onHideEndsChange = viewModel::setHideEnds,
        onShareInstagram = {
            val path = state.imagePath
            if (path != null && !context.shareToInstagramStory(File(path))) viewModel.onInstagramFailed()
        },
        onShareMore = { state.imagePath?.let { path -> context.shareImage(File(path), chooserTitle) } },
        onRetry = viewModel::retry,
        modifier = modifier,
    )
}

/**
 * Share to story: a preview of the card, the three looks, "Hide start and end", and the
 * buttons that send it to Instagram or any other app. Stateless.
 *
 * [preview] shows the saved picture; tests replace it with a plain box.
 */
@Composable
fun RunStoryScreen(
    state: RunStoryUiState,
    instagramAvailable: Boolean,
    onClose: () -> Unit,
    onStyleSelected: (StoryStyle) -> Unit,
    onHideEndsChange: (Boolean) -> Unit,
    onShareInstagram: () -> Unit,
    onShareMore: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    preview: @Composable (path: String, description: String, modifier: Modifier) -> Unit = { path, description, previewModifier ->
        StoryPicture(path = path, description = description, modifier = previewModifier)
    },
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("screen_run_story"),
    ) {
        Row(
            modifier = Modifier
                .readableWidth()
                .padding(start = 8.dp, end = 20.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose, modifier = Modifier.testTag("story_close")) {
                Icon(painterResource(R.drawable.ic_close), contentDescription = stringResource(R.string.story_close))
            }
            Text(
                text = stringResource(R.string.story_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .padding(start = 4.dp)
                    .semantics { heading() },
            )
        }

        if (state.loadFailed) {
            ErrorCard(
                message = stringResource(R.string.run_detail_load_failed),
                actionLabel = stringResource(R.string.error_try_again),
                onAction = onRetry,
                modifier = Modifier
                    .readableWidth()
                    .padding(20.dp)
                    .testTag("story_load_error"),
            )
            return@Column
        }

        // The card, as big as fits, in its real 9:16 shape.
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(STORY_RATIO, matchHeightConstraintsFirst = true)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                    .testTag("story_preview"),
                contentAlignment = Alignment.Center,
            ) {
                val run = state.run
                val path = state.imagePath
                if (path != null && run != null) {
                    val description = stringResource(
                        R.string.story_preview_description,
                        runName(state.dayPart, run.isWalk),
                        RunFormat.km(run.distanceM),
                    )
                    preview(path, description, Modifier.fillMaxSize())
                }
                when {
                    state.drawFailed -> ErrorCard(
                        message = stringResource(R.string.story_failed),
                        actionLabel = stringResource(R.string.error_try_again),
                        onAction = onRetry,
                        modifier = Modifier
                            .padding(12.dp)
                            .testTag("story_error"),
                    )
                    state.isLoading || state.isDrawing -> Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.45f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        // On its own surface, so it reads over a dark or a lime card in either theme.
                        Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface) {
                            Column(
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                Text(
                                    text = stringResource(R.string.story_drawing),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.testTag("story_drawing"),
                                )
                            }
                        }
                    }
                }
            }
        }

        // Scrolls on short screens (a phone on its side, or a large font), so the buttons stay reachable.
        Column(
            modifier = Modifier
                .readableWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                state.styles.forEach { style ->
                    FilterChip(
                        selected = state.options.style == style,
                        onClick = { onStyleSelected(style) },
                        label = { Text(stringResource(style.labelRes())) },
                        modifier = Modifier.testTag("story_style_${style.name.lowercase()}"),
                    )
                }
            }
            if (state.hasRoute) {
                HideEndsRow(checked = state.options.hideEnds, onCheckedChange = onHideEndsChange)
            }
            val note = when {
                state.instagramFailed -> stringResource(R.string.story_instagram_failed)
                state.mapUnavailable && state.options.style == StoryStyle.MAP -> stringResource(R.string.story_map_unavailable)
                else -> null
            }
            if (note != null) {
                Text(
                    text = note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier
                        .semantics { liveRegion = LiveRegionMode.Polite }
                        .testTag("story_note"),
                )
            }
            ShareButtons(
                enabled = state.canShare,
                instagramAvailable = instagramAvailable,
                onShareInstagram = onShareInstagram,
                onShareMore = onShareMore,
            )
        }
    }
}

@Composable
private fun HideEndsRow(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    // The whole row is one switch for TalkBack and for taps.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .testTag("story_hide_ends"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.story_hide_ends), style = MaterialTheme.typography.titleSmall)
            Text(
                text = stringResource(R.string.story_hide_ends_message),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun ShareButtons(
    enabled: Boolean,
    instagramAvailable: Boolean,
    onShareInstagram: () -> Unit,
    onShareMore: () -> Unit,
) {
    if (!instagramAvailable) {
        Button(
            onClick = onShareMore,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .testTag("story_share"),
        ) {
            Icon(painterResource(R.drawable.ic_share), contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(8.dp))
            Text(stringResource(R.string.story_share), style = MaterialTheme.typography.titleMedium)
        }
        return
    }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(
            onClick = onShareInstagram,
            enabled = enabled,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 56.dp)
                .testTag("story_instagram"),
        ) {
            Text(stringResource(R.string.story_instagram), style = MaterialTheme.typography.titleMedium)
        }
        OutlinedButton(
            onClick = onShareMore,
            enabled = enabled,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 56.dp)
                .testTag("story_more"),
        ) {
            Text(stringResource(R.string.story_more), style = MaterialTheme.typography.titleMedium)
        }
    }
}

/** The saved card from disk. Decoded at half size: plenty for a preview, a quarter of the memory. */
@Composable
private fun StoryPicture(path: String, description: String, modifier: Modifier = Modifier) {
    val picture by produceState<ImageBitmap?>(initialValue = null, path) {
        value = withContext(Dispatchers.IO) {
            BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = 2 })?.asImageBitmap()
        }
    }
    picture?.let {
        Image(bitmap = it, contentDescription = description, contentScale = ContentScale.Crop, modifier = modifier)
    }
}

private fun StoryStyle.labelRes(): Int = when (this) {
    StoryStyle.MAP -> R.string.story_style_map
    StoryStyle.DARK -> R.string.story_style_dark
    StoryStyle.LIME -> R.string.story_style_lime
}

/** Instagram stories are 9:16. */
private const val STORY_RATIO = 9f / 16f

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun RunStoryScreenPreview() {
    RepTrackTheme {
        RunStoryScreen(
            state = RunStoryUiState(
                isLoading = false,
                run = RunSummary(id = 4, startedAt = 0, finishedAt = 26 * 60_000L, distanceM = 5_020.0, durationMs = 26 * 60_000L, calories = 410),
                hasRoute = true,
            ),
            instagramAvailable = true,
            onClose = {},
            onStyleSelected = {},
            onHideEndsChange = {},
            onShareInstagram = {},
            onShareMore = {},
            onRetry = {},
        )
    }
}
