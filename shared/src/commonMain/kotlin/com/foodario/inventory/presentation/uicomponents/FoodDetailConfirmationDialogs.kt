package com.foodario.inventory.presentation.uicomponents

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.foodario.core.presentation.theme.FoodarioTheme

@Composable
internal fun DeleteConfirmationDialog(
    itemName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val colors = FoodarioTheme.colors
    val typography = FoodarioTheme.typography

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.paperElevated,
        title = {
            Text(
                text = "¿Eliminar alimento?",
                style = typography.titleHand,
                color = colors.ink,
            )
        },
        text = {
            Text(
                text = "Se va a eliminar \"$itemName\" de tu heladera.",
                style = typography.body,
                color = colors.ink,
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancelar", color = colors.inkSoft)
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = "Eliminar", color = colors.marginRed)
            }
        },
    )
}

@Composable
internal fun RestockDialog(
    itemName: String,
    onDismiss: () -> Unit,
    onAddToShoppingList: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = FoodarioTheme.colors
    val typography = FoodarioTheme.typography

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.paperElevated,
        title = {
            Text(
                text = "Se terminó $itemName",
                style = typography.titleHand,
                color = colors.ink,
            )
        },
        text = {
            Column {
                Text(
                    text = "¿Querés volver a comprarlo?",
                    style = typography.body,
                    color = colors.ink,
                )
                TextButton(onClick = onDelete) {
                    Text(text = "Eliminar alimento", color = colors.marginRed)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Ahora no", color = colors.inkSoft)
            }
        },
        confirmButton = {
            TextButton(onClick = onAddToShoppingList) {
                Text(text = "Agregar a compras", color = colors.penBlue)
            }
        },
    )
}
