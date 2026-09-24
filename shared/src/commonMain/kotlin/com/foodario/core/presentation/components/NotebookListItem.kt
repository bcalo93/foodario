package com.foodario.core.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.foodario.core.presentation.theme.FoodarioTheme
import com.foodario.core.presentation.theme.categoryColor
import com.foodario.inventory.domain.model.FoodCategory
import com.foodario.inventory.domain.model.QuantityUnit

fun formatQuantity(quantity: Double, unit: QuantityUnit): String {
    val number = if (quantity % 1.0 == 0.0) quantity.toLong().toString() else quantity.toString()
    return when (unit) {
        QuantityUnit.UNIT -> if (quantity == 1.0) "$number unidad" else "$number unidades"
        QuantityUnit.GRAMS -> "$number g"
        QuantityUnit.KILOGRAMS -> "$number kg"
        QuantityUnit.MILLILITERS -> "$number ml"
        QuantityUnit.LITERS -> "$number L"
    }
}

@Composable
fun NotebookListItem(
    name: String,
    category: FoodCategory,
    quantity: Double,
    unit: QuantityUnit,
    isFrozen: Boolean = false,
    expiresInDays: Int? = null,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val colors = FoodarioTheme.colors
    val typography = FoodarioTheme.typography

    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = FoodarioTheme.dimensions.rowHeight)
            .clickable(onClick = onClick)
            .drawBehind {
                drawLine(
                    color = colors.pencilGray.copy(alpha = 0.4f),
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx(),
                )
            }
            .padding(horizontal = FoodarioTheme.dimensions.lg, vertical = FoodarioTheme.dimensions.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.clearAndSetSemantics {},
            contentAlignment = Alignment.Center,
        ) {
            Text(text = category.emoji, fontSize = 32.sp)
        }
        Spacer(Modifier.width(FoodarioTheme.dimensions.md))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = name,
                    style = typography.titleHand,
                    color = colors.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (isFrozen) {
                    Spacer(Modifier.width(FoodarioTheme.dimensions.sm))
                    Text(
                        text = "❄",
                        fontSize = 16.sp,
                        modifier = Modifier.clearAndSetSemantics {},
                    )
                }
            }
            Spacer(Modifier.height(FoodarioTheme.dimensions.xxs))
            Text(
                text = category.displayName,
                style = typography.labelHand,
                color = colors.ink,
                modifier = Modifier
                    .background(categoryColor(category), RoundedCornerShape(4.dp))
                    .padding(horizontal = FoodarioTheme.dimensions.xs, vertical = FoodarioTheme.dimensions.xxs),
            )
            if (expiresInDays != null && expiresInDays <= 2) {
                Spacer(Modifier.height(FoodarioTheme.dimensions.xxs))
                Text(
                    text = "¡vence en $expiresInDays días!",
                    style = typography.labelHand,
                    color = colors.marginRed,
                )
            }
        }
        Spacer(Modifier.width(FoodarioTheme.dimensions.md))
        Text(
            text = formatQuantity(quantity, unit),
            style = typography.quantityHand,
            color = colors.ink,
        )
    }
}

@Preview
@Composable
private fun NotebookListItemLightPreview() {
    FoodarioTheme(darkTheme = false) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(FoodarioTheme.colors.paper),
        ) {
            NotebookListItem(
                name = "Leche",
                category = FoodCategory.DAIRY,
                quantity = 2.0,
                unit = QuantityUnit.UNIT,
                expiresInDays = 1,
            )
            NotebookListItem(
                name = "Pechuga",
                category = FoodCategory.MEAT,
                quantity = 300.0,
                unit = QuantityUnit.GRAMS,
                isFrozen = true,
            )
            NotebookListItem(
                name = "Manzanas",
                category = FoodCategory.FRUITS,
                quantity = 1.0,
                unit = QuantityUnit.KILOGRAMS,
            )
        }
    }
}

@Preview
@Composable
private fun NotebookListItemDarkPreview() {
    FoodarioTheme(darkTheme = true) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(FoodarioTheme.colors.paper),
        ) {
            NotebookListItem(
                name = "Leche",
                category = FoodCategory.DAIRY,
                quantity = 2.0,
                unit = QuantityUnit.UNIT,
                expiresInDays = 1,
            )
            NotebookListItem(
                name = "Pechuga",
                category = FoodCategory.MEAT,
                quantity = 300.0,
                unit = QuantityUnit.GRAMS,
                isFrozen = true,
            )
            NotebookListItem(
                name = "Manzanas",
                category = FoodCategory.FRUITS,
                quantity = 1.0,
                unit = QuantityUnit.KILOGRAMS,
            )
        }
    }
}
