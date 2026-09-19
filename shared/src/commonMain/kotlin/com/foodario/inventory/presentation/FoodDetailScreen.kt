package com.foodario.inventory.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.foodario.core.domain.usecase.ObserveUseCase
import com.foodario.core.domain.usecase.UseCase
import com.foodario.core.presentation.components.DoodleDivider
import com.foodario.core.presentation.components.HandDrawnCheckbox
import com.foodario.core.presentation.components.QuantityStepper
import com.foodario.core.presentation.components.emoji
import com.foodario.core.presentation.components.notebookMargin
import com.foodario.core.presentation.theme.FoodarioTheme
import com.foodario.core.presentation.theme.categoryColor
import com.foodario.inventory.domain.model.FoodCategory
import com.foodario.inventory.domain.model.FoodItem
import com.foodario.inventory.domain.model.QuantityUnit
import com.foodario.inventory.domain.usecase.ObserveFoodItemParams
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun FoodDetailScreen(
    itemId: Long,
    viewModel: FoodDetailViewModel = koinViewModel(parameters = { parametersOf(itemId) }),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = FoodarioTheme.colors
    val typography = FoodarioTheme.typography

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.paper),
    ) {
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
                        .padding(end = 16.dp),
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

            uiState.item == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .notebookMargin()
                        .padding(end = 16.dp),
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
                DetailContent(
                    item = uiState.item!!,
                    onEvent = viewModel::onEvent,
                )
            }
        }
    }
}

@Composable
private fun DetailContent(
    item: FoodItem,
    onEvent: (FoodDetailEvent) -> Unit,
) {
    val colors = FoodarioTheme.colors
    val typography = FoodarioTheme.typography
    var showExpirationDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .notebookMargin()
            .verticalScroll(rememberScrollState())
            .padding(end = 16.dp),
    ) {
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = item.category.emoji, fontSize = 32.sp)
            Spacer(Modifier.width(8.dp))
            Text(
                text = item.category.name,
                style = typography.labelHand,
                color = colors.ink,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(categoryColor(item.category))
                    .padding(horizontal = 4.dp, vertical = 1.dp),
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = item.name,
            style = typography.displayHand,
            color = colors.ink,
        )
        Spacer(Modifier.height(16.dp))
        DoodleDivider()
        Spacer(Modifier.height(16.dp))

        QuantityStepper(
            quantity = item.quantity,
            unit = item.unit,
            onIncrement = { onEvent(FoodDetailEvent.IncrementQuantity) },
            onDecrement = { onEvent(FoodDetailEvent.DecrementQuantity) },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))

        DetailActionButton(
            text = "Consumí 1",
            color = colors.penBlue,
            onClick = { onEvent(FoodDetailEvent.Consume) },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            HandDrawnCheckbox(
                checked = item.isFrozen,
                onCheckedChange = { onEvent(FoodDetailEvent.ToggleFrozen) },
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Congelado",
                style = typography.labelHand,
                color = colors.ink,
            )
        }
        Spacer(Modifier.height(16.dp))
        DoodleDivider()
        Spacer(Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Vencimiento",
                    style = typography.label,
                    color = colors.inkSoft,
                )
                Text(
                    text = item.expirationDate?.let { "Vence: ${formatDate(it)}" } ?: "Sin vencimiento",
                    style = typography.body,
                    color = colors.ink,
                )
            }
            TextButton(onClick = { showExpirationDialog = true }) {
                Text(text = "Editar", color = colors.penBlue)
            }
        }
        Spacer(Modifier.height(16.dp))

        DetailActionButton(
            text = "Agregar a la lista de compras",
            color = colors.penBlue,
            onClick = { onEvent(FoodDetailEvent.AddToShoppingList) },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))

        DetailActionButton(
            text = "Eliminar",
            color = colors.marginRed,
            onClick = { onEvent(FoodDetailEvent.Delete) },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(24.dp))
    }

    if (showExpirationDialog) {
        ExpirationDialog(
            onDismiss = { showExpirationDialog = false },
            onSelect = { date ->
                showExpirationDialog = false
                onEvent(FoodDetailEvent.ExpirationDateChanged(date))
            },
        )
    }
}

