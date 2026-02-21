package com.lifepad.app.finance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifepad.app.data.local.entity.AssetEntity
import com.lifepad.app.data.local.entity.NetWorthSnapshotEntity
import com.lifepad.app.data.repository.NetWorthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NetWorthUiState(
    val assets: List<AssetEntity> = emptyList(),
    val liabilities: List<AssetEntity> = emptyList(),
    val accountsTotal: Double = 0.0,
    val assetsTotal: Double = 0.0,
    val liabilitiesTotal: Double = 0.0,
    val currentNetWorth: Double = 0.0,
    val previousNetWorth: Double? = null,
    val snapshots: List<NetWorthSnapshotEntity> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

@HiltViewModel
class NetWorthViewModel @Inject constructor(
    private val netWorthRepository: NetWorthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NetWorthUiState())
    val uiState: StateFlow<NetWorthUiState> = _uiState.asStateFlow()

    init {
        // Take monthly snapshot if needed
        viewModelScope.launch {
            try {
                netWorthRepository.takeMonthlySnapshotIfNeeded()
            } catch (_: Exception) { }
        }

        // Collect assets
        viewModelScope.launch {
            netWorthRepository.getAssets().collect { assets ->
                _uiState.update { it.copy(assets = assets) }
            }
        }

        // Collect liabilities
        viewModelScope.launch {
            netWorthRepository.getLiabilities().collect { liabilities ->
                _uiState.update { it.copy(liabilities = liabilities) }
            }
        }

        // Collect totals
        viewModelScope.launch {
            netWorthRepository.getTotalAssetsValue().collect { total ->
                _uiState.update { it.copy(assetsTotal = total, isLoading = false) }
            }
        }

        viewModelScope.launch {
            netWorthRepository.getTotalLiabilitiesValue().collect { total ->
                _uiState.update { it.copy(liabilitiesTotal = total) }
            }
        }

        // Collect net worth
        viewModelScope.launch {
            netWorthRepository.getCurrentNetWorth().collect { netWorth ->
                _uiState.update { it.copy(currentNetWorth = netWorth) }
            }
        }

        // Collect snapshots
        viewModelScope.launch {
            netWorthRepository.getAllSnapshots().collect { snapshots ->
                val previous = if (snapshots.size >= 2) snapshots[snapshots.size - 2].netWorth else null
                _uiState.update { it.copy(snapshots = snapshots, previousNetWorth = previous) }
            }
        }
    }

    fun deleteAsset(id: Long) {
        viewModelScope.launch {
            try {
                netWorthRepository.deleteAsset(id)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to delete: ${e.message}") }
            }
        }
    }

    fun refreshSnapshot() {
        viewModelScope.launch {
            try {
                netWorthRepository.takeMonthlySnapshotIfNeeded()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to snapshot: ${e.message}") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
