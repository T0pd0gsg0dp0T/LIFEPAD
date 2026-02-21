package com.lifepad.app.domain.cbt

data class EmotionRating(
    val name: String,
    val intensity: Int // 0-100
)

object EmotionPresets {
    val COMMON_EMOTIONS = listOf(
        "Anxious",
        "Sad",
        "Angry",
        "Fearful",
        "Guilty",
        "Ashamed",
        "Frustrated",
        "Hopeless",
        "Lonely",
        "Disgusted",
        "Happy",
        "Calm",
        "Grateful",
        "Confident",
        "Excited"
    )
}
