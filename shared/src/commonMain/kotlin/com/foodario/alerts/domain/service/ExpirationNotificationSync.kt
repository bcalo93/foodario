package com.foodario.alerts.domain.service

import com.foodario.alerts.domain.model.AlertSettings
import com.foodario.alerts.domain.model.ExpirationNotification
import com.foodario.alerts.domain.usecase.ObserveAlertSettingsParams
import com.foodario.core.domain.usecase.ObserveUseCase
import com.foodario.inventory.domain.model.FoodItem
import com.foodario.inventory.domain.usecase.ObserveExpiringItemsParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class ExpirationNotificationSync(
    private val observeExpiringItems: ObserveUseCase<ObserveExpiringItemsParams, List<FoodItem>>,
    private val observeAlertSettings: ObserveUseCase<ObserveAlertSettingsParams, AlertSettings>,
    private val scheduler: NotificationScheduler,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    private val nowProvider: () -> Instant = { Clock.System.now() },
    private val timeZone: TimeZone = TimeZone.currentSystemDefault(),
) {

    fun start() {
        scope.launch {
            observeAlertSettings(ObserveAlertSettingsParams)
                .flatMapLatest { settings ->
                    if (!settings.enabled) {
                        flowOf(settings to emptyList())
                    } else {
                        observeExpiringItems(ObserveExpiringItemsParams)
                            .map { items -> settings to items }
                    }
                }
                .collectLatest { (settings, items) ->
                    if (!settings.enabled || items.isEmpty()) {
                        scheduler.cancelAll()
                    } else {
                        scheduler.reconcileExpirations(buildNotifications(items, settings))
                    }
                }
        }
    }

    internal fun buildNotifications(
        items: List<FoodItem>,
        settings: AlertSettings,
    ): List<ExpirationNotification> {
        val now = nowProvider()
        val today = now.toLocalDateTime(timeZone).date
        return items.mapNotNull { item ->
            val expiration = item.expirationDate ?: return@mapNotNull null
            val notifyDate = expiration.minus(settings.leadDays, DateTimeUnit.DAY)
            var trigger = LocalDateTime(notifyDate, LocalTime(settings.notifyHour, 0)).toInstant(timeZone)
            if (trigger <= now) trigger = now + 1.minutes
            ExpirationNotification(
                itemId = item.id,
                itemName = item.name,
                daysLeft = today.daysUntil(expiration),
                triggerAt = trigger,
            )
        }
    }
}
