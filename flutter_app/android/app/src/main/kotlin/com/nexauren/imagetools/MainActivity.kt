package com.nexauren.imagetools

import android.content.ContentValues
import android.os.Build
import android.provider.MediaStore
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {
    private val exportChannel = "com.nexauren.imagetools/image_export"
    private val intelligenceChannel = "com.nexauren.imagetools/image_intelligence"

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

                val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
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

        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, intelligenceChannel)
            .setMethodCallHandler { call, result ->
                val filePath = call.argument<String>("filePath")
                if (filePath.isNullOrBlank()) {
                    result.error("INVALID_IMAGE_PATH", "Select an image again and retry.", null)
                    return@setMethodCallHandler
                }

                when (call.method) {
                    "recognizeText" -> recognizeText(filePath, result)
                    "detectFaces" -> detectFaces(filePath, result)
                    else -> result.notImplemented()
                }
            }
    }

    private fun recognizeText(filePath: String, flutterResult: MethodChannel.Result) {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        try {
            val image = InputImage.fromFilePath(this, android.net.Uri.fromFile(java.io.File(filePath)))
            recognizer.process(image)
                .addOnSuccessListener { result ->
                    flutterResult.success(result.text.trim())
                }
                .addOnFailureListener { error ->
                    flutterResult.error(
                        "OCR_MODEL_NOT_READY",
                        error.message ?: "The OCR model is not ready. Connect to the internet and try again.",
                        null
                    )
                }
                .addOnCompleteListener {
                    recognizer.close()
                }
        } catch (error: Exception) {
            recognizer.close()
            flutterResult.error("OCR_FAILED", error.message ?: "OCR could not process this image.", null)
        }
    }

    private fun detectFaces(filePath: String, flutterResult: MethodChannel.Result) {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .enableTracking()
            .build()
        val detector = FaceDetection.getClient(options)
        try {
            val image = InputImage.fromFilePath(this, java.io.File(filePath).toUri())
            detector.process(image)
                .addOnSuccessListener { faces ->
                    flutterResult.success(
                        faces.map { face ->
                            mapOf(
                                "left" to face.boundingBox.left,
                                "top" to face.boundingBox.top,
                                "right" to face.boundingBox.right,
                                "bottom" to face.boundingBox.bottom
                            )
                        }
                    )
                }
                .addOnFailureListener { error ->
                    flutterResult.error(
                        "FACE_MODEL_NOT_READY",
                        error.message ?: "The face-detection model is not ready. Connect to the internet and try again.",
                        null
                    )
                }
                .addOnCompleteListener {
                    detector.close()
                }
        } catch (error: Exception) {
            detector.close()
            flutterResult.error("FACE_DETECTION_FAILED", error.message ?: "Face detection failed.", null)
        }
    }
}
