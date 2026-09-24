package com.foodario.di

import com.foodario.inventory.presentation.FoodDetailViewModel
import com.foodario.inventory.presentation.InventoryViewModel
import com.foodario.shoppinglist.presentation.ShoppingListViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

val presentationModule = module {
    viewModel {
        InventoryViewModel(
            observeInventory = get(named("observeInventory")),
            addFoodItem = get(named("addFoodItem")),
            updateQuantity = get(named("updateQuantity")),
            deleteFoodItem = get(named("deleteFoodItem")),
        )
    }
    viewModel { params ->
        FoodDetailViewModel(
            itemId = params.get(),
            observeFoodItem = get(named("observeFoodItem")),
            updateQuantity = get(named("updateQuantity")),
            updateCategory = get(named("updateCategory")),
            updateUnit = get(named("updateUnit")),
            toggleFrozen = get(named("toggleFrozen")),
            updateExpiration = get(named("updateExpiration")),
            deleteFoodItem = get(named("deleteFoodItem")),
            addToShoppingList = get(named("addToShoppingList")),
        )
    }
    viewModel {
        ShoppingListViewModel(
            observeShoppingList = get(named("observeShoppingList")),
            moveToInventory = get(named("moveToInventory")),
            addToShoppingList = get(named("addToShoppingList")),
        )
    }
}
