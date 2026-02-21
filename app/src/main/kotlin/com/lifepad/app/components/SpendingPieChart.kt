package com.lifepad.app.components

import android.graphics.Color
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter

@Composable
fun SpendingPieChart(
    data: List<Pair<String, Float>>,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp),
        factory = { context ->
            PieChart(context).apply {
                description.isEnabled = false
                isDrawHoleEnabled = true
                setHoleColor(Color.TRANSPARENT)
                holeRadius = 45f
                transparentCircleRadius = 50f
                setUsePercentValues(true)
                setEntryLabelTextSize(11f)
                setEntryLabelColor(0xFFE0E0E0.toInt())
                legend.isEnabled = true
                legend.textSize = 11f
                legend.textColor = 0xFFCAC4D0.toInt()
            }
        },
        update = { chart ->
            if (data.isEmpty()) {
                chart.clear()
                return@AndroidView
            }

            val entries = data.map { (label, value) -> PieEntry(value, label) }

            // Purple/green palette for AMOLED dark theme
            val colors = listOf(
                0xFFBB86FC.toInt(), // Deep purple bright
                0xFF00FF41.toInt(), // Matrix green
                0xFFCE93D8.toInt(), // Light purple
                0xFF69F0AE.toInt(), // Soft green
                0xFFFF5252.toInt(), // Red
                0xFF7B1FA2.toInt(), // Deep purple
                0xFF00CC33.toInt(), // Dim green
                0xFFFF6E40.toInt(), // Orange
                0xFF4A0072.toInt(), // Dark purple
                0xFFFFD740.toInt()  // Yellow
            )

            val dataSet = PieDataSet(entries, "").apply {
                setColors(colors.take(entries.size))
                sliceSpace = 2f
                valueTextSize = 12f
                valueTextColor = 0xFFE0E0E0.toInt()
                valueFormatter = PercentFormatter(chart)
            }

            chart.data = PieData(dataSet)
            chart.invalidate()
        }
    )
}
