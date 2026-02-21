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

data class IncomeExpenseEntry(
    val label: String,
    val income: Float,
    val expense: Float
)

@Composable
fun IncomeExpenseBarChart(
    data: List<IncomeExpenseEntry>,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp),
        factory = { context ->
            BarChart(context).apply {
                description.isEnabled = false
                setTouchEnabled(true)
                setScaleEnabled(false)
                setDrawGridBackground(false)
                setBackgroundColor(Color.TRANSPARENT)
                setFitBars(true)

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
                    setCenterAxisLabels(true)
                }

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

            val incomeEntries = data.mapIndexed { i, entry ->
                BarEntry(i.toFloat(), entry.income)
            }
            val expenseEntries = data.mapIndexed { i, entry ->
                BarEntry(i.toFloat(), entry.expense)
            }

            val incomeDataSet = BarDataSet(incomeEntries, "Income").apply {
                color = 0xFF00FF41.toInt()
                valueTextSize = 9f
                valueTextColor = 0xFFCAC4D0.toInt()
            }
            val expenseDataSet = BarDataSet(expenseEntries, "Expenses").apply {
                color = 0xFFFF5252.toInt()
                valueTextSize = 9f
                valueTextColor = 0xFFCAC4D0.toInt()
            }

            val labels = data.map { it.label }
            chart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            chart.xAxis.axisMinimum = -0.5f
            chart.xAxis.axisMaximum = data.size - 0.5f

            val barWidth = 0.35f
            val groupSpace = 0.1f
            val barSpace = 0.05f

            val barData = BarData(incomeDataSet, expenseDataSet).apply {
                this.barWidth = barWidth
            }

            chart.data = barData
            if (data.size > 1) {
                chart.xAxis.axisMinimum = 0f
                chart.xAxis.axisMaximum = barData.getGroupWidth(groupSpace, barSpace) * data.size
                chart.groupBars(0f, groupSpace, barSpace)
            }
            chart.invalidate()
        }
    )
}
