package edu.bu.reptrack.ui.progress

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import edu.bu.reptrack.R
import edu.bu.reptrack.domain.profile.formatKg
import edu.bu.reptrack.domain.weight.ChartPoint
import edu.bu.reptrack.domain.weight.WeightChartData

/**
 * Line chart of the weigh-ins, drawn on a Compose Canvas, with the goal as a dashed line.
 * The goal is dashed and the weight solid, so the two differ by shape, not only by color.
 * A screen reader reads [description] instead of the drawing and the labels.
 */
@Composable
fun WeightTrendChart(
    chart: WeightChartData,
    description: String,
    startDate: String,
    endDate: String,
    modifier: Modifier = Modifier,
) {
    val lineColor = MaterialTheme.colorScheme.primary
    val goalColor = MaterialTheme.colorScheme.tertiary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val holeColor = MaterialTheme.colorScheme.surfaceContainerLowest

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(CHART_HEIGHT),
        ) {
            // The weights at the top and bottom edge, so the slope has a scale.
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(end = 8.dp)
                    .clearAndSetSemantics {},
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                AxisLabel(stringResource(R.string.weight_kg, formatKg(chart.topKg)))
                AxisLabel(stringResource(R.string.weight_kg, formatKg(chart.bottomKg)))
            }
            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .testTag("weight_chart")
                    .clearAndSetSemantics { contentDescription = description },
            ) {
                drawChart(chart, lineColor, goalColor, gridColor, holeColor)
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clearAndSetSemantics {},
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            AxisLabel(startDate)
            AxisLabel(endDate)
        }
        Row(
            modifier = Modifier.clearAndSetSemantics {},
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            LegendItem(color = lineColor, dashed = false, label = stringResource(R.string.weight_legend_you))
            if (chart.goalY != null) {
                LegendItem(color = goalColor, dashed = true, label = stringResource(R.string.weight_legend_goal))
            }
        }
    }
}

private val CHART_HEIGHT = 160.dp

private fun DrawScope.drawChart(
    chart: WeightChartData,
    lineColor: Color,
    goalColor: Color,
    gridColor: Color,
    holeColor: Color,
) {
    // Room around the edges so the dots on the first and last weigh-in are not cut off.
    val inset = 8.dp.toPx()
    val width = size.width - 2 * inset
    val height = size.height - 2 * inset
    fun place(point: ChartPoint) = Offset(inset + point.x * width, inset + (1 - point.y) * height)

    // Lines at the top and bottom edge, matching the labels on the left.
    val hairline = 1.dp.toPx()
    drawLine(gridColor, Offset(0f, inset), Offset(size.width, inset), hairline)
    drawLine(gridColor, Offset(0f, size.height - inset), Offset(size.width, size.height - inset), hairline)

    chart.goalY?.let { goalY ->
        val y = inset + (1 - goalY) * height
        drawLine(
            color = goalColor,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 2.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx())),
        )
    }

    val path = Path()
    chart.points.forEachIndexed { index, point ->
        val at = place(point)
        if (index == 0) path.moveTo(at.x, at.y) else path.lineTo(at.x, at.y)
    }
    drawPath(
        path = path,
        color = lineColor,
        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
    chart.points.forEach { drawCircle(lineColor, radius = 3.dp.toPx(), center = place(it)) }

    // The newest weigh-in gets a ring, so "where am I now" is easy to spot.
    chart.points.lastOrNull()?.let { last ->
        drawCircle(lineColor, radius = 6.dp.toPx(), center = place(last))
        drawCircle(holeColor, radius = 3.dp.toPx(), center = place(last))
    }
}

@Composable
private fun AxisLabel(text: String) {
    Text(text = text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun LegendItem(color: Color, dashed: Boolean, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Canvas(modifier = Modifier.size(width = 20.dp, height = 8.dp)) {
            drawLine(
                color = color,
                start = Offset(0f, size.height / 2),
                end = Offset(size.width, size.height / 2),
                strokeWidth = 2.dp.toPx(),
                pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx())) else null,
            )
        }
        AxisLabel(label)
    }
}
