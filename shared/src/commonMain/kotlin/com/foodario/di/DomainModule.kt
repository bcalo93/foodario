package com.foodario.di

import com.foodario.core.domain.usecase.ObserveUseCase
import com.foodario.core.domain.usecase.UseCase
import com.foodario.inventory.domain.model.FoodItem
import com.foodario.inventory.domain.usecase.AddFoodItemParams
import com.foodario.inventory.domain.usecase.AddFoodItemUseCase
import com.foodario.inventory.domain.usecase.ConsumeFoodItemParams
import com.foodario.inventory.domain.usecase.ConsumeFoodItemUseCase
import com.foodario.inventory.domain.usecase.DeleteFoodItemParams
import com.foodario.inventory.domain.usecase.DeleteFoodItemUseCase
import com.foodario.inventory.domain.usecase.ObserveInventoryParams
import com.foodario.inventory.domain.usecase.ObserveInventoryUseCase
import com.foodario.inventory.domain.usecase.ObserveFoodItemParams
import com.foodario.inventory.domain.usecase.ObserveFoodItemUseCase
import com.foodario.inventory.domain.usecase.ToggleFrozenParams
import com.foodario.inventory.domain.usecase.ToggleFrozenUseCase
import com.foodario.inventory.domain.usecase.UpdateExpirationParams
import com.foodario.inventory.domain.usecase.UpdateExpirationUseCase
import com.foodario.inventory.domain.usecase.UpdateQuantityParams
import com.foodario.inventory.domain.usecase.UpdateQuantityUseCase
import com.foodario.shoppinglist.domain.model.ShoppingItem
import com.foodario.shoppinglist.domain.usecase.AddToShoppingListParams
import com.foodario.shoppinglist.domain.usecase.AddToShoppingListUseCase
import com.foodario.shoppinglist.domain.usecase.MoveToInventoryParams
import com.foodario.shoppinglist.domain.usecase.MoveToInventoryUseCase
import com.foodario.shoppinglist.domain.usecase.ObserveShoppingListParams
import com.foodario.shoppinglist.domain.usecase.ObserveShoppingListUseCase
import com.foodario.shoppinglist.domain.usecase.RemoveFromShoppingListParams
import com.foodario.shoppinglist.domain.usecase.RemoveFromShoppingListUseCase
import org.koin.core.qualifier.named
import org.koin.dsl.module

val domainModule = module {
    factory<ObserveUseCase<ObserveInventoryParams, List<FoodItem>>>(named("observeInventory")) { ObserveInventoryUseCase(get()) }
    factory<UseCase<AddFoodItemParams, FoodItem>>(named("addFoodItem")) { AddFoodItemUseCase(get()) }
    factory<UseCase<UpdateQuantityParams, Unit>>(named("updateQuantity")) { UpdateQuantityUseCase(get()) }
    factory<UseCase<ConsumeFoodItemParams, Unit>>(named("consumeFoodItem")) { ConsumeFoodItemUseCase(get()) }
    factory<UseCase<DeleteFoodItemParams, Unit>>(named("deleteFoodItem")) { DeleteFoodItemUseCase(get()) }
    factory<UseCase<ToggleFrozenParams, Unit>>(named("toggleFrozen")) { ToggleFrozenUseCase(get()) }
    factory<UseCase<UpdateExpirationParams, Unit>>(named("updateExpiration")) { UpdateExpirationUseCase(get()) }
    factory<ObserveUseCase<ObserveFoodItemParams, FoodItem?>>(named("observeFoodItem")) { ObserveFoodItemUseCase(get()) }
    factory<ObserveUseCase<ObserveShoppingListParams, List<ShoppingItem>>>(named("observeShoppingList")) { ObserveShoppingListUseCase(get()) }
    factory<UseCase<AddToShoppingListParams, Unit>>(named("addToShoppingList")) { AddToShoppingListUseCase(get()) }
    factory<UseCase<RemoveFromShoppingListParams, Unit>>(named("removeFromShoppingList")) { RemoveFromShoppingListUseCase(get()) }
    factory<UseCase<MoveToInventoryParams, FoodItem>>(named("moveToInventory")) { MoveToInventoryUseCase(get(), get()) }
}
