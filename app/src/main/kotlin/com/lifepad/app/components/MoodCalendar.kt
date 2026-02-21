package com.lifepad.app.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar
import java.util.Locale

@Composable
fun MoodCalendar(
    dailyMoodMap: Map<Long, Double>,
    periodDays: Int,
    modifier: Modifier = Modifier
) {
    val emptyColor = MaterialTheme.colorScheme.surfaceVariant
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant
    val density = LocalDensity.current
    val textSizePx = with(density) { 10.sp.toPx() }

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height((calculateCalendarHeight(periodDays) + 24).dp)
        ) {
            drawMoodCalendar(
                dailyMoodMap = dailyMoodMap,
                periodDays = periodDays,
                emptyColor = emptyColor,
                textColor = textColor,
                textSizePx = textSizePx
            )
        }
    }
}

private fun calculateCalendarHeight(periodDays: Int): Int {
    val weeks = (periodDays + 6) / 7 + 1
    return weeks * 18 + 20 // cell size + header
}

private fun DrawScope.drawMoodCalendar(
    dailyMoodMap: Map<Long, Double>,
    periodDays: Int,
    emptyColor: Color,
    textColor: Color,
    textSizePx: Float
) {
    // Pre-normalize all map keys from UTC-truncated to local-timezone midnight
    val normalizedMap = dailyMoodMap.entries.associate { (utcKey, value) ->
        val c = Calendar.getInstance()
        c.timeInMillis = utcKey
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        c.timeInMillis to value
    }

    val cellSize = 14.dp.toPx()
    val cellGap = 3.dp.toPx()
    val headerHeight = 20.dp.toPx()
    val leftMargin = 28.dp.toPx()

    val dayHeaders = listOf("M", "T", "W", "T", "F", "S", "S")

    val textPaint = android.graphics.Paint().apply {
        color = textColor.toArgb()
        this.textSize = textSizePx
        textAlign = android.graphics.Paint.Align.CENTER
        isAntiAlias = true
    }

    // Draw day-of-week headers
    for (i in dayHeaders.indices) {
        val x = leftMargin + i * (cellSize + cellGap) + cellSize / 2
        drawContext.canvas.nativeCanvas.drawText(
            dayHeaders[i],
            x,
            headerHeight - 4.dp.toPx(),
            textPaint
        )
    }

    // Calculate the date range
    val cal = Calendar.getInstance()
    val endDay = Calendar.getInstance()

    cal.add(Calendar.DAY_OF_YEAR, -periodDays)
    // Move to Monday of that week
    while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
        cal.add(Calendar.DAY_OF_YEAR, -1)
    }

    var col = 0
    var row = 0
    var lastMonth = -1

    while (!cal.after(endDay)) {
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        // Convert to Monday=0..Sunday=6
        col = when (dayOfWeek) {
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY -> 4
            Calendar.SATURDAY -> 5
            Calendar.SUNDAY -> 6
            else -> 0
        }

        val x = leftMargin + col * (cellSize + cellGap)
        val y = headerHeight + row * (cellSize + cellGap)

        // Normalize day timestamp
        val dayCal = Calendar.getInstance()
        dayCal.timeInMillis = cal.timeInMillis
        dayCal.set(Calendar.HOUR_OF_DAY, 0)
        dayCal.set(Calendar.MINUTE, 0)
        dayCal.set(Calendar.SECOND, 0)
        dayCal.set(Calendar.MILLISECOND, 0)

        // Find matching mood using normalized map
        val dayStart = dayCal.timeInMillis
        val mood = normalizedMap[dayStart]

        val cellColor = if (mood != null) {
            getMoodColor(mood.toInt().coerceIn(1, 10)).copy(alpha = 0.7f + (mood.toFloat() / 10f) * 0.3f)
        } else {
            emptyColor.copy(alpha = 0.3f)
        }

        drawRoundRect(
            color = cellColor,
            topLeft = Offset(x, y),
            size = Size(cellSize, cellSize),
            cornerRadius = CornerRadius(3.dp.toPx())
        )

        // Draw month label at the start of a new month
        val currentMonth = cal.get(Calendar.MONTH)
        if (currentMonth != lastMonth && col == 0) {
            val monthName = cal.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault()) ?: ""
            val monthPaint = android.graphics.Paint().apply {
                color = textColor.toArgb()
                this.textSize = textSizePx * 0.9f
                textAlign = android.graphics.Paint.Align.RIGHT
                isAntiAlias = true
            }
            drawContext.canvas.nativeCanvas.drawText(
                monthName,
                leftMargin - 4.dp.toPx(),
                y + cellSize / 2 + textSizePx / 3,
                monthPaint
            )
            lastMonth = currentMonth
        }

        cal.add(Calendar.DAY_OF_YEAR, 1)
        if (col == 6) {
            row++
        }
    }
}
