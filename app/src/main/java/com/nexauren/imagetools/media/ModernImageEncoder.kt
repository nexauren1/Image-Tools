package com.nexauren.imagetools.media

import android.content.Context
import android.graphics.Bitmap
import androidx.heifwriter.AvifWriter
import androidx.heifwriter.HeifWriter
import java.io.File

enum class ModernFormat(val extension: String, val mime: String) {
    HEIC("heic", "image/heic"),
    AVIF("avif", "image/avif")
}

object ModernImageEncoder {
    fun encode(context: Context, bitmap: Bitmap, format: ModernFormat, quality: Int): ByteArray {
        val file = File.createTempFile(
            "image-tools-",
            "." + format.extension,
            context.cacheDir
        )
        try {
            when (format) {
                ModernFormat.HEIC -> {
                    val writer = HeifWriter.Builder(
                        file.absolutePath,
                        bitmap.width,
                        bitmap.height,
                        HeifWriter.INPUT_MODE_BITMAP
                    )
                        .setQuality(quality.coerceIn(0, 100))
                        .build()
                    writer.start()
                    writer.addBitmap(bitmap)
                    writer.stop(10_000)
                    writer.close()
                }
                ModernFormat.AVIF -> {
                    val writer = AvifWriter.Builder(
                        file.absolutePath,
                        bitmap.width,
                        bitmap.height,
                        AvifWriter.INPUT_MODE_BITMAP
                    )
                        .setQuality(quality.coerceIn(0, 100))
                        .build()
                    writer.start()
                    writer.addBitmap(bitmap)
                    writer.stop(10_000)
                    writer.close()
                }
            }
            return file.readBytes()
        } finally {
            file.delete()
        }
    }
}
