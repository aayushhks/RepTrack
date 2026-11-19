package edu.bu.reptrack.ui.run

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import edu.bu.reptrack.R
import edu.bu.reptrack.domain.run.DayPart
import edu.bu.reptrack.domain.run.RoutePoint
import edu.bu.reptrack.domain.run.RunFormat
import edu.bu.reptrack.domain.run.RunSummary
import edu.bu.reptrack.ui.components.ErrorCard
import edu.bu.reptrack.ui.components.IconTile
import edu.bu.reptrack.ui.components.Overline
import edu.bu.reptrack.ui.components.StatTile
import edu.bu.reptrack.ui.components.VoltCard
import edu.bu.reptrack.ui.components.readableWidth
import edu.bu.reptrack.ui.theme.RepTrackTheme
import edu.bu.reptrack.ui.theme.statStyle
import java.time.ZoneId
import java.util.Locale

/** Connects the run detail screen to its ViewModel. */
@Composable
fun RunDetailRoute(
    onBack: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RunDetailViewModel = viewModel(factory = RunDetailViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    RunDetailScreen(
        state = state,
        onBack = onBack,
        onShare = onShare,
        onRetry = viewModel::retry,
        modifier = modifier,
    )
}

/**
 * A saved walk or run, like an activity on Strava: its name and start time, the route on a
 * map, the totals, and a button to share it as a story. Stateless.
 *
 * [map] draws the route. Tests pass a plain box instead, because the real map needs the
 * internet and a graphics driver.
 */
@Composable
fun RunDetailScreen(
    state: RunDetailUiState,
    onBack: () -> Unit,
    onShare: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    zone: ZoneId = ZoneId.systemDefault(),
    map: @Composable (route: List<RoutePoint>, modifier: Modifier) -> Unit = { route, mapModifier ->
        RouteMap(route = route, modifier = mapModifier)
    },
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("screen_run_detail"),
    ) {
        val run = state.run
        Row(
            modifier = Modifier
                .readableWidth()
                .padding(start = 8.dp, end = 20.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("run_detail_back")) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_back),
                    contentDescription = stringResource(R.string.run_detail_back),
                )
            }
            if (run != null) {
                Column(modifier = Modifier.padding(start = 4.dp)) {
                    Text(
                        text = runName(state.dayPart, run.isWalk).uppercase(Locale.getDefault()),
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier
                            .semantics { heading() }
                            .testTag("run_detail_title"),
                    )
                    Text(
                        text = formatRunStart(run.startedAt, zone),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        when {
            state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.loadFailed -> ErrorCard(
                message = stringResource(R.string.run_detail_load_failed),
                actionLabel = stringResource(R.string.error_try_again),
                onAction = onRetry,
                modifier = Modifier
                    .readableWidth()
                    .padding(20.dp)
                    .testTag("run_detail_error"),
            )
            run == null || state.notFound -> Text(
                text = stringResource(R.string.run_detail_not_found),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .readableWidth()
                    .padding(20.dp)
                    .testTag("run_detail_not_found"),
            )
            else -> RunDetailContent(state = state, run = run, onShare = onShare, map = map)
        }
    }
}

@Composable
private fun RunDetailContent(
    state: RunDetailUiState,
    run: RunSummary,
    onShare: () -> Unit,
    map: @Composable (route: List<RoutePoint>, modifier: Modifier) -> Unit,
) {
    Column(
        modifier = Modifier
            .readableWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (state.hasRoute) {
            val mapDescription = stringResource(R.string.run_detail_map_description)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(MAP_HEIGHT)
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                        // The tag goes first: clearAndSetSemantics would clear it otherwise.
                        .testTag("run_map")
                        // One label for TalkBack instead of the map's own pieces.
                        .clearAndSetSemantics { contentDescription = mapDescription },
                ) {
                    map(state.route, Modifier.fillMaxSize())
                }
                Text(
                    text = stringResource(R.string.map_attribution),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            NoRouteCard()
        }

        // The distance first and biggest, like on the run screen.
        Column(modifier = Modifier.semantics(mergeDescendants = true) {}) {
            Overline(stringResource(R.string.run_detail_distance))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = RunFormat.km(run.distanceM),
                    style = statStyle(72.sp),
                    modifier = Modifier.testTag("run_detail_distance"),
                )
                Text(
                    text = stringResource(R.string.run_detail_km),
                    style = statStyle(28.sp),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 6.dp, bottom = 8.dp),
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(
                label = stringResource(R.string.run_time),
                value = RunFormat.duration(run.durationMs),
                valueSize = 26.sp,
                modifier = Modifier.weight(1f),
                valueModifier = Modifier.testTag("run_detail_time"),
            )
            StatTile(
                label = stringResource(R.string.run_pace),
                value = stringResource(R.string.run_pace_value, RunFormat.pace(run.paceSecondsPerKm)),
                valueSize = 26.sp,
                modifier = Modifier.weight(1f),
                valueModifier = Modifier.testTag("run_detail_pace"),
            )
            StatTile(
                label = stringResource(R.string.progress_week_calories),
                value = stringResource(R.string.run_calories_value, run.calories),
                valueSize = 26.sp,
                valueColor = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.weight(1f),
                valueModifier = Modifier.testTag("run_detail_calories"),
            )
        }

        Button(
            onClick = onShare,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .testTag("run_share"),
        ) {
            Icon(painterResource(R.drawable.ic_share), contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(8.dp))
            Text(stringResource(R.string.run_detail_share), style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun NoRouteCard() {
    VoltCard(modifier = Modifier.testTag("run_no_route")) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            IconTile(
                icon = R.drawable.ic_run,
                tint = MaterialTheme.colorScheme.tertiary,
                container = MaterialTheme.colorScheme.tertiaryContainer,
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.run_detail_no_route_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(R.string.run_detail_no_route_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private val MAP_HEIGHT = 320.dp

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun RunDetailScreenPreview() {
    RepTrackTheme {
        RunDetailScreen(
            state = RunDetailUiState(
                isLoading = false,
                run = RunSummary(id = 4, startedAt = 0, finishedAt = 26 * 60_000L, distanceM = 5_020.0, durationMs = 26 * 60_000L, calories = 410),
                route = listOf(RoutePoint(42.35, -71.10), RoutePoint(42.36, -71.09)),
                dayPart = DayPart.MORNING,
            ),
            onBack = {},
            onShare = {},
            onRetry = {},
            map = { _, mapModifier -> Box(mapModifier.background(MaterialTheme.colorScheme.surfaceContainerHigh)) },
        )
    }
}
