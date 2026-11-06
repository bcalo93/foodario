package com.foodario.scanning.presentation.uicomponents

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.foodario.core.presentation.components.DoodleDivider
import com.foodario.core.presentation.components.notebookMargin
import com.foodario.core.presentation.theme.FoodarioTheme
import com.foodario.scanning.presentation.ReceiptScanEvent
import com.foodario.scanning.presentation.ScannedReviewItem

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReviewStepContent(
    items: List<ScannedReviewItem>,
    onEvent: (ReceiptScanEvent) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = FoodarioTheme.colors
    val typography = FoodarioTheme.typography
    var showDiscardDialog by remember { mutableStateOf(false) }

    Box(modifier = modifier.background(colors.paper)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = FoodarioTheme.dimensions.lg)
                    .padding(top = FoodarioTheme.dimensions.lg),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Revisá tu factura",
                        style = typography.displayHand,
                        color = colors.ink,
                    )
                    Text(
                        text = "Destildá lo que no va a la heladera",
                        style = typography.caption,
                        color = colors.inkSoft,
                    )
                }
                Box(
                    modifier = Modifier
                        .size(FoodarioTheme.dimensions.touchTarget)
                        .clickable(onClick = { showDiscardDialog = true }),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = "✕", fontSize = 28.sp, color = colors.ink)
                }
            }
            Spacer(Modifier.height(FoodarioTheme.dimensions.md))
            DoodleDivider()
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .notebookMargin(),
            ) {
                items(items, key = { it.key }) { item ->
                    ScannedItemRow(
                        item = item,
                        onEvent = onEvent,
                        modifier = Modifier.animateItem(),
                    )
                }
            }
            DoodleDivider()
            FooterCta(
                count = items.count { it.included },
                onClick = { onEvent(ReceiptScanEvent.ConfirmImport) },
            )
        }
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            containerColor = colors.paperElevated,
            title = {
                Text(
                    text = "Se pierden los productos detectados",
                    style = typography.titleHand,
                    color = colors.ink,
                )
            },
            text = {
                Text(
                    text = "Si descartás, vas a tener que escanear de nuevo.",
                    style = typography.body,
                    color = colors.inkSoft,
                )
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text(text = "Seguir revisando", color = colors.penBlue)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardDialog = false
                        onClose()
                    },
                ) {
                    Text(text = "Descartar", color = colors.marginRed)
                }
            },
        )
    }
}

@Composable
private fun FooterCta(
    count: Int,
    onClick: () -> Unit,
) {
    val colors = FoodarioTheme.colors
    val typography = FoodarioTheme.typography
    val enabled = count > 0
    val textColor = if (enabled) colors.penBlue else colors.inkSoft
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.paper)
            .padding(
                horizontal = FoodarioTheme.dimensions.lg,
                vertical = FoodarioTheme.dimensions.sm,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .then(
                    if (enabled) {
                        Modifier
                            .shadow(4.dp, RoundedCornerShape(8.dp))
                            .clickable(onClick = onClick)
                    } else {
                        Modifier
                    }
                )
                .background(colors.paperElevated, RoundedCornerShape(8.dp))
                .padding(
                    horizontal = FoodarioTheme.dimensions.xl,
                    vertical = FoodarioTheme.dimensions.md,
                ),
        ) {
            Text(
                text = "Agregar $count a la heladera",
                style = typography.labelHand,
                color = textColor,
            )
        }
    }
}
