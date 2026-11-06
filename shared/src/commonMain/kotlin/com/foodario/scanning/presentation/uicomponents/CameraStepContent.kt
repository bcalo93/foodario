package com.foodario.scanning.presentation.uicomponents

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.foodario.core.presentation.theme.FoodarioTheme
import com.foodario.scanning.presentation.ReceiptScanEvent
import com.foodario.scanning.presentation.camera.CameraController
import com.foodario.scanning.presentation.camera.CameraPreviewBox
import kotlinx.coroutines.launch

@Composable
fun CameraStepContent(
    controller: CameraController,
    isProcessing: Boolean,
    onEvent: (ReceiptScanEvent) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val colors = FoodarioTheme.colors
    val typography = FoodarioTheme.typography

    Box(modifier = modifier) {
        CameraPreviewBox(controller = controller, modifier = Modifier.fillMaxSize())

        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(FoodarioTheme.dimensions.lg)
                .size(FoodarioTheme.dimensions.touchTarget)
                .clickable(onClick = onClose),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "✕", fontSize = 28.sp, color = colors.ink)
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = FoodarioTheme.dimensions.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Alineá la factura y tocá el botón",
                style = typography.labelHand,
                color = colors.ink.copy(alpha = 0.9f),
                modifier = Modifier
                    .background(colors.paper.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                    .padding(
                        horizontal = FoodarioTheme.dimensions.md,
                        vertical = FoodarioTheme.dimensions.xs,
                    ),
            )
            Spacer(Modifier.height(FoodarioTheme.dimensions.md))
            ShutterButton(
                onClick = {
                    scope.launch {
                        val bytes = controller.capturePhoto()
                        if (bytes != null) {
                            onEvent(ReceiptScanEvent.PhotoCaptured(bytes))
                        } else {
                            onEvent(ReceiptScanEvent.PhotoCaptureFailed)
                        }
                    }
                },
            )
        }

        if (isProcessing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors.paper.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = colors.penBlue)
                    Spacer(Modifier.height(FoodarioTheme.dimensions.md))
                    Text(
                        text = "Leyendo tu factura…",
                        style = typography.body,
                        color = colors.ink,
                    )
                }
            }
        }
    }
}

@Composable
private fun ShutterButton(
    onClick: () -> Unit,
) {
    val colors = FoodarioTheme.colors
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(72.dp)
            .clickable(onClick = onClick),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                color = colors.paper.copy(alpha = 0.5f),
                radius = size.minDimension / 2f - 1.dp.toPx(),
            )
            drawWobblyCircle(
                color = colors.penBlue,
                radius = size.minDimension / 2f - 6.dp.toPx(),
                strokeWidth = 3.dp.toPx(),
            )
            drawWobblyCircle(
                color = colors.ink,
                radius = size.minDimension / 2f - 14.dp.toPx(),
                strokeWidth = 1.5.dp.toPx(),
            )
        }
    }
}

private fun DrawScope.drawWobblyCircle(color: Color, radius: Float, strokeWidth: Float) {
    val c = Offset(size.width / 2f, size.height / 2f)
    val w = 2.dp.toPx()
    val path = Path().apply {
        moveTo(c.x, c.y - radius + w)
        cubicTo(
            c.x + radius * 0.5f, c.y - radius - w,
            c.x + radius + w, c.y - radius * 0.5f,
            c.x + radius - w, c.y,
        )
        cubicTo(
            c.x + radius + w, c.y + radius * 0.5f,
            c.x + radius * 0.5f, c.y + radius + w,
            c.x, c.y + radius - w,
        )
        cubicTo(
            c.x - radius * 0.5f, c.y + radius + w,
            c.x - radius - w, c.y + radius * 0.5f,
            c.x - radius + w, c.y,
        )
        cubicTo(
            c.x - radius - w, c.y - radius * 0.5f,
            c.x - radius * 0.5f, c.y - radius - w,
            c.x, c.y - radius + w,
        )
        close()
    }
    drawPath(path = path, color = color, style = Stroke(width = strokeWidth))
}
