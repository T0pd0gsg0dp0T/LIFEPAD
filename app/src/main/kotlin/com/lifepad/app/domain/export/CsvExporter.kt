package com.lifepad.app.domain.export

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CsvExporter : JournalExporter {

    override val fileExtension = "csv"
    override val mimeType = "text/csv"

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    override fun export(entries: List<ExportableEntry>): String {
        val sb = StringBuilder()
        sb.appendLine("Date,Template,Mood,Content,Emotions Before,Emotions After,Thinking Traps")

        for (exportable in entries) {
            val date = dateFormat.format(Date(exportable.entry.entryDate))
            val template = exportable.entry.template
            val mood = exportable.entry.mood
            val content = escapeCsv(exportable.entry.content.take(500))

            val emotionsBefore = exportable.emotions
                .filter { it.phase == "before" }
                .joinToString("; ") { "${it.emotionName}(${it.intensity})" }

            val emotionsAfter = exportable.emotions
                .filter { it.phase == "after" }
                .joinToString("; ") { "${it.emotionName}(${it.intensity})" }

            val traps = exportable.traps.joinToString("; ") { it.trapType }

            sb.appendLine("$date,$template,$mood,$content,${escapeCsv(emotionsBefore)},${escapeCsv(emotionsAfter)},${escapeCsv(traps)}")
        }

        return sb.toString()
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(',') || value.contains('"') || value.contains('\n')) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }
}
