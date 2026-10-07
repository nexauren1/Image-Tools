package com.nexauren.imagetools.media

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore

object OutputExporter {
    fun saveImage(
        context: Context,
        bytes: ByteArray,
        format: OutputFormat,
        prefix: String,
        width: Int,
        height: Int,
        treeUri: Uri?
    ): ImageResult {
        if (treeUri != null) {
            return try {
                ImageProcessor.saveToFolder(context, treeUri, bytes, format, prefix, width, height)
            } catch (_: Exception) {
                ImageProcessor.save(context, bytes, format, prefix, width, height)
            }
        }
        return ImageProcessor.save(context, bytes, format, prefix, width, height)
    }

    fun saveText(
        context: Context,
        text: String,
        prefix: String,
        treeUri: Uri?
    ): Uri = ExportText.save(context, text, prefix, treeUri)

    fun savePdf(
        context: Context,
        bytes: ByteArray,
        prefix: String,
        treeUri: Uri?
    ): Uri {
        val name = prefix + "_" + System.currentTimeMillis() + ".pdf"
        val uri = if (treeUri != null) {
            DocumentsContract.createDocument(
                context.contentResolver,
                treeUri,
                "application/pdf",
                name
            ) ?: error("Could not create PDF file.")
        } else {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, name)
                put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                put(
                    MediaStore.Downloads.RELATIVE_PATH,
                    Environment.DIRECTORY_DOWNLOADS + "/Image Tools"
                )
                if (Build.VERSION.SDK_INT >= 29) {
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
            }
            context.contentResolver.insert(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                values
            ) ?: error("Could not create PDF file.")
        }

        return try {
            context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
                ?: error("Could not write PDF file.")
            if (treeUri == null && Build.VERSION.SDK_INT >= 29) {
                context.contentResolver.update(
                    uri,
                    ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) },
                    null,
                    null
                )
            }
            uri
        } catch (error: Exception) {
            if (treeUri != null) return savePdf(context, bytes, prefix, null)
            context.contentResolver.delete(uri, null, null)
            throw error
        }
    }
}
