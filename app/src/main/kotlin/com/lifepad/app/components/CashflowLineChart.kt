package com.lifepad.app.components

import android.graphics.Color
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.lifepad.app.domain.finance.ForecastPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CashflowLineChart(
    forecastPoints: List<ForecastPoint>,
    modifier: Modifier = Modifier
) {
    val dateFormat = SimpleDateFormat("M/d", Locale.US)

    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp),
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
            if (forecastPoints.isEmpty()) {
                chart.clear()
                return@AndroidView
            }

            val entries = forecastPoints.map {
                Entry(it.dayIndex.toFloat(), it.balance.toFloat())
            }
            val labels = forecastPoints.map { dateFormat.format(Date(it.date)) }

            val dataSet = LineDataSet(entries, "Balance").apply {
                mode = LineDataSet.Mode.CUBIC_BEZIER
                color = 0xFFBB86FC.toInt()
                lineWidth = 2.5f
                setDrawCircles(false)
                setDrawValues(false)
                setDrawFilled(true)
                fillColor = 0xFFBB86FC.toInt()
                fillAlpha = 30
            }

            // Add zero line
            chart.axisLeft.removeAllLimitLines()
            chart.axisLeft.addLimitLine(
                LimitLine(0f, "").apply {
                    lineColor = 0xFFFF5252.toInt()
                    lineWidth = 1f
                    enableDashedLine(10f, 10f, 0f)
                }
            )

            // Mark "today" with a vertical line at index 0
            chart.xAxis.removeAllLimitLines()
            chart.xAxis.addLimitLine(
                LimitLine(0f, "Today").apply {
                    lineColor = 0xFF00FF41.toInt()
                    lineWidth = 1f
                    textColor = 0xFF00FF41.toInt()
                    textSize = 10f
                    labelPosition = LimitLine.LimitLabelPosition.RIGHT_TOP
                }
            )

            chart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            chart.data = LineData(dataSet)
            chart.invalidate()
        }
    )
}
