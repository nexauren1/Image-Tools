package com.nexauren.imagetools.media

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore

data class RawExportItem(
    val bytes: ByteArray,
    val mime: String,
    val extension: String,
    val prefix: String
)

object RawExporter {
    fun save(context: Context, item: RawExportItem, treeUri: Uri?): Uri {
        val name = item.prefix + "_" + System.currentTimeMillis() + "." + item.extension
        if (treeUri != null) {
            try {
                val uri = DocumentsContract.createDocument(
                    context.contentResolver,
                    treeUri,
                    item.mime,
                    name
                ) ?: error("Could not create output file.")
                context.contentResolver.openOutputStream(uri)?.use { it.write(item.bytes) }
                    ?: error("Could not write output file.")
                return uri
            } catch (_: Exception) {
                return save(context, item, null)
            }
        }

        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, name)
            put(MediaStore.Downloads.MIME_TYPE, item.mime)
            put(
                MediaStore.Downloads.RELATIVE_PATH,
                Environment.DIRECTORY_DOWNLOADS + "/Image Tools"
            )
            if (Build.VERSION.SDK_INT >= 29) put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val uri = context.contentResolver.insert(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            values
        ) ?: error("Could not create output file.")
        try {
            context.contentResolver.openOutputStream(uri)?.use { it.write(item.bytes) }
                ?: error("Could not write output file.")
            if (Build.VERSION.SDK_INT >= 29) {
                context.contentResolver.update(
                    uri,
                    ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) },
                    null,
                    null
                )
            }
            return uri
        } catch (error: Exception) {
            context.contentResolver.delete(uri, null, null)
            throw error
        }
    }
}