@Composable
private fun DetailActionButton(
    text: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(12.dp)
    Text(
        text = text,
        style = FoodarioTheme.typography.titleHand,
        color = color,
        textAlign = TextAlign.Center,
        modifier = modifier
            .clip(shape)
            .border(1.5.dp, color, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
    )
}

@Composable
private fun ExpirationDialog(
    onDismiss: () -> Unit,
    onSelect: (LocalDate?) -> Unit,
) {
    val colors = FoodarioTheme.colors
    val typography = FoodarioTheme.typography
    val options = listOf(
        "Hoy" to 0,
        "+1 día" to 1,
        "+3 días" to 3,
        "+7 días" to 7,
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.paperElevated,
        title = {
            Text(
                text = "Vencimiento",
                style = typography.titleHand,
                color = colors.ink,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                options.forEach { (label, days) ->
                    Text(
                        text = label,
                        style = typography.body,
                        color = colors.ink,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(daysFromToday(days)) }
                            .padding(vertical = 8.dp),
                    )
                }
                Text(
                    text = "Sin vencimiento",
                    style = typography.body,
                    color = colors.marginRed,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(null) }
                        .padding(vertical = 8.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancelar", color = colors.inkSoft)
            }
        },
    )
}

private fun daysFromToday(days: Int): LocalDate =
    LocalDate.fromEpochDays(
        Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date
            .toEpochDays() + days
    )

private fun formatDate(date: LocalDate): String =
    "${date.dayOfMonth.toString().padStart(2, '0')}/${date.monthNumber.toString().padStart(2, '0')}/${date.year}"

private fun <P> unitUseCase(): UseCase<P, Unit> =
    object : UseCase<P, Unit> {
        override suspend fun invoke(params: P) {}
    }

private fun fakeObserveFoodItemUseCase(item: FoodItem): ObserveUseCase<ObserveFoodItemParams, FoodItem?> =
    object : ObserveUseCase<ObserveFoodItemParams, FoodItem?> {
        override fun invoke(params: ObserveFoodItemParams): Flow<FoodItem?> = flowOf(item)
    }

private fun sampleItem() = FoodItem(
    id = 1L,
    name = "Leche",
    category = FoodCategory.DAIRY,
    quantity = 2.0,
    unit = QuantityUnit.UNIT,
    isFrozen = false,
    expirationDate = LocalDate.fromEpochDays(20_000),
    createdAt = Instant.fromEpochMilliseconds(1_700_000_000_000L),
    updatedAt = Instant.fromEpochMilliseconds(1_700_000_000_000L),
)

@Preview
@Composable
private fun FoodDetailScreenLightPreview() {
    FoodarioTheme(darkTheme = false) {
        FoodDetailScreen(
            itemId = 1L,
            viewModel = FoodDetailViewModel(
                itemId = 1L,
                observeFoodItem = fakeObserveFoodItemUseCase(sampleItem()),
                updateQuantity = unitUseCase(),
                consumeFoodItem = unitUseCase(),
                toggleFrozen = unitUseCase(),
                updateExpiration = unitUseCase(),
                deleteFoodItem = unitUseCase(),
                addToShoppingList = unitUseCase(),
            ),
        )
    }
}

@Preview
@Composable
private fun FoodDetailScreenDarkPreview() {
    FoodarioTheme(darkTheme = true) {
        FoodDetailScreen(
            itemId = 1L,
            viewModel = FoodDetailViewModel(
                itemId = 1L,
                observeFoodItem = fakeObserveFoodItemUseCase(sampleItem()),
                updateQuantity = unitUseCase(),
                consumeFoodItem = unitUseCase(),
                toggleFrozen = unitUseCase(),
                updateExpiration = unitUseCase(),
                deleteFoodItem = unitUseCase(),
                addToShoppingList = unitUseCase(),
            ),
        )
    }
}
