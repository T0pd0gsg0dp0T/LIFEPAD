package com.lifepad.app.components

import android.graphics.Color
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter

@Composable
fun MoodDistributionChart(
    distribution: Map<Int, Int>,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp),
        factory = { context ->
            BarChart(context).apply {
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
                }
                axisRight.isEnabled = false

                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    granularity = 1f
                    textColor = 0xFFCAC4D0.toInt()
                    setDrawGridLines(false)
                }
            }
        },
        update = { chart ->
            if (distribution.isEmpty()) {
                chart.clear()
                return@AndroidView
            }

            val entries = (1..10).map { mood ->
                BarEntry(mood.toFloat(), (distribution[mood] ?: 0).toFloat())
            }

            // Red to matrix-green gradient for moods 1-10
            val colors = listOf(
                0xFFFF1744.toInt(), // 1
                0xFFFF5252.toInt(), // 2
                0xFFFF6E40.toInt(), // 3
                0xFFFFD740.toInt(), // 4
                0xFFFFFF00.toInt(), // 5
                0xFFB2FF59.toInt(), // 6
                0xFF69F0AE.toInt(), // 7
                0xFF00E676.toInt(), // 8
                0xFF00CC33.toInt(), // 9
                0xFF00FF41.toInt()  // 10
            )

            val dataSet = BarDataSet(entries, "Mood Distribution").apply {
                setColors(colors)
                setDrawValues(true)
                valueTextSize = 10f
                valueTextColor = 0xFFCAC4D0.toInt()
            }

            val labels = (0..10).map { if (it == 0) "" else it.toString() }
            chart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            chart.data = BarData(dataSet).apply { barWidth = 0.8f }
            chart.invalidate()
        }
    )
}
