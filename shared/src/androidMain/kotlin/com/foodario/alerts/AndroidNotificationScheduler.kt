package com.foodario.alerts

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.foodario.alerts.domain.model.ExpirationNotification
import com.foodario.alerts.domain.service.NotificationScheduler
import java.util.concurrent.TimeUnit
import kotlin.time.Clock

class AndroidNotificationScheduler(
    private val context: Context,
) : NotificationScheduler {

    init {
        createChannel()
    }

    override fun reconcileExpirations(notifications: List<ExpirationNotification>) {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelAllWorkByTag(ExpirationReminderWorker.WORK_TAG)
        val now = Clock.System.now()
        notifications.forEach { notification ->
            val delayMillis = (notification.triggerAt - now).inWholeMilliseconds.coerceAtLeast(0)
            val request = OneTimeWorkRequestBuilder<ExpirationReminderWorker>()
                .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                .addTag(ExpirationReminderWorker.WORK_TAG)
                .setInputData(
                    workDataOf(
                        ExpirationReminderWorker.KEY_ITEM_ID to notification.itemId,
                        ExpirationReminderWorker.KEY_ITEM_NAME to notification.itemName,
                        ExpirationReminderWorker.KEY_DAYS_LEFT to notification.daysLeft,
                    )
                )
                .build()
            workManager.enqueueUniqueWork(
                ExpirationReminderWorker.workName(notification.itemId),
                ExistingWorkPolicy.REPLACE,
                request,
            )
        }
    }

    override fun cancelAll() {
        WorkManager.getInstance(context).cancelAllWorkByTag(ExpirationReminderWorker.WORK_TAG)
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                ExpirationReminderWorker.CHANNEL_ID,
                "Vencimientos",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Avisos de productos próximos a vencer"
            }
            context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }
}
