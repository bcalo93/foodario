package com.foodario.alerts.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.foodario.alerts.domain.model.AlertSettings
import com.foodario.alerts.domain.repository.AlertSettingsRepository
import com.foodario.database.AppSettingsQueries
import com.foodario.database.App_setting
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class AlertSettingsRepositoryImpl(
    private val queries: AppSettingsQueries,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : AlertSettingsRepository {

    override fun observeSettings(): Flow<AlertSettings> =
        queries.selectAll()
            .asFlow()
            .mapToList(ioDispatcher)
            .map { rows -> rows.toSettings() }

    override suspend fun getSettings(): AlertSettings = withContext(ioDispatcher) {
        queries.selectAll().executeAsList().toSettings()
    }

    override suspend fun setEnabled(enabled: Boolean) {
        withContext(ioDispatcher) { queries.upsert(KEY_ENABLED, enabled.toString()) }
    }

    override suspend fun setLeadDays(leadDays: Int) {
        withContext(ioDispatcher) { queries.upsert(KEY_LEAD_DAYS, leadDays.toString()) }
    }

    override suspend fun setNotifyHour(hour: Int) {
        withContext(ioDispatcher) { queries.upsert(KEY_NOTIFY_HOUR, hour.toString()) }
    }

    private fun List<App_setting>.toSettings(): AlertSettings {
        val map = associate { it.key to it.value_ }
        return AlertSettings(
            enabled = map[KEY_ENABLED]?.toBooleanStrictOrNull() ?: true,
            leadDays = map[KEY_LEAD_DAYS]?.toIntOrNull() ?: AlertSettings.DEFAULT_LEAD_DAYS,
            notifyHour = map[KEY_NOTIFY_HOUR]?.toIntOrNull() ?: AlertSettings.DEFAULT_NOTIFY_HOUR,
        )
    }

    private companion object {
        const val KEY_ENABLED = "alerts.enabled"
        const val KEY_LEAD_DAYS = "alerts.leadDays"
        const val KEY_NOTIFY_HOUR = "alerts.notifyHour"
    }
}
