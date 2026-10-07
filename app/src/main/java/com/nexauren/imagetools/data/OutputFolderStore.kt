package com.nexauren.imagetools.data

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns

object OutputFolderStore {
    private const val PREFS = "image_tools_storage"
    private const val KEY_TREE_URI = "output_tree_uri"

    fun getTreeUri(context: Context): Uri? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_TREE_URI, null)
            ?.let(Uri::parse)

    fun saveTreeUri(context: Context, uri: Uri) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_TREE_URI, uri.toString())
            .apply()
    }

    fun folderName(context: Context, uri: Uri? = getTreeUri(context)): String? {
        uri ?: return null
        return try {
            val documentUri = DocumentsContract.buildDocumentUriUsingTree(
                uri,
                DocumentsContract.getTreeDocumentId(uri)
            )
            context.contentResolver.query(
                documentUri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        } catch (_: Exception) {
            null
        }
    }
}
