package com.lifepad.app.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.lifepad.app.ui.theme.HashtagColor

@Composable
fun HashtagChip(
    name: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(HashtagColor.copy(alpha = 0.1f))
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick)
                else Modifier
            )
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = "#$name",
            style = MaterialTheme.typography.labelMedium,
            color = HashtagColor
        )
    }
}
