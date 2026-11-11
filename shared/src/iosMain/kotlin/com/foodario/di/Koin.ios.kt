package com.foodario.di

import com.foodario.database.DatabaseDriverFactory
import org.koin.core.context.startKoin
import org.koin.dsl.module

fun platformModule() = module {
    single { DatabaseDriverFactory() }
    single<com.foodario.alerts.domain.service.NotificationScheduler> {
        com.foodario.alerts.IosNotificationScheduler()
    }
    single<com.foodario.scanning.domain.service.ReceiptTextRecognizer> {
        com.foodario.scanning.VisionReceiptTextRecognizer()
    }
}

fun initKoin() = startKoin {
    modules(platformModule() + sharedModules())
}
