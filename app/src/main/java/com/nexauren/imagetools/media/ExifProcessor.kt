package com.nexauren.imagetools.media

import android.content.Context
import android.net.Uri
import androidx.exifinterface.media.ExifInterface

object ExifProcessor {
    private val tags = listOf(
        ExifInterface.TAG_MAKE,
        ExifInterface.TAG_MODEL,
        ExifInterface.TAG_LENS_MODEL,
        ExifInterface.TAG_DATETIME_ORIGINAL,
        ExifInterface.TAG_F_NUMBER,
        ExifInterface.TAG_EXPOSURE_TIME,
        ExifInterface.TAG_ISO_SPEED_RATINGS,
        ExifInterface.TAG_FOCAL_LENGTH,
        ExifInterface.TAG_FLASH,
        ExifInterface.TAG_WHITE_BALANCE,
        ExifInterface.TAG_GPS_LATITUDE,
        ExifInterface.TAG_GPS_LONGITUDE,
        ExifInterface.TAG_ORIENTATION
    )

    fun read(context: Context, uri: Uri): String {
        val exif = context.contentResolver.openInputStream(uri)?.use { ExifInterface(it) }
            ?: error("Could not open image metadata.")
        val lines = mutableListOf<String>()
        tags.forEach { tag ->
            val value = exif.getAttribute(tag)
            if (!value.isNullOrBlank()) {
                lines += "$tag: $value"
            }
        }
        return if (lines.isEmpty()) "No readable EXIF metadata found." else lines.joinToString("\n")
    }
}
