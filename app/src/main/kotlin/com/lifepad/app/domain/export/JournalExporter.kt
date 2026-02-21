package com.lifepad.app.domain.export

import com.lifepad.app.data.local.entity.EntryEmotionEntity
import com.lifepad.app.data.local.entity.EntryThinkingTrapEntity
import com.lifepad.app.data.local.entity.JournalEntryEntity

data class ExportableEntry(
    val entry: JournalEntryEntity,
    val emotions: List<EntryEmotionEntity>,
    val traps: List<EntryThinkingTrapEntity>
)

interface JournalExporter {
    fun export(entries: List<ExportableEntry>): String
    val fileExtension: String
    val mimeType: String
}
