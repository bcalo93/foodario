package com.foodario.inventory.presentation.uicomponents

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import com.foodario.core.presentation.components.emoji
import com.foodario.core.presentation.theme.FoodarioTheme
import com.foodario.inventory.domain.model.FoodCategory

@Composable
internal fun CategorySelectionDialog(
    currentCategory: FoodCategory,
    onDismiss: () -> Unit,
    onSelect: (FoodCategory) -> Unit,
) {
    val colors = FoodarioTheme.colors
    val typography = FoodarioTheme.typography

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.paperElevated,
        title = {
            Text(
                text = "Tipo de alimento",
                style = typography.titleHand,
                color = colors.ink,
            )
        },
        text = {
            Column {
                FoodCategory.entries.forEach { category ->
                    TextButton(
                        onClick = { onSelect(category) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(text = category.emoji, fontSize = 24.sp)
                            Spacer(Modifier.width(FoodarioTheme.dimensions.sm))
                            Text(
                                text = category.displayName,
                                style = typography.body,
                                color = if (category == currentCategory) {
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
