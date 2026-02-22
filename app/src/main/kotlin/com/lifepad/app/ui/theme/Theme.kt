package com.lifepad.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LifepadDarkScheme = darkColorScheme(
    // Purple primary
    primary = DeepPurpleBright,
    onPrimary = AmoledBlack,
    primaryContainer = DeepPurpleDark,
    onPrimaryContainer = DeepPurpleLight,

    // Green secondary (matrix accent)
    secondary = MatrixGreenDim,
    onSecondary = AmoledBlack,
    secondaryContainer = MatrixGreenDark,
    onSecondaryContainer = MatrixGreenSoft,

    // Green tertiary (matrix accent)
    tertiary = MatrixGreen,
    onTertiary = AmoledBlack,
    tertiaryContainer = MatrixGreenDark,
    onTertiaryContainer = MatrixGreen,

    // AMOLED black surfaces
    background = AmoledBlack,
    onBackground = Color(0xFFE0E0E0),
    surface = AmoledBlack,
    onSurface = Color(0xFFE0E0E0),
    surfaceVariant = SurfaceElevated,
    onSurfaceVariant = Color(0xFFCAC4D0),

    // Outlines
    outline = Color(0xFF6B6174),
    outlineVariant = Color(0xFF3D2E50),

    // Error
    error = Color(0xFFCF6679),
    onError = AmoledBlack,
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),

    // Inverse
    inverseSurface = Color(0xFFE6E1E5),
    inverseOnSurface = Color(0xFF1C1B1F),
    inversePrimary = DeepPurple,

    // Surface tint
    surfaceTint = DeepPurpleBright
)

@Composable
fun LifepadTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = LifepadDarkScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = LifepadShapes,
        content = content
    )
}
