package io.github.l0gic0l.npptracker.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import io.github.l0gic0l.npptracker.R
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

data class ChartPoint(val date: LocalDate, val value: Float)

enum class ChartRange(val days: Long, @StringRes val label: Int) {
    MONTH(30, R.string.range_30d),
    QUARTER(90, R.string.range_90d),
    YEAR(365, R.string.range_1y),
}

@Composable
fun RangeSelector(selected: ChartRange, onSelect: (ChartRange) -> Unit, modifier: Modifier = Modifier) {
    SingleChoiceSegmentedButtonRow(modifier) {
        ChartRange.entries.forEachIndexed { index, range ->
            SegmentedButton(
                selected = range == selected,
                onClick = { onSelect(range) },
                shape = SegmentedButtonDefaults.itemShape(index, ChartRange.entries.size),
                icon = {},
                label = { Text(stringResource(range.label)) },
            )
        }
    }
}

/**
 * A 0-100% line chart over time. Tap or drag across the chart to read the value for a day.
 */
@Composable
fun PowerChart(
    points: List<ChartPoint>,
    contentDescription: String,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary,
) {
    if (points.isEmpty()) return
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
    val tooltipStyle = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.inverseOnSurface)
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val tooltipColor = MaterialTheme.colorScheme.inverseSurface
    val markerColor = MaterialTheme.colorScheme.onSurface
    val holeColor = MaterialTheme.colorScheme.surfaceContainerHighest
    var selected by remember(points) { mutableStateOf<Int?>(null) }

    val start = points.first().date
    val totalDays = ChronoUnit.DAYS.between(start, points.last().date).coerceAtLeast(1).toFloat()

    // Chart area insets: room for y labels on the left and x labels at the bottom.
    val leftInset = 40.dp
    val bottomInset = 22.dp
    val topInset = 30.dp

    fun indexAt(x: Float, left: Float, width: Float): Int {
        val day = ((x - left) / width * totalDays).coerceIn(0f, totalDays)
        val target = start.plusDays(day.roundToInt().toLong())
        return points.indices.minBy { kotlin.math.abs(ChronoUnit.DAYS.between(target, points[it].date)) }
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .semantics { this.contentDescription = contentDescription }
            .pointerInput(points) {
                val left = leftInset.toPx()
                detectTapGestures { offset ->
                    val index = indexAt(offset.x, left, size.width - left)
                    selected = if (selected == index) null else index
                }
            }
            .pointerInput(points) {
                val left = leftInset.toPx()
                detectHorizontalDragGestures(
                    onDragStart = { offset -> selected = indexAt(offset.x, left, size.width - left) },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        selected = indexAt(change.position.x, left, size.width - left)
                    },
                )
            },
    ) {
        val left = leftInset.toPx()
        val top = topInset.toPx()
        val chartWidth = size.width - left
        val chartHeight = size.height - top - bottomInset.toPx()
        val bottom = top + chartHeight

        fun xOf(date: LocalDate) = left + ChronoUnit.DAYS.between(start, date) / totalDays * chartWidth
        fun yOf(value: Float) = bottom - value.coerceIn(0f, 100f) / 100f * chartHeight

        // Horizontal grid lines with percentage labels.
        for (level in listOf(0, 50, 100)) {
            val y = yOf(level.toFloat())
            drawLine(
                color = gridColor,
                start = Offset(left, y),
                end = Offset(size.width, y),
                strokeWidth = 1.dp.toPx(),
                pathEffect = if (level == 0) null else PathEffect.dashPathEffect(floatArrayOf(6f, 6f)),
            )
            val label = textMeasurer.measure("$level%", labelStyle)
            drawText(label, topLeft = Offset(left - label.size.width - 8.dp.toPx(), y - label.size.height / 2f))
        }

        // Date labels: first, last and as many in between as fit (about one per 100dp).
        val labelCount = (chartWidth / 100.dp.toPx()).toInt().coerceIn(2, 5)
        for (i in 0 until labelCount) {
            val date = start.plusDays((totalDays * i / (labelCount - 1)).roundToInt().toLong())
            val label = textMeasurer.measure(date.formatShort(), labelStyle)
            val x = left + chartWidth * i / (labelCount - 1)
            val labelX = when (i) {
                0 -> x
                labelCount - 1 -> x - label.size.width
                else -> x - label.size.width / 2f
            }
            drawText(label, topLeft = Offset(labelX, bottom + 4.dp.toPx()))
        }

        // The power line and a soft fill under it.
        val line = Path()
        points.forEachIndexed { i, point ->
            val x = xOf(point.date)
            val y = yOf(point.value)
            if (i == 0) line.moveTo(x, y) else line.lineTo(x, y)
        }
        if (points.size == 1) line.lineTo(size.width, yOf(points[0].value))
        val fill = Path().apply {
            addPath(line)
            lineTo(if (points.size == 1) size.width else xOf(points.last().date), bottom)
            lineTo(xOf(points.first().date), bottom)
            close()
        }
        drawPath(fill, Brush.verticalGradient(listOf(lineColor.copy(alpha = 0.28f), lineColor.copy(alpha = 0.02f)), top, bottom))
        drawPath(line, lineColor, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))

        // Selected day: a marker and a label at the top of the chart.
        selected?.let { index ->
            val point = points[index]
            val x = xOf(point.date)
            val y = yOf(point.value)
            drawLine(markerColor.copy(alpha = 0.5f), Offset(x, top), Offset(x, bottom), strokeWidth = 1.dp.toPx())
            drawCircle(lineColor, radius = 5.dp.toPx(), center = Offset(x, y))
            drawCircle(holeColor, radius = 2.dp.toPx(), center = Offset(x, y))

            val text = textMeasurer.measure("${point.date.formatShort()} · ${point.value.roundToInt()}%", tooltipStyle)
            val padH = 8.dp.toPx()
            val padV = 4.dp.toPx()
            val boxWidth = text.size.width + padH * 2
            val boxHeight = text.size.height + padV * 2
            val boxLeft = (x - boxWidth / 2).coerceIn(left, size.width - boxWidth)
            drawRoundRect(
                color = tooltipColor,
                topLeft = Offset(boxLeft, 0f),
                size = Size(boxWidth, boxHeight),
                cornerRadius = CornerRadius(boxHeight / 2),
            )
            drawText(text, topLeft = Offset(boxLeft + padH, padV))
        }
    }
}
