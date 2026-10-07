package com.nexauren.imagetools.media

import android.graphics.Bitmap
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation

object BackgroundRemovalProcessor {
    fun removeBackground(bitmap: Bitmap): Bitmap {
        val options = SubjectSegmenterOptions.Builder()
            .enableForegroundBitmap()
            .build()
        val segmenter = SubjectSegmentation.getClient(options)
        return try {
            val result = Tasks.await(segmenter.process(InputImage.fromBitmap(bitmap, 0)))
            result.foregroundBitmap ?: error("No foreground bitmap was returned.")
        } finally {
            segmenter.close()
        }
    }
}
