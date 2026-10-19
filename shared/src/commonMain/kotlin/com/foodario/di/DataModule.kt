package com.foodario.di

import com.foodario.alerts.data.AlertSettingsRepositoryImpl
import com.foodario.alerts.domain.repository.AlertSettingsRepository
import com.foodario.database.DatabaseDriverFactory
import com.foodario.database.FoodarioDatabase
import com.foodario.inventory.data.InventoryRepositoryImpl
import com.foodario.inventory.domain.repository.InventoryRepository
import com.foodario.shoppinglist.data.ShoppingListRepositoryImpl
import com.foodario.shoppinglist.domain.repository.ShoppingListRepository
import org.koin.dsl.module

val dataModule = module {
    single { get<DatabaseDriverFactory>().createDriver() }
    single { FoodarioDatabase(get()) }
    single { get<FoodarioDatabase>().foodItemQueries }
    single { get<FoodarioDatabase>().shoppingItemQueries }
    single { get<FoodarioDatabase>().appSettingsQueries }
    single<InventoryRepository> { InventoryRepositoryImpl(get()) }
    single<ShoppingListRepository> { ShoppingListRepositoryImpl(get()) }
    single<AlertSettingsRepository> { AlertSettingsRepositoryImpl(get()) }
}
