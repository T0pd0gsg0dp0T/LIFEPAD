package com.lifepad.app.domain.export

import com.lifepad.app.domain.cbt.StructuredDataSerializer
import com.lifepad.app.domain.cbt.ThinkingTrap
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MarkdownExporter : JournalExporter {

    override val fileExtension = "md"
    override val mimeType = "text/markdown"

    private val dateFormat = SimpleDateFormat("EEEE, MMM d, yyyy 'at' HH:mm", Locale.getDefault())

    override fun export(entries: List<ExportableEntry>): String {
        val sb = StringBuilder()
        sb.appendLine("# LIFEPAD Journal Export")
        sb.appendLine()
        sb.appendLine("${entries.size} entries exported on ${dateFormat.format(Date())}")
        sb.appendLine()
        sb.appendLine("---")
        sb.appendLine()

        for (exportable in entries) {
            val entry = exportable.entry
            val date = dateFormat.format(Date(entry.entryDate))
            val templateLabel = entry.template.replace("_", " ")
                .replaceFirstChar { it.uppercase() }

            sb.appendLine("## $date")
            sb.appendLine()
            sb.appendLine("**Template:** $templateLabel | **Mood:** ${entry.mood}/10")
            sb.appendLine()

            // Structured data sections
            if (entry.structuredData.isNotBlank()) {
                when (entry.template) {
                    "thought_record" -> {
                        val data = StructuredDataSerializer.decodeThought(entry.structuredData)
                        if (data.situation.isNotBlank()) {
                            sb.appendLine("### Situation")
                            sb.appendLine(data.situation)
                            sb.appendLine()
                        }
                        if (data.automaticThoughts.isNotBlank()) {
                            sb.appendLine("### Automatic Thoughts")
                            sb.appendLine("${data.automaticThoughts} (belief: ${data.beliefBefore}%)")
                            sb.appendLine()
                        }
                        if (data.evidenceFor.isNotBlank() || data.evidenceAgainst.isNotBlank()) {
                            sb.appendLine("### Evidence")
                            sb.appendLine("**For:** ${data.evidenceFor.ifBlank { "None" }}")
                            sb.appendLine()
                            sb.appendLine("**Against:** ${data.evidenceAgainst.ifBlank { "None" }}")
                            sb.appendLine()
                        }
                        if (data.alternativeThought.isNotBlank()) {
                            sb.appendLine("### Alternative Thought")
                            sb.appendLine("${data.alternativeThought} (belief: ${data.beliefAfter}%)")
                            sb.appendLine()
                        }
                    }
                    "exposure" -> {
                        val data = StructuredDataSerializer.decodeExposure(entry.structuredData)
                        if (data.fearDescription.isNotBlank()) {
                            sb.appendLine("### Fear")
                            sb.appendLine(data.fearDescription)
                            sb.appendLine()
                        }
                        if (data.avoidanceBehavior.isNotBlank()) {
                            sb.appendLine("### Avoidance")
                            sb.appendLine(data.avoidanceBehavior)
                            sb.appendLine()
                        }
                        sb.appendLine("### SUDS Ratings")
                        sb.appendLine("- Before: ${data.sudsBefore}")
                        sb.appendLine("- During: ${data.sudsDuring}")
                        sb.appendLine("- After: ${data.sudsAfter}")
                        sb.appendLine()
                        if (data.exposurePlan.isNotBlank()) {
                            sb.appendLine("### Exposure Plan")
                            sb.appendLine(data.exposurePlan)
                            sb.appendLine()
                        }
                        if (data.reflection.isNotBlank()) {
                            sb.appendLine("### Reflection")
                            sb.appendLine(data.reflection)
                            sb.appendLine()
                        }
                    }
                }
            }

            // Content (for non-structured or as fallback)
            if (entry.content.isNotBlank() && entry.structuredData.isBlank()) {
                sb.appendLine("### Content")
                sb.appendLine(entry.content)
                sb.appendLine()
            }

            // Emotions
            val emotionsBefore = exportable.emotions.filter { it.phase == "before" }
            val emotionsAfter = exportable.emotions.filter { it.phase == "after" }
            if (emotionsBefore.isNotEmpty() || emotionsAfter.isNotEmpty()) {
                sb.appendLine("### Emotions")
                if (emotionsBefore.isNotEmpty()) {
                    sb.appendLine("**Before:** ${emotionsBefore.joinToString(", ") { "${it.emotionName} (${it.intensity}/100)" }}")
                }
                if (emotionsAfter.isNotEmpty()) {
                    sb.appendLine("**After:** ${emotionsAfter.joinToString(", ") { "${it.emotionName} (${it.intensity}/100)" }}")
                }
                sb.appendLine()
            }

            // Thinking traps
            if (exportable.traps.isNotEmpty()) {
                sb.appendLine("### Thinking Traps")
                exportable.traps.forEach { trap ->
                    val displayName = ThinkingTrap.entries.find { it.name == trap.trapType }?.displayName
                        ?: trap.trapType
                    sb.appendLine("- $displayName")
                }
                sb.appendLine()
            }

            sb.appendLine("---")
            sb.appendLine()
        }

        return sb.toString()
    }
}
