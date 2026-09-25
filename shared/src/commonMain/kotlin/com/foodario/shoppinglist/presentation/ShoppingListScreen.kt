package com.foodario.shoppinglist.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.foodario.shoppinglist.presentation.uicomponents.ShoppingListContent
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ShoppingListScreen(
    viewModel: ShoppingListViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val quickAddCategory by viewModel.quickAddCategory.collectAsStateWithLifecycle()

    ShoppingListContent(
        uiState = uiState,
        quickAddCategory = quickAddCategory,
        onEvent = viewModel::onEvent,
    )
}
