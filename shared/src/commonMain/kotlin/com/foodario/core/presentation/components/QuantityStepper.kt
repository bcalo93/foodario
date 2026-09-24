package com.foodario.core.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.foodario.core.presentation.theme.FoodarioTheme
import com.foodario.inventory.domain.model.QuantityUnit
import com.foodario.inventory.domain.model.formatQuantityNumber
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun QuantityStepper(
    quantity: Double,
    unit: QuantityUnit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onQuantityClick: () -> Unit,
    onUnitClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = FoodarioTheme.colors
    val typography = FoodarioTheme.typography

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        StepperButton(
            symbol = "−",
            contentDescription = "Quitar",
            color = colors.penBlue,
            onClick = onDecrement,
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = formatQuantityNumber(quantity),
                style = typography.quantityHand,
                color = colors.ink,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .clickable(onClick = onQuantityClick)
                    .semantics {
                        role = Role.Button
                        contentDescription = "Editar cantidad"
                    },
            )
            Text(
                text = unitLabel(quantity, unit),
                style = typography.label,
                color = colors.inkSoft,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .clickable(onClick = onUnitClick)
                    .semantics {
                        role = Role.Button
                        contentDescription = "Cambiar unidad"
                    },
            )
            if (unit.step != 1.0) {
                Text(
                    text = "de a ${formatStep(unit)}",
                    style = typography.caption,
                    color = colors.inkSoft,
                )
            }
        }
        StepperButton(
            symbol = "+",
            contentDescription = "Agregar",
            color = colors.penBlue,
            onClick = onIncrement,
        )
    }
}

private fun unitLabel(quantity: Double, unit: QuantityUnit): String = when (unit) {
    QuantityUnit.UNIT -> if (quantity == 1.0) "unidad" else "unidades"
    QuantityUnit.GRAMS -> "g"
    QuantityUnit.KILOGRAMS -> "kg"
    QuantityUnit.MILLILITERS -> "ml"
    QuantityUnit.LITERS -> "L"
}

private fun formatStep(unit: QuantityUnit): String =
    "${formatQuantityNumber(unit.step)} ${unitLabel(unit.step, unit)}"

@Composable
private fun StepperButton(
    symbol: String,
    contentDescription: String,
    color: Color,
    onClick: () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(48.dp)
            .repeatPress(onClick)
            .semantics {
                role = Role.Button
                this.contentDescription = contentDescription
                this.onClick { onClick(); true }
            },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                color = color,
                radius = size.minDimension / 2f - 2.dp.toPx(),
                style = Stroke(width = 1.5.dp.toPx()),
            )
        }
        Text(
            text = symbol,
            style = FoodarioTheme.typography.quantityHand.copy(fontSize = 24.sp),
            color = color,
        )
    }
}

@Composable
private fun Modifier.repeatPress(onAction: () -> Unit): Modifier {
    val currentAction by rememberUpdatedState(onAction)
    return pointerInput(Unit) {
        coroutineScope {
            val scope = this
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                down.consume()
                var held = true
                scope.launch {
                    currentAction()
                    delay(400)
                    while (held) {
                        currentAction()
                        delay(90)
                    }
                }
                waitForUpOrCancellation()
                held = false
            }
        }
    }
}

@Preview
@Composable
private fun QuantityStepperUnitLightPreview() {
    FoodarioTheme(darkTheme = false) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(FoodarioTheme.colors.paper)
                .padding(16.dp),
        ) {
            QuantityStepper(
                quantity = 2.0,
                unit = QuantityUnit.UNIT,
                onIncrement = {},
                onDecrement = {},
                onQuantityClick = {},
                onUnitClick = {},
            )
        }
    }
}

@Preview
@Composable
private fun QuantityStepperUnitDarkPreview() {
    FoodarioTheme(darkTheme = true) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(FoodarioTheme.colors.paper)
                .padding(16.dp),
        ) {
            QuantityStepper(
                quantity = 2.0,
                unit = QuantityUnit.UNIT,
                onIncrement = {},
                onDecrement = {},
                onQuantityClick = {},
                onUnitClick = {},
            )
        }
    }
}

@Preview
@Composable
private fun QuantityStepperGramsLightPreview() {
    FoodarioTheme(darkTheme = false) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(FoodarioTheme.colors.paper)
                .padding(16.dp),
        ) {
            QuantityStepper(
                quantity = 300.0,
                unit = QuantityUnit.GRAMS,
                onIncrement = {},
                onDecrement = {},
                onQuantityClick = {},
                onUnitClick = {},
            )
        }
    }
}

@Preview
@Composable
private fun QuantityStepperGramsDarkPreview() {
    FoodarioTheme(darkTheme = true) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(FoodarioTheme.colors.paper)
                .padding(16.dp),
        ) {
            QuantityStepper(
                quantity = 300.0,
                unit = QuantityUnit.GRAMS,
                onIncrement = {},
                onDecrement = {},
                onQuantityClick = {},
                onUnitClick = {},
            )
        }
    }
}
