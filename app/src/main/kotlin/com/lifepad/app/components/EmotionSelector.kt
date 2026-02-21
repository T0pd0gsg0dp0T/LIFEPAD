package com.lifepad.app.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lifepad.app.domain.cbt.EmotionPresets
import com.lifepad.app.domain.cbt.EmotionRating
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EmotionSelector(
    emotions: List<EmotionRating>,
    onEmotionsChange: (List<EmotionRating>) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Select Emotions"
) {
    var customEmotionText by remember { mutableStateOf("") }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Emotion chips
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            EmotionPresets.COMMON_EMOTIONS.forEach { emotionName ->
                val existing = emotions.find { it.name == emotionName }
                val isSelected = existing != null
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        if (isSelected) {
                            onEmotionsChange(emotions.filter { it.name != emotionName })
                        } else {
                            onEmotionsChange(emotions + EmotionRating(emotionName, 50))
                        }
                    },
                    label = {
                        Text(
                            text = if (isSelected) "$emotionName (${existing!!.intensity})" else emotionName,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                )
            }
            // Show custom emotion chips
            emotions.filter { it.name !in EmotionPresets.COMMON_EMOTIONS }.forEach { custom ->
                FilterChip(
                    selected = true,
                    onClick = {
                        onEmotionsChange(emotions.filter { it.name != custom.name })
                    },
                    label = {
                        Text(
                            text = "${custom.name} (${custom.intensity})",
                            style = MaterialTheme.typography.bodySmall
                        )
                    },
                    trailingIcon = {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Remove",
                            modifier = Modifier.height(14.dp)
                        )
                    }
                )
            }
        }

        // Custom emotion input
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = customEmotionText,
                onValueChange = { customEmotionText = it },
                placeholder = { Text("Custom emotion") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    val trimmed = customEmotionText.trim()
                    if (trimmed.isNotBlank() && emotions.none { it.name.equals(trimmed, ignoreCase = true) }) {
                        onEmotionsChange(emotions + EmotionRating(trimmed, 50))
                        customEmotionText = ""
                    }
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add emotion")
            }
        }

        // Intensity sliders for selected emotions
        AnimatedVisibility(visible = emotions.isNotEmpty()) {
            Column(
                modifier = Modifier.padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                emotions.forEach { emotion ->
                    IntensitySlider(
                        emotionName = emotion.name,
                        intensity = emotion.intensity,
                        onIntensityChange = { newIntensity ->
                            onEmotionsChange(
                                emotions.map {
                                    if (it.name == emotion.name) it.copy(intensity = newIntensity)
                                    else it
                                }
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun IntensitySlider(
    emotionName: String,
    intensity: Int,
    onIntensityChange: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = emotionName,
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "$intensity / 100",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Slider(
            value = intensity.toFloat(),
            onValueChange = { onIntensityChange(it.roundToInt()) },
            valueRange = 0f..100f,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}
