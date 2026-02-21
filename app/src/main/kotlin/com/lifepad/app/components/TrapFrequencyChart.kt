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
import com.lifepad.app.data.local.dao.TrapFrequencyRow
import com.lifepad.app.domain.cbt.ThinkingTrap

@Composable
fun TrapFrequencyChart(
    trapFrequency: List<TrapFrequencyRow>,
    modifier: Modifier = Modifier
) {
    val chartHeight = (trapFrequency.size * 36).coerceIn(120, 400)

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
            if (trapFrequency.isEmpty()) {
                chart.clear()
                return@AndroidView
            }

            val sorted = trapFrequency.sortedBy { it.count }

            val entries = sorted.mapIndexed { index, row ->
                BarEntry(index.toFloat(), row.count.toFloat())
            }

            val labels = sorted.map { row ->
                ThinkingTrap.entries.find { it.name == row.trapType }?.displayName
                    ?: row.trapType.replace("_", " ").lowercase()
                        .replaceFirstChar { it.uppercase() }
            }

            val dataSet = BarDataSet(entries, "Thinking Traps").apply {
                color = 0xFFBB86FC.toInt()
                setDrawValues(true)
                valueTextSize = 10f
                valueTextColor = 0xFFCAC4D0.toInt()
            }

            chart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            chart.xAxis.labelCount = labels.size
            chart.data = BarData(dataSet).apply { barWidth = 0.7f }
            chart.invalidate()
        }
    )
}
