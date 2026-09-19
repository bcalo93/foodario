package com.foodario

import androidx.compose.ui.window.ComposeUIViewController
import com.foodario.di.initKoin
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController {
    initKoin()
    return ComposeUIViewController { App() }
}
