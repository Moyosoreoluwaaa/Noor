package com.noor.base_app_imageviewer_w_ocr

import android.content.Context
import android.graphics.BitmapFactory
import androidx.core.net.toUri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class OCRProcessor(private val context: Context) {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun extractText(imageUri: String): String = suspendCoroutine { continuation ->
        try {
            val uri = imageUri.toUri()
            val inputStream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (bitmap == null) {
                continuation.resume("")
                return@suspendCoroutine
            }

            val image = InputImage.fromBitmap(bitmap, 0)

            recognizer.process(image)
                .addOnSuccessListener { result ->
                    continuation.resume(result.text)
                }
                .addOnFailureListener {
                    continuation.resume("")
                }
        } catch (_: Exception) {
            continuation.resume("")
        }
    }
}