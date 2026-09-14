package com.foodario.core.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.foodario.core.presentation.theme.FoodarioTheme

@Composable
fun HandDrawnCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = FoodarioTheme.colors
    val borderColor = if (enabled) colors.penBlue else colors.pencilGray
    val checkColor = colors.penBlue
    val progress by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "checkProgress",
    )

    Box(
        modifier = modifier
            .size(48.dp)
            .toggleable(
                value = checked,
                enabled = enabled,
                onValueChange = onCheckedChange,
            )
            .semantics {
                contentDescription = if (checked) "Marcado" else "Sin marcar"
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawWobblySquare(borderColor)
            if (progress > 0f) {
                drawWobblyCheck(checkColor, progress)
            }
        }
    }
}

private fun DrawScope.drawWobblySquare(color: Color) {
    val w = size.width
    val h = size.height
    val inset = 4.dp.toPx()
    val wobble = 2.dp.toPx()
    val path = Path().apply {
        moveTo(inset, inset + wobble)
        cubicTo(
            w * 0.25f, inset - wobble,
            w * 0.75f, inset + wobble,
            w - inset, inset,
        )
        cubicTo(
            w - inset + wobble, h * 0.25f,
            w - inset - wobble, h * 0.75f,
            w - inset, h - inset,
        )
        cubicTo(
            w * 0.75f, h - inset + wobble,
            w * 0.25f, h - inset - wobble,
            inset, h - inset,
        )
        cubicTo(
            inset - wobble, h * 0.75f,
            inset + wobble, h * 0.25f,
            inset, inset + wobble,
        )
        close()
    }
    drawPath(path = path, color = color, style = Stroke(width = 1.5.dp.toPx()))
}

private fun DrawScope.drawWobblyCheck(color: Color, progress: Float) {
    val w = size.width
    val h = size.height
    val path = Path().apply {
        moveTo(w * 0.26f, h * 0.52f)
        cubicTo(
            w * 0.30f, h * 0.56f,
            w * 0.34f, h * 0.62f,
            w * 0.44f, h * 0.68f,
        )
        cubicTo(
            w * 0.54f, h * 0.52f,
            w * 0.66f, h * 0.38f,
            w * 0.76f, h * 0.26f,
        )
    }
    val measure = PathMeasure().apply { setPath(path, false) }
    val partial = Path()
    measure.getSegment(0f, measure.length * progress, partial, true)
    drawPath(
        path = partial,
        color = color,
        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
}

@Preview
@Composable
private fun HandDrawnCheckboxLightPreview() {
    FoodarioTheme(darkTheme = false) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(FoodarioTheme.colors.paper)
                .padding(16.dp),
        ) {
            Row {
                HandDrawnCheckbox(checked = false, onCheckedChange = {})
                HandDrawnCheckbox(checked = true, onCheckedChange = {})
                HandDrawnCheckbox(checked = true, onCheckedChange = {}, enabled = false)
            }
        }
    }
}

@Preview
@Composable
private fun HandDrawnCheckboxDarkPreview() {
    FoodarioTheme(darkTheme = true) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(FoodarioTheme.colors.paper)
                .padding(16.dp),
        ) {
            Row {
                HandDrawnCheckbox(checked = false, onCheckedChange = {})
                HandDrawnCheckbox(checked = true, onCheckedChange = {})
                HandDrawnCheckbox(checked = true, onCheckedChange = {}, enabled = false)
            }
        }
    }
}
