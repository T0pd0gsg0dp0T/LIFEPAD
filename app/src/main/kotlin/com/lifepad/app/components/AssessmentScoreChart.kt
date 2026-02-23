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
import com.lifepad.app.data.local.entity.AssessmentEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AssessmentScoreChart(
    data: List<AssessmentEntity>,
    maxScore: Int,
    modifier: Modifier = Modifier,
    chartColor: Int = 0xFFBB86FC.toInt()
) {
    val dateFormat = SimpleDateFormat("M/d", Locale.getDefault())

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
                    axisMaximum = maxScore.toFloat()
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
            if (data.isEmpty()) {
                chart.clear()
                return@AndroidView
            }

            val entries = data.mapIndexed { index, entity ->
                Entry(index.toFloat(), entity.score.toFloat())
            }
            val labels = data.map { dateFormat.format(Date(it.date)) }

            val dataSet = LineDataSet(entries, "Score").apply {
                mode = LineDataSet.Mode.CUBIC_BEZIER
                color = chartColor
                lineWidth = 2.5f
                setDrawCircles(true)
                circleRadius = 4f
                setCircleColor(chartColor)
                setDrawValues(true)
                valueTextSize = 10f
                valueTextColor = 0xFFCAC4D0.toInt()
                valueFormatter = CompactValueFormatter()
                setDrawFilled(true)
                fillColor = chartColor
                fillAlpha = 20
            }

            chart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            chart.data = LineData(dataSet)
            chart.invalidate()
        }
    )
}
