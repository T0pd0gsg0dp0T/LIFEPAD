package com.lifepad.app.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lifepad.app.ui.theme.MoodHigh
import com.lifepad.app.ui.theme.MoodLow
import com.lifepad.app.ui.theme.MoodMedium
import com.lifepad.app.ui.theme.MoodMediumHigh
import com.lifepad.app.ui.theme.MoodMediumLow
import com.lifepad.app.ui.theme.MoodVeryHigh
import com.lifepad.app.ui.theme.MoodVeryLow

@Composable
fun MoodIndicator(
    mood: Int,
    modifier: Modifier = Modifier,
    showNumber: Boolean = true
) {
    val color = getMoodColor(mood)
    val emoji = getMoodEmoji(mood)

    Box(
        modifier = modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.2f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (showNumber) mood.toString() else emoji,
            style = MaterialTheme.typography.labelMedium,
            color = color
        )
    }
}

fun getMoodColor(mood: Int): Color {
    return when (mood) {
        1, 2 -> MoodVeryLow
        3 -> MoodLow
        4 -> MoodMediumLow
        5, 6 -> MoodMedium
        7 -> MoodMediumHigh
        8, 9 -> MoodHigh
        10 -> MoodVeryHigh
        else -> MoodMedium
    }
}

fun getMoodEmoji(mood: Int): String {
    return when (mood) {
        1 -> "😢"
        2 -> "😞"
        3 -> "😔"
        4 -> "😕"
        5 -> "😐"
        6 -> "🙂"
        7 -> "😊"
        8 -> "😄"
        9 -> "😁"
        10 -> "🤩"
        else -> "😐"
    }
}
