package com.foodario.alerts.domain.model

import kotlin.time.Instant

data class ExpirationNotification(
    val itemId: Long,
    val itemName: String,
    val daysLeft: Int,
    val triggerAt: Instant,
)
