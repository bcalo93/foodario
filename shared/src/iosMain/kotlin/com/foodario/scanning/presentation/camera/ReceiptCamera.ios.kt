@file:OptIn(ExperimentalForeignApi::class)

package com.foodario.scanning.presentation.camera

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import com.foodario.core.presentation.theme.FoodarioTheme
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVAuthorizationStatusDenied
import platform.AVFoundation.AVAuthorizationStatusNotDetermined
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVCaptureDeviceInput
import platform.AVFoundation.AVCaptureDevicePositionBack
import platform.AVFoundation.AVCapturePhoto
import platform.AVFoundation.AVCapturePhotoCaptureDelegateProtocol
import platform.AVFoundation.AVCapturePhotoOutput
import platform.AVFoundation.AVCapturePhotoSettings
import platform.AVFoundation.AVCaptureSession
import platform.AVFoundation.AVCaptureVideoPreviewLayer
import platform.AVFoundation.AVLayerVideoGravityResizeAspectFill
import platform.AVFoundation.AVMediaTypeVideo
import platform.Foundation.NSError
import platform.UIKit.UIView
import platform.AVFoundation.authorizationStatusForMediaType
import platform.AVFoundation.fileDataRepresentation
import platform.AVFoundation.position
import platform.AVFoundation.requestAccessForMediaType
import platform.darwin.NSObject
import platform.posix.memcpy
import kotlin.coroutines.resume

actual class CameraController {
    internal var photoOutput: AVCapturePhotoOutput? = null

    actual suspend fun capturePhoto(): ByteArray? = suspendCancellableCoroutine { cont ->
        val output = photoOutput ?: return@suspendCancellableCoroutine cont.resume(null)
        val settings = AVCapturePhotoSettings.photoSettings()
        val delegate = PhotoCaptureDelegate { bytes -> if (cont.isActive) cont.resume(bytes) }
        output.capturePhotoWithSettings(settings, delegate)
    }
}

private class PhotoCaptureDelegate(
    private val onResult: (ByteArray?) -> Unit,
) : NSObject(), AVCapturePhotoCaptureDelegateProtocol {
    override fun captureOutput(
        output: AVCapturePhotoOutput,
        didFinishProcessingPhoto: AVCapturePhoto,
        error: NSError?,
    ) {
        val data = didFinishProcessingPhoto.fileDataRepresentation()
        if (error != null || data == null) {
            onResult(null)
        } else {
            val bytes = ByteArray(data.length.toInt())
            bytes.usePinned { memcpy(it.addressOf(0), data.bytes, data.length) }
            onResult(bytes)
        }
    }
}

@Composable
actual fun rememberCameraController(): CameraController = remember { CameraController() }

@Composable
actual fun CameraPreviewBox(
    controller: CameraController,
    modifier: Modifier,
    permissionMessage: String,
    permissionDeniedMessage: String,
) {
    var status by remember {
        mutableStateOf(AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo))
    }

    LaunchedEffect(Unit) {
        if (status == AVAuthorizationStatusNotDetermined) {
            AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { granted ->
                status = if (granted) AVAuthorizationStatusAuthorized else AVAuthorizationStatusDenied
            }
        }
    }

    when (status) {
        AVAuthorizationStatusAuthorized -> CameraPreview(controller, modifier)
        else -> CameraPermissionMessage(permissionDeniedMessage, onRetry = {
            AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { granted ->
                status = if (granted) AVAuthorizationStatusAuthorized else AVAuthorizationStatusDenied
            }
        }, modifier)
    }
}

@Composable
private fun CameraPreview(
    controller: CameraController,
    modifier: Modifier,
) {
    val session = remember { AVCaptureSession() }
    val photoOutput = remember { AVCapturePhotoOutput() }
    var previewLayer by remember { mutableStateOf<AVCaptureVideoPreviewLayer?>(null) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.Default) {
            val device = AVCaptureDevice.devicesWithMediaType(AVMediaTypeVideo)
                .filterIsInstance<AVCaptureDevice>()
                .firstOrNull { it.position == AVCaptureDevicePositionBack }
            val input = device?.let { d ->
                memScoped {
                    val error = alloc<ObjCObjectVar<NSError?>>()
                    AVCaptureDeviceInput.deviceInputWithDevice(d, error.ptr)
                }
            }
            if (input != null && session.canAddInput(input)) {
                session.addInput(input)
            }
            if (session.canAddOutput(photoOutput)) {
                session.addOutput(photoOutput)
            }
            controller.photoOutput = photoOutput
            session.startRunning()
        }
    }

    DisposableEffect(Unit) {
        onDispose { session.stopRunning() }
    }

    UIKitView(
        factory = {
            val view = UIView()
            val layer = AVCaptureVideoPreviewLayer(session = session)
            layer.videoGravity = AVLayerVideoGravityResizeAspectFill
            view.layer.addSublayer(layer)
            previewLayer = layer
            view
        },
        modifier = modifier,
        update = { view ->
            previewLayer?.frame = view.bounds
        },
    )
}

@Composable
private fun CameraPermissionMessage(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(FoodarioTheme.dimensions.xl),
        ) {
            Text(
                text = message,
                style = FoodarioTheme.typography.titleHand,
                color = FoodarioTheme.colors.ink,
            )
            TextButton(onClick = onRetry) {
                Text("Reintentar", color = FoodarioTheme.colors.penBlue)
            }
        }
    }
}
