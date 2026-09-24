package com.foodario.core.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.foodario.core.presentation.theme.FoodarioTheme
import com.foodario.core.presentation.theme.categoryColor
import com.foodario.inventory.domain.model.FoodCategory

private const val CategoryColumnCount = 5
private const val CollapsedCategoryCount = 4

@Composable
fun CategoryGrid(
    selectedCategory: FoodCategory?,
    onCategoryClick: (FoodCategory) -> Unit,
    modifier: Modifier = Modifier,
    categories: List<FoodCategory> = FoodCategory.entries,
) {
    var expanded by remember { mutableStateOf(false) }

    val head = categories.take(CollapsedCategoryCount).toMutableList()
    if (selectedCategory != null && selectedCategory !in head) {
        head[CollapsedCategoryCount - 1] = selectedCategory
    }
    val tail = categories.filter { it !in head }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(FoodarioTheme.dimensions.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            head.forEach { category ->
                CategoryChip(
                    category = category,
                    selected = category == selectedCategory,
                    onClick = { onCategoryClick(category) },
                )
            }
            MoreButton(expanded = expanded, onClick = { expanded = !expanded })
        }
        AnimatedVisibility(
            visible = expanded,
            modifier = Modifier.fillMaxWidth(),
            enter = expandVertically(animationSpec = tween(200), expandFrom = Alignment.Top) + fadeIn(tween(200)),
            exit = shrinkVertically(animationSpec = tween(200), shrinkTowards = Alignment.Top) + fadeOut(tween(150)),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(FoodarioTheme.dimensions.sm),
            ) {
                Spacer(Modifier.height(FoodarioTheme.dimensions.sm))
                tail.chunked(CategoryColumnCount).forEach { row ->
                    CategoryRow(row, onCategoryClick)
                }
            }
        }
    }
}

@Composable
private fun CategoryRow(
    categories: List<FoodCategory>,
    onCategoryClick: (FoodCategory) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(FoodarioTheme.dimensions.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        categories.forEach { category ->
            CategoryChip(
                category = category,
                selected = false,
                onClick = { onCategoryClick(category) },
            )
        }
    }
}

@Composable
private fun CategoryChip(
    category: FoodCategory,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = FoodarioTheme.colors
    val borderColor = if (selected) colors.penBlue else colors.pencilGray
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(FoodarioTheme.dimensions.touchTarget)
            .clip(CircleShape)
            .background(categoryColor(category))
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = borderColor,
                shape = CircleShape,
            )
            .clickable(onClick = onClick)
            .semantics { contentDescription = category.displayName },
    ) {
        Text(text = category.emoji, fontSize = 20.sp)
    }
}

@Composable
private fun MoreButton(
    expanded: Boolean,
    onClick: () -> Unit,
) {
    val colors = FoodarioTheme.colors
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(FoodarioTheme.dimensions.touchTarget)
            .clip(CircleShape)
            .background(colors.paper)
            .border(1.dp, colors.pencilGray, CircleShape)
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = if (expanded) "Ver menos categorías" else "Ver más categorías"
            },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 2.dp.toPx()
            val inset = size.width * 0.3f
            if (expanded) {
                val apexY = size.height * 0.35f
                val baseY = size.height * 0.65f
                drawLine(
                    color = colors.penBlue,
                    start = Offset(inset, baseY),
                    end = Offset(size.width / 2f, apexY),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = colors.penBlue,
                    start = Offset(size.width - inset, baseY),
                    end = Offset(size.width / 2f, apexY),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                )
            } else {
                val apexY = size.height * 0.65f
                val baseY = size.height * 0.35f
                drawLine(
                    color = colors.penBlue,
                    start = Offset(inset, baseY),
                    end = Offset(size.width / 2f, apexY),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = colors.penBlue,
                    start = Offset(size.width - inset, baseY),
                    end = Offset(size.width / 2f, apexY),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}

@Preview
@Composable
private fun CategoryGridNullPreview() {
    FoodarioTheme(darkTheme = false) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(FoodarioTheme.colors.paper)
                .padding(FoodarioTheme.dimensions.lg),
        ) {
            CategoryGrid(
                selectedCategory = null,
                onCategoryClick = {},
            )
        }
    }
}

@Preview
@Composable
private fun CategoryGridOtherPreview() {
    FoodarioTheme(darkTheme = false) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(FoodarioTheme.colors.paper)
                .padding(FoodarioTheme.dimensions.lg),
        ) {
            CategoryGrid(
                selectedCategory = FoodCategory.OTHER,
                onCategoryClick = {},
            )
        }
    }
}
