package com.foodario.alerts.domain.usecase

import com.foodario.alerts.domain.repository.AlertSettingsRepository
import com.foodario.catchError
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertIs

class AlertSettingsUseCasesTest {

    @Test
    fun `set lead days rejects out of range`() = runTest {
        val repository = mockk<AlertSettingsRepository>()
        val useCase = SetAlertLeadDaysUseCase(repository)

        assertIs<IllegalArgumentException>(catchError { useCase(SetAlertLeadDaysParams(0)) })
        assertIs<IllegalArgumentException>(catchError { useCase(SetAlertLeadDaysParams(15)) })

        coVerify(exactly = 0) { repository.setLeadDays(any()) }
    }

    @Test
    fun `set lead days accepts range boundaries`() = runTest {
        val repository = mockk<AlertSettingsRepository>()
        coEvery { repository.setLeadDays(any()) } returns Unit
        val useCase = SetAlertLeadDaysUseCase(repository)

        useCase(SetAlertLeadDaysParams(1))
        useCase(SetAlertLeadDaysParams(14))

        coVerify { repository.setLeadDays(1) }
        coVerify { repository.setLeadDays(14) }
    }

    @Test
    fun `set notify hour rejects out of range`() = runTest {
        val repository = mockk<AlertSettingsRepository>()
        val useCase = SetAlertNotifyHourUseCase(repository)

        assertIs<IllegalArgumentException>(catchError { useCase(SetAlertNotifyHourParams(-1)) })
        assertIs<IllegalArgumentException>(catchError { useCase(SetAlertNotifyHourParams(24)) })

        coVerify(exactly = 0) { repository.setNotifyHour(any()) }
    }

    @Test
    fun `set notify hour accepts range boundaries`() = runTest {
        val repository = mockk<AlertSettingsRepository>()
        coEvery { repository.setNotifyHour(any()) } returns Unit
        val useCase = SetAlertNotifyHourUseCase(repository)

        useCase(SetAlertNotifyHourParams(0))
        useCase(SetAlertNotifyHourParams(23))

        coVerify { repository.setNotifyHour(0) }
        coVerify { repository.setNotifyHour(23) }
    }

    @Test
    fun `set notifications enabled delegates to repository`() = runTest {
        val repository = mockk<AlertSettingsRepository>()
        coEvery { repository.setEnabled(any()) } returns Unit
        val useCase = SetAlertNotificationsEnabledUseCase(repository)

        useCase(SetAlertNotificationsEnabledParams(false))

        coVerify { repository.setEnabled(false) }
    }
}
