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
import com.github.mikephil.charting.formatter.ValueFormatter
import java.util.Locale

data class TimeOfDayMoodEntry(
    val label: String,
    val averageMood: Float,
    val count: Int
)

@Composable
fun MoodTimeOfDayChart(
    entries: List<TimeOfDayMoodEntry>,
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
                    axisMaximum = 10f
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
            if (entries.isEmpty()) {
                chart.clear()
                return@AndroidView
            }

            val barEntries = entries.mapIndexed { index, entry ->
                BarEntry(index.toFloat(), entry.averageMood)
            }

            val dataSet = BarDataSet(barEntries, "Mood by Time of Day").apply {
                color = 0xFF00E676.toInt()
                setDrawValues(true)
                valueTextSize = 10f
                valueTextColor = 0xFFCAC4D0.toInt()
                valueFormatter = object : ValueFormatter() {
                    override fun getBarLabel(barEntry: BarEntry): String {
                        return String.format(Locale.getDefault(), "%.1f", barEntry.y)
                    }
                }
            }

            chart.xAxis.valueFormatter = IndexAxisValueFormatter(entries.map { it.label })
            chart.data = BarData(dataSet).apply { barWidth = 0.6f }
            chart.invalidate()
        }
    )
}
