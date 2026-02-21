package com.lifepad.app.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import kotlin.math.roundToInt

@Composable
fun SudsRatingBar(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val fraction = value / 100f
    val trackColor = sudsColor(fraction)

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "$value / 100",
                style = MaterialTheme.typography.bodyMedium,
                color = trackColor
            )
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.roundToInt()) },
            valueRange = 0f..100f,
            steps = 3, // Creates tick marks at 25, 50, 75
            colors = SliderDefaults.colors(
                thumbColor = trackColor,
                activeTrackColor = trackColor,
                inactiveTrackColor = trackColor.copy(alpha = 0.24f)
            )
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("0", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("25", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("50", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("75", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("100", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun sudsColor(fraction: Float): Color {
    val green = Color(0xFF00FF41)  // Matrix green (low distress)
    val yellow = Color(0xFFFFD740)
    val orange = Color(0xFFFF6E40)
    val red = Color(0xFFFF1744)    // Bright red (high distress)

    return when {
        fraction <= 0.33f -> lerp(green, yellow, fraction / 0.33f)
        fraction <= 0.66f -> lerp(yellow, orange, (fraction - 0.33f) / 0.33f)
        else -> lerp(orange, red, (fraction - 0.66f) / 0.34f)
    }
}
