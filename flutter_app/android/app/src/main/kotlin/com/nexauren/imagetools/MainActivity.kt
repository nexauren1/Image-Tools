package com.nexauren.imagetools

import android.content.ContentValues
import android.os.Build
import android.provider.MediaStore
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {
    private val exportChannel = "com.nexauren.imagetools/image_export"

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, exportChannel)
            .setMethodCallHandler { call, result ->
                if (call.method != "saveImage") {
                    result.notImplemented()
                    return@setMethodCallHandler
                }

                val bytes = call.argument<ByteArray>("bytes")
                val fileName = call.argument<String>("fileName") ?: "ImageTools_export.png"
                val mimeType = call.argument<String>("mimeType") ?: "image/png"

                if (bytes == null || bytes.isEmpty()) {
                    result.error("EMPTY_IMAGE", "The image data is empty.", null)
                    return@setMethodCallHandler
                }

                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Image Tools")
                        put(MediaStore.Images.Media.IS_PENDING, 1)
                    }
                }

                val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                val uri = contentResolver.insert(collection, values)
                if (uri == null) {
                    result.error("CREATE_FAILED", "Android could not create the output image.", null)
                    return@setMethodCallHandler
                }

                try {
                    contentResolver.openOutputStream(uri, "w")?.use { it.write(bytes) }
                        ?: throw IllegalStateException("Android could not open the output stream.")

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val published = ContentValues().apply {
                            put(MediaStore.Images.Media.IS_PENDING, 0)
                        }
                        contentResolver.update(uri, published, null, null)
                    }
                    result.success(uri.toString())
                } catch (error: Exception) {
                    contentResolver.delete(uri, null, null)
                    result.error("WRITE_FAILED", error.message ?: "Could not save the image.", null)
                }
            }
    }
}
