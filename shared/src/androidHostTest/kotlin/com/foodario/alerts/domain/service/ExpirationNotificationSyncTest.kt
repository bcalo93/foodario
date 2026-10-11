package com.foodario.alerts.domain.service

import com.foodario.alerts.domain.model.AlertSettings
import com.foodario.alerts.domain.model.ExpirationNotification
import com.foodario.alerts.domain.usecase.ObserveAlertSettingsParams
import com.foodario.core.domain.usecase.ObserveUseCase
import com.foodario.inventory.domain.model.FoodCategory
import com.foodario.inventory.domain.model.FoodItem
import com.foodario.inventory.domain.model.QuantityUnit
import com.foodario.inventory.domain.usecase.ObserveExpiringItemsParams
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ExpirationNotificationSyncTest {

    private class FakeScheduler : NotificationScheduler {
        val calls = mutableListOf<List<ExpirationNotification>>()
        var cancelCount = 0
        override fun reconcileExpirations(notifications: List<ExpirationNotification>) { calls += notifications }
        override fun cancelAll() { cancelCount++ }
    }

    private val observeExpiringItems = mockk<ObserveUseCase<ObserveExpiringItemsParams, List<FoodItem>>>()
    private val observeAlertSettings = mockk<ObserveUseCase<ObserveAlertSettingsParams, AlertSettings>>()

    private val now: Instant =
        LocalDateTime(LocalDate(2026, 10, 11), LocalTime(0, 0)).toInstant(TimeZone.UTC)

    private fun foodItem(id: Long = 1L, name: String = "Leche", expirationDate: LocalDate? = null) = FoodItem(
        id = id,
        name = name,
        category = FoodCategory.DAIRY,
        quantity = 1.0,
        unit = QuantityUnit.UNIT,
        isFrozen = false,
        expirationDate = expirationDate,
        createdAt = Instant.fromEpochMilliseconds(1_700_000_000_000L),
        updatedAt = Instant.fromEpochMilliseconds(1_700_000_000_000L),
    )

    private fun newSync(
        scheduler: NotificationScheduler,
        scope: CoroutineScope,
        nowProvider: () -> Instant = { now },
    ) = ExpirationNotificationSync(
        observeExpiringItems = observeExpiringItems,
        observeAlertSettings = observeAlertSettings,
        scheduler = scheduler,
        scope = scope,
        nowProvider = nowProvider,
        timeZone = TimeZone.UTC,
    )

    @Test
    fun `disabled settings cancel all notifications`() = runTest {
        val scheduler = FakeScheduler()
        every { observeAlertSettings(ObserveAlertSettingsParams) } returns flowOf(AlertSettings(enabled = false))
        val sync = newSync(scheduler, TestScope(UnconfinedTestDispatcher()))

        sync.start()

        assertEquals(1, scheduler.cancelCount)
        assertTrue(scheduler.calls.isEmpty())
    }

    @Test
    fun `enabled with items schedules at notifyHour leadDays before expiration`() {
        val scheduler = FakeScheduler()
        val sync = newSync(scheduler, TestScope())
        val items = listOf(foodItem(id = 1L, name = "Leche", expirationDate = LocalDate(2026, 10, 20)))

        val at9 = sync.buildNotifications(items, AlertSettings(leadDays = 2, notifyHour = 9))
        val expected9 = LocalDateTime(LocalDate(2026, 10, 18), LocalTime(9, 0)).toInstant(TimeZone.UTC)
        assertEquals(listOf(ExpirationNotification(1L, "Leche", 9, expected9)), at9)

        val at18 = sync.buildNotifications(items, AlertSettings(leadDays = 2, notifyHour = 18))
        val expected18 = LocalDateTime(LocalDate(2026, 10, 18), LocalTime(18, 0)).toInstant(TimeZone.UTC)
        assertEquals(listOf(ExpirationNotification(1L, "Leche", 9, expected18)), at18)
    }

    @Test
    fun `past trigger time is clamped to now`() {
        val scheduler = FakeScheduler()
        val sync = newSync(scheduler, TestScope())
        val items = listOf(foodItem(id = 1L, name = "Yogur", expirationDate = LocalDate(2026, 10, 12)))

        val result = sync.buildNotifications(items, AlertSettings(leadDays = 2, notifyHour = 9))

        val notification = result.single()
        assertTrue(notification.triggerAt > now)
        assertTrue(notification.triggerAt <= now + 2.minutes)
        assertEquals(1, notification.daysLeft)
    }

    @Test
    fun `items without expiration date are ignored`() {
        val scheduler = FakeScheduler()
        val sync = newSync(scheduler, TestScope())
        val items = listOf(foodItem(id = 1L, name = "Sin vencimiento"))

        val result = sync.buildNotifications(items, AlertSettings(leadDays = 2, notifyHour = 9))

        assertTrue(result.isEmpty())
    }
}
