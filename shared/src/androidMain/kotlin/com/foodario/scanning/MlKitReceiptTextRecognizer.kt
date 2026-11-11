package com.foodario.scanning

import android.graphics.BitmapFactory
import com.foodario.scanning.domain.service.ReceiptTextRecognizer
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class MlKitReceiptTextRecognizer : ReceiptTextRecognizer {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    override suspend fun recognizeText(imageBytes: ByteArray): String =
        suspendCancellableCoroutine { cont ->
            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            if (bitmap == null) {
                cont.resumeWithException(IllegalArgumentException("No se pudo leer la imagen"))
                return@suspendCancellableCoroutine
            }
            recognizer.process(InputImage.fromBitmap(bitmap, 0))
                .addOnSuccessListener { if (cont.isActive) cont.resume(it.text) }
                .addOnFailureListener { if (cont.isActive) cont.resumeWithException(it) }
        }
}
