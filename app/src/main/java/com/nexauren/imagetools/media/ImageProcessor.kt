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
import kotlin.math.sqrt
import kotlin.math.pow
import kotlin.math.roundToInt

enum class OutputFormat(val label: String, val mime: String, val extension: String) {
    JPEG("JPEG", "image/jpeg", "jpg"),
    PNG("PNG", "image/png", "png"),
    WEBP("WEBP", "image/webp", "webp")
}

enum class ImageFilter(val label: String) {
    ORIGINAL("Original"),
    GRAYSCALE("Grayscale"),
    SEPIA("Sepia"),
    HIGH_CONTRAST("High Contrast"),
    VIBRANT("Vibrant"),
    WARM("Warm"),
    COOL("Cool"),
    FADE("Fade")
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
            ImageFilter.VIBRANT -> adjust(bitmap, 0.02f, 0.10f, 1.30f)
            ImageFilter.WARM -> colorGrade(bitmap, 8f, 1.04f, 0.88f)
            ImageFilter.COOL -> colorGrade(bitmap, -8f, 0.94f, 1.06f)
            ImageFilter.FADE -> adjust(bitmap, 0.04f, -0.08f, 0.86f)
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
            this.textSize = textSize
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

    fun autoEnhance(bitmap: Bitmap): Bitmap {
        val sampleStep = max(1, min(bitmap.width, bitmap.height) / 160)
        var sum = 0.0
        var count = 0
        var sumSq = 0.0
        val pixel = IntArray(1)
        var y = 0
        while (y < bitmap.height) {
            var x = 0
            while (x < bitmap.width) {
                bitmap.getPixels(pixel, 0, 1, x, y, 1, 1)
                val c = pixel[0]
                val l = (0.2126 * Color.red(c) + 0.7152 * Color.green(c) + 0.0722 * Color.blue(c)) / 255.0
                sum += l
                sumSq += l * l
                count++
                x += sampleStep
            }
            y += sampleStep
        }
        val mean = if (count == 0) 0.5 else sum / count
        val variance = max(0.0, (sumSq / max(1, count)) - mean * mean)
        val target = 0.52
        val brightness = ((target - mean) * 1.2).toFloat().coerceIn(-0.18f, 0.18f)
        val contrast = (0.08f + ((0.20 - variance) * 0.55)).toFloat().coerceIn(0.04f, 0.16f)
        return adjust(bitmap, brightness, contrast, 1.08f)
    }

    fun sharpen(bitmap: Bitmap, amount: Float): Bitmap {
        val strength = amount.coerceIn(0.15f, 1.0f)
        val width = bitmap.width
        val height = bitmap.height
        val source = IntArray(width * height)
        bitmap.getPixels(source, 0, width, 0, 0, width, height)
        val output = source.copyOf()
        if (width < 3 || height < 3) {
            return bitmap.copy(Bitmap.Config.ARGB_8888, true)
        }
        for (y in 1 until height - 1) {
            val row = y * width
            for (x in 1 until width - 1) {
                val i = row + x
                val c = source[i]
                val up = source[i - width]
                val down = source[i + width]
                val left = source[i - 1]
                val right = source[i + 1]
                fun channel(shift: Int): Int {
                    val center = (c shr shift) and 255
                    val neighbors = ((up shr shift) and 255) + ((down shr shift) and 255) +
                        ((left shr shift) and 255) + ((right shr shift) and 255)
                    return (center + strength * (4f * center - neighbors))
                        .toInt()
                        .coerceIn(0, 255)
                }
                output[i] = (Color.alpha(c) shl 24) or
                    (channel(16) shl 16) or
                    (channel(8) shl 8) or
                    channel(0)
            }
        }
        return Bitmap.createBitmap(output, width, height, Bitmap.Config.ARGB_8888)
    }


