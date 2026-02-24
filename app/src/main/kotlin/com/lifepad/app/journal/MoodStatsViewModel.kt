package com.lifepad.app.journal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifepad.app.components.MoodDataPoint
import com.lifepad.app.components.TimeOfDayMoodEntry
import com.lifepad.app.data.local.dao.EmotionFrequencyRow
import com.lifepad.app.data.local.dao.MoodDataRow
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
    val weekComparison: PeriodComparison? = null,
    val monthComparison: PeriodComparison? = null,
    val trapFrequency: List<TrapFrequencyRow> = emptyList(),
    val emotionFrequency: List<EmotionFrequencyRow> = emptyList(),
    val isLoading: Boolean = true
)

data class PeriodSummary(
    val averageMood: Double,
    val entryCount: Int,
    val trendData: List<MoodDataPoint>,
    val distribution: Map<Int, Int>,
    val timeOfDay: List<TimeOfDayMoodEntry>
)

data class PeriodComparison(
    val labelCurrent: String,
    val labelPrevious: String,
    val current: PeriodSummary,
    val previous: PeriodSummary
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

            val weekComparison = buildComparison(
                days = 7,
                labelCurrent = "This week",
                labelPrevious = "Last week",
                labelFormat = SimpleDateFormat("E", Locale.getDefault())
            )
            val monthComparison = buildComparison(
                days = 30,
                labelCurrent = "This month",
                labelPrevious = "Last month",
                labelFormat = SimpleDateFormat("M/d", Locale.getDefault())
            )

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
                    weekComparison = weekComparison,
                    monthComparison = monthComparison,
                    trapFrequency = trapFrequency,
                    emotionFrequency = emotionFrequency,
                    isLoading = false
                )
            }
        }
    }

    private suspend fun buildComparison(
        days: Int,
        labelCurrent: String,
        labelPrevious: String,
        labelFormat: SimpleDateFormat
    ): PeriodComparison {
        val dayMillis = 24 * 60 * 60 * 1000L
        val endCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        val currentEnd = endCal.timeInMillis
        val currentStart = currentEnd - (days - 1) * dayMillis
        val previousEnd = currentStart - 1
        val previousStart = previousEnd - (days - 1) * dayMillis

        val currentSummary = buildSummary(currentStart, currentEnd, labelFormat)
        val previousSummary = buildSummary(previousStart, previousEnd, labelFormat)
        return PeriodComparison(
            labelCurrent = labelCurrent,
            labelPrevious = labelPrevious,
            current = currentSummary,
            previous = previousSummary
        )
    }

    private suspend fun buildSummary(
        startDate: Long,
        endDate: Long,
        labelFormat: SimpleDateFormat
    ): PeriodSummary {
        val trend = journalRepository.getMoodTrend(startDate, endDate)
        val dist = journalRepository.getMoodDistribution(startDate, endDate)
        val avgMood = journalRepository.getAverageMood(startDate, endDate) ?: 0.0
        val trendPoints = trend.mapIndexed { index, row ->
            MoodDataPoint(
                dayIndex = index.toFloat(),
                mood = row.mood.toFloat(),
                label = labelFormat.format(Date(row.entryDate))
            )
        }
        val distMap = dist.associate { it.mood to it.count }
        val timeOfDay = buildTimeOfDay(trend)
        return PeriodSummary(
            averageMood = avgMood,
            entryCount = trend.size,
            trendData = trendPoints,
            distribution = distMap,
            timeOfDay = timeOfDay
        )
    }

    private fun buildTimeOfDay(rows: List<MoodDataRow>): List<TimeOfDayMoodEntry> {
        if (rows.isEmpty()) return emptyList()
        val buckets = listOf(
            "Night" to 0..5,
            "Morning" to 6..11,
            "Afternoon" to 12..17,
            "Evening" to 18..23
        )
        val sums = IntArray(buckets.size)
        val counts = IntArray(buckets.size)
        rows.forEach { row ->
            val cal = Calendar.getInstance().apply { timeInMillis = row.entryDate }
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            val index = when (hour) {
                in 0..5 -> 0
                in 6..11 -> 1
                in 12..17 -> 2
                else -> 3
            }
            sums[index] += row.mood
            counts[index] += 1
        }
        return buckets.mapIndexed { index, (label, _) ->
            val avg = if (counts[index] == 0) 0f else sums[index].toFloat() / counts[index]
            TimeOfDayMoodEntry(label = label, averageMood = avg, count = counts[index])
        }
    }
}
