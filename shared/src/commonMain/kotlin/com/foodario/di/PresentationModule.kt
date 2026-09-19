package com.foodario.di

import com.foodario.inventory.presentation.FoodDetailViewModel
import com.foodario.inventory.presentation.InventoryViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val presentationModule = module {
    viewModel { InventoryViewModel(get(), get()) }
    viewModel { params -> FoodDetailViewModel(params.get(), get(), get(), get(), get(), get(), get(), get()) }
}
