package com.foodario.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.foodario.core.presentation.components.DoodleDivider
import com.foodario.core.presentation.theme.FoodarioTheme
import com.foodario.inventory.presentation.FoodDetailScreen
import com.foodario.inventory.presentation.InventoryScreen
import com.foodario.shoppinglist.presentation.ShoppingListScreen
import kotlinx.serialization.Serializable

@Serializable
data object InventoryRoute

@Serializable
data object ShoppingListRoute

@Serializable
data class FoodDetailRoute(val itemId: Long)

@Composable
fun FoodarioNavHost(navController: NavHostController = rememberNavController()) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val showBottomBar = currentDestination?.hasRoute<InventoryRoute>() == true ||
        currentDestination?.hasRoute<ShoppingListRoute>() == true

    val colors = FoodarioTheme.colors

    Scaffold(
        containerColor = colors.paper,
        bottomBar = {
            if (showBottomBar) {
                FoodarioBottomBar(
                    selected = currentDestination,
                    onInventoryClick = { navController.navigateTopLevel(InventoryRoute) },
                    onShoppingListClick = { navController.navigateTopLevel(ShoppingListRoute) },
                )
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = InventoryRoute,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable<InventoryRoute> {
                InventoryScreen(onItemClick = { itemId -> navController.navigate(FoodDetailRoute(itemId)) })
            }
            composable<ShoppingListRoute> {
                ShoppingListScreen()
            }
            composable<FoodDetailRoute> { entry ->
                val route = entry.toRoute<FoodDetailRoute>()
                FoodDetailScreen(itemId = route.itemId)
            }
        }
    }
}

private fun NavHostController.navigateTopLevel(route: Any) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun FoodarioBottomBar(
    selected: NavDestination?,
    onInventoryClick: () -> Unit,
    onShoppingListClick: () -> Unit,
) {
    val colors = FoodarioTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.paperElevated)
            .navigationBarsPadding(),
    ) {
        DoodleDivider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BottomBarItem(
                emoji = "🧊",
                label = "Heladera",
                selected = selected?.hasRoute<InventoryRoute>() == true,
                onClick = onInventoryClick,
                modifier = Modifier.weight(1f),
            )
            BottomBarItem(
                emoji = "🛒",
                label = "Compras",
                selected = selected?.hasRoute<ShoppingListRoute>() == true,
                onClick = onShoppingListClick,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun BottomBarItem(
    emoji: String,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = FoodarioTheme.colors
    val typography = FoodarioTheme.typography
    val tint = if (selected) colors.penBlue else colors.inkSoft
    Column(
        modifier = modifier
            .fillMaxSize()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = emoji, fontSize = 24.sp)
        Spacer(Modifier.height(2.dp))
        Text(text = label, style = typography.labelHand, color = tint)
    }
}
