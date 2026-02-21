package com.lifepad.app.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifepad.app.data.local.entity.JournalEntryEntity
import com.lifepad.app.data.local.entity.NoteEntity
import com.lifepad.app.data.local.entity.TransactionEntity
import com.lifepad.app.data.repository.FinanceRepository
import com.lifepad.app.data.repository.GoalRepository
import com.lifepad.app.data.repository.JournalRepository
import com.lifepad.app.data.repository.NoteRepository
import com.lifepad.app.data.repository.RecurringBillRepository
import com.lifepad.app.domain.finance.SafeToSpendCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class DashboardUiState(
    val recentNotes: List<NoteEntity> = emptyList(),
    val recentEntries: List<JournalEntryEntity> = emptyList(),
    val recentTransactions: List<TransactionEntity> = emptyList(),
    val todayMood: Int? = null,
    val netBalance: Double = 0.0,
    val safeToSpendDaily: Double = 0.0,
    val noteCount: Int = 0,
    val entryCount: Int = 0,
    val isLoading: Boolean = true
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    private val journalRepository: JournalRepository,
    private val financeRepository: FinanceRepository,
    private val recurringBillRepository: RecurringBillRepository,
    private val goalRepository: GoalRepository
) : ViewModel() {

    private val _safeToSpend = MutableStateFlow(0.0)

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
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        noteRepository.getAllNotes(),
        journalRepository.getAllEntries(),
        financeRepository.getAllTransactions(),
        financeRepository.getNetBalance()
    ) { notes, entries, transactions, balance ->
        // Get today's entries
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val todayStart = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        val todayEnd = calendar.timeInMillis

        val todayEntry = entries.find { it.entryDate in todayStart until todayEnd }

        DashboardUiState(
            recentNotes = notes.take(3),
            recentEntries = entries.take(3),
            recentTransactions = transactions.take(3),
            todayMood = todayEntry?.mood,
            netBalance = balance,
            safeToSpendDaily = _safeToSpend.value,
            noteCount = notes.size,
            entryCount = entries.size,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )
}
