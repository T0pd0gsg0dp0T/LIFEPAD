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
import kotlinx.coroutines.flow.map
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
    val weekComparison: PeriodComparison? = null,
    val monthComparison: PeriodComparison? = null,
    val recentNoteId: Long? = null,
    val recentNoteTitle: String? = null,
    val netBalance: Double = 0.0,
    val safeToSpendDaily: Double = 0.0,
    val categorySpendingData: List<Pair<String, Float>> = emptyList(),
    val spendingTrendPoints: List<com.lifepad.app.components.TrendPoint> = emptyList(),
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

    private data class DashboardInputs(
        val notes: List<com.lifepad.app.data.local.entity.NoteEntity>,
        val entries: List<com.lifepad.app.data.local.entity.JournalEntryEntity>,
        val transactions: List<com.lifepad.app.data.local.entity.TransactionEntity>,
        val balance: Double,
        val bills: List<com.lifepad.app.data.local.entity.RecurringBillEntity>,
        val netWorthSnapshots: List<NetWorthSnapshotEntity>,
        val financeWidget: FinanceWidget,
        val moodWidget: MoodWidget,
        val moodWidgetPeriod: MoodWidgetPeriod,
        val emotionFrequency: List<EmotionFrequencyRow>,
        val trapFrequency: List<TrapFrequencyRow>,
        val safeToSpend: Double = 0.0,
        val categories: List<com.lifepad.app.data.local.entity.CategoryEntity> = emptyList()
    )

    private val dashboardInputs: StateFlow<DashboardInputs> = combine(
        noteRepository.getAllNotes(),
        journalRepository.getAllEntries(),
        financeRepository.getAllTransactions(),
        financeRepository.getNetBalance(),
        recurringBillRepository.getConfirmedBills()
    ) { notes, entries, transactions, balance, bills ->
        DashboardInputs(
            notes = notes,
            entries = entries,
            transactions = transactions,
            balance = balance,
            bills = bills,
            netWorthSnapshots = emptyList(),
            financeWidget = FinanceWidget.INCOME_EXPENSE,
            moodWidget = MoodWidget.MOOD_LINE,
            moodWidgetPeriod = MoodWidgetPeriod.MONTH,
            emotionFrequency = emptyList(),
            trapFrequency = emptyList()
        )
    }.combine(
        netWorthRepository.getAllSnapshots()
    ) { inputs, netWorthSnapshots ->
        inputs.copy(netWorthSnapshots = netWorthSnapshots)
    }.combine(
        settingsRepository.financeWidget
    ) { inputs, financeWidget ->
        inputs.copy(financeWidget = financeWidget)
    }.combine(
        settingsRepository.moodWidget
    ) { inputs, moodWidget ->
        inputs.copy(moodWidget = moodWidget)
    }.combine(
        settingsRepository.moodWidgetPeriod
    ) { inputs, moodWidgetPeriod ->
        inputs.copy(moodWidgetPeriod = moodWidgetPeriod)
    }.combine(
        _emotionFrequency
    ) { inputs, emotionFrequency ->
        inputs.copy(emotionFrequency = emotionFrequency)
    }.combine(
        _trapFrequency
    ) { inputs, trapFrequency ->
        inputs.copy(trapFrequency = trapFrequency)
    }.combine(
        _safeToSpend
    ) { inputs, safeToSpend ->
        inputs.copy(safeToSpend = safeToSpend)
    }.combine(
        financeRepository.getAllCategories()
    ) { inputs, categories ->
        inputs.copy(categories = categories)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardInputs(
            notes = emptyList(),
            entries = emptyList(),
            transactions = emptyList(),
            balance = 0.0,
            bills = emptyList(),
            netWorthSnapshots = emptyList(),
            financeWidget = FinanceWidget.INCOME_EXPENSE,
            moodWidget = MoodWidget.MOOD_LINE,
            moodWidgetPeriod = MoodWidgetPeriod.MONTH,
            emotionFrequency = emptyList(),
            trapFrequency = emptyList(),
            safeToSpend = 0.0,
            categories = emptyList()
        )
    )

    val uiState: StateFlow<DashboardUiState> = dashboardInputs.map { inputs ->
        val notes = inputs.notes
        val entries = inputs.entries
        val transactions = inputs.transactions
        val balance = inputs.balance
        val bills = inputs.bills
        val netWorthSnapshots = inputs.netWorthSnapshots
        val financeWidget = inputs.financeWidget
        val moodWidget = inputs.moodWidget
        val moodWidgetPeriod = inputs.moodWidgetPeriod
        val emotionFrequency = inputs.emotionFrequency
        val trapFrequency = inputs.trapFrequency

        val recentNote = notes.firstOrNull()
        val moodLinePoints = buildMoodLine(entries, moodWidgetPeriod.days)
        val moodCalendarMap = buildMoodCalendar(entries, moodWidgetPeriod.days)
        val moodDistribution = buildMoodDistribution(entries, moodWidgetPeriod.days)
        val moodByTimeOfDay = buildMoodByTimeOfDay(entries, moodWidgetPeriod.days)
        val weekComparison = buildComparison(entries, 7, "This week", "Last week")
        val monthComparison = buildComparison(entries, 30, "This month", "Last month")
        val incomeExpenseData = buildIncomeExpenseData(transactions, 6)
        val cashflowPoints = CashflowForecaster.forecast(
            currentBalance = balance,
            confirmedBills = bills,
            forecastDays = 14
        )
        val categorySpendingData = buildCategorySpending(inputs.transactions, inputs.categories)
        val spendingTrendPoints = buildSpendingTrend(inputs.transactions)

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
            weekComparison = weekComparison,
            monthComparison = monthComparison,
            recentNoteId = recentNote?.id,
            recentNoteTitle = recentNote?.title?.ifBlank { "Untitled" },
            netBalance = balance,
            safeToSpendDaily = inputs.safeToSpend,
            categorySpendingData = categorySpendingData,
            spendingTrendPoints = spendingTrendPoints,
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

    private fun buildComparison(
        entries: List<com.lifepad.app.data.local.entity.JournalEntryEntity>,
        days: Int,
        labelCurrent: String,
        labelPrevious: String
    ): PeriodComparison? {
        if (entries.isEmpty()) return null
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

        val currentSummary = buildSummary(entries, currentStart, currentEnd, days)
        val previousSummary = buildSummary(entries, previousStart, previousEnd, days)
        return PeriodComparison(
            labelCurrent = labelCurrent,
            labelPrevious = labelPrevious,
            current = currentSummary,
            previous = previousSummary
        )
    }

    private fun buildSummary(
        entries: List<com.lifepad.app.data.local.entity.JournalEntryEntity>,
        start: Long,
        end: Long,
        days: Int
    ): PeriodSummary {
        val rangeEntries = entries.filter { it.entryDate in start..end }
        val avg = if (rangeEntries.isEmpty()) 0.0 else rangeEntries.map { it.mood }.average()
        val trend = buildMoodLine(rangeEntries, days)
        val dist = rangeEntries.groupingBy { it.mood }.eachCount()
        val timeOfDay = buildMoodByTimeOfDay(rangeEntries, days)
        return PeriodSummary(
            averageMood = avg,
            entryCount = rangeEntries.size,
            trendData = trend,
            distribution = dist,
            timeOfDay = timeOfDay
        )
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

    private fun buildCategorySpending(
        transactions: List<com.lifepad.app.data.local.entity.TransactionEntity>,
        categories: List<com.lifepad.app.data.local.entity.CategoryEntity>
    ): List<Pair<String, Float>> {
        val cal = Calendar.getInstance()
        val monthEnd = cal.timeInMillis
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val monthStart = cal.timeInMillis

        val expenses = transactions.filter {
            it.type == com.lifepad.app.data.local.entity.TransactionType.EXPENSE &&
            it.transactionDate in monthStart..monthEnd
        }
        val total = expenses.sumOf { it.amount }
        if (total == 0.0) return emptyList()

        return expenses.groupBy { it.categoryId }
            .map { (categoryId, txList) ->
                val name = categories.firstOrNull { it.id == categoryId }?.name ?: "Other"
                val amount = txList.sumOf { it.amount }
                name to (amount / total * 100).toFloat()
            }
            .sortedByDescending { it.second }
            .take(6)
    }

    private fun buildSpendingTrend(
        transactions: List<com.lifepad.app.data.local.entity.TransactionEntity>
    ): List<com.lifepad.app.components.TrendPoint> {
        val dayMillis = 24 * 60 * 60 * 1000L
        val days = 30
        val endCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        val endMs = endCal.timeInMillis
        val startMs = endMs - (days - 1) * dayMillis

        val expenses = transactions.filter {
            it.type == com.lifepad.app.data.local.entity.TransactionType.EXPENSE &&
            it.transactionDate in startMs..endMs
        }
        if (expenses.isEmpty()) return emptyList()

        val byDay = expenses.groupBy { dayStart(it.transactionDate) }
        return (0 until days).mapNotNull { index ->
            val dayMs = startMs + index * dayMillis
            val total = byDay[dayStart(dayMs)]?.sumOf { it.amount }?.toFloat() ?: return@mapNotNull null
            val label = formatShortDay(dayMs)
            com.lifepad.app.components.TrendPoint(index = index.toFloat(), value = total, label = label)
        }
    }

}
