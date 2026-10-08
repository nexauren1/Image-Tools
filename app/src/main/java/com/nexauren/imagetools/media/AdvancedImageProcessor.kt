package com.nexauren.imagetools.media

import android.content.ContentValues
import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import kotlin.math.roundToInt
import kotlin.math.max

data class PdfResult(val uri: Uri, val bytes: Long)
object AdvancedImageProcessor {
    fun adjustColor(bitmap: Bitmap, brightness: Int, contrast: Int, saturation: Int, warmth: Int): Bitmap {
        val b = brightness.coerceIn(-100, 100).toFloat()
        val c = ((contrast.coerceIn(-100, 100) + 100) / 100f)
        val s = ((saturation.coerceIn(-100, 100) + 100) / 100f)
        val w = warmth.coerceIn(-100, 100).toFloat()
        val offset = 128f * (1f - c) + b * 1.15f
        val r = 1f + w / 500f
        val g = 1f
        val bl = 1f - w / 500f
        val m = ColorMatrix().apply {
            setSaturation(s)
            val tune = ColorMatrix(floatArrayOf(
                c * r, 0f, 0f, 0f, offset,
                0f, c * g, 0f, 0f, offset,
                0f, 0f, c * bl, 0f, offset,
                0f, 0f, 0f, 1f, 0f
            ))
            postConcat(tune)
        }
        return drawWithMatrix(bitmap, m)
    }

    fun autoEnhance(bitmap: Bitmap): Bitmap = adjustColor(bitmap, 8, 14, 12, 3)

    fun exposure(bitmap: Bitmap, amount: Int): Bitmap =
        adjustColor(bitmap, (amount.coerceIn(-100, 100) * 0.85f).toInt(), 0, 0, 0)

    fun tint(bitmap: Bitmap, amount: Int): Bitmap {
        val a = amount.coerceIn(-100, 100) / 100f
        val matrix = ColorMatrix(floatArrayOf(
            1f + 0.18f * a, 0f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f, 0f,
            0f, 0f, 1f - 0.18f * a, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        ))
        return drawWithMatrix(bitmap, matrix)
    }

