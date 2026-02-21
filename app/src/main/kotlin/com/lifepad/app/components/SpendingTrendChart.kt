package com.lifepad.app.components

import android.graphics.Color
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter

data class TrendPoint(
    val index: Float,
    val value: Float,
    val label: String
)

@Composable
fun SpendingTrendChart(
    dataPoints: List<TrendPoint>,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp),
        factory = { context ->
            LineChart(context).apply {
                description.isEnabled = false
                setTouchEnabled(true)
                setScaleEnabled(false)
                setPinchZoom(false)
                legend.isEnabled = false
                setDrawGridBackground(false)
                setBackgroundColor(Color.TRANSPARENT)

                axisLeft.apply {
                    axisMinimum = 0f
                    textColor = 0xFFCAC4D0.toInt()
                    gridColor = 0xFF3D2E50.toInt()
                }
                axisRight.isEnabled = false

                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    granularity = 1f
                    textColor = 0xFFCAC4D0.toInt()
                    setDrawGridLines(false)
                    labelRotationAngle = -45f
                }
            }
        },
        update = { chart ->
            if (dataPoints.isEmpty()) {
                chart.clear()
                return@AndroidView
            }

            val entries = dataPoints.map { Entry(it.index, it.value) }
            val labels = dataPoints.map { it.label }

            val dataSet = LineDataSet(entries, "Spending").apply {
                mode = LineDataSet.Mode.CUBIC_BEZIER
                color = 0xFFFF5252.toInt()
                lineWidth = 2.5f
                setDrawCircles(true)
                circleRadius = 3f
                setCircleColor(0xFFFF5252.toInt())
                setDrawValues(false)
                setDrawFilled(true)
                fillColor = 0xFFFF5252.toInt()
                fillAlpha = 20
            }

            chart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            chart.data = LineData(dataSet)
            chart.invalidate()
        }
    )
}
