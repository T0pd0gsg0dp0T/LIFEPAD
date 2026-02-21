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
import com.lifepad.app.data.local.entity.NetWorthSnapshotEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NetWorthLineChart(
    snapshots: List<NetWorthSnapshotEntity>,
    modifier: Modifier = Modifier
) {
    val dateFormat = SimpleDateFormat("MMM yy", Locale.US)

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
            if (snapshots.isEmpty()) {
                chart.clear()
                return@AndroidView
            }

            val entries = snapshots.mapIndexed { index, snapshot ->
                Entry(index.toFloat(), snapshot.netWorth.toFloat())
            }
            val labels = snapshots.map { dateFormat.format(Date(it.snapshotDate)) }

            val dataSet = LineDataSet(entries, "Net Worth").apply {
                mode = LineDataSet.Mode.CUBIC_BEZIER
                color = 0xFF00FF41.toInt()
                lineWidth = 2.5f
                setDrawCircles(true)
                circleRadius = 3f
                setCircleColor(0xFF00FF41.toInt())
                setDrawValues(false)
                setDrawFilled(true)
                fillColor = 0xFF00FF41.toInt()
                fillAlpha = 20
            }

            chart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            chart.data = LineData(dataSet)
            chart.invalidate()
        }
    )
}
