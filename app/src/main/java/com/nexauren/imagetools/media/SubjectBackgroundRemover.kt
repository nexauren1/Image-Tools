package com.nexauren.imagetools.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.subjectsegmentation.SubjectSegmentation
import com.google.mlkit.vision.subjectsegmentation.SubjectSegmenterOptions
import kotlinx.coroutines.tasks.await
import kotlin.math.min

/**
 * AI-assisted subject extraction.
 *
 * ML Kit's Subject Segmentation API is used when the on-device model is available.
 * The caller can fall back to ImageProcessor.backgroundCutout when this returns null.
 */
object SubjectBackgroundRemover {
    suspend fun remove(context: Context, source: Bitmap): Bitmap? {
        return runCatching {
            val inputBitmap = prepareForSegmentation(source)
            val options = SubjectSegmenterOptions.Builder()
                .enableForegroundBitmap()
                .build()

            val segmenter = SubjectSegmentation.getClient(options)
            val result = segmenter.process(InputImage.fromBitmap(inputBitmap, 0)).await()
            val foreground = result.foregroundBitmap ?: return@runCatching null

            if (foreground.width == source.width && foreground.height == source.height) {
                foreground
            } else {
                val output = Bitmap.createBitmap(
                    source.width,
                    source.height,
                    Bitmap.Config.ARGB_8888
                )
                val canvas = Canvas(output)
                canvas.drawBitmap(
                    foreground,
                    null,
                    android.graphics.Rect(0, 0, source.width, source.height),
                    Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
                )
                if (!foreground.isRecycled) foreground.recycle()
                output
            }.also {
                if (!inputBitmap.isRecycled && inputBitmap !== source) {
                    inputBitmap.recycle()
                }
            }
        }.getOrNull()
    }

    private fun prepareForSegmentation(source: Bitmap): Bitmap {
        val maxDimension = 1536
        val longest = maxOf(source.width, source.height)
        if (longest <= maxDimension) return source

        val scale = maxDimension.toFloat() / longest.toFloat()
        val width = (source.width * scale).toInt().coerceAtLeast(1)
        val height = (source.height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(source, width, height, true)
    }
}
