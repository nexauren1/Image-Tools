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
        require(source.width >= 256 && source.height >= 256) {
            "Use an image with at least 256 × 256 pixels for background removal."
        }
        require(pixelCount <= 20_000_000L) {
            "This image is too large for safe background removal on this device. Try an image up to 20 megapixels."
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
            val pixels = IntArray(pixelCount.toInt())
            source.getPixels(pixels, 0, source.width, 0, 0, source.width, source.height)

            for (index in pixels.indices) {
                val alpha = (mask.get(index).coerceIn(0f, 1f) * 255f)
                    .roundToInt()
                    .coerceIn(0, 255)
                pixels[index] = (pixels[index] and 0x00FFFFFF) or (alpha shl 24)
            }
            output.setPixels(pixels, 0, source.width, 0, 0, source.width, source.height)
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
