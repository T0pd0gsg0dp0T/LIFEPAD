package com.lifepad.app.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.material3.MaterialTheme

@Composable
fun LifepadBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val gradient = Brush.verticalGradient(
        listOf(
            colors.background,
            colors.surfaceVariant,
            colors.background
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(gradient),
        content = content
    )
}
