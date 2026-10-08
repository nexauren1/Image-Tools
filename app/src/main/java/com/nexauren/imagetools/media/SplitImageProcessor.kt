package com.nexauren.imagetools.media

import android.graphics.Bitmap

object SplitImageProcessor {
    fun split(bitmap: Bitmap, rows: Int, columns: Int): List<Bitmap> {
        val safeRows = rows.coerceIn(2, 4)
        val safeColumns = columns.coerceIn(2, 4)
        val output = mutableListOf<Bitmap>()
        for (row in 0 until safeRows) {
            val top = bitmap.height * row / safeRows
            val bottom = bitmap.height * (row + 1) / safeRows
            for (column in 0 until safeColumns) {
                val left = bitmap.width * column / safeColumns
                val right = bitmap.width * (column + 1) / safeColumns
                output += Bitmap.createBitmap(
                    bitmap,
                    left,
                    top,
                    (right - left).coerceAtLeast(1),
                    (bottom - top).coerceAtLeast(1)
                )
            }
        }
        return output
    }
}
