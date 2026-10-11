package com.foodario.alerts.presentation

data class AlertSettingsUiState(
    val enabled: Boolean = true,
    val leadDays: Int = 2,
    val notifyHour: Int = 9,
    val isLoading: Boolean = true,
    val error: String? = null,
)
