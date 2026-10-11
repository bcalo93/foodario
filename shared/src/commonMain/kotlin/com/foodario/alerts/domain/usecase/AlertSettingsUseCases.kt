package com.foodario.alerts.domain.usecase

import com.foodario.alerts.domain.model.AlertSettings
import com.foodario.alerts.domain.repository.AlertSettingsRepository
import com.foodario.core.domain.usecase.ObserveUseCase
import com.foodario.core.domain.usecase.UseCase
import kotlinx.coroutines.flow.Flow

object ObserveAlertSettingsParams

class ObserveAlertSettingsUseCase(
    private val repository: AlertSettingsRepository,
) : ObserveUseCase<ObserveAlertSettingsParams, AlertSettings> {
    override fun invoke(params: ObserveAlertSettingsParams): Flow<AlertSettings> =
        repository.observeSettings()
}

data class SetAlertNotificationsEnabledParams(val enabled: Boolean)

class SetAlertNotificationsEnabledUseCase(
    private val repository: AlertSettingsRepository,
) : UseCase<SetAlertNotificationsEnabledParams, Unit> {
    override suspend fun invoke(params: SetAlertNotificationsEnabledParams) {
        repository.setEnabled(params.enabled)
    }
}

data class SetAlertLeadDaysParams(val leadDays: Int)

class SetAlertLeadDaysUseCase(
    private val repository: AlertSettingsRepository,
) : UseCase<SetAlertLeadDaysParams, Unit> {
    override suspend fun invoke(params: SetAlertLeadDaysParams) {
        require(params.leadDays in AlertSettings.LEAD_DAYS_RANGE) {
            "Los días de anticipación deben estar entre ${AlertSettings.LEAD_DAYS_RANGE.first} y ${AlertSettings.LEAD_DAYS_RANGE.last}"
        }
        repository.setLeadDays(params.leadDays)
    }
}

data class SetAlertNotifyHourParams(val notifyHour: Int)

class SetAlertNotifyHourUseCase(
    private val repository: AlertSettingsRepository,
) : UseCase<SetAlertNotifyHourParams, Unit> {
    override suspend fun invoke(params: SetAlertNotifyHourParams) {
        require(params.notifyHour in AlertSettings.NOTIFY_HOUR_RANGE) {
            "La hora de aviso debe estar entre 0 y 23"
        }
        repository.setNotifyHour(params.notifyHour)
    }
}
