package com.foodario.scanning.presentation.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.view.Surface
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.foodario.core.presentation.theme.FoodarioTheme
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

actual class CameraController {
    internal var imageCapture: ImageCapture? = null
    internal var appContext: Context? = null

    actual suspend fun capturePhoto(): ByteArray? = suspendCancellableCoroutine { cont ->
        val capture = imageCapture ?: return@suspendCancellableCoroutine cont.resume(null)
        val context = appContext ?: return@suspendCancellableCoroutine cont.resume(null)
        val file = File.createTempFile("receipt_scan_", ".jpg", context.cacheDir)
        val options = ImageCapture.OutputFileOptions.Builder(file).build()
        capture.takePicture(
            options,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val bytes = runCatching { file.readBytes() }.getOrNull()
                    file.delete()
                    if (cont.isActive) cont.resume(bytes)
                }
                override fun onError(exception: ImageCaptureException) {
                    file.delete()
                    if (cont.isActive) cont.resume(null)
                }
            },
        )
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
    val context = LocalContext.current
    var permissionState by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var denied by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> permissionState = granted; denied = !granted }

    LaunchedEffect(Unit) { if (!permissionState && !denied) launcher.launch(Manifest.permission.CAMERA) }

    when {
        permissionState -> CameraPreview(controller, modifier)
        denied -> CameraPermissionMessage(permissionDeniedMessage, onRetry = { launcher.launch(Manifest.permission.CAMERA) }, modifier)
        else -> CameraPermissionMessage(permissionMessage, onRetry = { launcher.launch(Manifest.permission.CAMERA) }, modifier)
    }
}

@Composable
private fun CameraPreview(
    controller: CameraController,
    modifier: Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }

    AndroidView(factory = { previewView }, modifier = modifier)

    var provider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    LaunchedEffect(Unit) {
        val p = awaitCameraProvider(context)
        provider = p
        val preview = Preview.Builder().build()
        val imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .build()
        previewView.display?.let { imageCapture.targetRotation = it.rotation }
        controller.imageCapture = imageCapture
        controller.appContext = context
        p.unbindAll()
        p.bindToLifecycle(
            lifecycleOwner,
            CameraSelector.DEFAULT_BACK_CAMERA,
            preview,
            imageCapture,
        )
        preview.setSurfaceProvider(previewView.surfaceProvider)
    }

    DisposableEffect(Unit) {
        onDispose { provider?.unbindAll() }
    }
}

private suspend fun awaitCameraProvider(context: Context): ProcessCameraProvider =
    suspendCancellableCoroutine { cont ->
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener(
            {
                runCatching { future.get() }
                    .onSuccess { if (cont.isActive) cont.resume(it) }
                    .onFailure { if (cont.isActive) cont.resumeWithException(it) }
            },
            ContextCompat.getMainExecutor(context),
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
