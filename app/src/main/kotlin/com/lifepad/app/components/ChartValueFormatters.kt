package com.lifepad.app.components

import com.github.mikephil.charting.formatter.ValueFormatter
import java.text.NumberFormat
import java.util.Locale

class CompactValueFormatter : ValueFormatter() {
    private val numberFormat: NumberFormat = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
        maximumFractionDigits = 2
        minimumFractionDigits = 0
        isGroupingUsed = true
    }

    override fun getFormattedValue(value: Float): String {
        return numberFormat.format(value.toDouble())
    }
}
