package com.foodario.inventory.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.foodario.inventory.presentation.uicomponents.InventoryContent
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun InventoryScreen(
    onItemClick: (Long) -> Unit = {},
    viewModel: InventoryViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val quickAddCategory by viewModel.quickAddCategory.collectAsStateWithLifecycle()
    var flight by remember { mutableStateOf<InventoryEffect.ItemAdded?>(null) }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is InventoryEffect.ItemAdded -> flight = effect
            }
        }
    }

    InventoryContent(
        uiState = uiState,
        quickAddCategory = quickAddCategory,
        flight = flight,
        onEvent = viewModel::onEvent,
        onItemClick = onItemClick,
        onFlightFinished = { flight = null },
    )
}
