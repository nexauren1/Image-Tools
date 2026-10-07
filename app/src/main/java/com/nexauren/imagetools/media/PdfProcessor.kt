package com.nexauren.imagetools.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import java.io.ByteArrayOutputStream

object PdfProcessor {
    fun merge(context: Context, uris: List<Uri>): ByteArray {
        require(uris.isNotEmpty()) { "Select at least one PDF." }
        val output = PdfDocument()
        var pageNumber = 1
        try {
            for (uri in uris) {
                val pfd = context.contentResolver.openFileDescriptor(uri, "r")
                    ?: error("Could not open PDF.")
                pfd.use {
                    PdfRenderer(it).use { renderer ->
                        for (index in 0 until renderer.pageCount) {
                            renderer.openPage(index).use { page ->
                                val width = 595
                                val height = 842
                                val pageInfo = PdfDocument.PageInfo.Builder(width, height, pageNumber++).create()
                                val outPage = output.startPage(pageInfo)
                                outPage.canvas.drawColor(Color.WHITE)
                                val scale = minOf(
                                    width.toFloat() / page.width,
                                    height.toFloat() / page.height
                                )
                                val dstW = page.width * scale
                                val dstH = page.height * scale
                                val left = (width - dstW) / 2f
                                val top = (height - dstH) / 2f
                                val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                                bitmap.eraseColor(Color.WHITE)
                                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                                outPage.canvas.drawBitmap(
                                    bitmap,
                                    null,
                                    RectF(left, top, left + dstW, top + dstH),
                                    Paint(Paint.ANTI_ALIAS_FLAG)
                                )
                                bitmap.recycle()
                                output.finishPage(outPage)
                            }
                        }
                    }
                }
            }
            val stream = ByteArrayOutputStream()
            output.writeTo(stream)
            return stream.toByteArray()
        } finally {
            output.close()
        }
    }
}
