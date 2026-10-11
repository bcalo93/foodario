package com.foodario.alerts.domain.service

import com.foodario.alerts.domain.model.ExpirationNotification

interface NotificationScheduler {
    /** Cancela lo pendiente y programa la lista recibida (lista vacía = cancelar todo). */
    fun reconcileExpirations(notifications: List<ExpirationNotification>)
    fun cancelAll()
}
