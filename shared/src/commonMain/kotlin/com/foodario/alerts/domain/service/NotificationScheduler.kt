package com.foodario.alerts.domain.service

import com.foodario.alerts.domain.model.ExpirationNotification

interface NotificationScheduler {
    /** Cancels what's pending and schedules the received list (empty list = cancel all). */
    fun reconcileExpirations(notifications: List<ExpirationNotification>)
    fun cancelAll()
}
