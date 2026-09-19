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
import org.koin.dsl.module

val domainModule = module {
    factory<ObserveUseCase<ObserveInventoryParams, List<FoodItem>>> { ObserveInventoryUseCase(get()) }
    factory<UseCase<AddFoodItemParams, FoodItem>> { AddFoodItemUseCase(get()) }
    factory<UseCase<UpdateQuantityParams, Unit>> { UpdateQuantityUseCase(get()) }
    factory<UseCase<ConsumeFoodItemParams, Unit>> { ConsumeFoodItemUseCase(get()) }
    factory<UseCase<DeleteFoodItemParams, Unit>> { DeleteFoodItemUseCase(get()) }
    factory<UseCase<ToggleFrozenParams, Unit>> { ToggleFrozenUseCase(get()) }
    factory<UseCase<UpdateExpirationParams, Unit>> { UpdateExpirationUseCase(get()) }
    factory<ObserveUseCase<ObserveFoodItemParams, FoodItem?>> { ObserveFoodItemUseCase(get()) }
    factory<ObserveUseCase<ObserveShoppingListParams, List<ShoppingItem>>> { ObserveShoppingListUseCase(get()) }
    factory<UseCase<AddToShoppingListParams, Unit>> { AddToShoppingListUseCase(get()) }
    factory<UseCase<RemoveFromShoppingListParams, Unit>> { RemoveFromShoppingListUseCase(get()) }
    factory<UseCase<MoveToInventoryParams, FoodItem>> { MoveToInventoryUseCase(get(), get()) }
}
