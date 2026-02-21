package com.lifepad.app.domain.export

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Serializable
private data class JsonEntry(
    val id: Long,
    val date: String,
    val template: String,
    val mood: Int,
    val content: String,
    val structuredData: String,
    val isPinned: Boolean,
    val emotions: List<JsonEmotion>,
    val thinkingTraps: List<String>
)

@Serializable
private data class JsonEmotion(
    val name: String,
    val intensity: Int,
    val phase: String
)

class JsonExporter : JournalExporter {

    override val fileExtension = "json"
    override val mimeType = "application/json"

    private val json = Json { prettyPrint = true }
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())

    override fun export(entries: List<ExportableEntry>): String {
        val jsonEntries = entries.map { exportable ->
            JsonEntry(
                id = exportable.entry.id,
                date = dateFormat.format(Date(exportable.entry.entryDate)),
                template = exportable.entry.template,
                mood = exportable.entry.mood,
                content = exportable.entry.content,
                structuredData = exportable.entry.structuredData,
                isPinned = exportable.entry.isPinned,
                emotions = exportable.emotions.map { emotion ->
                    JsonEmotion(
                        name = emotion.emotionName,
                        intensity = emotion.intensity,
                        phase = emotion.phase
                    )
                },
                thinkingTraps = exportable.traps.map { it.trapType }
            )
        }
        return json.encodeToString(jsonEntries)
    }
}
