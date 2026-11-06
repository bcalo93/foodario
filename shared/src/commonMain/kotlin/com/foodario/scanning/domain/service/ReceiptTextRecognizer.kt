package com.foodario.scanning.domain.service

interface ReceiptTextRecognizer {
    /** OCR on-device sobre bytes JPEG. Devuelve el texto crudo completo (líneas separadas por \n). */
    suspend fun recognizeText(imageBytes: ByteArray): String
}
