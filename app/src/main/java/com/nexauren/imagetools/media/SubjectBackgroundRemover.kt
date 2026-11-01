package com.nexauren.imagetools.media

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import kotlinx.coroutines.tasks.await
import kotlin.math.roundToInt

object SubjectBackgroundRemover {
    suspend fun remove(source: Bitmap): Bitmap {
        val pixelCount = source.width.toLong() * source.height.toLong()
        require(source.width >= 512 && source.height >= 512) {
            "For accurate background removal, use an image with at least 512 × 512 pixels."
        }
        require(pixelCount <= 12_000_000L) {
            "This image is too large for safe background removal on this device. Try an image up to 12 megapixels."
        }

        val options = SubjectSegmenterOptions.Builder()
            .enableForegroundConfidenceMask()
            .build()
        val segmenter = SubjectSegmentation.getClient(options)
        try {
            // The model is delivered by Google Play services; first use may download it.
            segmenter.initTask.await()
            val result = segmenter.process(InputImage.fromBitmap(source, 0)).await()
            val mask = requireNotNull(result.foregroundConfidenceMask) {
                "The background-removal model did not return a confidence mask."
            }
            require(mask.capacity().toLong() >= pixelCount) {
                "The background-removal model returned an incomplete mask. Please try again."
            }

            val output = source.copy(Bitmap.Config.ARGB_8888, true)
                ?: error("Could not create a transparent image.")

            // Process one row at a time to avoid allocating another full-size pixel array.
            val row = IntArray(source.width)
            for (y in 0 until source.height) {
                source.getPixels(row, 0, source.width, 0, y, source.width, 1)
                val maskOffset = y * source.width
                for (x in row.indices) {
                    val alpha = (mask.get(maskOffset + x).coerceIn(0f, 1f) * 255f)
                        .roundToInt()
                        .coerceIn(0, 255)
                    row[x] = (row[x] and 0x00FFFFFF) or (alpha shl 24)
                }
                output.setPixels(row, 0, source.width, 0, y, source.width, 1)
            }
            return output
        } catch (error: Exception) {
            throw IllegalStateException(
                error.message?.takeIf { it.isNotBlank() }
                    ?: "Background removal could not start. Check Google Play services and internet access, then try again.",
                error
            )
        } finally {
            segmenter.close()
        }
    }
}
