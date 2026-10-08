package com.nexauren.imagetools.media

import androidx.exifinterface.media.ExifInterface
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
    HIGH_CONTRAST("High contrast"),
    VIVID("Vivid"),
    COOL("Cool"),
    WARM("Warm"),
    FADE("Fade"),
    CINEMATIC("Cinematic")
}

data class ImageResult(
    val uri: Uri,
    val bytes: Long,
    val width: Int,
    val height: Int,
    val format: OutputFormat
)

object ImageProcessor {
    fun decode(context: Context, uri: Uri): Bitmap? {
        val bitmap = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) } ?: return null
        val orientation = runCatching {
            context.contentResolver.openInputStream(uri)?.use {
                ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            }
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
        return applyExifOrientation(bitmap, orientation)
    }

    fun sourceBytes(context: Context, uri: Uri): Long? =
        context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { descriptor ->
            descriptor.length.takeIf { it >= 0L }
        }

    fun resize(bitmap: Bitmap, width: Int, height: Int): Bitmap =
        Bitmap.createScaledBitmap(bitmap, width.coerceAtLeast(1), height.coerceAtLeast(1), true)

    fun cropToAspect(bitmap: Bitmap, targetRatio: Float): Bitmap {
        require(targetRatio > 0f) { "Invalid aspect ratio." }
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
            ImageFilter.VIVID -> ColorMatrix().apply {
                setSaturation(1.35f)
                postConcat(
                    ColorMatrix(
                        floatArrayOf(
                            1.05f, 0f, 0f, 0f, -6f,
                            0f, 1.05f, 0f, 0f, -6f,
                            0f, 0f, 1.05f, 0f, -6f,
                            0f, 0f, 0f, 1f, 0f
                        )
                    )
                )
            }
            ImageFilter.COOL -> ColorMatrix(
                floatArrayOf(
                    0.94f, 0f, 0f, 0f, 0f,
                    0f, 1.0f, 0f, 0f, 0f,
                    0f, 0f, 1.10f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            ImageFilter.WARM -> ColorMatrix(
                floatArrayOf(
                    1.10f, 0f, 0f, 0f, 0f,
                    0f, 1.02f, 0f, 0f, 0f,
                    0f, 0f, 0.92f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            ImageFilter.FADE -> ColorMatrix(
                floatArrayOf(
                    0.86f, 0f, 0f, 0f, 18f,
                    0f, 0.86f, 0f, 0f, 18f,
                    0f, 0f, 0.86f, 0f, 18f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            ImageFilter.CINEMATIC -> ColorMatrix(
                floatArrayOf(
                    1.10f, 0f, 0f, 0f, -10f,
                    0f, 1.04f, 0f, 0f, -4f,
                    0f, 0f, 0.94f, 0f, 8f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
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
        val watermarkTextSize = (min(bitmap.width, bitmap.height) * 0.065f).coerceIn(28f, 120f)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            alpha = opacity.coerceIn(10, 100) * 255 / 100
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = watermarkTextSize
            setShadowLayer(watermarkTextSize * 0.14f, 0f, watermarkTextSize * 0.08f, android.graphics.Color.BLACK)
        }
        val margin = watermarkTextSize.toInt()
        val width = paint.measureText(safeText)
        val x = when (position) {
            "Top left", "Bottom left" -> margin.toFloat()
            "Center" -> (bitmap.width - width) / 2f
            else -> bitmap.width - width - margin
        }
        val y = when (position) {
            "Top left" -> margin.toFloat() + watermarkTextSize
            "Center" -> (bitmap.height.toFloat() + watermarkTextSize) / 2f
            "Bottom left" -> bitmap.height.toFloat() - margin
            else -> bitmap.height.toFloat() - margin
        }
        canvas.drawText(safeText, x.coerceAtLeast(0f), y.coerceIn(watermarkTextSize, bitmap.height.toFloat() - 4f), paint)
        return output
    }

    fun encodeJpegUnderSize(
        bitmap: Bitmap,
        targetBytes: Int,
        format: OutputFormat = OutputFormat.JPEG
    ): ByteArray {
        require(targetBytes > 0) { "Target size must be greater than zero." }
        var low = 1
        var high = 100
        var best = encode(bitmap, format, 1)
        while (low <= high) {
            val mid = (low + high) ushr 1
            val candidate = encode(bitmap, format, mid)
            if (candidate.size <= targetBytes) {
                best = candidate
                low = mid + 1
            } else {
                high = mid - 1
            }
        }
        return best
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

    fun saveToFolder(
        context: Context,
        treeUri: Uri,
        bytes: ByteArray,
        format: OutputFormat,
        prefix: String,
        width: Int,
        height: Int
    ): ImageResult {
        val fileName = prefix + "_" + System.currentTimeMillis() + "." + format.extension
        val uri = android.provider.DocumentsContract.createDocument(
            context.contentResolver,
            treeUri,
            format.mime,
            fileName
        ) ?: error("create")
        return try {
            saveToUri(context, uri, bytes, format, width, height)
        } catch (error: Exception) {
            context.contentResolver.delete(uri, null, null)
            throw error
        }
    }

    private fun applyExifOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            androidx.exifinterface.media.ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)
            androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
            androidx.exifinterface.media.ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.setScale(1f, -1f)
            androidx.exifinterface.media.ExifInterface.ORIENTATION_TRANSPOSE -> { matrix.setRotate(90f); matrix.postScale(-1f, 1f) }
            androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
            androidx.exifinterface.media.ExifInterface.ORIENTATION_TRANSVERSE -> { matrix.setRotate(270f); matrix.postScale(-1f, 1f) }
            androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(270f)
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    fun humanBytes(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return DecimalFormat("#,##0.0").format(kb) + " KB"
        return DecimalFormat("#,##0.0").format(kb / 1024.0) + " MB"
    }
}