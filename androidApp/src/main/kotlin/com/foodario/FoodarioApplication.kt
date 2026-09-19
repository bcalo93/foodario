package com.foodario

import android.app.Application
import com.foodario.di.initKoin

class FoodarioApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin(this)
    }
}
