package com.foodario.scanning.presentation.camera

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

expect class CameraController {
    suspend fun capturePhoto(): ByteArray?
}

@Composable
expect fun rememberCameraController(): CameraController

@Composable
expect fun CameraPreviewBox(
    controller: CameraController,
    modifier: Modifier = Modifier,
    permissionMessage: String = "Necesitamos la cámara para escanear tu factura",
    permissionDeniedMessage: String = "Sin permiso de cámara no se puede escanear. Habilitalo en los ajustes del sistema.",
)
