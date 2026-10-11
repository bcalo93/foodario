package com.foodario.alerts.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.cash.turbine.test
import com.foodario.alerts.domain.model.AlertSettings
import com.foodario.database.FoodarioDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class AlertSettingsRepositoryImplTest {

    private fun inMemoryRepository(): AlertSettingsRepositoryImpl {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        FoodarioDatabase.Schema.create(driver)
        val database = FoodarioDatabase(driver)
        return AlertSettingsRepositoryImpl(database.appSettingsQueries, Dispatchers.Unconfined)
    }

    @Test
    fun `returns defaults when table is empty`() = runTest {
        val repository = inMemoryRepository()

        assertEquals(AlertSettings(), repository.getSettings())
    }

    @Test
    fun `persists enabled, lead days and notify hour`() = runTest {
        val repository = inMemoryRepository()

        repository.setEnabled(false)
        repository.setLeadDays(7)
        repository.setNotifyHour(18)

        assertEquals(AlertSettings(enabled = false, leadDays = 7, notifyHour = 18), repository.getSettings())
    }

    @Test
    fun `observeSettings emits on change`() = runTest {
        val repository = inMemoryRepository()

        repository.observeSettings().test {
            assertEquals(AlertSettings(), awaitItem())
            repository.setLeadDays(5)
            assertEquals(AlertSettings(leadDays = 5), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
