package com.foodario.alerts.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.foodario.alerts.domain.model.AlertSettings
import com.foodario.alerts.domain.usecase.ObserveAlertSettingsParams
import com.foodario.alerts.domain.usecase.SetAlertLeadDaysParams
import com.foodario.alerts.domain.usecase.SetAlertNotificationsEnabledParams
import com.foodario.alerts.domain.usecase.SetAlertNotifyHourParams
import com.foodario.core.domain.usecase.ObserveUseCase
import com.foodario.core.domain.usecase.UseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface AlertSettingsEvent {
    data class SetEnabled(val enabled: Boolean) : AlertSettingsEvent
    data class SetLeadDays(val leadDays: Int) : AlertSettingsEvent
    data class SetNotifyHour(val notifyHour: Int) : AlertSettingsEvent
}

class AlertSettingsViewModel(
    private val observeAlertSettings: ObserveUseCase<ObserveAlertSettingsParams, AlertSettings>,
    private val setAlertNotificationsEnabled: UseCase<SetAlertNotificationsEnabledParams, Unit>,
    private val setAlertLeadDays: UseCase<SetAlertLeadDaysParams, Unit>,
    private val setAlertNotifyHour: UseCase<SetAlertNotifyHourParams, Unit>,
) : ViewModel() {

    val uiState: StateFlow<AlertSettingsUiState> =
        observeAlertSettings(ObserveAlertSettingsParams)
            .map {
                AlertSettingsUiState(
                    enabled = it.enabled,
                    leadDays = it.leadDays,
                    notifyHour = it.notifyHour,
                    isLoading = false,
                )
            }
            .catch { e -> emit(AlertSettingsUiState(isLoading = false, error = e.message)) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = AlertSettingsUiState(),
            )

    fun onEvent(event: AlertSettingsEvent) {
        when (event) {
            is AlertSettingsEvent.SetEnabled -> viewModelScope.launch {
                runCatching {
                    setAlertNotificationsEnabled(SetAlertNotificationsEnabledParams(event.enabled))
                }
            }
            is AlertSettingsEvent.SetLeadDays -> viewModelScope.launch {
                runCatching { setAlertLeadDays(SetAlertLeadDaysParams(event.leadDays)) }
            }
            is AlertSettingsEvent.SetNotifyHour -> viewModelScope.launch {
                runCatching { setAlertNotifyHour(SetAlertNotifyHourParams(event.notifyHour)) }
            }
        }
    }
}
