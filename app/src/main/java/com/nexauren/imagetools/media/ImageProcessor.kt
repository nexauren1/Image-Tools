package com.nexauren.imagetools.media

import android.content.ContentValues
import android.content.Context
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.DocumentsContract
import java.io.ByteArrayOutputStream
import java.text.DecimalFormat
import kotlin.math.max
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
    HIGH_CONTRAST("High Contrast")
}

data class ImageResult(
    val uri: Uri,
    val bytes: Long,
    val width: Int,
    val height: Int,
    val format: OutputFormat
)

data class SaveOutcome(
    val result: ImageResult,
    val usedDefaultGallery: Boolean
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
            "3:4" -> 3f / 4f
            "16:9" -> 16f / 9f
            "9:16" -> 9f / 16f
            "4:3" -> 4f / 3f
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

    fun adjust(bitmap: Bitmap, brightness: Float, contrast: Float, saturation: Float): Bitmap {
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val b = brightness.coerceIn(-1f, 1f) * 255f
        val c = contrast.coerceIn(-1f, 1f)
        val scale = c + 1f
        val offset = 128f * (1f - scale) + b
        val matrix = ColorMatrix().apply {
            setSaturation(saturation.coerceIn(0f, 2f))
            postConcat(
                ColorMatrix(
                    floatArrayOf(
                        scale, 0f, 0f, 0f, offset,
                        0f, scale, 0f, 0f, offset,
                        0f, 0f, scale, 0f, offset,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            )
        }
        paint.colorFilter = ColorMatrixColorFilter(matrix)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return output
    }

    fun filter(bitmap: Bitmap, filter: ImageFilter): Bitmap {
        if (filter == ImageFilter.ORIGINAL) return bitmap
        return when (filter) {
            ImageFilter.GRAYSCALE -> adjust(bitmap, 0f, 0f, 0f)
            ImageFilter.SEPIA -> {
                val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(output)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG)
                paint.colorFilter = ColorMatrixColorFilter(
                    ColorMatrix(
                        floatArrayOf(
                            0.393f, 0.769f, 0.189f, 0f, 0f,
                            0.349f, 0.686f, 0.168f, 0f, 0f,
                            0.272f, 0.534f, 0.131f, 0f, 0f,
                            0f, 0f, 0f, 1f, 0f
                        )
                    )
                )
                canvas.drawBitmap(bitmap, 0f, 0f, paint)
                output
            }
            ImageFilter.HIGH_CONTRAST -> adjust(bitmap, 0f, 0.35f, 1.0f)
            ImageFilter.ORIGINAL -> bitmap
        }
    }

    fun frame(bitmap: Bitmap, border: Int, backgroundColor: Int): Bitmap {
        val pad = border.coerceIn(0, min(bitmap.width, bitmap.height) / 2)
        val output = Bitmap.createBitmap(bitmap.width + pad * 2, bitmap.height + pad * 2, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        canvas.drawColor(backgroundColor)
        canvas.drawBitmap(bitmap, pad.toFloat(), pad.toFloat(), Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        return output
    }

    fun pixelate(bitmap: Bitmap, blockSize: Int): Bitmap {
        val size = blockSize.coerceIn(2, 48)
        val smallW = max(1, bitmap.width / size)
        val smallH = max(1, bitmap.height / size)
        val small = Bitmap.createScaledBitmap(bitmap, smallW, smallH, false)
        val output = Bitmap.createScaledBitmap(small, bitmap.width, bitmap.height, false)
        if (small !== bitmap && !small.isRecycled) small.recycle()
        return output
    }

    fun meme(bitmap: Bitmap, topText: String, bottomText: String): Bitmap {
        val output = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(output)
        val textSize = (bitmap.width * 0.085f).coerceIn(34f, 110f)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = textSize
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            style = Paint.Style.FILL
            setShadowLayer(textSize * 0.11f, 0f, 0f, Color.BLACK)
        }
        if (topText.isNotBlank()) {
            canvas.drawText(topText.trim().uppercase(), bitmap.width / 2f, textSize + 16f, paint)
        }
        if (bottomText.isNotBlank()) {
            canvas.drawText(bottomText.trim().uppercase(), bitmap.width / 2f, bitmap.height - 20f, paint)
        }
        return output
    }

    fun collage(bitmaps: List<Bitmap>, columns: Int, gap: Int = 10, background: Int = Color.WHITE): Bitmap {
        require(bitmaps.isNotEmpty())
        val cols = columns.coerceIn(1, 3)
        val rows = (bitmaps.size + cols - 1) / cols
        val cell = bitmaps.maxOf { max(it.width, it.height) }.coerceIn(320, 1400)
        val width = cols * cell + (cols + 1) * gap
        val height = rows * cell + (rows + 1) * gap
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        canvas.drawColor(background)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        bitmaps.forEachIndexed { index, bitmap ->
            val row = index / cols
            val col = index % cols
            val left = gap + col * (cell + gap)
            val top = gap + row * (cell + gap)
            val ratio = min(cell / bitmap.width.toFloat(), cell / bitmap.height.toFloat())
            val dw = (bitmap.width * ratio).toInt()
            val dh = (bitmap.height * ratio).toInt()
            val dst = Rect(
                left + (cell - dw) / 2,
                top + (cell - dh) / 2,
                left + (cell - dw) / 2 + dw,
                top + (cell - dh) / 2 + dh
            )
            canvas.drawBitmap(bitmap, null, dst, paint)
        }
        return output
    }

    fun watermark(bitmap: Bitmap, text: String, opacity: Int, position: String): Bitmap {
        val safeText = text.trim().ifBlank { "IMAGE TOOLS" }
        val output = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(output)
        val watermarkTextSize = (min(bitmap.width, bitmap.height) * 0.065f).coerceIn(28f, 120f)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            alpha = opacity.coerceIn(10, 100) * 255 / 100
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = watermarkTextSize
            setShadowLayer(watermarkTextSize * 0.14f, 0f, watermarkTextSize * 0.08f, Color.BLACK)
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
            else -> bitmap.height.toFloat() - margin
        }
        canvas.drawText(safeText, x.coerceAtLeast(0f), y.coerceIn(watermarkTextSize, bitmap.height.toFloat() - 4f), paint)
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
        val collection = if (Build.VERSION.SDK_INT >= 29) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, prefix + "_" + System.currentTimeMillis() + "." + format.extension)
            put(MediaStore.Images.Media.MIME_TYPE, format.mime)
            if (Build.VERSION.SDK_INT >= 29) {
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Image Tools")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val uri = context.contentResolver.insert(collection, values) ?: error("Could not create output image")
        try {
            context.contentResolver.openOutputStream(uri, "w")?.use { it.write(bytes) }
                ?: error("Could not open output stream")
            if (Build.VERSION.SDK_INT >= 29) {
                context.contentResolver.update(uri, ContentValues().apply {
                    put(MediaStore.Images.Media.IS_PENDING, 0)
                }, null, null)
            }
        } catch (error: Exception) {
            context.contentResolver.delete(uri, null, null)
            throw error
        }
        return ImageResult(uri, bytes.size.toLong(), width, height, format)
    }

    fun saveToUri(context: Context, uri: Uri, bytes: ByteArray, format: OutputFormat, width: Int, height: Int): ImageResult {
        context.contentResolver.openOutputStream(uri, "w")?.use { it.write(bytes) }
            ?: error("Could not open output stream")
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
        if (!DocumentsContract.isTreeUri(treeUri)) error("Invalid folder")
        val fileName = prefix + "_" + System.currentTimeMillis() + "." + format.extension
        val uri = DocumentsContract.createDocument(context.contentResolver, treeUri, format.mime, fileName)
            ?: error("Could not create output file")
        return try {
            saveToUri(context, uri, bytes, format, width, height)
        } catch (error: Exception) {
            context.contentResolver.delete(uri, null, null)
            throw error
        }
    }

    fun saveWithFallback(
        context: Context,
        preferredTree: Uri?,
        bytes: ByteArray,
        format: OutputFormat,
        prefix: String,
        width: Int,
        height: Int
    ): SaveOutcome {
        if (preferredTree != null) {
            runCatching {
                val persisted = context.contentResolver.persistedUriPermissions
                    .firstOrNull { it.uri == preferredTree && it.isWritePermission }
                if (persisted != null) {
                    return SaveOutcome(
                        saveToFolder(context, preferredTree, bytes, format, prefix, width, height),
                        false
                    )
                }
            }
        }
        return SaveOutcome(
            save(context, bytes, format, prefix, width, height),
            true
        )
    }

    fun humanBytes(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return DecimalFormat("#,##0.0").format(kb) + " KB"
        return DecimalFormat("#,##0.0").format(kb / 1024.0) + " MB"
    }
}
