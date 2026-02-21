package com.lifepad.app.security

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class PinUiState(
    val pin: String = "",
    val confirmPin: String = "",
    val isConfirmStep: Boolean = false,
    val errorMessage: String? = null,
    val attemptsRemaining: Int = 5,
    val isLocked: Boolean = false
)

@HiltViewModel
class PinViewModel @Inject constructor(
    private val securityManager: SecurityManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(PinUiState())
    val uiState: StateFlow<PinUiState> = _uiState.asStateFlow()

    val isPinSet: Boolean get() = securityManager.isPinSet()

    init {
        // Restore persistent lockout state on every launch
        val locked = securityManager.isLockedOut()
        val remaining = securityManager.getAttemptsRemaining()
        if (locked || remaining <= 0) {
            _uiState.update {
                it.copy(isLocked = true, attemptsRemaining = 0, errorMessage = buildLockoutMessage())
            }
        } else {
            _uiState.update { it.copy(attemptsRemaining = remaining) }
        }
    }

    private fun buildLockoutMessage(): String {
        val remainingMs = securityManager.getLockoutRemainingMs()
        return if (remainingMs > 0) {
            val minutes = ((remainingMs + 59_999L) / 60_000L)
            "Too many attempts. Try again in ${minutes}min."
        } else {
            "Too many attempts. Restart app to try again."
        }
    }

    fun onDigitEntered(digit: String) {
        _uiState.update { state ->
            val currentPin = if (state.isConfirmStep) state.confirmPin else state.pin
            if (currentPin.length >= 6) return@update state
            val newPin = currentPin + digit
            if (state.isConfirmStep) {
                state.copy(confirmPin = newPin, errorMessage = null)
            } else {
                state.copy(pin = newPin, errorMessage = null)
            }
        }
    }

    fun onBackspace() {
        _uiState.update { state ->
            if (state.isConfirmStep) {
                state.copy(confirmPin = state.confirmPin.dropLast(1), errorMessage = null)
            } else {
                state.copy(pin = state.pin.dropLast(1), errorMessage = null)
            }
        }
    }

    fun onClear() {
        _uiState.update { state ->
            if (state.isConfirmStep) {
                state.copy(confirmPin = "", errorMessage = null)
            } else {
                state.copy(pin = "", errorMessage = null)
            }
        }
    }

    fun clearPin() {
        _uiState.update {
            it.copy(pin = "", confirmPin = "")
        }
    }

    // For PIN setup: move to confirm step
    fun onSetupSubmit(): Boolean {
        val state = _uiState.value
        if (!state.isConfirmStep) {
            if (state.pin.length < 4) {
                _uiState.update { it.copy(errorMessage = "PIN must be at least 4 digits") }
                return false
            }
            _uiState.update { it.copy(isConfirmStep = true, errorMessage = null) }
            return false
        } else {
            if (state.pin != state.confirmPin) {
                _uiState.update {
                    it.copy(confirmPin = "", errorMessage = "PINs don't match. Try again.")
                }
                return false
            }
            securityManager.setPin(state.pin)
            return true
        }
    }

    // For PIN unlock: verify
    fun onUnlockSubmit(): Boolean {
        val state = _uiState.value
        if (state.isLocked) return false
        if (state.pin.length < 4) {
            _uiState.update { it.copy(errorMessage = "Enter your PIN") }
            return false
        }
        if (securityManager.verifyPin(state.pin)) {
            securityManager.resetFailedAttempts()
            return true
        } else {
            val remaining = securityManager.recordFailedAttempt()
            val locked = remaining <= 0
            _uiState.update {
                it.copy(
                    pin = "",
                    attemptsRemaining = remaining,
                    isLocked = locked,
                    errorMessage = if (locked) buildLockoutMessage() else "Wrong PIN. $remaining attempts left."
                )
            }
            return false
        }
    }

    fun onSkipSetup() {
        // Skip is session-only — the setup dialog will reappear on next launch
        // until the user actually creates a PIN.
    }

    fun resetState() {
        _uiState.value = PinUiState()
    }
}
