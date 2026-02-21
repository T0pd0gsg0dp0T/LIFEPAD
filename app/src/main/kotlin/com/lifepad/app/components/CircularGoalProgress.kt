package com.lifepad.app.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lifepad.app.ui.theme.IncomeColor

@Composable
fun CircularGoalProgress(
    progress: Float,
    size: Dp = 64.dp,
    strokeWidth: Dp = 6.dp,
    showPercentage: Boolean = true,
    modifier: Modifier = Modifier
) {
    val clampedProgress = progress.coerceIn(0f, 1f)
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val progressColor = IncomeColor
    val textColor = MaterialTheme.colorScheme.onSurface

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val strokePx = strokeWidth.toPx()
            val arcSize = Size(
                this.size.width - strokePx,
                this.size.height - strokePx
            )
            val topLeft = Offset(strokePx / 2, strokePx / 2)

            // Background track
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            // Progress arc
            drawArc(
                color = progressColor,
                startAngle = -90f,
                sweepAngle = 360f * clampedProgress,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
        }

        if (showPercentage) {
            Text(
                text = "${(clampedProgress * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = textColor
            )
        }
    }
}
