package com.foodario.di

import com.foodario.alerts.domain.model.AlertSettings
import com.foodario.alerts.domain.service.ExpirationNotificationSync
import com.foodario.alerts.domain.usecase.ObserveAlertSettingsParams
import com.foodario.alerts.domain.usecase.ObserveAlertSettingsUseCase
import com.foodario.alerts.domain.usecase.SetAlertLeadDaysParams
import com.foodario.alerts.domain.usecase.SetAlertLeadDaysUseCase
import com.foodario.alerts.domain.usecase.SetAlertNotificationsEnabledParams
import com.foodario.alerts.domain.usecase.SetAlertNotificationsEnabledUseCase
import com.foodario.alerts.domain.usecase.SetAlertNotifyHourParams
import com.foodario.alerts.domain.usecase.SetAlertNotifyHourUseCase
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
import com.foodario.inventory.domain.usecase.ObserveExpiringItemsParams
import com.foodario.inventory.domain.usecase.ObserveExpiringItemsUseCase
import com.foodario.inventory.domain.usecase.ObserveFoodItemUseCase
import com.foodario.inventory.domain.usecase.ToggleFrozenParams
import com.foodario.inventory.domain.usecase.ToggleFrozenUseCase
import com.foodario.inventory.domain.usecase.UpdateExpirationParams
import com.foodario.inventory.domain.usecase.UpdateExpirationUseCase
import com.foodario.inventory.domain.usecase.UpdateCategoryParams
import com.foodario.inventory.domain.usecase.UpdateCategoryUseCase
import com.foodario.inventory.domain.usecase.UpdateQuantityParams
import com.foodario.inventory.domain.usecase.UpdateQuantityUseCase
import com.foodario.inventory.domain.usecase.UpdateUnitParams
import com.foodario.inventory.domain.usecase.UpdateUnitUseCase
import com.foodario.shoppinglist.domain.model.ShoppingItem
import com.foodario.shoppinglist.domain.usecase.AddToShoppingListParams
import com.foodario.shoppinglist.domain.usecase.AddToShoppingListUseCase
import com.foodario.shoppinglist.domain.usecase.MoveToInventoryParams
import com.foodario.shoppinglist.domain.usecase.MoveToInventoryUseCase
import com.foodario.shoppinglist.domain.usecase.ObserveShoppingListParams
import com.foodario.shoppinglist.domain.usecase.ObserveShoppingListUseCase
import com.foodario.shoppinglist.domain.usecase.RemoveFromShoppingListParams
import com.foodario.shoppinglist.domain.usecase.RemoveFromShoppingListUseCase
import com.foodario.shoppinglist.domain.usecase.UpdateShoppingItemQuantityParams
import com.foodario.shoppinglist.domain.usecase.UpdateShoppingItemQuantityUseCase
import org.koin.core.qualifier.named
import org.koin.dsl.module

val domainModule = module {
    factory<ObserveUseCase<ObserveInventoryParams, List<FoodItem>>>(named("observeInventory")) { ObserveInventoryUseCase(get()) }
    factory<UseCase<AddFoodItemParams, FoodItem>>(named("addFoodItem")) { AddFoodItemUseCase(get()) }
    factory<UseCase<UpdateQuantityParams, Unit>>(named("updateQuantity")) { UpdateQuantityUseCase(get()) }
    factory<UseCase<UpdateCategoryParams, Unit>>(named("updateCategory")) { UpdateCategoryUseCase(get()) }
    factory<UseCase<UpdateUnitParams, Unit>>(named("updateUnit")) { UpdateUnitUseCase(get()) }
    factory<UseCase<ConsumeFoodItemParams, Unit>>(named("consumeFoodItem")) { ConsumeFoodItemUseCase(get()) }
    factory<UseCase<DeleteFoodItemParams, Unit>>(named("deleteFoodItem")) { DeleteFoodItemUseCase(get()) }
    factory<UseCase<ToggleFrozenParams, Unit>>(named("toggleFrozen")) { ToggleFrozenUseCase(get()) }
    factory<UseCase<UpdateExpirationParams, Unit>>(named("updateExpiration")) { UpdateExpirationUseCase(get()) }
    factory<ObserveUseCase<ObserveFoodItemParams, FoodItem?>>(named("observeFoodItem")) { ObserveFoodItemUseCase(get()) }
    factory<ObserveUseCase<ObserveShoppingListParams, List<ShoppingItem>>>(named("observeShoppingList")) { ObserveShoppingListUseCase(get()) }
    factory<UseCase<AddToShoppingListParams, Unit>>(named("addToShoppingList")) { AddToShoppingListUseCase(get()) }
    factory<UseCase<UpdateShoppingItemQuantityParams, Unit>>(named("updateShoppingItemQuantity")) { UpdateShoppingItemQuantityUseCase(get()) }
    factory<UseCase<RemoveFromShoppingListParams, Unit>>(named("removeFromShoppingList")) { RemoveFromShoppingListUseCase(get()) }
    factory<UseCase<MoveToInventoryParams, FoodItem>>(named("moveToInventory")) { MoveToInventoryUseCase(get(), get()) }
    factory<ObserveUseCase<ObserveExpiringItemsParams, List<FoodItem>>>(named("observeExpiringItems")) {
        ObserveExpiringItemsUseCase(get())
    }
    factory<ObserveUseCase<ObserveAlertSettingsParams, AlertSettings>>(named("observeAlertSettings")) {
        ObserveAlertSettingsUseCase(get())
    }
    factory<UseCase<SetAlertNotificationsEnabledParams, Unit>>(named("setAlertNotificationsEnabled")) {
        SetAlertNotificationsEnabledUseCase(get())
    }
    factory<UseCase<SetAlertLeadDaysParams, Unit>>(named("setAlertLeadDays")) {
        SetAlertLeadDaysUseCase(get())
    }
    factory<UseCase<SetAlertNotifyHourParams, Unit>>(named("setAlertNotifyHour")) {
        SetAlertNotifyHourUseCase(get())
    }
    single {
        ExpirationNotificationSync(
            observeExpiringItems = get(named("observeExpiringItems")),
            observeAlertSettings = get(named("observeAlertSettings")),
            scheduler = get(),
        )
    }
}
