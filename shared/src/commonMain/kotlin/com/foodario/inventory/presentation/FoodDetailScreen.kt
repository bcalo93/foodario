package com.foodario.inventory.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.foodario.core.presentation.components.notebookMargin
import com.foodario.core.presentation.theme.FoodarioTheme
import com.foodario.inventory.presentation.uicomponents.DeleteConfirmationDialog
import com.foodario.inventory.presentation.uicomponents.FoodDetailContent
import com.foodario.inventory.presentation.uicomponents.RestockDialog
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun FoodDetailScreen(
    itemId: Long,
    onDeleted: () -> Unit = {},
    viewModel: FoodDetailViewModel = koinViewModel(parameters = { parametersOf(itemId) }),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = FoodarioTheme.colors
    val typography = FoodarioTheme.typography
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showRestockDialog by remember { mutableStateOf(false) }
    val item = uiState.item

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                FoodDetailEffect.AddedToShoppingList -> {
                    snackbarHostState.showSnackbar("Agregado a la lista de compras")
                }

                FoodDetailEffect.QuantityDepleted -> showRestockDialog = true

                FoodDetailEffect.Deleted -> onDeleted()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.paper),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .notebookMargin(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = colors.penBlue)
                    }
                }

                uiState.error != null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .notebookMargin()
                            .padding(end = FoodarioTheme.dimensions.lg),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = uiState.error.orEmpty(),
                            style = typography.body,
                            color = colors.marginRed,
                            textAlign = TextAlign.Center,
                        )
                    }
                }

                item == null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .notebookMargin()
                            .padding(end = FoodarioTheme.dimensions.lg),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Este alimento ya no está",
                            style = typography.titleHand,
                            color = colors.inkSoft,
                            textAlign = TextAlign.Center,
                        )
                    }
                }

                else -> {
                    FoodDetailContent(
                        item = item,
                        onEvent = viewModel::onEvent,
                        onDeleteRequest = { showDeleteDialog = true },
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(FoodarioTheme.dimensions.lg),
        )

        if (showDeleteDialog && item != null) {
            DeleteConfirmationDialog(
                itemName = item.name,
                onDismiss = { showDeleteDialog = false },
                onConfirm = {
                    showDeleteDialog = false
                    viewModel.onEvent(FoodDetailEvent.Delete)
                },
            )
        }

        if (showRestockDialog && item != null) {
            RestockDialog(
                itemName = item.name,
                onDismiss = { showRestockDialog = false },
                onAddToShoppingList = {
                    showRestockDialog = false
                    viewModel.onEvent(FoodDetailEvent.AddToShoppingList)
                },
                onDelete = {
                    showRestockDialog = false
                    viewModel.onEvent(FoodDetailEvent.Delete)
                },
            )
        }
    }
}
