package com.foodario.alerts.domain.model

data class AlertSettings(
    val enabled: Boolean = true,
    val leadDays: Int = DEFAULT_LEAD_DAYS,
    val notifyHour: Int = DEFAULT_NOTIFY_HOUR,
) {
    companion object {
        const val DEFAULT_LEAD_DAYS = 2
        val LEAD_DAYS_RANGE = 1..14
        const val DEFAULT_NOTIFY_HOUR = 9
        val NOTIFY_HOUR_RANGE = 0..23
    }
}
