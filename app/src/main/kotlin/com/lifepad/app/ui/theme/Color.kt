package com.lifepad.app.ui.theme

import androidx.compose.ui.graphics.Color

// ── Core Palette: AMOLED Black + Deep Purple + Matrix Green ──

// Blacks & surfaces
val AmoledBlack = Color(0xFF000000)
val SurfaceDark = Color(0xFF0D0D0D)
val SurfaceElevated = Color(0xFF1A0A2E)    // Dark purple-tinted
val SurfaceElevated2 = Color(0xFF231432)   // Slightly lighter

// Deep Purple range
val DeepPurple = Color(0xFF7B1FA2)
val DeepPurpleLight = Color(0xFFCE93D8)
val DeepPurpleBright = Color(0xFFBB86FC)
val DeepPurpleDark = Color(0xFF4A0072)
val DeepPurpleMuted = Color(0xFF9C27B0)

// Matrix Green range
val MatrixGreen = Color(0xFF00FF41)
val MatrixGreenDim = Color(0xFF00CC33)
val MatrixGreenDark = Color(0xFF003D00)
val MatrixGreenSoft = Color(0xFF69F0AE)

// ── Module-specific colors ──
val NotepadPrimary = DeepPurpleBright
val JournalPrimary = MatrixGreenDim
val FinancePrimary = MatrixGreenSoft

// ── Mood colors (1-10 scale, vibrant on AMOLED) ──
val MoodVeryLow = Color(0xFFFF1744)
val MoodLow = Color(0xFFFF6E40)
val MoodMediumLow = Color(0xFFFFD740)
val MoodMedium = Color(0xFFFFFF00)
val MoodMediumHigh = Color(0xFFB2FF59)
val MoodHigh = Color(0xFF69F0AE)
val MoodVeryHigh = Color(0xFF00E676)

// ── Transaction colors ──
val IncomeColor = MatrixGreen
val ExpenseColor = Color(0xFFFF5252)

// ── Link & tag colors ──
val HashtagColor = MatrixGreenDim
val WikilinkColor = DeepPurpleBright
val WikilinkBrokenColor = Color(0xFFFF5252)
