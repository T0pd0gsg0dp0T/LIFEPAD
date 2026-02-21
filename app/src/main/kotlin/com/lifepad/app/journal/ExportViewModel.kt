package com.lifepad.app.journal

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifepad.app.data.local.entity.EntryEmotionEntity
import com.lifepad.app.data.local.entity.EntryThinkingTrapEntity
import com.lifepad.app.data.repository.JournalRepository
import com.lifepad.app.domain.export.CsvExporter
import com.lifepad.app.domain.export.ExportableEntry
import com.lifepad.app.domain.export.JsonExporter
import com.lifepad.app.domain.export.JournalExporter
import com.lifepad.app.domain.export.MarkdownExporter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.Calendar
import javax.inject.Inject

enum class ExportFormat(val label: String) {
    JSON("JSON"),
    CSV("CSV"),
    MARKDOWN("Markdown")
}

data class ExportUiState(
    val selectedFormat: ExportFormat = ExportFormat.JSON,
    val startDate: Long = System.currentTimeMillis() - 30L * 86400000L,
    val endDate: Long = System.currentTimeMillis(),
    val isExporting: Boolean = false,
    val entryCount: Int = 0,
    val exportComplete: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ExportViewModel @Inject constructor(
    private val journalRepository: JournalRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExportUiState())
    val uiState: StateFlow<ExportUiState> = _uiState.asStateFlow()

    init {
        loadEntryCount()
    }

    fun onFormatChange(format: ExportFormat) {
        _uiState.update { it.copy(selectedFormat = format) }
    }

    fun onStartDateChange(date: Long) {
        _uiState.update { it.copy(startDate = date) }
        loadEntryCount()
    }

    fun onEndDateChange(date: Long) {
        // Set to end of day
        val cal = Calendar.getInstance().apply {
            timeInMillis = date
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
        }
        _uiState.update { it.copy(endDate = cal.timeInMillis) }
        loadEntryCount()
    }

    private fun loadEntryCount() {
        viewModelScope.launch {
            val state = _uiState.value
            val entries = journalRepository.getEntriesByDateRange(state.startDate, state.endDate).first()
            _uiState.update { it.copy(entryCount = entries.size) }
        }
    }

    fun export(context: Context) {
        // Capture application context immediately to avoid Activity leak
        val appContext = context.applicationContext
        val packageName = appContext.packageName

        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, error = null, exportComplete = false) }

            try {
                val state = _uiState.value
                val entries = journalRepository.getEntriesByDateRange(state.startDate, state.endDate).first()

                val exportableEntries = entries.map { entry ->
                    val emotions: List<EntryEmotionEntity> =
                        journalRepository.getEmotionsForEntry(entry.id).first()
                    val traps: List<EntryThinkingTrapEntity> =
                        journalRepository.getTrapsForEntry(entry.id).first()
                    ExportableEntry(entry = entry, emotions = emotions, traps = traps)
                }

                val exporter: JournalExporter = when (state.selectedFormat) {
                    ExportFormat.JSON -> JsonExporter()
                    ExportFormat.CSV -> CsvExporter()
                    ExportFormat.MARKDOWN -> MarkdownExporter()
                }

                val content = exporter.export(exportableEntries)
                val fileName = "lifepad_export.${exporter.fileExtension}"

                // Write to cache dir for sharing
                val cacheDir = File(appContext.cacheDir, "exports")
                cacheDir.mkdirs()
                val file = File(cacheDir, fileName)
                file.writeText(content)

                val uri = FileProvider.getUriForFile(
                    appContext,
                    "$packageName.fileprovider",
                    file
                )

                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = exporter.mimeType
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "LIFEPAD Journal Export")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

                appContext.startActivity(Intent.createChooser(shareIntent, "Share Journal Export").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })

                _uiState.update { it.copy(isExporting = false, exportComplete = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isExporting = false, error = e.message ?: "Export failed") }
            }
        }
    }
}