    private fun colorGrade(bitmap: Bitmap, redOffset: Float, redScale: Float, blueScale: Float): Bitmap {
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.colorFilter = ColorMatrixColorFilter(
            ColorMatrix(
                floatArrayOf(
                    redScale, 0f, 0f, 0f, redOffset,
                    0f, 1f, 0f, 0f, 0f,
                    0f, 0f, blueScale, 0f, -redOffset,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        )
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return output
    }

    fun blur(bitmap: Bitmap, strength: Float): Bitmap {
        val amount = strength.coerceIn(0.1f, 1f)
        val scale = (20f - amount * 14f).toInt().coerceIn(5, 18)
        val small = Bitmap.createScaledBitmap(
            bitmap,
            max(1, bitmap.width / scale),
            max(1, bitmap.height / scale),
            true
        )
        val blurred = Bitmap.createScaledBitmap(small, bitmap.width, bitmap.height, true)
        small.recycle()
        return blurred
    }

    fun negative(bitmap: Bitmap): Bitmap {
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.colorFilter = ColorMatrixColorFilter(
            ColorMatrix(
                floatArrayOf(
                    -1f, 0f, 0f, 0f, 255f,
                    0f, -1f, 0f, 0f, 255f,
                    0f, 0f, -1f, 0f, 255f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        )
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return output
    }

    fun duotone(bitmap: Bitmap, shadowColor: Int, highlightColor: Int): Bitmap {
        val source = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(source, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val sr = Color.red(shadowColor)
        val sg = Color.green(shadowColor)
        val sb = Color.blue(shadowColor)
        val hr = Color.red(highlightColor)
        val hg = Color.green(highlightColor)
        val hb = Color.blue(highlightColor)

        for (i in source.indices) {
            val c = source[i]
            val luminance = (
                0.2126f * Color.red(c) +
                0.7152f * Color.green(c) +
                0.0722f * Color.blue(c)
            ) / 255f
            source[i] = Color.argb(
                Color.alpha(c),
                (sr + (hr - sr) * luminance).toInt().coerceIn(0, 255),
                (sg + (hg - sg) * luminance).toInt().coerceIn(0, 255),
                (sb + (hb - sb) * luminance).toInt().coerceIn(0, 255)
            )
        }
        return Bitmap.createBitmap(source, bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
    }

    fun socialCanvas(bitmap: Bitmap, preset: String, backgroundColor: Int): Bitmap {
        val (targetW, targetH) = when (preset) {
            "Story 9:16" -> 1080 to 1920
            "Portrait 4:5" -> 1080 to 1350
            "Landscape 16:9" -> 1920 to 1080
            else -> 1080 to 1080
        }
        val fit = min(targetW / bitmap.width.toFloat(), targetH / bitmap.height.toFloat())
        val drawW = (bitmap.width * fit).toInt().coerceAtLeast(1)
        val drawH = (bitmap.height * fit).toInt().coerceAtLeast(1)
        val output = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        canvas.drawColor(backgroundColor)
        val left = (targetW - drawW) / 2
        val top = (targetH - drawH) / 2
        canvas.drawBitmap(
            bitmap,
            null,
            Rect(left, top, left + drawW, top + drawH),
            Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        )
        return output
    }

    fun roundedCorners(bitmap: Bitmap, radius: Float): Bitmap {
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val path = Path().apply {
            addRoundRect(
                0f,
                0f,
                bitmap.width.toFloat(),
                bitmap.height.toFloat(),
                radius.coerceAtLeast(0f),
                radius.coerceAtLeast(0f),
                Path.Direction.CW
            )
        }
        canvas.save()
        canvas.clipPath(path)
        canvas.drawBitmap(bitmap, 0f, 0f, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        canvas.restore()
        return output
    }

    fun backgroundCutout(bitmap: Bitmap, tolerance: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width < 3 || height < 3) return bitmap.copy(Bitmap.Config.ARGB_8888, true)

        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        fun patchAverage(centerX: Int, centerY: Int): Int {
            val patch = min(18, min(width, height) / 6).coerceAtLeast(2)
            var r = 0L
            var g = 0L
            var b = 0L
            var n = 0
            val left = (centerX - patch / 2).coerceIn(0, width - 1)
            val right = (centerX + patch / 2).coerceIn(left + 1, width)
            val top = (centerY - patch / 2).coerceIn(0, height - 1)
            val bottom = (centerY + patch / 2).coerceIn(top + 1, height)
            for (yy in top until bottom) {
                for (xx in left until right) {
                    val c = pixels[yy * width + xx]
                    r += Color.red(c)
                    g += Color.green(c)
                    b += Color.blue(c)
                    n++
                }
            }
            return Color.rgb(
                (r / max(1, n)).toInt(),
                (g / max(1, n)).toInt(),
                (b / max(1, n)).toInt()
            )
        }

        val samplePoints = listOf(
            0 to 0,
            width / 2 to 0,
            width - 1 to 0,
            0 to height / 2,
            width - 1 to height / 2,
            0 to height - 1,
            width / 2 to height - 1,
            width - 1 to height - 1
        )
        val backgroundSamples = samplePoints.map { patchAverage(it.first, it.second) }

        fun rgbDistance(a: Int, b: Int): Float {
            val dr = (Color.red(a) - Color.red(b)).toFloat()
            val dg = (Color.green(a) - Color.green(b)).toFloat()
            val db = (Color.blue(a) - Color.blue(b)).toFloat()
            return sqrt(dr * dr + dg * dg + db * db)
        }

        fun backgroundDistance(c: Int): Float =
            backgroundSamples.minOf { rgbDistance(c, it) }

        val threshold = tolerance.coerceIn(12, 150).toFloat()
        val localThreshold = (threshold * 0.72f).coerceIn(8f, 95f)
        val visited = BooleanArray(width * height)
        val queue = IntArray(width * height)
        var head = 0
        var tail = 0

        fun tryEnqueue(index: Int, parentColor: Int?) {
            if (index !in pixels.indices || visited[index]) return
            val color = pixels[index]
            if (backgroundDistance(color) > threshold) return
            if (parentColor != null && rgbDistance(color, parentColor) > localThreshold) return
            visited[index] = true
            queue[tail++] = index
        }

        for (x in 0 until width) {
            tryEnqueue(x, null)
            tryEnqueue((height - 1) * width + x, null)
        }
        for (y in 0 until height) {
            tryEnqueue(y * width, null)
            tryEnqueue(y * width + width - 1, null)
        }

        while (head < tail) {
            val index = queue[head++]
            val currentColor = pixels[index]
            val x = index % width
            val y = index / width
            if (x > 0) tryEnqueue(index - 1, currentColor)
            if (x < width - 1) tryEnqueue(index + 1, currentColor)
            if (y > 0) tryEnqueue(index - width, currentColor)
            if (y < height - 1) tryEnqueue(index + width, currentColor)
        }

        val output = pixels.copyOf()
        for (index in output.indices) {
            if (!visited[index]) continue
            output[index] = output[index] and 0x00FFFFFF
        }

        // Feather only the cutout boundary, preserving a cleaner subject edge.
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val index = y * width + x
                if (visited[index]) continue
                var erasedNeighbors = 0
                for (dy in -1..1) {
                    for (dx in -1..1) {
                        if (dx == 0 && dy == 0) continue
                        if (visited[(y + dy) * width + (x + dx)]) erasedNeighbors++
                    }
                }
                if (erasedNeighbors > 0) {
                    val alpha = when {
                        erasedNeighbors >= 5 -> 125
                        erasedNeighbors >= 3 -> 165
                        else -> 205
                    }
                    output[index] = Color.argb(
                        min(Color.alpha(output[index]), alpha),
                        Color.red(output[index]),
                        Color.green(output[index]),
                        Color.blue(output[index])
                    )
                }
            }
        }

        return Bitmap.createBitmap(output, width, height, Bitmap.Config.ARGB_8888)
    }

    fun portraitBlur(bitmap: Bitmap, intensity: Float): Bitmap {
        val strength = intensity.coerceIn(0.15f, 1f)
        val scale = 12
        val smallW = max(1, bitmap.width / scale)
        val smallH = max(1, bitmap.height / scale)
        val small = Bitmap.createScaledBitmap(bitmap, smallW, smallH, true)
        val blurred = Bitmap.createScaledBitmap(small, bitmap.width, bitmap.height, true)
        small.recycle()

        val width = bitmap.width
        val height = bitmap.height
        val source = IntArray(width * height)
        val blur = IntArray(width * height)
        bitmap.getPixels(source, 0, width, 0, 0, width, height)
        blurred.getPixels(blur, 0, width, 0, 0, width, height)
        blurred.recycle()

        val output = IntArray(source.size)
        val cx = width * 0.5f
        val cy = height * 0.46f
        val rx = width * 0.38f
        val ry = height * 0.44f
        for (y in 0 until height) {
            for (x in 0 until width) {
                val dx = (x - cx) / rx
                val dy = (y - cy) / ry
                val distance = sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                val outside = ((distance - 0.70f) / 0.50f).coerceIn(0f, 1f)
                val mix = outside * strength
                val i = y * width + x
                val a = Color.alpha(source[i])
                val r = (Color.red(source[i]) * (1f - mix) + Color.red(blur[i]) * mix).toInt()
                val g = (Color.green(source[i]) * (1f - mix) + Color.green(blur[i]) * mix).toInt()
                val b = (Color.blue(source[i]) * (1f - mix) + Color.blue(blur[i]) * mix).toInt()
                output[i] = Color.argb(a, r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255))
            }
        }
        return Bitmap.createBitmap(output, width, height, Bitmap.Config.ARGB_8888)
    }


    suspend fun smartBackgroundCutout(context: Context, bitmap: Bitmap, tolerance: Int): Bitmap {
        return SubjectBackgroundRemover.remove(context, bitmap)
            ?: backgroundCutout(bitmap, tolerance)
    }

    fun exposure(bitmap: Bitmap, stops: Float): Bitmap {
        val factor = Math.pow(2.0, stops.coerceIn(-2f, 2f).toDouble()).toFloat()
        return adjust(bitmap, (factor - 1f) * 0.30f, (factor - 1f) * 0.08f, 1f)
    }

    fun gamma(bitmap: Bitmap, gamma: Float): Bitmap {
        val g = gamma.coerceIn(0.25f, 3f)
        val lut = IntArray(256) { value ->
            (((value / 255f).toDouble().pow(1.0 / g) * 255.0).toInt()).coerceIn(0, 255)
        }
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        for (i in pixels.indices) {
            val c = pixels[i]
            pixels[i] = Color.argb(
                Color.alpha(c),
                lut[Color.red(c)],
                lut[Color.green(c)],
                lut[Color.blue(c)]
            )
        }
        return Bitmap.createBitmap(pixels, bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
    }

    fun rgbBalance(bitmap: Bitmap, red: Float, green: Float, blue: Float): Bitmap {
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.colorFilter = ColorMatrixColorFilter(
            ColorMatrix(
                floatArrayOf(
                    1f, 0f, 0f, 0f, red.coerceIn(-80f, 80f),
                    0f, 1f, 0f, 0f, green.coerceIn(-80f, 80f),
                    0f, 0f, 1f, 0f, blue.coerceIn(-80f, 80f),
                    0f, 0f, 0f, 1f, 0f
                )
            )
        )
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return output
    }

    fun colorTint(bitmap: Bitmap, red: Int, green: Int, blue: Int, amount: Float): Bitmap {
        val mix = amount.coerceIn(0f, 1f)
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        for (i in pixels.indices) {
            val c = pixels[i]
            val r = (Color.red(c) * (1f - mix) + red * mix).toInt().coerceIn(0, 255)
            val g = (Color.green(c) * (1f - mix) + green * mix).toInt().coerceIn(0, 255)
            val b = (Color.blue(c) * (1f - mix) + blue * mix).toInt().coerceIn(0, 255)
            pixels[i] = Color.argb(Color.alpha(c), r, g, b)
        }
        return Bitmap.createBitmap(pixels, bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
    }

    fun vignette(bitmap: Bitmap, strength: Float): Bitmap {
        val amount = strength.coerceIn(0f, 1f)
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        val cx = width / 2f
        val cy = height / 2f
        val maxDistance = sqrt(cx * cx + cy * cy)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val dx = x - cx
                val dy = y - cy
                val distance = sqrt(dx * dx + dy * dy) / maxDistance
                val factor = 1f - (distance.coerceIn(0f, 1f).pow(2f) * amount * 0.72f)
                val i = y * width + x
                val c = pixels[i]
                pixels[i] = Color.argb(
                    Color.alpha(c),
                    (Color.red(c) * factor).toInt().coerceIn(0, 255),
                    (Color.green(c) * factor).toInt().coerceIn(0, 255),
                    (Color.blue(c) * factor).toInt().coerceIn(0, 255)
                )
            }
        }
        return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
    }

    fun filmGrain(bitmap: Bitmap, amount: Float): Bitmap {
        val strength = (amount.coerceIn(0f, 1f) * 54f).toInt()
        if (strength == 0) return bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val hash = (x * 73856093) xor (y * 19349663)
                val noise = ((hash ushr 16) and 255) - 128
                val delta = noise * strength / 128
                val i = y * width + x
                val c = pixels[i]
                pixels[i] = Color.argb(
                    Color.alpha(c),
                    (Color.red(c) + delta).coerceIn(0, 255),
                    (Color.green(c) + delta).coerceIn(0, 255),
                    (Color.blue(c) + delta).coerceIn(0, 255)
                )
            }
        }
        return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
    }

    fun posterize(bitmap: Bitmap, levels: Int): Bitmap {
        val count = levels.coerceIn(2, 16)
        val step = 255f / (count - 1)
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        for (i in pixels.indices) {
            val c = pixels[i]
            fun quantize(value: Int): Int =
                (((value / 255f) * (count - 1)).roundToInt() * step).toInt().coerceIn(0, 255)
            pixels[i] = Color.argb(
                Color.alpha(c),
                quantize(Color.red(c)),
                quantize(Color.green(c)),
                quantize(Color.blue(c))
            )
        }
        return Bitmap.createBitmap(pixels, bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
    }

    fun edgeDetect(bitmap: Bitmap, strength: Float): Bitmap {
        val amount = strength.coerceIn(0.1f, 1f)
        val width = bitmap.width
        val height = bitmap.height
        if (width < 3 || height < 3) return bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val source = IntArray(width * height)
        bitmap.getPixels(source, 0, width, 0, 0, width, height)
        val output = source.copyOf()
        fun lum(c: Int): Float =
            0.2126f * Color.red(c) + 0.7152f * Color.green(c) + 0.0722f * Color.blue(c)
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val i = y * width + x
                val gx = -lum(source[i - width - 1]) - 2f * lum(source[i - 1]) - lum(source[i + width - 1]) +
                    lum(source[i - width + 1]) + 2f * lum(source[i + 1]) + lum(source[i + width + 1])
                val gy = -lum(source[i - width - 1]) - 2f * lum(source[i - width]) - lum(source[i - width + 1]) +
                    lum(source[i + width - 1]) + 2f * lum(source[i + width]) + lum(source[i + width + 1])
                val edge = (sqrt(gx * gx + gy * gy) / 4f * amount).coerceIn(0f, 255f).toInt()
                output[i] = Color.argb(Color.alpha(source[i]), edge, edge, edge)
            }
        }
        return Bitmap.createBitmap(output, width, height, Bitmap.Config.ARGB_8888)
    }

