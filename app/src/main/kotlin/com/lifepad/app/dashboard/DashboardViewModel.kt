package com.lifepad.app.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifepad.app.data.repository.FinanceRepository
import com.lifepad.app.data.repository.GoalRepository
import com.lifepad.app.data.repository.JournalRepository
import com.lifepad.app.data.repository.NoteRepository
import com.lifepad.app.data.repository.NetWorthRepository
import com.lifepad.app.data.repository.RecurringBillRepository
import com.lifepad.app.domain.finance.CashflowForecaster
import com.lifepad.app.domain.finance.SafeToSpendCalculator
import com.lifepad.app.components.MoodDataPoint
import com.lifepad.app.components.TimeOfDayMoodEntry
import com.lifepad.app.components.IncomeExpenseEntry
import com.lifepad.app.data.local.dao.EmotionFrequencyRow
import com.lifepad.app.data.local.dao.TrapFrequencyRow
import com.lifepad.app.data.local.entity.NetWorthSnapshotEntity
import com.lifepad.app.domain.finance.ForecastPoint
import com.lifepad.app.settings.FinanceWidget
import com.lifepad.app.settings.MoodWidget
import com.lifepad.app.settings.MoodWidgetPeriod
import com.lifepad.app.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class DashboardUiState(
    val financeWidget: FinanceWidget = FinanceWidget.INCOME_EXPENSE,
    val moodWidget: MoodWidget = MoodWidget.MOOD_LINE,
    val moodWidgetPeriod: MoodWidgetPeriod = MoodWidgetPeriod.MONTH,
    val incomeExpenseData: List<IncomeExpenseEntry> = emptyList(),
    val cashflowPoints: List<ForecastPoint> = emptyList(),
    val netWorthSnapshots: List<NetWorthSnapshotEntity> = emptyList(),
    val moodLinePoints: List<MoodDataPoint> = emptyList(),
    val moodCalendarMap: Map<Long, Double> = emptyMap(),
    val moodDistribution: Map<Int, Int> = emptyMap(),
    val moodByTimeOfDay: List<TimeOfDayMoodEntry> = emptyList(),
    val emotionFrequency: List<EmotionFrequencyRow> = emptyList(),
    val trapFrequency: List<TrapFrequencyRow> = emptyList(),
    val recentNoteId: Long? = null,
    val recentNoteTitle: String? = null,
    val netBalance: Double = 0.0,
    val safeToSpendDaily: Double = 0.0,
    val isLoading: Boolean = true
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    private val journalRepository: JournalRepository,
    private val financeRepository: FinanceRepository,
    private val recurringBillRepository: RecurringBillRepository,
    private val goalRepository: GoalRepository,
    private val netWorthRepository: NetWorthRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _safeToSpend = MutableStateFlow(0.0)
    private val _emotionFrequency = MutableStateFlow<List<EmotionFrequencyRow>>(emptyList())
    private val _trapFrequency = MutableStateFlow<List<TrapFrequencyRow>>(emptyList())

    init {
        // Compute safe-to-spend in a separate flow to avoid 5-param combine issue
        viewModelScope.launch {
            combine(
                financeRepository.getNetBalance(),
                recurringBillRepository.getConfirmedBills(),
                goalRepository.getTotalMonthlyContributions()
            ) { balance, bills, goals ->
                val cal = Calendar.getInstance()
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                val monthEnd = cal.timeInMillis
                val monthBills = bills.filter { it.nextDueDate <= monthEnd }
                SafeToSpendCalculator.compute(balance, monthBills, goals).dailyAllowance
            }.collect { _safeToSpend.value = it }
        }

        viewModelScope.launch {
            combine(
                settingsRepository.moodWidgetPeriod,
                journalRepository.getAllEntries()
            ) { period, entries -> period to entries }
                .collect { (period, entries) ->
                    if (entries.isEmpty()) {
                        _emotionFrequency.value = emptyList()
                        _trapFrequency.value = emptyList()
                        return@collect
                    }
                    val cal = Calendar.getInstance()
                    val endDate = cal.timeInMillis
                    cal.add(Calendar.DAY_OF_YEAR, -period.days)
                    val startDate = cal.timeInMillis
                    _emotionFrequency.value = journalRepository.getEmotionFrequency(startDate, endDate)
                    _trapFrequency.value = journalRepository.getTrapFrequency(startDate, endDate)
                }
        }
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        listOf(
            noteRepository.getAllNotes(),
            journalRepository.getAllEntries(),
            financeRepository.getAllTransactions(),
            financeRepository.getNetBalance(),
            recurringBillRepository.getConfirmedBills(),
            netWorthRepository.getAllSnapshots(),
            settingsRepository.financeWidget,
            settingsRepository.moodWidget,
            settingsRepository.moodWidgetPeriod,
            _emotionFrequency,
            _trapFrequency
        )
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val notes = values[0] as List<com.lifepad.app.data.local.entity.NoteEntity>
        @Suppress("UNCHECKED_CAST")
        val entries = values[1] as List<com.lifepad.app.data.local.entity.JournalEntryEntity>
        @Suppress("UNCHECKED_CAST")
        val transactions = values[2] as List<com.lifepad.app.data.local.entity.TransactionEntity>
        val balance = values[3] as Double
        @Suppress("UNCHECKED_CAST")
        val bills = values[4] as List<com.lifepad.app.data.local.entity.RecurringBillEntity>
        @Suppress("UNCHECKED_CAST")
        val netWorthSnapshots = values[5] as List<NetWorthSnapshotEntity>
        val financeWidget = values[6] as FinanceWidget
        val moodWidget = values[7] as MoodWidget
        val moodWidgetPeriod = values[8] as MoodWidgetPeriod
        val emotionFrequency = values[9] as List<EmotionFrequencyRow>
        val trapFrequency = values[10] as List<TrapFrequencyRow>

        val recentNote = notes.firstOrNull()
        val moodLinePoints = buildMoodLine(entries, moodWidgetPeriod.days)
        val moodCalendarMap = buildMoodCalendar(entries, moodWidgetPeriod.days)
        val moodDistribution = buildMoodDistribution(entries, moodWidgetPeriod.days)
        val moodByTimeOfDay = buildMoodByTimeOfDay(entries, moodWidgetPeriod.days)
        val incomeExpenseData = buildIncomeExpenseData(transactions, 6)
        val cashflowPoints = CashflowForecaster.forecast(
            currentBalance = balance,
            confirmedBills = bills,
            forecastDays = 14
        )

        DashboardUiState(
            financeWidget = financeWidget,
            moodWidget = moodWidget,
            moodWidgetPeriod = moodWidgetPeriod,
            incomeExpenseData = incomeExpenseData,
            cashflowPoints = cashflowPoints,
            netWorthSnapshots = netWorthSnapshots,
            moodLinePoints = moodLinePoints,
            moodCalendarMap = moodCalendarMap,
            moodDistribution = moodDistribution,
            moodByTimeOfDay = moodByTimeOfDay,
            emotionFrequency = emotionFrequency,
            trapFrequency = trapFrequency,
            recentNoteId = recentNote?.id,
            recentNoteTitle = recentNote?.title?.ifBlank { "Untitled" },
            netBalance = balance,
            safeToSpendDaily = _safeToSpend.value,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )

    private fun buildMoodLine(entries: List<com.lifepad.app.data.local.entity.JournalEntryEntity>, days: Int): List<MoodDataPoint> {
        if (entries.isEmpty()) return emptyList()
        val dayMillis = 24 * 60 * 60 * 1000L
        val endCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startMillis = endCal.timeInMillis - (days - 1) * dayMillis

        val entriesByDay = entries.groupBy { dayStart(it.entryDate) }
        return (0 until days).mapNotNull { index ->
            val dayStart = startMillis + index * dayMillis
            val dayEntries = entriesByDay[dayStart].orEmpty()
            val mood = dayEntries.maxByOrNull { it.entryDate }?.mood
            if (mood != null) {
                val label = formatShortDay(dayStart)
                MoodDataPoint(index.toFloat(), mood.toFloat(), label)
            } else null
        }
    }

    private fun buildMoodCalendar(entries: List<com.lifepad.app.data.local.entity.JournalEntryEntity>, days: Int): Map<Long, Double> {
        val dayMillis = 24 * 60 * 60 * 1000L
        val endCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startMillis = endCal.timeInMillis - (days - 1) * dayMillis
        return entries
            .filter { it.entryDate in startMillis..endCal.timeInMillis + dayMillis }
            .groupBy { dayStart(it.entryDate) }
            .mapValues { (_, dayEntries) ->
                dayEntries.maxByOrNull { it.entryDate }?.mood?.toDouble() ?: 0.0
            }
    }

    private fun buildMoodDistribution(entries: List<com.lifepad.app.data.local.entity.JournalEntryEntity>, days: Int): Map<Int, Int> {
        val dayMillis = 24 * 60 * 60 * 1000L
        val endCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        val startMillis = endCal.timeInMillis - (days - 1) * dayMillis
        return entries
            .filter { it.entryDate in startMillis..endCal.timeInMillis }
            .groupingBy { it.mood }
            .eachCount()
    }

    private fun buildMoodByTimeOfDay(
        entries: List<com.lifepad.app.data.local.entity.JournalEntryEntity>,
        days: Int
    ): List<TimeOfDayMoodEntry> {
        if (entries.isEmpty()) return emptyList()
        val dayMillis = 24 * 60 * 60 * 1000L
        val endCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        val startMillis = endCal.timeInMillis - (days - 1) * dayMillis
        val recentEntries = entries.filter { it.entryDate in startMillis..endCal.timeInMillis }
        if (recentEntries.isEmpty()) return emptyList()
        val buckets = listOf(
            "Night" to 0..5,
            "Morning" to 6..11,
            "Afternoon" to 12..17,
            "Evening" to 18..23
        )
        val sums = IntArray(buckets.size)
        val counts = IntArray(buckets.size)
        recentEntries.forEach { entry ->
            val cal = Calendar.getInstance().apply { timeInMillis = entry.entryDate }
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            val index = when (hour) {
                in 0..5 -> 0
                in 6..11 -> 1
                in 12..17 -> 2
                else -> 3
            }
            sums[index] += entry.mood
            counts[index] += 1
        }
        return buckets.mapIndexed { index, (label, _) ->
            val avg = if (counts[index] == 0) 0f else sums[index].toFloat() / counts[index]
            TimeOfDayMoodEntry(label = label, averageMood = avg, count = counts[index])
        }
    }

    private fun buildIncomeExpenseData(
        transactions: List<com.lifepad.app.data.local.entity.TransactionEntity>,
        months: Int
    ): List<IncomeExpenseEntry> {
        if (transactions.isEmpty()) return emptyList()
        val labels = mutableListOf<IncomeExpenseEntry>()
        val base = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        for (i in (months - 1) downTo 0) {
            val cal = (base.clone() as Calendar).apply { add(Calendar.MONTH, -i) }
            val start = cal.timeInMillis
            cal.add(Calendar.MONTH, 1)
            val end = cal.timeInMillis - 1
            cal.add(Calendar.MONTH, -1)
            val label = cal.getDisplayName(Calendar.MONTH, Calendar.SHORT, java.util.Locale.getDefault()) ?: ""
            val rangeTx = transactions.filter { it.transactionDate in start..end }
            val income = rangeTx.filter { it.type == com.lifepad.app.data.local.entity.TransactionType.INCOME }
                .sumOf { it.amount }.toFloat()
            val expense = rangeTx.filter { it.type == com.lifepad.app.data.local.entity.TransactionType.EXPENSE }
                .sumOf { it.amount }.toFloat()
            labels.add(IncomeExpenseEntry(label, income, expense))
        }
        return labels
    }

    private fun dayStart(timeMs: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = timeMs
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    private fun formatShortDay(timeMs: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = timeMs }
        val day = cal.get(Calendar.DAY_OF_MONTH)
        return day.toString()
    }
}
