package com.foodario.alerts.presentation.uicomponents

import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.foodario.alerts.domain.model.AlertSettings
import com.foodario.alerts.presentation.AlertSettingsEvent
import com.foodario.alerts.presentation.AlertSettingsViewModel
import com.foodario.core.presentation.theme.FoodarioTheme
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AlertSettingsDialog(
    onDismiss: () -> Unit,
    viewModel: AlertSettingsViewModel = koinViewModel(),
) {
    val colors = FoodarioTheme.colors
    val typography = FoodarioTheme.typography
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.paperElevated,
        titleContentColor = colors.ink,
        textContentColor = colors.ink,
        title = {
            Text(
                text = "Avisos de vencimiento",
                style = typography.titleHand,
                color = colors.ink,
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Activar avisos",
                        style = typography.body,
                        color = colors.ink,
                    )
                    Spacer(Modifier.weight(1f))
                    Switch(
                        checked = uiState.enabled,
                        onCheckedChange = { viewModel.onEvent(AlertSettingsEvent.SetEnabled(it)) },
                    )
                }

                Spacer(Modifier.height(FoodarioTheme.dimensions.md))

                val stepperAlpha = if (uiState.enabled) 1f else 0.5f

                Column(modifier = Modifier.alpha(stepperAlpha)) {
                    StepperRow(
                        labelTop = "Avisar",
                        value = uiState.leadDays.toString(),
                        labelBottom = "días antes",
                        decrementEnabled = uiState.enabled &&
                            uiState.leadDays > AlertSettings.LEAD_DAYS_RANGE.first,
                        incrementEnabled = uiState.enabled &&
                            uiState.leadDays < AlertSettings.LEAD_DAYS_RANGE.last,
                        onDecrement = {
                            viewModel.onEvent(AlertSettingsEvent.SetLeadDays(uiState.leadDays - 1))
                        },
                        onIncrement = {
                            viewModel.onEvent(AlertSettingsEvent.SetLeadDays(uiState.leadDays + 1))
                        },
                    )

                    Spacer(Modifier.height(FoodarioTheme.dimensions.md))

                    StepperRow(
                        labelTop = "Avisar a las",
                        value = uiState.notifyHour.toString().padStart(2, '0') + ":00",
                        labelBottom = "h",
                        decrementEnabled = uiState.enabled &&
                            uiState.notifyHour > AlertSettings.NOTIFY_HOUR_RANGE.first,
                        incrementEnabled = uiState.enabled &&
                            uiState.notifyHour < AlertSettings.NOTIFY_HOUR_RANGE.last,
                        onDecrement = {
                            viewModel.onEvent(AlertSettingsEvent.SetNotifyHour(uiState.notifyHour - 1))
                        },
                        onIncrement = {
                            viewModel.onEvent(AlertSettingsEvent.SetNotifyHour(uiState.notifyHour + 1))
                        },
                    )
                }

                Spacer(Modifier.height(FoodarioTheme.dimensions.md))

                Text(
                    text = "Podés usar ese producto en una comida antes de que venza",
                    style = typography.caption,
                    color = colors.inkSoft,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Listo", color = colors.penBlue)
            }
        },
    )
}

@Composable
private fun StepperRow(
    labelTop: String,
    value: String,
    labelBottom: String,
    decrementEnabled: Boolean,
    incrementEnabled: Boolean,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
) {
    val colors = FoodarioTheme.colors
    val typography = FoodarioTheme.typography

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepperButton(
            symbol = "−",
            enabled = decrementEnabled,
            onClick = onDecrement,
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = labelTop,
                style = typography.body,
                color = colors.ink,
            )
            Text(
                text = value,
                style = typography.quantityHand,
                color = colors.ink,
            )
            Text(
                text = labelBottom,
                style = typography.body,
                color = colors.ink,
            )
        }
        StepperButton(
            symbol = "+",
            enabled = incrementEnabled,
            onClick = onIncrement,
        )
    }
}

@Composable
private fun StepperButton(
    symbol: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val colors = FoodarioTheme.colors
    val color = if (enabled) colors.penBlue else colors.inkSoft

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(FoodarioTheme.dimensions.touchTarget)
            .clickable(enabled = enabled, onClick = onClick),
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
