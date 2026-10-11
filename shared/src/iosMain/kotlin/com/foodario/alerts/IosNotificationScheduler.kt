package com.foodario.alerts

import com.foodario.alerts.domain.model.ExpirationNotification
import com.foodario.alerts.domain.service.NotificationScheduler
import platform.Foundation.NSDate
import platform.Foundation.date
import platform.Foundation.timeIntervalSince1970
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNTimeIntervalNotificationTrigger
import platform.UserNotifications.UNUserNotificationCenter

class IosNotificationScheduler : NotificationScheduler {

    private val center = UNUserNotificationCenter.currentNotificationCenter()

    override fun reconcileExpirations(notifications: List<ExpirationNotification>) {
        center.getPendingNotificationRequestsWithCompletionHandler { requests ->
            val ours = requests.orEmpty()
                .mapNotNull { (it as? UNNotificationRequest)?.identifier }
                .filter { it.startsWith(IDENTIFIER_PREFIX) }
            center.removePendingNotificationRequestsWithIdentifiers(ours)
            if (notifications.isEmpty()) return@getPendingNotificationRequestsWithCompletionHandler

            center.requestAuthorizationWithOptions(
                UNAuthorizationOptionAlert or UNAuthorizationOptionSound,
            ) { granted, _ ->
                if (!granted) return@requestAuthorizationWithOptions
                val now = NSDate.date().timeIntervalSince1970
                notifications.forEach { notification ->
                    val interval = (notification.triggerAt.epochSeconds - now)
                        .coerceAtLeast(MINIMUM_INTERVAL_SECONDS)
                    val content = UNMutableNotificationContent().apply {
                        setTitle("¡${notification.itemName} está por vencer!")
                        setBody(bodyFor(notification.daysLeft))
                        setSound(UNNotificationSound.defaultSound)
                    }
                    val trigger = UNTimeIntervalNotificationTrigger
                        .triggerWithTimeInterval(interval, repeats = false)
                    val request = UNNotificationRequest.requestWithIdentifier(
                        identifier = "$IDENTIFIER_PREFIX${notification.itemId}",
                        content = content,
                        trigger = trigger,
                    )
                    center.addNotificationRequest(request, null)
                }
            }
        }
    }

    override fun cancelAll() {
        center.getPendingNotificationRequestsWithCompletionHandler { requests ->
            val ours = requests.orEmpty()
                .mapNotNull { (it as? UNNotificationRequest)?.identifier }
                .filter { it.startsWith(IDENTIFIER_PREFIX) }
            center.removePendingNotificationRequestsWithIdentifiers(ours)
        }
    }

    private fun bodyFor(daysLeft: Int): String = when (daysLeft) {
        0 -> "Vence hoy. ¡Usalo en una comida!"
        1 -> "Vence mañana. ¡Usalo en una comida!"
        else -> "Vence en $daysLeft días. ¡Usalo en una comida!"
    }

    private companion object {
        const val IDENTIFIER_PREFIX = "exp_alert_"
        const val MINIMUM_INTERVAL_SECONDS = 60.0
    }
}
