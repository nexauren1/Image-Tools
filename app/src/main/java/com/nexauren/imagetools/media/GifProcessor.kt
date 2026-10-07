package com.nexauren.imagetools.media

import android.graphics.Bitmap
import com.squareup.gifencoder.GifEncoder
import com.squareup.gifencoder.ImageOptions
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

object GifProcessor {
    fun encode(frames: List<Bitmap>, delayMs: Long = 500): ByteArray {
        require(frames.isNotEmpty()) { "Select at least one image." }
        val normalizedW = 640
        val firstRatio = frames.first().height.toFloat() / frames.first().width.toFloat()
        val normalizedH = (normalizedW * firstRatio).toInt().coerceIn(120, 640)
        val stream = ByteArrayOutputStream()
        val encoder = GifEncoder(stream, normalizedW, normalizedH, 0)
        val options = ImageOptions().apply {
            setDelay(delayMs, TimeUnit.MILLISECONDS)
        }
        frames.take(20).forEach { frame ->
            val bitmap = ImageProcessor.resize(frame, normalizedW, normalizedH)
            val pixels = IntArray(normalizedW * normalizedH)
            bitmap.getPixels(pixels, 0, normalizedW, 0, 0, normalizedW, normalizedH)
            val rgb = Array(normalizedW) { IntArray(normalizedH) }
            for (x in 0 until normalizedW) {
                for (y in 0 until normalizedH) {
                    rgb[x][y] = pixels[y * normalizedW + x]
                }
            }
            encoder.addImage(rgb, options)
        }
        encoder.finishEncoding()
        return stream.toByteArray()
    }
}
