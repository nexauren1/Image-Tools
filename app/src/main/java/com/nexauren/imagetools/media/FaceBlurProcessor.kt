package com.nexauren.imagetools.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions

object FaceBlurProcessor {
    fun blurFaces(context: Context, bitmap: Bitmap, strength: Int): Bitmap {
        val detector = FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .enableTracking()
                .build()
        )
        return try {
            val faces = Tasks.await(
                detector.process(InputImage.fromBitmap(bitmap, 0))
            )
            val output = bitmap.copy(Bitmap.Config.ARGB_8888, true)
            val canvas = Canvas(output)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            val radiusPad = ((strength.coerceIn(5, 100) / 100f) * 0.28f)
            for (face in faces) {
                val box = face.boundingBox
                val padX = (box.width() * radiusPad).toInt()
                val padY = (box.height() * radiusPad).toInt()
                val rect = Rect(
                    (box.left - padX).coerceAtLeast(0),
                    (box.top - padY).coerceAtLeast(0),
                    (box.right + padX).coerceAtMost(bitmap.width),
                    (box.bottom + padY).coerceAtMost(bitmap.height)
                )
                if (rect.width() > 0 && rect.height() > 0) {
                    val region = Bitmap.createBitmap(
                        bitmap,
                        rect.left,
                        rect.top,
                        rect.width(),
                        rect.height()
                    )
                    val blurred = AdvancedImageProcessor.blur(
                        region,
                        (strength / 25f).toInt().coerceIn(1, 4)
                    )
                    canvas.drawBitmap(blurred, rect.left.toFloat(), rect.top.toFloat(), paint)
                }
            }
            output
        } finally {
            detector.close()
        }
    }
}
