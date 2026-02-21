package com.lifepad.app.domain.assessment

data class AssessmentQuestion(
    val text: String,
    val options: List<String> = defaultOptions
) {
    companion object {
        val defaultOptions = listOf(
            "Not at all",
            "Several days",
            "More than half the days",
            "Nearly every day"
        )
    }
}

data class SeverityBand(
    val range: IntRange,
    val label: String
)

object AssessmentQuestions {

    val GAD7_QUESTIONS = listOf(
        AssessmentQuestion("Feeling nervous, anxious, or on edge"),
        AssessmentQuestion("Not being able to stop or control worrying"),
        AssessmentQuestion("Worrying too much about different things"),
        AssessmentQuestion("Trouble relaxing"),
        AssessmentQuestion("Being so restless that it's hard to sit still"),
        AssessmentQuestion("Becoming easily annoyed or irritable"),
        AssessmentQuestion("Feeling afraid, as if something awful might happen")
    )

    val PHQ9_QUESTIONS = listOf(
        AssessmentQuestion("Little interest or pleasure in doing things"),
        AssessmentQuestion("Feeling down, depressed, or hopeless"),
        AssessmentQuestion("Trouble falling or staying asleep, or sleeping too much"),
        AssessmentQuestion("Feeling tired or having little energy"),
        AssessmentQuestion("Poor appetite or overeating"),
        AssessmentQuestion("Feeling bad about yourself \u2014 or that you are a failure or have let yourself or your family down"),
        AssessmentQuestion("Trouble concentrating on things, such as reading the newspaper or watching television"),
        AssessmentQuestion("Moving or speaking so slowly that other people could have noticed? Or the opposite \u2014 being so fidgety or restless that you have been moving around a lot more than usual"),
        AssessmentQuestion("Thoughts that you would be better off dead, or thoughts of hurting yourself in some way")
    )

    val GAD7_SEVERITY = listOf(
        SeverityBand(0..4, "Minimal anxiety"),
        SeverityBand(5..9, "Mild anxiety"),
        SeverityBand(10..14, "Moderate anxiety"),
        SeverityBand(15..21, "Severe anxiety")
    )

    val PHQ9_SEVERITY = listOf(
        SeverityBand(0..4, "Minimal depression"),
        SeverityBand(5..9, "Mild depression"),
        SeverityBand(10..14, "Moderate depression"),
        SeverityBand(15..19, "Moderately severe depression"),
        SeverityBand(20..27, "Severe depression")
    )

    fun getQuestions(type: String): List<AssessmentQuestion> = when (type) {
        "GAD7" -> GAD7_QUESTIONS
        "PHQ9" -> PHQ9_QUESTIONS
        else -> emptyList()
    }

    fun getSeverity(type: String, score: Int): String {
        val bands = when (type) {
            "GAD7" -> GAD7_SEVERITY
            "PHQ9" -> PHQ9_SEVERITY
            else -> return "Unknown"
        }
        return bands.find { score in it.range }?.label ?: "Unknown"
    }

    fun getMaxScore(type: String): Int = when (type) {
        "GAD7" -> 21
        "PHQ9" -> 27
        else -> 0
    }

    fun getTitle(type: String): String = when (type) {
        "GAD7" -> "GAD-7 Anxiety Assessment"
        "PHQ9" -> "PHQ-9 Depression Assessment"
        else -> "Assessment"
    }

    fun getShortTitle(type: String): String = when (type) {
        "GAD7" -> "GAD-7"
        "PHQ9" -> "PHQ-9"
        else -> type
    }
}
