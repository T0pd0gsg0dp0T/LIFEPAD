package com.lifepad.app.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun StepperIndicator(
    steps: List<String>,
    currentStep: Int,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val outlineColor = MaterialTheme.colorScheme.outlineVariant
    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary
    val surfaceColor = MaterialTheme.colorScheme.surface

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Circles with connecting lines
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            steps.forEachIndexed { index, _ ->
                val isCompleted = index < currentStep
                val isCurrent = index == currentStep

                if (index > 0) {
                    // Connecting line
                    Canvas(
                        modifier = Modifier
                            .weight(1f)
                            .height(2.dp)
                    ) {
                        drawLine(
                            color = if (index <= currentStep) primaryColor else outlineColor,
                            start = Offset(0f, size.height / 2),
                            end = Offset(size.width, size.height / 2),
                            strokeWidth = 2.dp.toPx()
                        )
                    }
                }

                // Circle
                Canvas(modifier = Modifier.size(28.dp)) {
                    val radius = size.minDimension / 2
                    when {
                        isCompleted -> {
                            // Filled circle with checkmark
                            drawCircle(color = primaryColor, radius = radius)
                            // Simple checkmark
                            val checkPath = androidx.compose.ui.graphics.Path().apply {
                                moveTo(size.width * 0.3f, size.height * 0.5f)
                                lineTo(size.width * 0.45f, size.height * 0.65f)
                                lineTo(size.width * 0.7f, size.height * 0.35f)
                            }
                            drawPath(
                                checkPath,
                                color = onPrimaryColor,
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }
                        isCurrent -> {
                            drawCircle(color = primaryColor, radius = radius)
                            drawCircle(color = surfaceColor, radius = radius * 0.6f)
                            drawCircle(color = primaryColor, radius = radius * 0.35f)
                        }
                        else -> {
                            drawCircle(
                                color = outlineColor,
                                radius = radius,
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }
                    }
                }
            }
        }

        // Labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            steps.forEachIndexed { index, label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (index <= currentStep) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