    fun vignette(bitmap: Bitmap, strength: Int): Bitmap {
        val out = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(out)
        val radius = maxOf(bitmap.width, bitmap.height) * 0.72f
        val alpha = (strength.coerceIn(0, 100) * 2.35f).toInt().coerceIn(0, 235)
        val shader = RadialGradient(
            bitmap.width / 2f,
            bitmap.height / 2f,
            radius,
            intArrayOf(Color.TRANSPARENT, Color.argb(alpha, 0, 0, 0)),
            floatArrayOf(0.40f, 1f),
            Shader.TileMode.CLAMP
        )
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.shader = shader }
        canvas.drawRect(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat(), paint)
        return out
    }

    fun posterize(bitmap: Bitmap, levels: Int): Bitmap {
        val levelCount = levels.coerceIn(2, 12)
        val step = 255f / (levelCount - 1)
        val out = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val src = IntArray(bitmap.width * bitmap.height)
        val dst = IntArray(src.size)
        bitmap.getPixels(src, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        for (i in src.indices) {
            val p = src[i]
            fun q(v: Int): Int = (((v / step).roundToInt()) * step).roundToInt().coerceIn(0, 255)
            dst[i] = Color.argb(Color.alpha(p), q(Color.red(p)), q(Color.green(p)), q(Color.blue(p)))
        }
        out.setPixels(dst, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return out
    }

    fun duotone(bitmap: Bitmap, darkColor: Int, lightColor: Int): Bitmap {
        val out = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val src = IntArray(bitmap.width * bitmap.height)
        val dst = IntArray(src.size)
        bitmap.getPixels(src, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        for (i in src.indices) {
            val p = src[i]
            val luma = (0.2126f * Color.red(p) + 0.7152f * Color.green(p) + 0.0722f * Color.blue(p)) / 255f
            val r = Color.red(darkColor) + (Color.red(lightColor) - Color.red(darkColor)) * luma
            val g = Color.green(darkColor) + (Color.green(lightColor) - Color.green(darkColor)) * luma
            val b = Color.blue(darkColor) + (Color.blue(lightColor) - Color.blue(darkColor)) * luma
            dst[i] = Color.argb(Color.alpha(p), r.roundToInt(), g.roundToInt(), b.roundToInt())
        }
        out.setPixels(dst, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return out
    }

    fun mirror(bitmap: Bitmap, horizontal: Boolean): Bitmap =
        ImageProcessor.rotate(bitmap, 0, horizontal, !horizontal)

    fun denoise(bitmap: Bitmap, strength: Int): Bitmap =
        blur(bitmap, (strength.coerceIn(0, 100) / 34f).toInt().coerceIn(1, 3))

    fun filmGrain(bitmap: Bitmap, strength: Int): Bitmap {
        val amount = strength.coerceIn(0, 100) / 100f
        val w = bitmap.width
        val h = bitmap.height
        val src = IntArray(w * h)
        val dst = IntArray(src.size)
        bitmap.getPixels(src, 0, w, 0, 0, w, h)
        for (i in src.indices) {
            val p = src[i]
            var n = i * -1640531525 + 1013904223
            n = n xor (n ushr 16)
            val noise = (n and 0xFF) - 128
            val delta = (noise * amount * 0.55f).roundToInt()
            dst[i] = Color.argb(
                Color.alpha(p),
                (Color.red(p) + delta).coerceIn(0, 255),
                (Color.green(p) + delta).coerceIn(0, 255),
                (Color.blue(p) + delta).coerceIn(0, 255)
            )
        }
        return Bitmap.createBitmap(dst, w, h, Bitmap.Config.ARGB_8888)
    }

    fun colorPop(bitmap: Bitmap, strength: Int): Bitmap {
        val amount = strength.coerceIn(0, 100) / 100f
        val w = bitmap.width
        val h = bitmap.height
        val src = IntArray(w * h)
        val dst = IntArray(src.size)
        val hsv = FloatArray(3)
        bitmap.getPixels(src, 0, w, 0, 0, w, h)
        for (i in src.indices) {
            val p = src[i]
            Color.colorToHSV(p, hsv)
            val keep = (hsv[1] * amount * 1.55f).coerceIn(0f, 1f)
            val gray = (0.2126f * Color.red(p) + 0.7152f * Color.green(p) + 0.0722f * Color.blue(p)).roundToInt()
            dst[i] = Color.argb(
                Color.alpha(p),
                (gray + (Color.red(p) - gray) * keep).roundToInt().coerceIn(0, 255),
                (gray + (Color.green(p) - gray) * keep).roundToInt().coerceIn(0, 255),
                (gray + (Color.blue(p) - gray) * keep).roundToInt().coerceIn(0, 255)
            )
        }
        return Bitmap.createBitmap(dst, w, h, Bitmap.Config.ARGB_8888)
    }

    fun outline(bitmap: Bitmap, strength: Int): Bitmap {
        val amount = strength.coerceIn(0, 100) / 100f
        val w = bitmap.width
        val h = bitmap.height
        val src = IntArray(w * h)
        val dst = IntArray(src.size)
        bitmap.getPixels(src, 0, w, 0, 0, w, h)

        fun gray(p: Int): Float =
            0.2126f * Color.red(p) + 0.7152f * Color.green(p) + 0.0722f * Color.blue(p)

        for (y in 0 until h) for (x in 0 until w) {
            fun g(dx: Int, dy: Int): Float {
                val xx = (x + dx).coerceIn(0, w - 1)
                val yy = (y + dy).coerceIn(0, h - 1)
                return gray(src[yy * w + xx])
            }
            val gx = -g(-1, -1) + g(1, -1) - 2f * g(-1, 0) + 2f * g(1, 0) - g(-1, 1) + g(1, 1)
            val gy = -g(-1, -1) - 2f * g(0, -1) - g(1, -1) + g(-1, 1) + 2f * g(0, 1) + g(1, 1)
            val edge = (kotlin.math.sqrt(gx * gx + gy * gy) * (0.8f + amount * 2.4f))
                .roundToInt()
                .coerceIn(0, 255)
            val ink = edge
            val paper = (255 - edge * (0.55f + amount * 0.35f)).roundToInt().coerceIn(0, 255)
            val r = (paper - ink * 0.42f).roundToInt().coerceIn(0, 255)
            val gg = (paper - ink * 0.36f).roundToInt().coerceIn(0, 255)
            val b = (paper - ink * 0.18f).roundToInt().coerceIn(0, 255)
            dst[y * w + x] = Color.rgb(r, gg, b)
        }
        return Bitmap.createBitmap(dst, w, h, Bitmap.Config.ARGB_8888)
    }

    fun glitch(bitmap: Bitmap, strength: Int): Bitmap {
        val amount = strength.coerceIn(0, 100) / 100f
        val w = bitmap.width
        val h = bitmap.height
        val src = IntArray(w * h)
        val dst = IntArray(src.size)
        bitmap.getPixels(src, 0, w, 0, 0, w, h)
        val maxShift = (w * 0.06f * amount).roundToInt().coerceAtMost(w / 4)
        val stripe = 12
        for (y in 0 until h) {
            val band = y / stripe
            val shift = if (band % 4 == 0) {
                ((band * 37) % (maxShift * 2 + 1)) - maxShift
            } else 0
            for (x in 0 until w) {
                val srcX = (x - shift).coerceIn(0, w - 1)
                val p = src[y * w + srcX]
                val channelShift = if (band % 5 == 0) {
                    (((x + band * 11) % (4 + (amount * 8).roundToInt())) - 2).coerceIn(-6, 6)
                } else 0
                dst[y * w + x] = Color.rgb(
                    (Color.red(p) + channelShift * 8).coerceIn(0, 255),
                    Color.green(p),
                    (Color.blue(p) - channelShift * 8).coerceIn(0, 255)
                )
            }
        }
        return Bitmap.createBitmap(dst, w, h, Bitmap.Config.ARGB_8888)
    }

    fun scanDocument(bitmap: Bitmap, strength: Int): Bitmap {
        val amount = strength.coerceIn(0, 100) / 100f
        val contrast = 1.15f + amount * 2.7f
        val brightness = -22f + amount * 15f
        val out = adjustColor(
            bitmap,
            brightness.roundToInt(),
            ((contrast - 1f) * 100f).roundToInt(),
            -100,
            0
        )
        return sharpen(out, 2)
    }

    fun negative(bitmap: Bitmap): Bitmap = drawWithMatrix(bitmap, ColorMatrix(floatArrayOf(
        -1f,0f,0f,0f,255f, 0f,-1f,0f,0f,255f, 0f,0f,-1f,0f,255f, 0f,0f,0f,1f,0f
    )))

    fun blur(bitmap: Bitmap, strength: Int): Bitmap {
        var current = bitmap
        repeat(strength.coerceIn(1, 4)) { current = boxBlur(current) }
        return current
    }

    fun sharpen(bitmap: Bitmap, strength: Int): Bitmap {
        var current = bitmap
        repeat(strength.coerceIn(1, 3)) { current = convolve(current, intArrayOf(0,-1,0,-1,5,-1,0,-1,0)) }
        return current
    }

    fun pixelate(bitmap: Bitmap, block: Int): Bitmap {
        val size = block.coerceIn(2, 64)
        val smallW = (bitmap.width / size).coerceAtLeast(1)
        val smallH = (bitmap.height / size).coerceAtLeast(1)
        val small = Bitmap.createScaledBitmap(bitmap, smallW, smallH, true)
        return Bitmap.createScaledBitmap(small, bitmap.width, bitmap.height, false)
    }

    fun addBorder(bitmap: Bitmap, px: Int, color: Int): Bitmap {
        val p = px.coerceIn(1, 300)
        val out = Bitmap.createBitmap(bitmap.width + p * 2, bitmap.height + p * 2, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        c.drawColor(color)
        c.drawBitmap(bitmap, p.toFloat(), p.toFloat(), Paint(Paint.ANTI_ALIAS_FLAG))
        return out
    }

    fun roundCorners(bitmap: Bitmap, radius: Float): Bitmap {
        val out = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        val path = Path()
        path.addRoundRect(RectF(0f,0f,bitmap.width.toFloat(),bitmap.height.toFloat()), radius.coerceAtMost(minOf(bitmap.width, bitmap.height) / 2f), radius, Path.Direction.CW)
        c.save()
        c.clipPath(path)
        c.drawBitmap(bitmap, 0f, 0f, Paint(Paint.ANTI_ALIAS_FLAG))
        c.restore()
        return out
    }

    fun palette(bitmap: Bitmap, count: Int): List<Int> {
        val scaled = Bitmap.createScaledBitmap(bitmap, 64, (64f * bitmap.height / bitmap.width).roundToInt().coerceAtLeast(1), true)
        val counts = LinkedHashMap<Int, Int>()
        val step = (scaled.width * scaled.height / 1400).coerceAtLeast(1)
        for (i in 0 until scaled.width * scaled.height step step) {
            val x = i % scaled.width
            val y = i / scaled.width
            val p = scaled.getPixel(x, y)
            val r = Color.red(p) / 32 * 32
            val g = Color.green(p) / 32 * 32
            val b = Color.blue(p) / 32 * 32
            val key = Color.rgb(r.coerceIn(0,255), g.coerceIn(0,255), b.coerceIn(0,255))
            counts[key] = (counts[key] ?: 0) + 1
        }
        return counts.entries.sortedByDescending { it.value }.take(count).map { it.key }
    }

    fun collage(bitmaps: List<Bitmap>): Bitmap {
        val items = bitmaps.take(4).map {
            val ratio = it.width.toFloat() / it.height.toFloat()
            val h = 520
            Bitmap.createScaledBitmap(it, (h * ratio).roundToInt().coerceAtLeast(120), h, true)
        }
        val cellW = 520
        val cellH = 520
        val rows = if (items.size <= 2) 1 else 2
        val cols = minOf(2, items.size)
        val out = Bitmap.createBitmap(cellW * cols, cellH * rows, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        c.drawColor(Color.WHITE)
        items.forEachIndexed { index, bmp ->
            val col = index % 2
            val row = index / 2
            val left = col * cellW + (cellW - bmp.width) / 2
            val top = row * cellH + (cellH - bmp.height) / 2
            c.drawBitmap(bmp, left.toFloat(), top.toFloat(), Paint(Paint.ANTI_ALIAS_FLAG))
        }
        return out
    }

    fun pdfBytes(bitmap: Bitmap): ByteArray {
        val pdf = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdf.startPage(pageInfo)
        val canvas = page.canvas
        canvas.drawColor(Color.WHITE)
        val margin = 32f
        val scale = minOf(
            (pageInfo.pageWidth - margin * 2) / bitmap.width,
            (pageInfo.pageHeight - margin * 2) / bitmap.height
        )
        val w = bitmap.width * scale
        val h = bitmap.height * scale
        val left = (pageInfo.pageWidth - w) / 2f
        val top = (pageInfo.pageHeight - h) / 2f
        canvas.drawBitmap(bitmap, null, RectF(left, top, left + w, top + h), Paint(Paint.ANTI_ALIAS_FLAG))
        pdf.finishPage(page)

        val output = java.io.ByteArrayOutputStream()
        pdf.writeTo(output)
        pdf.close()
        return output.toByteArray()
    }

    fun savePdf(context: Context, bitmap: Bitmap, treeUri: Uri?, prefix: String): PdfResult {
        val bytes = pdfBytes(bitmap)
        val name = prefix + "_" + System.currentTimeMillis() + ".pdf"
        val uri = if (treeUri != null) {
            DocumentsContract.createDocument(context.contentResolver, treeUri, "application/pdf", name)
                ?: error("Could not create PDF")
        } else {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, name)
                put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Image Tools")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: error("Could not create PDF")
        }
        context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) } ?: error("Could not write PDF")
        if (treeUri == null && Build.VERSION.SDK_INT >= 29) {
            context.contentResolver.update(
                uri,
                ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) },
                null,
                null
            )
        }
        return PdfResult(uri, bytes.size.toLong())
    }

    private fun drawWithMatrix(bitmap: Bitmap, matrix: ColorMatrix): Bitmap {
        val out = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { colorFilter = ColorMatrixColorFilter(matrix) }
        c.drawBitmap(bitmap, 0f, 0f, p)
        return out
    }

    private fun boxBlur(bitmap: Bitmap): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        val src = IntArray(w * h)
        val dst = IntArray(w * h)
        bitmap.getPixels(src, 0, w, 0, 0, w, h)
        for (y in 0 until h) for (x in 0 until w) {
            var ar=0; var ag=0; var ab=0; var aa=0; var n=0
            for (dy in -1..1) for (dx in -1..1) {
                val xx=(x+dx).coerceIn(0,w-1)
                val yy=(y+dy).coerceIn(0,h-1)
                val p=src[yy*w+xx]
                aa+=Color.alpha(p); ar+=Color.red(p); ag+=Color.green(p); ab+=Color.blue(p); n++
            }
            dst[y*w+x]=Color.argb(aa/n,ar/n,ag/n,ab/n)
        }
        return Bitmap.createBitmap(dst,w,h,Bitmap.Config.ARGB_8888)
    }

    private fun convolve(bitmap: Bitmap, kernel: IntArray): Bitmap {
        val w=bitmap.width; val h=bitmap.height
        val src=IntArray(w*h); val dst=IntArray(w*h)
        bitmap.getPixels(src,0,w,0,0,w,h)
        for (y in 0 until h) for (x in 0 until w) {
            var r=0; var g=0; var b=0
            var k=0
            for (dy in -1..1) for (dx in -1..1) {
                val xx=(x+dx).coerceIn(0,w-1); val yy=(y+dy).coerceIn(0,h-1); val p=src[yy*w+xx]
                val q=kernel[k++]; r+=Color.red(p)*q; g+=Color.green(p)*q; b+=Color.blue(p)*q
            }
            dst[y*w+x]=Color.rgb(r.coerceIn(0,255),g.coerceIn(0,255),b.coerceIn(0,255))
        }
        return Bitmap.createBitmap(dst,w,h,Bitmap.Config.ARGB_8888)
    }
}