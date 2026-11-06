package com.foodario.scanning.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.foodario.core.presentation.theme.FoodarioTheme
import com.foodario.scanning.presentation.camera.rememberCameraController
import com.foodario.scanning.presentation.uicomponents.CameraStepContent
import com.foodario.scanning.presentation.uicomponents.ReviewStepContent
import kotlinx.coroutines.delay
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ReceiptScanScreen(
    onClose: () -> Unit = {},
    viewModel: ReceiptScanViewModel = koinViewModel(),
) {
    val colors = FoodarioTheme.colors
    val typography = FoodarioTheme.typography
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var successNoteCount by remember { mutableStateOf<Int?>(null) }

    val controller = rememberCameraController()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is ReceiptScanEffect.Imported -> successNoteCount = effect.count
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.paper),
    ) {
        when (uiState.step) {
            ReceiptScanStep.Capturing -> CameraStepContent(
                controller = controller,
                isProcessing = uiState.isProcessing,
                onEvent = viewModel::onEvent,
                onClose = onClose,
                modifier = Modifier.fillMaxSize(),
            )
            ReceiptScanStep.Reviewing -> ReviewStepContent(
                items = uiState.items,
                onEvent = viewModel::onEvent,
                onClose = onClose,
                modifier = Modifier.fillMaxSize(),
            )
        }

        uiState.error?.let { error ->
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .shadow(4.dp, RoundedCornerShape(8.dp))
                    .background(colors.paperElevated, RoundedCornerShape(8.dp))
                    .padding(
                        horizontal = FoodarioTheme.dimensions.lg,
                        vertical = FoodarioTheme.dimensions.md,
                    ),
            ) {
                Text(
                    text = error,
                    style = typography.body,
                    color = colors.marginRed,
                )
            }
        }

        successNoteCount?.let { count ->
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .rotate(-1f)
                        .shadow(8.dp, RoundedCornerShape(8.dp))
                        .background(colors.paperElevated, RoundedCornerShape(8.dp))
                        .padding(FoodarioTheme.dimensions.xl),
                ) {
                    Text(
                        text = "¡Listo! $count productos en tu heladera ✓",
                        style = typography.titleHand,
                        color = colors.ink,
                    )
                }
            }
            LaunchedEffect(count) {
                delay(1200)
                onClose()
            }
        }
    }
}
