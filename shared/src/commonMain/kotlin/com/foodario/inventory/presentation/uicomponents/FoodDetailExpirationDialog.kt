package com.foodario.inventory.presentation.uicomponents

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.foodario.core.presentation.theme.FoodarioTheme
import com.foodario.inventory.presentation.formatDate
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ExpirationDialog(
    currentDate: LocalDate?,
    onDismiss: () -> Unit,
    onSelect: (LocalDate?) -> Unit,
) {
    val colors = FoodarioTheme.colors
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = currentDate?.toPickerDateMillis(),
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = datePickerState.selectedDateMillis != null,
                onClick = {
                    datePickerState.selectedDateMillis?.let { onSelect(it.toLocalDate()) }
                },
            ) {
                Text(text = "Guardar", color = colors.penBlue)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancelar", color = colors.inkSoft)
            }
        },
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            DatePicker(
                state = datePickerState,
                title = null,
                headline = {
                    Text(
                        text = datePickerState.selectedDateMillis
                            ?.let { formatDate(it.toLocalDate()) }
                            ?: "Elegí una fecha",
                        style = FoodarioTheme.typography.titleHand,
                        color = colors.ink,
                        maxLines = 1,
                        modifier = Modifier.padding(
                            start = FoodarioTheme.dimensions.xl,
                            end = FoodarioTheme.dimensions.md,
                            bottom = FoodarioTheme.dimensions.md,
                        ),
                    )
                },
            )
            TextButton(
                onClick = { onSelect(null) },
                modifier = Modifier.align(Alignment.End),
            ) {
                Text(text = "Sin vencimiento", color = colors.marginRed)
            }
        }
    }
}

private fun LocalDate.toPickerDateMillis(): Long =
    toEpochDays() * MILLIS_PER_DAY

private fun Long.toLocalDate(): LocalDate =
    Instant.fromEpochMilliseconds(this)
        .toLocalDateTime(TimeZone.UTC)
        .date

private const val MILLIS_PER_DAY = 86_400_000L
