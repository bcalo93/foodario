package com.foodario.di

import android.content.Context
import com.foodario.database.DatabaseDriverFactory
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.dsl.module

fun platformModule() = module {
    single { DatabaseDriverFactory(androidContext()) }
}

fun initKoin(context: Context) = startKoin {
    androidContext(context)
    modules(platformModule() + sharedModules())
}
