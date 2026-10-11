package com.foodario

import androidx.compose.ui.window.ComposeUIViewController
import com.foodario.alerts.domain.service.ExpirationNotificationSync
import com.foodario.di.initKoin
import org.koin.mp.KoinPlatform
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController {
    initKoin()
    KoinPlatform.getKoin().get<ExpirationNotificationSync>().start()
    return ComposeUIViewController { App() }
}
