package com.foodario

import android.app.Application
import com.foodario.alerts.domain.service.ExpirationNotificationSync
import com.foodario.di.initKoin

class FoodarioApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val koinApp = initKoin(this)
        koinApp.koin.get<ExpirationNotificationSync>().start()
    }
}
