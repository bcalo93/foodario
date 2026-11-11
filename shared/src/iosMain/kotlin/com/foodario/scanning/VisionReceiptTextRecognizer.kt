package com.foodario.scanning

import com.foodario.scanning.domain.service.ReceiptTextRecognizer
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSData
import platform.Foundation.create
import platform.UIKit.UIImage
import platform.Vision.VNImageRequestHandler
import platform.Vision.VNRecognizeTextRequest
import platform.Vision.VNRecognizedText
import platform.Vision.VNRecognizedTextObservation
import platform.Vision.VNRequestTextRecognitionLevelAccurate
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class VisionReceiptTextRecognizer : ReceiptTextRecognizer {

    override suspend fun recognizeText(imageBytes: ByteArray): String =
        suspendCancellableCoroutine { cont ->
            val data = imageBytes.usePinned {
                NSData.create(bytes = it.addressOf(0), length = imageBytes.size.toULong())
            }
            val cgImage = UIImage(data = data).CGImage
            if (cgImage == null) {
                cont.resumeWithException(IllegalArgumentException("No se pudo leer la imagen"))
                return@suspendCancellableCoroutine
            }
            val request = VNRecognizeTextRequest { req, error ->
                if (error != null) {
                    cont.resumeWithException(IllegalStateException(error.localizedDescription))
                    return@VNRecognizeTextRequest
                }
                val lines = req?.results.orEmpty()
                    .mapNotNull { it as? VNRecognizedTextObservation }
                    .sortedByDescending { it.boundingBox.useContents { origin.y } }
                    .mapNotNull { (it.topCandidates(1uL).firstOrNull() as? VNRecognizedText)?.string }
                if (cont.isActive) cont.resume(lines.joinToString("\n"))
            }.apply {
                recognitionLevel = VNRequestTextRecognitionLevelAccurate
                recognitionLanguages = listOf("es-AR", "es-ES", "en-US")
                usesLanguageCorrection = true
            }
            val handler = VNImageRequestHandler(cGImage = cgImage, options = emptyMap<Any?, Any?>())
            try {
                handler.performRequests(listOf(request), error = null)
            } catch (t: Throwable) {
                if (cont.isActive) cont.resumeWithException(t)
            }
        }
}
