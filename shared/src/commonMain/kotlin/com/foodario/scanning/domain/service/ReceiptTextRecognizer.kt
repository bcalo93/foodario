package com.foodario.scanning.domain.service

interface ReceiptTextRecognizer {
    /** On-device OCR over JPEG bytes. Returns the full raw text (lines separated by \n). */
    suspend fun recognizeText(imageBytes: ByteArray): String
}
