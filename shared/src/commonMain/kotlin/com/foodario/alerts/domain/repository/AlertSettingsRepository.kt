package com.foodario.alerts.domain.repository

import com.foodario.alerts.domain.model.AlertSettings
import kotlinx.coroutines.flow.Flow

interface AlertSettingsRepository {
    fun observeSettings(): Flow<AlertSettings>
    suspend fun getSettings(): AlertSettings
    suspend fun setEnabled(enabled: Boolean)
    suspend fun setLeadDays(leadDays: Int)
    suspend fun setNotifyHour(hour: Int)
}
