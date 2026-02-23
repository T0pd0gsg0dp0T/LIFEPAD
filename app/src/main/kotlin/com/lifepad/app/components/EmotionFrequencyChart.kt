package com.lifepad.app.components

import android.graphics.Color
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.HorizontalBarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.lifepad.app.data.local.dao.EmotionFrequencyRow

private val NEGATIVE_EMOTIONS = setOf(
    "Anxious", "Sad", "Angry", "Fearful", "Guilty",
    "Ashamed", "Frustrated", "Hopeless", "Lonely", "Disgusted"
)

@Composable
fun EmotionFrequencyChart(
    emotionFrequency: List<EmotionFrequencyRow>,
    modifier: Modifier = Modifier
) {
    val chartHeight = (emotionFrequency.size * 36).coerceIn(120, 400)

    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .height(chartHeight.dp),
        factory = { context ->
            HorizontalBarChart(context).apply {
                description.isEnabled = false
                setTouchEnabled(false)
                setScaleEnabled(false)
                legend.isEnabled = false
                setDrawGridBackground(false)
                setBackgroundColor(Color.TRANSPARENT)
                setFitBars(true)

                axisLeft.apply {
                    axisMinimum = 0f
                    granularity = 1f
                    textColor = 0xFFCAC4D0.toInt()
                    gridColor = 0xFF3D2E50.toInt()
                    setDrawGridLines(true)
                }
                axisRight.isEnabled = false

                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    granularity = 1f
                    textColor = 0xFFCAC4D0.toInt()
                    textSize = 10f
                    setDrawGridLines(false)
                }

                setExtraOffsets(4f, 4f, 12f, 4f)
            }
        },
        update = { chart ->
            if (emotionFrequency.isEmpty()) {
                chart.clear()
                return@AndroidView
            }

            val sorted = emotionFrequency.sortedBy { it.count }

            val entries = sorted.mapIndexed { index, row ->
                BarEntry(index.toFloat(), row.count.toFloat())
            }

            val colors = sorted.map { row ->
                if (row.emotionName in NEGATIVE_EMOTIONS) {
                    0xFFFF5252.toInt() // red for negative
                } else {
                    0xFF00FF41.toInt() // matrix green for positive
                }
            }

            val labels = sorted.map { it.emotionName }

            val dataSet = BarDataSet(entries, "Emotions").apply {
                setColors(colors)
                setDrawValues(true)
                valueTextSize = 10f
                valueTextColor = 0xFFCAC4D0.toInt()
                valueFormatter = CompactValueFormatter()
            }

            chart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            chart.xAxis.labelCount = labels.size
            chart.data = BarData(dataSet).apply { barWidth = 0.7f }
            chart.invalidate()
        }
    )
}