    fun highlightsShadows(bitmap: Bitmap, shadows: Float, highlights: Float): Bitmap {
        val shadowAmount = shadows.coerceIn(-1f, 1f)
        val highlightAmount = highlights.coerceIn(-1f, 1f)
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        for (i in pixels.indices) {
            val c = pixels[i]
            val luminance = (0.2126f * Color.red(c) + 0.7152f * Color.green(c) + 0.0722f * Color.blue(c)) / 255f
            val weight = if (luminance < 0.5f) (1f - luminance * 2f) else (luminance - 0.5f) * 2f
            val delta = if (luminance < 0.5f) shadowAmount * weight * 85f else highlightAmount * weight * 85f
            pixels[i] = Color.argb(
                Color.alpha(c),
                (Color.red(c) + delta).toInt().coerceIn(0, 255),
                (Color.green(c) + delta).toInt().coerceIn(0, 255),
                (Color.blue(c) + delta).toInt().coerceIn(0, 255)
            )
        }
        return Bitmap.createBitmap(pixels, bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
    }

    fun photoStrip(
        bitmaps: List<Bitmap>,
        vertical: Boolean,
        gap: Int = 18,
        background: Int = Color.WHITE
    ): Bitmap {
        require(bitmaps.isNotEmpty())
        val safeGap = gap.coerceIn(0, 64)
        val target = bitmaps.maxOf { if (vertical) it.width else it.height }.coerceIn(480, 1600)
        val fitted = bitmaps.map { source ->
            val ratio = if (vertical) target.toFloat() / source.width else target.toFloat() / source.height
            Bitmap.createScaledBitmap(
                source,
                (source.width * ratio).toInt().coerceAtLeast(1),
                (source.height * ratio).toInt().coerceAtLeast(1),
                true
            )
        }
        val width = if (vertical) target else fitted.sumOf { it.width } + safeGap * (fitted.size - 1)
        val height = if (vertical) fitted.sumOf { it.height } + safeGap * (fitted.size - 1) else target
        val output = Bitmap.createBitmap(width.coerceAtLeast(1), height.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        canvas.drawColor(background)
        var cursor = 0
        fitted.forEachIndexed { index, bitmap ->
            val left = if (vertical) (width - bitmap.width) / 2 else cursor
            val top = if (vertical) cursor else (height - bitmap.height) / 2
            canvas.drawBitmap(bitmap, left.toFloat(), top.toFloat(), Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
            cursor += if (vertical) bitmap.height else bitmap.width
            if (index != fitted.lastIndex) cursor += safeGap
        }
        fitted.forEach { if (!it.isRecycled) it.recycle() }
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
