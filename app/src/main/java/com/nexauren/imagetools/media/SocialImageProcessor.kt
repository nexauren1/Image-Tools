package com.nexauren.imagetools.media

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import kotlin.math.max
import kotlin.math.roundToInt

data class SocialPreset(
    val id: String,
    val title: String,
    val width: Int,
    val height: Int,
    val crop: String
)

object SocialImageProcessor {
    val presets = listOf(
        SocialPreset("instagram_square", "Instagram 1:1", 1080, 1080, "1:1"),
        SocialPreset("instagram_portrait", "Instagram 4:5", 1080, 1350, "4:5"),
        SocialPreset("instagram_story", "Instagram Story / Reel", 1080, 1920, "9:16"),
        SocialPreset("whatsapp_status", "WhatsApp Status", 1080, 1920, "9:16"),
        SocialPreset("youtube_thumbnail", "YouTube thumbnail", 1280, 720, "16:9"),
        SocialPreset("youtube_shorts", "YouTube Shorts", 1080, 1920, "9:16"),
        SocialPreset("profile_square", "Profile picture", 1080, 1080, "1:1")
    )

    fun apply(bitmap: Bitmap, preset: SocialPreset): Bitmap {
        val cropped = ImageProcessor.cropCenter(bitmap, preset.crop)
        val result = ImageProcessor.resize(cropped, preset.width, preset.height)
        return result
    }

    fun smartResize(bitmap: Bitmap, maxWidth: Int, maxHeight: Int, allowUpscale: Boolean = false): Bitmap {
        val scale = minOf(
            maxWidth.toFloat() / bitmap.width,
            maxHeight.toFloat() / bitmap.height
        )
        val safeScale = if (allowUpscale) scale else minOf(scale, 1f)
        val width = max(1, (bitmap.width * safeScale).roundToInt())
        val height = max(1, (bitmap.height * safeScale).roundToInt())
        return ImageProcessor.resize(bitmap, width, height)
    }

    fun letterbox(bitmap: Bitmap, width: Int, height: Int, background: Int = android.graphics.Color.BLACK): Bitmap {
        val scaled = smartResize(bitmap, width, height, false)
        val out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        canvas.drawColor(background)
        val left = (width - scaled.width) / 2f
        val top = (height - scaled.height) / 2f
        canvas.drawBitmap(scaled, left, top, null)
        return out
    }
}
