package com.foodario.alerts

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class ExpirationReminderWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val itemId = inputData.getLong(KEY_ITEM_ID, -1L)
        val itemName = inputData.getString(KEY_ITEM_NAME) ?: return Result.success()
        val daysLeft = inputData.getInt(KEY_DAYS_LEFT, 0)
        if (itemId < 0L) return Result.success()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return Result.success()
        }
        if (!NotificationManagerCompat.from(applicationContext).areNotificationsEnabled()) {
            return Result.success()
        }

        val launchIntent = applicationContext.packageManager
            .getLaunchIntentForPackage(applicationContext.packageName)
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            itemId.toInt(),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val body = when (daysLeft) {
            0 -> "Vence hoy. ¡Usalo en una comida!"
            1 -> "Vence mañana. ¡Usalo en una comida!"
            else -> "Vence en $daysLeft días. ¡Usalo en una comida!"
        }
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(applicationContext.applicationInfo.icon)
            .setContentTitle("¡$itemName está por vencer!")
            .setContentText(body)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(applicationContext).notify(itemId.toInt(), notification)
        return Result.success()
    }

    companion object {
        const val KEY_ITEM_ID = "itemId"
        const val KEY_ITEM_NAME = "itemName"
        const val KEY_DAYS_LEFT = "daysLeft"
        const val CHANNEL_ID = "expirations"
        const val WORK_TAG = "expiration_alert"
        fun workName(itemId: Long) = "exp_alert_$itemId"
    }
}
