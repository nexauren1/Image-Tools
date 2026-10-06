package com.nexauren.imagetools.media

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Matrix
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.ByteArrayOutputStream
import java.text.DecimalFormat
import kotlin.math.min

enum class OutputFormat(val label: String, val mime: String, val extension: String) {
    JPEG("JPEG", "image/jpeg", "jpg"),
    PNG("PNG", "image/png", "png"),
    WEBP("WEBP", "image/webp", "webp")
}

enum class ImageFilter(val label: String) {
    ORIGINAL("Original"),
    GRAYSCALE("Grayscale"),
    SEPIA("Sepia"),
    HIGH_CONTRAST("High contrast")
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

    fun sourceBytes(context: Context, uri: Uri): Long? =
        context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { descriptor ->
            descriptor.length.takeIf { it >= 0L }
        }

    fun resize(bitmap: Bitmap, width: Int, height: Int): Bitmap =
        Bitmap.createScaledBitmap(bitmap, width.coerceAtLeast(1), height.coerceAtLeast(1), true)

    fun cropCenter(bitmap: Bitmap, mode: String): Bitmap {
        if (mode == "Original") return bitmap
        val targetRatio = when (mode) {
            "1:1" -> 1f
            "4:5" -> 4f / 5f
            "16:9" -> 16f / 9f
            "9:16" -> 9f / 16f
            else -> bitmap.width.toFloat() / bitmap.height.toFloat()
        }
        val sourceRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
        val cropWidth: Int
        val cropHeight: Int
        if (sourceRatio > targetRatio) {
            cropHeight = bitmap.height
            cropWidth = (cropHeight * targetRatio).toInt().coerceAtLeast(1)
        } else {
            cropWidth = bitmap.width
            cropHeight = (cropWidth / targetRatio).toInt().coerceAtLeast(1)
        }
        val left = ((bitmap.width - cropWidth) / 2).coerceAtLeast(0)
        val top = ((bitmap.height - cropHeight) / 2).coerceAtLeast(0)
        return Bitmap.createBitmap(bitmap, left, top, cropWidth, cropHeight)
    }

    fun rotate(bitmap: Bitmap, angle: Int, flipHorizontal: Boolean, flipVertical: Boolean): Bitmap {
        val matrix = Matrix().apply {
            postRotate(angle.toFloat())
            if (flipHorizontal) postScale(-1f, 1f)
            if (flipVertical) postScale(1f, -1f)
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    fun filter(bitmap: Bitmap, filter: ImageFilter): Bitmap {
        if (filter == ImageFilter.ORIGINAL) return bitmap
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val matrix = when (filter) {
            ImageFilter.GRAYSCALE -> ColorMatrix().apply { setSaturation(0f) }
            ImageFilter.SEPIA -> ColorMatrix(
                floatArrayOf(
                    0.393f, 0.769f, 0.189f, 0f, 0f,
                    0.349f, 0.686f, 0.168f, 0f, 0f,
                    0.272f, 0.534f, 0.131f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            ImageFilter.HIGH_CONTRAST -> ColorMatrix().apply {
                set(
                    floatArrayOf(
                        1.35f, 0f, 0f, 0f, -35f,
                        0f, 1.35f, 0f, 0f, -35f,
                        0f, 0f, 1.35f, 0f, -35f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            }
            ImageFilter.ORIGINAL -> ColorMatrix()
        }
        paint.colorFilter = ColorMatrixColorFilter(matrix)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return output
    }

    fun watermark(bitmap: Bitmap, text: String, opacity: Int, position: String): Bitmap {
        val safeText = text.trim().ifBlank { "IMAGE TOOLS" }
        val output = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            alpha = opacity.coerceIn(10, 100) * 255 / 100
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = (min(bitmap.width, bitmap.height) * 0.065f).coerceIn(28f, 120f)
            setShadowLayer(textSize * 0.14f, 0f, textSize * 0.08f, android.graphics.Color.BLACK)
        }
        val margin = textSize.toInt()
        val width = paint.measureText(safeText)
        val x = when (position) {
            "Bottom left" -> margin.toFloat()
            "Center" -> (bitmap.width - width) / 2f
            else -> bitmap.width - width - margin
        }
        val y = when (position) {
            "Top left" -> margin + textSize
            "Center" -> (bitmap.height + textSize) / 2f
            "Bottom left" -> bitmap.height - margin
            else -> bitmap.height - margin
        }
        canvas.drawText(safeText, x.coerceAtLeast(0f), y.coerceIn(textSize, bitmap.height.toFloat() - 4f), paint)
        return output
    }

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

    fun save(
        context: Context,
        bytes: ByteArray,
        format: OutputFormat,
        prefix: String,
        width: Int,
        height: Int
    ): ImageResult {
        val values = ContentValues().apply {
            put(
                MediaStore.Images.Media.DISPLAY_NAME,
                prefix + "_" + System.currentTimeMillis() + "." + format.extension
            )
            put(MediaStore.Images.Media.MIME_TYPE, format.mime)
            put(
                MediaStore.Images.Media.RELATIVE_PATH,
                Environment.DIRECTORY_PICTURES + "/Image Tools"
            )
            if (Build.VERSION.SDK_INT >= 29) {
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val uri = context.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            values
        ) ?: error("save")
        try {
            context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) } ?: error("write")
            if (Build.VERSION.SDK_INT >= 29) {
                context.contentResolver.update(
                    uri,
                    ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) },
                    null,
                    null
                )
            }
        } catch (error: Exception) {
            context.contentResolver.delete(uri, null, null)
            throw error
        }
        return ImageResult(uri, bytes.size.toLong(), width, height, format)
    }

    fun saveToUri(
        context: Context,
        uri: Uri,
        bytes: ByteArray,
        format: OutputFormat,
        width: Int,
        height: Int
    ): ImageResult {
        context.contentResolver.openOutputStream(uri, "w")?.use { it.write(bytes) } ?: error("write")
        return ImageResult(uri, bytes.size.toLong(), width, height, format)
    }

    fun humanBytes(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return DecimalFormat("#,##0.0").format(kb) + " KB"
        return DecimalFormat("#,##0.0").format(kb / 1024.0) + " MB"
    }
}