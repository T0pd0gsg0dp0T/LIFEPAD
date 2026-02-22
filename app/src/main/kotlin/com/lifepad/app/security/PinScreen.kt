package com.lifepad.app.security

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun PinSetupScreen(
    onPinSet: () -> Unit,
    showSkip: Boolean = true,
    showCancel: Boolean = false,
    onCancel: () -> Unit = {},
    viewModel: PinViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    PinContent(
        title = if (uiState.isConfirmStep) "Confirm your PIN" else "Create a PIN",
        subtitle = if (uiState.isConfirmStep) "Enter your PIN again" else "Choose a 4-6 digit PIN",
        pin = if (uiState.isConfirmStep) uiState.confirmPin else uiState.pin,
        errorMessage = uiState.errorMessage,
        onDigit = viewModel::onDigitEntered,
        onBackspace = viewModel::onBackspace,
        onSubmit = {
            if (viewModel.onSetupSubmit()) {
                onPinSet()
            }
        },
        submitLabel = if (uiState.isConfirmStep) "Confirm" else "Next",
        isSubmitEnabled = if (uiState.isConfirmStep) uiState.confirmPin.length >= 4 else uiState.pin.length >= 4,
        showSkip = showSkip && !uiState.isConfirmStep,
        onSkip = {
            viewModel.onSkipSetup()
            onPinSet()
        },
        showCancel = showCancel,
        onCancel = onCancel
    )
}

@Composable
fun PinLockScreen(
    onUnlocked: () -> Unit,
    showCancel: Boolean = false,
    onCancel: () -> Unit = {},
    viewModel: PinViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.clearPin()
    }

    PinContent(
        title = "LIFEPAD",
        subtitle = "Enter your PIN to unlock",
        pin = uiState.pin,
        errorMessage = uiState.errorMessage,
        onDigit = viewModel::onDigitEntered,
        onBackspace = viewModel::onBackspace,
        onSubmit = {
            if (!uiState.isLocked && viewModel.onUnlockSubmit()) {
                onUnlocked()
            }
        },
        submitLabel = "Unlock",
        isSubmitEnabled = uiState.pin.length >= 4 && !uiState.isLocked,
        showSkip = false,
        onSkip = {},
        showCancel = showCancel,
        onCancel = onCancel
    )
}

@Composable
private fun PinContent(
    title: String,
    subtitle: String,
    pin: String,
    errorMessage: String?,
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    onSubmit: () -> Unit,
    submitLabel: String,
    isSubmitEnabled: Boolean,
    showSkip: Boolean,
    onSkip: () -> Unit,
    showCancel: Boolean,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(32.dp)
            .testTag("screen_pin"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        // PIN dots
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            for (i in 0 until 6) {
                val isFilled = i < pin.length
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .then(
                            if (isFilled)
                                Modifier.background(MaterialTheme.colorScheme.primary)
                            else
                                Modifier.border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Error message
        if (errorMessage != null) {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Number pad
        val buttons = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("", "0", "back")
        )

        buttons.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                row.forEach { key ->
                    when (key) {
                        "" -> Spacer(modifier = Modifier.size(72.dp))
                        "back" -> {
                            IconButton(
                                onClick = onBackspace,
                                modifier = Modifier.size(72.dp)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Backspace,
                                    contentDescription = "Backspace",
                                    tint = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }
                        else -> {
                            Button(
                                onClick = { onDigit(key) },
                                modifier = Modifier.size(72.dp),
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                Text(
                                    text = key,
                                    style = MaterialTheme.typography.headlineSmall
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onSubmit,
            enabled = isSubmitEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text(submitLabel)
        }

        if (showCancel) {
            TextButton(
                onClick = onCancel,
                modifier = Modifier.testTag("pin_cancel")
            ) {
                Text("Cancel")
            }
        }

        if (showSkip) {
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
                onClick = onSkip,
                modifier = Modifier.testTag("pin_skip")
            ) {
                Text("Skip for now")
            }
        }
    }
}
