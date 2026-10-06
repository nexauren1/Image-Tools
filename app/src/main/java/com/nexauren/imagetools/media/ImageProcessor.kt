package com.nexauren.imagetools.media

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.ByteArrayOutputStream
import java.text.DecimalFormat

enum class OutputFormat(val label: String, val mime: String, val extension: String) {
    JPEG("JPEG", "image/jpeg", "jpg"),
    PNG("PNG", "image/png", "png"),
    WEBP("WEBP", "image/webp", "webp")
}

data class ImageResult(
    val uri: Uri,
    val bytes: Long,
    val width: Int,
    val height: Int,
    val format: OutputFormat
)

object ImageProcessor {
    fun decode(context: Context, uri: Uri): Bitmap? =
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }

    fun resize(bitmap: Bitmap, width: Int, height: Int): Bitmap =
        Bitmap.createScaledBitmap(bitmap, width.coerceAtLeast(1), height.coerceAtLeast(1), true)

    fun encode(bitmap: Bitmap, format: OutputFormat, quality: Int): ByteArray {
        val out = ByteArrayOutputStream()
        val type = when (format) {
            OutputFormat.JPEG -> Bitmap.CompressFormat.JPEG
            OutputFormat.PNG -> Bitmap.CompressFormat.PNG
            OutputFormat.WEBP -> if (Build.VERSION.SDK_INT >= 30) Bitmap.CompressFormat.WEBP_LOSSY else Bitmap.CompressFormat.WEBP
        }
        check(bitmap.compress(type, quality.coerceIn(1, 100), out))
        return out.toByteArray()
    }

    fun save(context: Context, bytes: ByteArray, format: OutputFormat, prefix: String, width: Int, height: Int): ImageResult {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, prefix + "_" + System.currentTimeMillis() + "." + format.extension)
            put(MediaStore.Images.Media.MIME_TYPE, format.mime)
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Image Tools")
            if (Build.VERSION.SDK_INT >= 29) put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: error("Could not create the output image.")
        context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
            ?: error("Could not write the output image.")
        if (Build.VERSION.SDK_INT >= 29) {
            context.contentResolver.update(uri, ContentValues().apply {
                put(MediaStore.Images.Media.IS_PENDING, 0)
            }, null, null)
        }
        return ImageResult(uri, bytes.size.toLong(), width, height, format)
    }

    fun humanBytes(bytes: Long): String {
        if (bytes < 1024) return bytes.toString() + " B"
        val kb = bytes / 1024.0
        if (kb < 1024) return DecimalFormat("#,##0.0").format(kb) + " KB"
        return DecimalFormat("#,##0.0").format(kb / 1024.0) + " MB"
    }
}