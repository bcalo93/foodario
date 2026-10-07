package com.foodario.core.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.foodario.core.presentation.theme.FoodarioTheme
import com.foodario.inventory.domain.model.QuantityUnit
import com.foodario.inventory.domain.model.formatQuantityNumber

@Composable
internal fun UnitSelectionDialog(
    currentUnit: QuantityUnit,
    onDismiss: () -> Unit,
    onSelect: (QuantityUnit) -> Unit,
) {
    val colors = FoodarioTheme.colors
    val typography = FoodarioTheme.typography

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.paperElevated,
        title = {
            Text(
                text = "Tipo de unidad",
                style = typography.titleHand,
                color = colors.ink,
            )
        },
        text = {
            Column {
                QuantityUnit.entries.forEach { unit ->
                    TextButton(
                        onClick = { onSelect(unit) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = unit.displayName,
                                style = typography.body,
                                color = if (unit == currentUnit) {
                                    colors.penBlue
                                } else {
                                    colors.ink
                                },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
    )
}

@Composable
internal fun QuantityEditDialog(
    quantity: Double,
    unit: QuantityUnit,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit,
    onUnitClick: (() -> Unit)? = null,
    allowZero: Boolean = true,
) {
    val colors = FoodarioTheme.colors
    val typography = FoodarioTheme.typography
    var text by remember { mutableStateOf(formatQuantityNumber(quantity)) }

    val parsed = text.trim().replace(',', '.').toDoubleOrNull()
    val isValid = parsed != null && if (allowZero) parsed >= 0.0 else parsed > 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.paperElevated,
        title = {
            Text(
                text = "Cantidad",
                style = typography.titleHand,
                color = colors.ink,
            )
        },
        text = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(FoodarioTheme.dimensions.sm))
                Text(
                    text = unitLabel(unit),
                    style = typography.body,
                    color = colors.ink,
                    modifier = if (onUnitClick != null) {
                        Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable(role = Role.Button, onClick = onUnitClick)
                            .padding(FoodarioTheme.dimensions.xs)
                    } else {
                        Modifier
                    },
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancelar", color = colors.inkSoft)
            }
        },
        confirmButton = {
            TextButton(
                enabled = isValid,
                onClick = { parsed?.let { onConfirm(it) } },
            ) {
                Text(text = "Guardar", color = colors.penBlue)
            }
        },
    )
}

private fun unitLabel(unit: QuantityUnit): String = when (unit) {
    QuantityUnit.UNIT -> "unidad"
    QuantityUnit.GRAMS -> "g"
    QuantityUnit.KILOGRAMS -> "kg"
    QuantityUnit.MILLILITERS -> "ml"
    QuantityUnit.LITERS -> "L"
}
