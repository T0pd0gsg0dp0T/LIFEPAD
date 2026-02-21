package com.lifepad.app.journal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifepad.app.components.MoodDataPoint
import com.lifepad.app.data.local.dao.EmotionFrequencyRow
import com.lifepad.app.data.local.dao.TrapFrequencyRow
import com.lifepad.app.data.repository.JournalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

enum class MoodStatsPeriod(val days: Int, val label: String) {
    WEEK(7, "Week"),
    MONTH(30, "Month"),
    QUARTER(90, "3 Months")
}

data class MoodStatsUiState(
    val selectedPeriod: MoodStatsPeriod = MoodStatsPeriod.MONTH,
    val averageMood: Double = 0.0,
    val entryCount: Int = 0,
    val trendData: List<MoodDataPoint> = emptyList(),
    val distribution: Map<Int, Int> = emptyMap(),
    val dailyMoodMap: Map<Long, Double> = emptyMap(),
    val totalEntries: Int = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val avgWordsPerEntry: Int = 0,
    val mostCommonMood: Int? = null,
    val topTemplate: String? = null,
    val trapFrequency: List<TrapFrequencyRow> = emptyList(),
    val emotionFrequency: List<EmotionFrequencyRow> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class MoodStatsViewModel @Inject constructor(
    private val journalRepository: JournalRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MoodStatsUiState())
    val uiState: StateFlow<MoodStatsUiState> = _uiState.asStateFlow()

    init {
        loadStats()
    }

    fun onPeriodChange(period: MoodStatsPeriod) {
        _uiState.update { it.copy(selectedPeriod = period) }
        loadStats()
    }

    private fun loadStats() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val period = _uiState.value.selectedPeriod
            val cal = Calendar.getInstance()
            val endDate = cal.timeInMillis
            cal.add(Calendar.DAY_OF_YEAR, -period.days)
            val startDate = cal.timeInMillis

            val trend = journalRepository.getMoodTrend(startDate, endDate)
            val dist = journalRepository.getMoodDistribution(startDate, endDate)
            val avgMood = journalRepository.getAverageMood(startDate, endDate) ?: 0.0

            val dateFormat = SimpleDateFormat("M/d", Locale.getDefault())
            val trendPoints = trend.mapIndexed { index, row ->
                MoodDataPoint(
                    dayIndex = index.toFloat(),
                    mood = row.mood.toFloat(),
                    label = dateFormat.format(Date(row.entryDate))
                )
            }

            val distMap = dist.associate { it.mood to it.count }

            // Daily mood map for calendar heatmap
            val dailyMoods = journalRepository.getDailyMoodMap(startDate, endDate)
            val dailyMoodMap = dailyMoods.associate { it.dayTimestamp to it.avgMood }

            // Overall journaling stats
            val totalEntries = journalRepository.getEntryCount()
            val (currentStreak, longestStreak) = journalRepository.calculateStreaks()
            val avgWords = journalRepository.getAverageWordCount()
            val mostCommonMood = journalRepository.getMostCommonMood()?.mood
            val templateDist = journalRepository.getTemplateDistribution()
            val topTemplate = templateDist.firstOrNull()?.template

            // CBT insights
            val trapFrequency = journalRepository.getTrapFrequency(startDate, endDate)
            val emotionFrequency = journalRepository.getEmotionFrequency(startDate, endDate)

            _uiState.update {
                it.copy(
                    averageMood = avgMood,
                    entryCount = trend.size,
                    trendData = trendPoints,
                    distribution = distMap,
                    dailyMoodMap = dailyMoodMap,
                    totalEntries = totalEntries,
                    currentStreak = currentStreak,
                    longestStreak = longestStreak,
                    avgWordsPerEntry = avgWords,
                    mostCommonMood = mostCommonMood,
                    topTemplate = topTemplate,
                    trapFrequency = trapFrequency,
                    emotionFrequency = emotionFrequency,
                    isLoading = false
                )
            }
        }
    }
}
