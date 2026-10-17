package com.nexauren.imagetools.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexauren.imagetools.data.HistoryEntry
import com.nexauren.imagetools.data.HistoryStore
import com.nexauren.imagetools.data.OutputFolderStore
import com.nexauren.imagetools.data.ProcessingStatsStore
import com.nexauren.imagetools.data.Recipe
import com.nexauren.imagetools.data.RecipeStore
import com.nexauren.imagetools.media.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date

@Composable
fun ToolWorkspaceV6(
    tool: AnyToolDef,
    premium: Boolean,
    onBack: () -> Unit,
    onNeedPremium: () -> Unit
) {
    val context = LocalContext.current
    val strings = LocalUiText.current
    val scope = rememberCoroutineScope()

    var batchMode by remember { mutableStateOf(tool.id == "collage" || tool.id == "gif_creator" || tool.id == "pdf_merge" || tool.id == "split_grid") }
    var sourceUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var previewBitmaps by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var pendingItems by remember { mutableStateOf<List<RawExportItem>>(emptyList()) }
    var exportedUri by remember { mutableStateOf<Uri?>(null) }
    var exportedMime by remember { mutableStateOf("image/*") }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    var statusIsError by remember { mutableStateOf(false) }
    var textResult by remember { mutableStateOf<String?>(null) }
    var sourcePreviewBitmaps by remember { mutableStateOf<List<Bitmap>>(emptyList()) }

    var width by remember { mutableStateOf("1280") }
    var height by remember { mutableStateOf("1280") }
    var quality by remember { mutableFloatStateOf(82f) }
    var format by remember { mutableStateOf(OutputFormat.JPEG) }
    var cropRatio by remember { mutableStateOf("1:1") }
    var angle by remember { mutableIntStateOf(90) }
    var flipH by remember { mutableStateOf(false) }
    var flipV by remember { mutableStateOf(false) }
    var imageFilter by remember { mutableStateOf(ImageFilter.ORIGINAL) }
    var watermark by remember { mutableStateOf("IMAGE TOOLS") }
    var opacity by remember { mutableFloatStateOf(65f) }
    var position by remember { mutableStateOf("bottom_right") }
    var amount by remember { mutableFloatStateOf(50f) }
    var pixelSize by remember { mutableFloatStateOf(18f) }
    var borderSize by remember { mutableFloatStateOf(24f) }
    var cornerRadius by remember { mutableFloatStateOf(36f) }
    var duotonePreset by remember { mutableStateOf("ocean") }
    var socialPreset by remember { mutableStateOf("instagram_square") }
    var modernFormat by remember { mutableStateOf(ModernFormat.HEIC) }
    var smartWidth by remember { mutableStateOf("1920") }
    var smartHeight by remember { mutableStateOf("1920") }
    var gifDelay by remember { mutableFloatStateOf(500f) }
    var fitBackground by remember { mutableStateOf("white") }
    var targetKb by remember { mutableFloatStateOf(500f) }
    var gridRows by remember { mutableIntStateOf(2) }
    var gridColumns by remember { mutableIntStateOf(2) }
    var mirrorAxis by remember { mutableStateOf("horizontal") }

    LaunchedEffect(tool.id) {
        batchMode = tool.id in listOf("collage", "gif_creator", "pdf_merge", "split_grid")
        sourceUris = emptyList()
        sourcePreviewBitmaps = emptyList()
        previewBitmaps = emptyList()
        pendingItems = emptyList()
        textResult = null
        exportedUri = null
        status = null
        val recipe = RecipeStore.consumePending(context)
        if (recipe != null && recipe.toolId == tool.id) {
            val cfg = recipe.config
            cfg["width"]?.let { width = it }
            cfg["height"]?.let { height = it }
            cfg["quality"]?.toFloatOrNull()?.let { quality = it.coerceIn(10f, 100f) }
            cfg["format"]?.let { value -> runCatching { format = OutputFormat.valueOf(value) } }
            cfg["cropRatio"]?.let { cropRatio = it }
            cfg["angle"]?.toIntOrNull()?.let { angle = it }
            cfg["flipH"]?.toBooleanStrictOrNull()?.let { flipH = it }
            cfg["flipV"]?.toBooleanStrictOrNull()?.let { flipV = it }
            cfg["imageFilter"]?.let { value -> runCatching { imageFilter = ImageFilter.valueOf(value) } }
            cfg["watermark"]?.let { watermark = it }
            cfg["opacity"]?.toFloatOrNull()?.let { opacity = it.coerceIn(10f, 100f) }
            cfg["position"]?.let { position = it }
            cfg["amount"]?.toFloatOrNull()?.let { amount = it }
            cfg["pixelSize"]?.toFloatOrNull()?.let { pixelSize = it }
            cfg["borderSize"]?.toFloatOrNull()?.let { borderSize = it }
            cfg["cornerRadius"]?.toFloatOrNull()?.let { cornerRadius = it }
            cfg["duotonePreset"]?.let { duotonePreset = it }
            cfg["socialPreset"]?.let { socialPreset = it }
            cfg["modernFormat"]?.let { value -> runCatching { modernFormat = ModernFormat.valueOf(value) } }
            cfg["smartWidth"]?.let { smartWidth = it }
            cfg["smartHeight"]?.let { smartHeight = it }
            cfg["gifDelay"]?.toFloatOrNull()?.let { gifDelay = it.coerceIn(100f, 1500f) }
            cfg["fitBackground"]?.let { fitBackground = it }
            cfg["targetKb"]?.toFloatOrNull()?.let { targetKb = it.coerceIn(50f, 5000f) }
            cfg["gridRows"]?.toIntOrNull()?.let { gridRows = it.coerceIn(2, 4) }
            cfg["gridColumns"]?.toIntOrNull()?.let { gridColumns = it.coerceIn(2, 4) }
            cfg["mirrorAxis"]?.let { mirrorAxis = it }
            status = strings.get("ready")
        }
    }

    fun loadSourceThumbnails(uris: List<Uri>) {
        sourcePreviewBitmaps = emptyList()
        scope.launch {
            val thumbnails = withContext(Dispatchers.IO) {
                uris.take(4).mapNotNull { uri ->
                    runCatching {
                        decodeToolThumbnail(context, uri)
                    }.getOrNull()
                }
            }
            sourcePreviewBitmaps = thumbnails
        }
    }

    val singlePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            sourceUris = listOf(uri)
            previewBitmaps = emptyList()
            pendingItems = emptyList()
            textResult = null
            status = strings.get("ready")
            statusIsError = false
            loadSourceThumbnails(listOf(uri))
        }
    }

    val multiPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(20)
    ) { uris ->
        if (uris.isNotEmpty()) {
            sourceUris = uris.take(20)
            previewBitmaps = emptyList()
            pendingItems = emptyList()
            textResult = null
            status = strings.get("ready")
            statusIsError = false
            loadSourceThumbnails(sourceUris)
        }
    }

    val pdfPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            sourceUris = uris.take(20)
            previewBitmaps = emptyList()
            pendingItems = emptyList()
            textResult = null
            sourcePreviewBitmaps = emptyList()
            status = strings.get("ready")
            statusIsError = false
        }
    }

    fun chooseInput() {
        if (tool.premium && !premium) {
            onNeedPremium()
            return
        }
        when (tool.id) {
            "pdf_merge" -> pdfPicker.launch(arrayOf("application/pdf"))
            "base64" -> singlePicker.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
            "gif_creator", "collage" -> multiPicker.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
            else -> if (batchMode) {
                multiPicker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            } else {
                singlePicker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            }
        }
    }

    suspend fun transformBitmap(bitmap: Bitmap, uri: Uri): RawExportItem {
        return when (tool.id) {
            "resize" -> {
                val w = width.toIntOrNull()?.coerceAtLeast(1) ?: bitmap.width
                val h = if (height.toIntOrNull() != null) height.toInt().coerceAtLeast(1) else bitmap.height
                RawExportItem(
                    ImageProcessor.encode(
                        if (w > 0 && h > 0) ImageProcessor.resize(bitmap, w, h) else bitmap,
                        format,
                        100
                    ),
                    format.mime,
                    format.extension,
                    tool.id
                )
            }
            "compress" -> RawExportItem(
                ImageProcessor.encode(bitmap, format, quality.toInt()),
                format.mime, format.extension, tool.id
            )
            "convert", "metadata" -> RawExportItem(
                ImageProcessor.encode(bitmap, format, 95),
                format.mime, format.extension, tool.id
            )
            "crop" -> {
                val out = ImageProcessor.cropCenter(bitmap, cropRatio)
                RawExportItem(ImageProcessor.encode(out, format, 100), format.mime, format.extension, tool.id)
            }
            "rotate" -> {
                val out = ImageProcessor.rotate(bitmap, angle, flipH, flipV)
                RawExportItem(ImageProcessor.encode(out, format, 100), format.mime, format.extension, tool.id)
            }
            "filter" -> {
                val out = ImageProcessor.filter(bitmap, imageFilter)
                RawExportItem(ImageProcessor.encode(out, format, 100), format.mime, format.extension, tool.id)
            }
            "watermark" -> {
                val out = ImageProcessor.watermark(
                    bitmap,
                    watermark,
                    opacity.toInt(),
                    when (position) {
                        "top_left" -> "Top left"
                        "top_right" -> "Top right"
                        "center" -> "Center"
                        "bottom_left" -> "Bottom left"
                        else -> "Bottom right"
                    }
                )
                RawExportItem(ImageProcessor.encode(out, format, 100), format.mime, format.extension, tool.id)
            }
            "brightness" -> RawExportItem(
                ImageProcessor.encode(AdvancedImageProcessor.adjustColor(bitmap, amount.toInt(), 0, 0, 0), format, 100),
                format.mime, format.extension, tool.id
            )
            "contrast" -> RawExportItem(
                ImageProcessor.encode(AdvancedImageProcessor.adjustColor(bitmap, 0, amount.toInt(), 0, 0), format, 100),
                format.mime, format.extension, tool.id
            )
            "saturation" -> RawExportItem(
                ImageProcessor.encode(AdvancedImageProcessor.adjustColor(bitmap, 0, 0, amount.toInt(), 0), format, 100),
                format.mime, format.extension, tool.id
            )
            "warmth" -> RawExportItem(
                ImageProcessor.encode(AdvancedImageProcessor.adjustColor(bitmap, 0, 0, 0, amount.toInt()), format, 100),
                format.mime, format.extension, tool.id
            )
            "negative" -> RawExportItem(
                ImageProcessor.encode(AdvancedImageProcessor.negative(bitmap), format, 100),
                format.mime, format.extension, tool.id
            )
            "blur" -> RawExportItem(
                ImageProcessor.encode(AdvancedImageProcessor.blur(bitmap, (amount / 25f).toInt().coerceIn(1, 4)), format, 100),
                format.mime, format.extension, tool.id
            )
            "sharpen" -> RawExportItem(
                ImageProcessor.encode(AdvancedImageProcessor.sharpen(bitmap, (amount / 25f).toInt().coerceIn(1, 3)), format, 100),
                format.mime, format.extension, tool.id
            )
            "pixelate" -> RawExportItem(
                ImageProcessor.encode(AdvancedImageProcessor.pixelate(bitmap, pixelSize.toInt()), format, 100),
                format.mime, format.extension, tool.id
            )
            "border" -> RawExportItem(
                ImageProcessor.encode(
                    AdvancedImageProcessor.addBorder(bitmap, borderSize.toInt(), android.graphics.Color.WHITE),
                    format, 100
                ),
                format.mime, format.extension, tool.id
            )
            "round" -> RawExportItem(
                ImageProcessor.encode(AdvancedImageProcessor.roundCorners(bitmap, cornerRadius), OutputFormat.PNG, 100),
                OutputFormat.PNG.mime, OutputFormat.PNG.extension, tool.id
            )
            "pdf" -> {
                val pdf = withContext(Dispatchers.Default) {
                    AdvancedImageProcessor.pdfBytes(bitmap)
                }
                RawExportItem(pdf, "application/pdf", "pdf", "image")
            }
            "auto_enhance" -> RawExportItem(
                ImageProcessor.encode(AdvancedImageProcessor.autoEnhance(bitmap), format, 100),
                format.mime, format.extension, tool.id
            )
            "exposure" -> RawExportItem(
                ImageProcessor.encode(AdvancedImageProcessor.exposure(bitmap, amount.toInt()), format, 100),
                format.mime, format.extension, tool.id
            )
            "tint" -> RawExportItem(
                ImageProcessor.encode(AdvancedImageProcessor.tint(bitmap, amount.toInt()), format, 100),
                format.mime, format.extension, tool.id
            )
            "vignette" -> RawExportItem(
                ImageProcessor.encode(AdvancedImageProcessor.vignette(bitmap, amount.toInt()), format, 100),
                format.mime, format.extension, tool.id
            )
            "posterize" -> RawExportItem(
                ImageProcessor.encode(AdvancedImageProcessor.posterize(bitmap, amount.toInt()), format, 100),
                format.mime, format.extension, tool.id
            )
            "duotone" -> {
                val colors = when (duotonePreset) {
                    "sunset" -> android.graphics.Color.rgb(60, 12, 50) to android.graphics.Color.rgb(255, 182, 92)
                    "ink" -> android.graphics.Color.rgb(15, 23, 42) to android.graphics.Color.rgb(241, 245, 249)
                    else -> android.graphics.Color.rgb(8, 47, 73) to android.graphics.Color.rgb(103, 232, 249)
                }
                val out = AdvancedImageProcessor.duotone(bitmap, colors.first, colors.second)
                RawExportItem(ImageProcessor.encode(out, format, 100), format.mime, format.extension, tool.id)
            }
            "mirror" -> RawExportItem(
                ImageProcessor.encode(AdvancedImageProcessor.mirror(bitmap, mirrorAxis == "horizontal"), format, 100),
                format.mime, format.extension, tool.id
            )
            "fit_canvas" -> {
                val bg = if (fitBackground == "black") android.graphics.Color.BLACK else android.graphics.Color.WHITE
                val out = SocialImageProcessor.letterbox(
                    bitmap,
                    width.toIntOrNull()?.coerceAtLeast(1) ?: 1280,
                    height.toIntOrNull()?.coerceAtLeast(1) ?: 1280,
                    bg
                )
                RawExportItem(ImageProcessor.encode(out, format, 100), format.mime, format.extension, tool.id)
            }
            "target_size" -> {
                val target = (targetKb * 1024f).toInt().coerceAtLeast(50 * 1024)
                val outBytes = ImageProcessor.encodeJpegUnderSize(bitmap, target, format)
                RawExportItem(outBytes, format.mime, format.extension, tool.id)
            }
            "split_grid" -> {
                val tile = SplitImageProcessor.split(bitmap, gridRows, gridColumns).first()
                RawExportItem(ImageProcessor.encode(tile, OutputFormat.PNG, 100), OutputFormat.PNG.mime, OutputFormat.PNG.extension, "split_1")
            }
            "noise_reduction" -> RawExportItem(
                ImageProcessor.encode(AdvancedImageProcessor.denoise(bitmap, amount.toInt()), format, 100),
                format.mime, format.extension, tool.id
            )
            "social_presets" -> {
                val preset = SocialImageProcessor.presets.first { it.id == socialPreset }
                val out = SocialImageProcessor.apply(bitmap, preset)
                RawExportItem(
                    ImageProcessor.encode(out, OutputFormat.JPEG, 95),
                    OutputFormat.JPEG.mime, OutputFormat.JPEG.extension, "social"
                )
            }
            "smart_resize" -> {
                val out = SocialImageProcessor.smartResize(
                    bitmap,
                    smartWidth.toIntOrNull()?.coerceAtLeast(1) ?: 1920,
                    smartHeight.toIntOrNull()?.coerceAtLeast(1) ?: 1920
                )
                RawExportItem(
                    ImageProcessor.encode(out, format, 95),
                    format.mime, format.extension, tool.id
                )
            }
            "face_blur" -> {
                val out = withContext(Dispatchers.Default) {
                    FaceBlurProcessor.blurFaces(context, bitmap, amount.toInt().coerceIn(5, 100))
                }
                RawExportItem(
                    ImageProcessor.encode(out, OutputFormat.JPEG, 95),
                    OutputFormat.JPEG.mime, OutputFormat.JPEG.extension, "face_blur"
                )
            }
            "heic_avif" -> {
                val modernBytes = withContext(Dispatchers.IO) {
                    ModernImageEncoder.encode(context, bitmap, modernFormat, quality.toInt())
                }
                RawExportItem(
                    modernBytes,
                    modernFormat.mime,
                    modernFormat.extension,
                    modernFormat.extension
                )
            }
            else -> RawExportItem(
                ImageProcessor.encode(bitmap, format, 95),
                format.mime, format.extension, tool.id
            )
        }
    }

    suspend fun analyzeBitmap(bitmap: Bitmap, uri: Uri): RawExportItem {
        return when (tool.id) {
            "details" -> {
                val sourceBytes = withContext(Dispatchers.IO) { ImageProcessor.sourceBytes(context, uri) }
                val text = buildString {
                    append(strings.get("details.result"))
                    append("\nResolution: ").append(bitmap.width).append(" × ").append(bitmap.height)
                    append("\nAspect ratio: ")
                    append("%.3f".format(bitmap.width.toFloat() / bitmap.height))
                    if (sourceBytes != null) append("\nSource size: ").append(ImageProcessor.humanBytes(sourceBytes))
                }
                RawExportItem(text.toByteArray(), "text/plain", "txt", "details")
            }
            "ocr" -> {
                val text = withContext(Dispatchers.Default) { OcrProcessor.recognize(bitmap) }
                RawExportItem(text.toByteArray(), "text/plain", "txt", "ocr")
            }
            "exif" -> {
                val text = withContext(Dispatchers.IO) { ExifProcessor.read(context, uri) }
                RawExportItem(text.toByteArray(), "text/plain", "txt", "exif")
            }
            "palette" -> {
                val colors = withContext(Dispatchers.Default) { AdvancedImageProcessor.palette(bitmap, 5) }
                val text = colors.joinToString("\n") { "#%06X".format(it and 0xFFFFFF) }
                RawExportItem(text.toByteArray(), "text/plain", "txt", "palette")
            }
            "css_palette" -> {
                val colors = withContext(Dispatchers.Default) { AdvancedImageProcessor.palette(bitmap, 5) }
                val css = buildString {
                    append(":root {\n")
                    colors.forEachIndexed { index, color ->
                        append("  --image-tools-color-").append(index + 1).append(": #")
                            .append("%06X".format(color and 0xFFFFFF)).append(";\n")
                    }
                    append("}\n")
                }
                RawExportItem(css.toByteArray(Charsets.UTF_8), "text/css", "css", "css_palette")
            }
            "base64" -> {
                val dataUrl = withContext(Dispatchers.Default) {
                    val bytes = ImageProcessor.encode(bitmap, format, quality.toInt())
                    require(bytes.size <= 4 * 1024 * 1024) { strings.get("base64.too.large") }
                    "data:${format.mime};base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
                }
                RawExportItem(dataUrl.toByteArray(Charsets.UTF_8), "text/plain", "txt", "base64")
            }
            else -> withContext(Dispatchers.Default) { transformBitmap(bitmap, uri) }
        }
    }

    fun recipeConfig(): Map<String, String> = mapOf(
        "width" to width,
        "height" to height,
        "quality" to quality.toInt().toString(),
        "format" to format.name,
        "cropRatio" to cropRatio,
        "angle" to angle.toString(),
        "flipH" to flipH.toString(),
        "flipV" to flipV.toString(),
        "imageFilter" to imageFilter.name,
        "watermark" to watermark,
        "opacity" to opacity.toInt().toString(),
        "position" to position,
        "amount" to amount.toInt().toString(),
        "pixelSize" to pixelSize.toInt().toString(),
        "borderSize" to borderSize.toInt().toString(),
        "cornerRadius" to cornerRadius.toInt().toString(),
        "duotonePreset" to duotonePreset,
        "socialPreset" to socialPreset,
        "modernFormat" to modernFormat.name,
        "smartWidth" to smartWidth,
        "smartHeight" to smartHeight,
        "gifDelay" to gifDelay.toInt().toString(),
        "fitBackground" to fitBackground,
        "targetKb" to targetKb.toInt().toString(),
        "gridRows" to gridRows.toString(),
        "gridColumns" to gridColumns.toString(),
        "mirrorAxis" to mirrorAxis
    )

    fun saveRecipe() {
        if (!premium) {
            onNeedPremium()
            return
        }
        val name = strings.toolTitle(tool.id) + " " + System.currentTimeMillis().toString().takeLast(4)
        RecipeStore.save(context, Recipe(name, tool.id, recipeConfig()))
        status = strings.get("saved")
    }

    fun exportResults() {
        if (pendingItems.isEmpty()) {
            status = strings.get("no.result")
            return
        }
        scope.launch {
            busy = true
            try {
                val tree = OutputFolderStore.getTreeUri(context)
                val first = pendingItems.first()
                val uris = withContext(Dispatchers.IO) {
                    pendingItems.map { RawExporter.save(context, it, tree) }
                }
                exportedUri = uris.firstOrNull()
                exportedMime = first.mime
                ProcessingStatsStore.recordExported(context, pendingItems.size)
                HistoryStore.add(
                    context,
                    HistoryEntry(
                        tool.id,
                        strings.toolTitle(tool.id),
                        System.currentTimeMillis(),
                        sourceUris.size.coerceAtLeast(1),
                        uris.firstOrNull()?.toString()
                    )
                )
                status = strings.get("saved")
                statusIsError = false
            } catch (e: Exception) {
                status = e.message ?: strings.get("save.error")
                statusIsError = true
            } finally {
                busy = false
            }
        }
    }

    fun processAction() {
        if (tool.premium && !premium) {
            onNeedPremium()
            return
        }
        scope.launch {
            busy = true
            pendingItems = emptyList()
            previewBitmaps = emptyList()
            textResult = null
            exportedUri = null
            status = strings.get("processing")
            statusIsError = false
            try {
                if (tool.id == "pdf_merge") {
                    if (sourceUris.size < 2) {
                        status = strings.get("select.first")
                        chooseInput()
                    } else {
                        val pdf = withContext(Dispatchers.IO) { PdfProcessor.merge(context, sourceUris) }
                        pendingItems = listOf(RawExportItem(pdf, "application/pdf", "pdf", "merged"))
                        status = strings.get("ready")
                    }
                } else {
                    if (sourceUris.isEmpty()) {
                        status = strings.get("select.first")
                        chooseInput()
                        return@launch
                    }
                    val bitmaps = withContext(Dispatchers.IO) {
                        sourceUris.mapNotNull { uri -> ImageProcessor.decode(context, uri)?.let { uri to it } }
                    }
                    if (bitmaps.isEmpty()) error(strings.get("process.error"))

                    if (tool.id == "split_grid") {
                        val splitResults = withContext(Dispatchers.Default) {
                            val outputs = mutableListOf<RawExportItem>()
                            var imageNumber = 1
                            for ((_, bitmap) in bitmaps) {
                                val tiles = SplitImageProcessor.split(bitmap, gridRows, gridColumns)
                                tiles.forEachIndexed { index, tile ->
                                    outputs += RawExportItem(
                                        ImageProcessor.encode(tile, OutputFormat.PNG, 100),
                                        OutputFormat.PNG.mime,
                                        OutputFormat.PNG.extension,
                                        "split_" + imageNumber + "_" + (index + 1)
                                    )
                                }
                                imageNumber++
                            }
                            val previews = outputs.take(6).mapNotNull {
                                decodeToolPreview(it.bytes)
                            }
                            outputs.toList() to previews
                        }
                        pendingItems = splitResults.first
                        previewBitmaps = splitResults.second
                    } else if (tool.id == "gif_creator") {
                        val gif = withContext(Dispatchers.Default) {
                            GifProcessor.encode(bitmaps.map { it.second }, gifDelay.toLong())
                        }
                        pendingItems = listOf(RawExportItem(gif, "image/gif", "gif", "animation"))
                        previewBitmaps = bitmaps.take(4).map { createToolThumbnail(it.second) }
                    } else if (tool.id == "collage") {
                        val collage = withContext(Dispatchers.Default) {
                            AdvancedImageProcessor.collage(bitmaps.take(4).map { it.second })
                        }
                        val collageBytes = withContext(Dispatchers.Default) {
                            ImageProcessor.encode(collage, OutputFormat.PNG, 100)
                        }
                        pendingItems = listOf(
                            RawExportItem(
                                collageBytes,
                                OutputFormat.PNG.mime,
                                OutputFormat.PNG.extension,
                                "collage"
                            )
                        )
                        previewBitmaps = listOf(createToolThumbnail(collage))
                    } else {
                        val items = bitmaps.map { (uri, bitmap) -> analyzeBitmap(bitmap, uri) }
                        pendingItems = items
                        textResult = items.firstOrNull { it.mime == "text/plain" || it.mime == "text/css" }
                            ?.let { String(it.bytes, Charsets.UTF_8) }
                        val decodedResults = items.take(6).mapNotNull { item ->
                            if (item.mime.startsWith("image/")) decodeToolPreview(item.bytes) else null
                        }
                        previewBitmaps = if (decodedResults.isNotEmpty()) decodedResults
                        else bitmaps.take(6).map { createToolThumbnail(it.second) }
                    }

                    ProcessingStatsStore.recordProcessed(context, bitmaps.size)
                    status = strings.get("ready")
                    statusIsError = false
                }
            } catch (e: Exception) {
                status = e.message ?: strings.get("process.error")
                statusIsError = true
            } finally {
                busy = false
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { StudioHeroV8(tool, strings.toolTitle(tool.id), strings.toolSubtitle(tool.id), tool.premium, onBack) }

        item {
            StudioSectionV8(
                number = "01", title = strings.get("choose.image"),
                subtitle = when (tool.id) {
                    "pdf_merge" -> strings.get("choose.pdfs")
                    "gif_creator", "collage" -> strings.get("choose.frames")
                    else -> strings.get("local")
                },
                icon = Icons.Default.AddPhotoAlternate, accent = tool.start
            ) {
                if (tool.id !in listOf("pdf_merge", "gif_creator", "collage", "base64")) {
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant).padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        StudioModeChipV8(strings.get("batch.single"), !batchMode, tool.start, Modifier.weight(1f)) {
                            batchMode = false
                            sourceUris = emptyList(); sourcePreviewBitmaps = emptyList()
                            previewBitmaps = emptyList(); pendingItems = emptyList(); textResult = null; exportedUri = null
                        }
                        StudioModeChipV8(strings.get("batch.multiple"), batchMode, tool.start, Modifier.weight(1f)) {
                            batchMode = true
                            sourceUris = emptyList(); sourcePreviewBitmaps = emptyList()
                            previewBitmaps = emptyList(); pendingItems = emptyList(); textResult = null; exportedUri = null
                        }
                    }
                }

                OutlinedButton(
                    onClick = { chooseInput() },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(17.dp),
                    border = BorderStroke(1.dp, tool.start.copy(alpha = 0.55f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = tool.start)
                ) {
                    Icon(
                        when (tool.id) {
                            "pdf_merge" -> Icons.Default.PictureAsPdf
                            "gif_creator", "collage" -> Icons.Default.Collections
                            else -> if (batchMode) Icons.Default.Collections else Icons.Default.AddPhotoAlternate
                        }, null
                    )
                    Spacer(Modifier.width(9.dp))
                    Text(
                        when (tool.id) {
                            "pdf_merge" -> strings.get("choose.pdfs")
                            "gif_creator", "collage" -> strings.get("choose.frames")
                            else -> if (sourceUris.isEmpty()) {
                                if (batchMode) strings.get("choose.images") else strings.get("choose.image")
                            } else strings.get("choose.another")
                        }, fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.Default.ArrowForward, null)
                }

                if (sourceUris.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF0D9488), modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(7.dp))
                        Text(sourceUris.size.toString() + " " + strings.get("batch.selected"), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = {
                            sourceUris = emptyList(); sourcePreviewBitmaps = emptyList(); previewBitmaps = emptyList()
                            pendingItems = emptyList(); textResult = null; exportedUri = null; status = null; statusIsError = false
                        }) { Text(strings.get("cancel"), fontSize = 11.sp) }
                    }
                    if (sourcePreviewBitmaps.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                            sourcePreviewBitmaps.forEachIndexed { index, thumbnail ->
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Image(
                                        bitmap = thumbnail.asImageBitmap(), contentDescription = null,
                                        modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(17.dp))
                                            .border(1.dp, tool.start.copy(alpha = 0.28f), RoundedCornerShape(17.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    if (index == 0) Text(
                                        if (sourceUris.size == 1) strings.get("choose.image") else strings.get("batch.image") + " 1",
                                        maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if (sourceUris.size > sourcePreviewBitmaps.size) {
                                Box(
                                    Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(17.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.Collections, null, tint = tool.start)
                                        Text("+" + (sourceUris.size - sourcePreviewBitmaps.size), fontWeight = FontWeight.ExtraBold)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                        Text(strings.get("private"), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        if (sourceUris.isNotEmpty() && tool.id != "pdf_merge") {
            item {
                StudioSectionV8(
                    number = "02", title = strings.get("config"),
                    subtitle = strings.toolSubtitle(tool.id),
                    icon = Icons.Default.Tune, accent = tool.end
                ) {
                    ToolControlsV6(
                        tool, strings, format, { format = it }, quality, { quality = it },
                        width, { width = it }, height, { height = it }, cropRatio, { cropRatio = it },
                        angle, { angle = it }, flipH, { flipH = it }, flipV, { flipV = it },
                        imageFilter, { imageFilter = it }, watermark, { watermark = it }, opacity, { opacity = it },
                        position, { position = it }, amount, { amount = it }, pixelSize, { pixelSize = it },
                        borderSize, { borderSize = it }, cornerRadius, { cornerRadius = it },
                        duotonePreset, { duotonePreset = it }, socialPreset, { socialPreset = it },
                        modernFormat, { modernFormat = it }, smartWidth, { smartWidth = it }, smartHeight, { smartHeight = it },
                        gifDelay, { gifDelay = it }, fitBackground, { fitBackground = it }, targetKb, { targetKb = it },
                        gridRows, { gridRows = it }, gridColumns, { gridColumns = it }, mirrorAxis, { mirrorAxis = it }
                    )
                }
            }
        }

        if (sourceUris.isNotEmpty()) {
            item {
                StudioActionCardV8(
                    accent = tool.start,
                    title = if (pendingItems.isNotEmpty()) strings.get("ready") else strings.get("action"),
                    description = if (batchMode) strings.get("processing.batch") else strings.get("processing.subtitle"),
                    enabled = !busy, buttonTitle = strings.get("preview.button"),
                    onRun = { processAction() },
                    leadingIcon = if (pendingItems.isNotEmpty()) Icons.Default.Refresh else Icons.Default.AutoAwesome
                )
            }
        }

        if (status != null) {
            item { StudioStatusCardV8(status.orEmpty(), statusIsError, tool.start) }
        }

        if (previewBitmaps.isNotEmpty() || textResult != null || pendingItems.isNotEmpty()) {
            item {
                StudioSectionV8(
                    number = "03", title = strings.get("preview"),
                    subtitle = if (pendingItems.isNotEmpty()) pendingItems.size.toString() + " " + strings.get("batch.results") else strings.get("ready"),
                    icon = Icons.Default.Visibility, accent = Color(0xFF0D9488)
                ) {
                    textResult?.let { result ->
                        Text(strings.get("result.content"), fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                result.take(1200) + if (result.length > 1200) "…" else "",
                                Modifier.fillMaxWidth().padding(13.dp), fontSize = 11.sp, lineHeight = 16.sp,
                                maxLines = 7, overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (result.length > 1200) Text(
                            strings.get("base64.truncated"),
                            color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp
                        )
                        if (result.length <= 250_000) {
                            Button(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText(strings.get("result.content"), result))
                                    status = strings.get("base64.copy"); statusIsError = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488))
                            ) {
                                Icon(Icons.Default.ContentCopy, null); Spacer(Modifier.width(7.dp)); Text(strings.get("base64.copy"))
                            }
                        } else Text(
                            strings.get("base64.copy.too.large"),
                            color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp
                        )
                    }

                    if (previewBitmaps.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                            previewBitmaps.take(4).forEach { bmp ->
                                Image(
                                    bitmap = bmp.asImageBitmap(), contentDescription = null,
                                    modifier = Modifier.weight(1f).aspectRatio(0.92f).clip(RoundedCornerShape(17.dp))
                                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), RoundedCornerShape(17.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                    if (textResult == null && previewBitmaps.isEmpty()) Text(
                        pendingItems.size.toString() + " " + strings.get("batch.results"),
                        color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp
                    )
                }
            }
        }

        if (pendingItems.isNotEmpty()) {
            item {
                StudioExportCardV8(
                    count = pendingItems.size, accent = Color(0xFF0D9488), title = strings.get("export"),
                    subtitle = strings.get("save.text"), saveLabel = strings.get("save"),
                    enabled = !busy, onExport = { exportResults() }
                )
            }
        }

        exportedUri?.let { uri ->
            item {
                StudioShareCardV8(
                    title = strings.get("saved"), subtitle = strings.get("share"),
                    onOpen = {
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW).apply {
                                data = uri; type = exportedMime; addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            })
                        }.onFailure {
                            status = strings.get("open") + ": " + (it.message ?: strings.get("process.error"))
                            statusIsError = true
                        }
                    },
                    onShare = {
                        runCatching {
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = exportedMime; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(send, strings.get("share")))
                        }.onFailure {
                            status = strings.get("share") + ": " + (it.message ?: strings.get("process.error"))
                            statusIsError = true
                        }
                    }
                )
            }
        }
    }

    if (busy) {
        PolishedProcessingDialogV7(
            title = strings.get("processing.title"),
            subtitle = if (batchMode) strings.get("processing.batch") else strings.get("processing.subtitle"),
            localMessage = strings.get("local")
        )
    }
}

data class AnyToolDef(
    val id: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val start: Color,
    val end: Color,
    val premium: Boolean
)


@Composable
private fun StudioHeroV8(
    tool: AnyToolDef, title: String, subtitle: String, premium: Boolean, onBack: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF11172A))
    ) {
        Box(
            Modifier.fillMaxWidth()
                .background(Brush.linearGradient(listOf(Color(0xFF11172A), tool.start, tool.end)))
        ) {
            Box(
                Modifier.align(Alignment.TopEnd).padding(top = 8.dp, end = 14.dp)
                    .size(116.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.08f))
            )
            Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                    }
                    Spacer(Modifier.width(3.dp))
                    Column(Modifier.weight(1f)) {
                        Text("IMAGE STUDIO", color = Color.White.copy(alpha = 0.75f), fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.5.sp)
                        Text(title, color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    Box(
                        Modifier.size(54.dp).clip(RoundedCornerShape(18.dp)).background(Color.White.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(tool.icon, null, tint = Color.White, modifier = Modifier.size(29.dp))
                    }
                }
                Text(subtitle, color = Color.White.copy(alpha = 0.86f), fontSize = 12.sp, lineHeight = 17.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StudioHeroBadgeV8(Icons.Default.Security, "LOCAL")
                    StudioHeroBadgeV8(
                        if (premium) Icons.Default.WorkspacePremium else Icons.Default.AutoAwesome,
                        if (premium) "PRO" else "READY"
                    )
                }
            }
        }
    }
}

@Composable
private fun StudioHeroBadgeV8(icon: ImageVector, label: String) {
    Surface(color = Color.White.copy(alpha = 0.13f), shape = RoundedCornerShape(11.dp)) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.6.sp)
        }
    }
}

@Composable
private fun StudioSectionV8(
    number: String,
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(26.dp)
    Card(
        modifier = Modifier.fillMaxWidth().border(BorderStroke(1.dp, accent.copy(alpha = 0.22f)), shape),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(46.dp).clip(RoundedCornerShape(15.dp))
                        .background(Brush.linearGradient(listOf(accent, accent.copy(alpha = 0.65f)))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = Color.White, modifier = Modifier.size(23.dp))
                }
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(title, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 15.sp)
                }
                Surface(color = accent.copy(alpha = 0.12f), shape = RoundedCornerShape(10.dp)) {
                    Text(number, Modifier.padding(horizontal = 9.dp, vertical = 6.dp), color = accent, fontWeight = FontWeight.ExtraBold, fontSize = 10.sp)
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            content()
        }
    }
}

@Composable
private fun StudioModeChipV8(title: String, selected: Boolean, accent: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) accent else Color.Transparent,
        contentColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Text(title, Modifier.padding(horizontal = 8.dp, vertical = 11.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@Composable
private fun StudioActionCardV8(
    accent: Color, title: String, description: String, enabled: Boolean,
    buttonTitle: String, leadingIcon: ImageVector, onRun: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(accent.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
                    Icon(leadingIcon, null, tint = accent, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                    Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                }
            }
            Button(
                onClick = onRun, enabled = enabled,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(17.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.White)
            ) {
                Icon(Icons.Default.AutoAwesome, null)
                Spacer(Modifier.width(8.dp))
                Text(buttonTitle, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.weight(1f))
                Icon(Icons.Default.ArrowForward, null)
            }
        }
    }
}

@Composable
private fun StudioStatusCardV8(message: String, isError: Boolean, accent: Color) {
    val tint = if (isError) MaterialTheme.colorScheme.error else accent
    val background = if (isError) MaterialTheme.colorScheme.errorContainer else tint.copy(alpha = 0.10f)
    Surface(
        color = background,
        shape = RoundedCornerShape(17.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(if (isError) Icons.Default.ErrorOutline else Icons.Default.CheckCircle, null, tint = tint)
            Spacer(Modifier.width(9.dp))
            Text(message, color = if (isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun StudioExportCardV8(count: Int, accent: Color, title: String, subtitle: String, saveLabel: String, enabled: Boolean, onExport: () -> Unit) {
    StudioSectionV8("04", title, subtitle, Icons.Default.SaveAlt, accent) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(count.toString() + " " + LocalUiText.current.get("batch.results"), fontWeight = FontWeight.ExtraBold)
                Text(LocalUiText.current.get("private"), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.VerifiedUser, null, tint = accent, modifier = Modifier.size(26.dp))
        }
        Button(
            onClick = onExport, enabled = enabled,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(17.dp),
            colors = ButtonDefaults.buttonColors(containerColor = accent)
        ) {
            Icon(Icons.Default.Save, null); Spacer(Modifier.width(8.dp)); Text(saveLabel, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun StudioShareCardV8(title: String, subtitle: String, onOpen: () -> Unit, onShare: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().border(BorderStroke(1.dp, Color(0xFF0D9488).copy(alpha = 0.25f)), RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFF0D9488).copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF0D9488))
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, fontWeight = FontWeight.ExtraBold)
                    Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                Button(onClick = onOpen, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) {
                    Icon(Icons.Default.OpenInNew, null); Spacer(Modifier.width(5.dp)); Text(LocalUiText.current.get("open"))
                }
                OutlinedButton(onClick = onShare, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) {
                    Icon(Icons.Default.Share, null); Spacer(Modifier.width(5.dp)); Text(LocalUiText.current.get("share"))
                }
            }
        }
    }
}

@Composable
private fun PolishedProcessingDialogV7(title: String, subtitle: String, localMessage: String) {
    Dialog(onDismissRequest = {}) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(30.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    Modifier.size(82.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(Color(0xFF6D28D9), Color(0xFF0891B2)))),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(62.dp),
                        color = Color.White,
                        strokeWidth = 3.dp
                    )
                    Icon(Icons.Default.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(25.dp))
                }
                Text(title, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Text(
                    subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(13.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Security, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(7.dp))
                        Text(localMessage, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolControlsV6(
    tool: AnyToolDef, strings: UiText,
    format: OutputFormat, onFormat: (OutputFormat) -> Unit,
    quality: Float, onQuality: (Float) -> Unit,
    width: String, onWidth: (String) -> Unit, height: String, onHeight: (String) -> Unit,
    cropRatio: String, onCropRatio: (String) -> Unit,
    angle: Int, onAngle: (Int) -> Unit, flipH: Boolean, onFlipH: (Boolean) -> Unit, flipV: Boolean, onFlipV: (Boolean) -> Unit,
    imageFilter: ImageFilter, onFilter: (ImageFilter) -> Unit,
    watermark: String, onWatermark: (String) -> Unit, opacity: Float, onOpacity: (Float) -> Unit,
    position: String, onPosition: (String) -> Unit,
    amount: Float, onAmount: (Float) -> Unit,
    pixelSize: Float, onPixelSize: (Float) -> Unit,
    borderSize: Float, onBorderSize: (Float) -> Unit,
    cornerRadius: Float, onCornerRadius: (Float) -> Unit,
    duotonePreset: String, onDuotonePreset: (String) -> Unit,
    socialPreset: String, onSocialPreset: (String) -> Unit,
    modernFormat: ModernFormat, onModernFormat: (ModernFormat) -> Unit,
    smartWidth: String, onSmartWidth: (String) -> Unit, smartHeight: String, onSmartHeight: (String) -> Unit,
    gifDelay: Float, onGifDelay: (Float) -> Unit,
    fitBackground: String, onFitBackground: (String) -> Unit,
    targetKb: Float, onTargetKb: (Float) -> Unit,
    gridRows: Int, onGridRows: (Int) -> Unit,
    gridColumns: Int, onGridColumns: (Int) -> Unit,
    mirrorAxis: String, onMirrorAxis: (String) -> Unit
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(11.dp)) {
        when (tool.id) {
            "resize" -> StudioControlPanelV8(strings.get("width") + " × " + strings.get("height"), strings.get("aspect"), Icons.Default.AspectRatio, tool.start) {
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    OutlinedTextField(
                        value = width, onValueChange = { onWidth(it.filter(Char::isDigit)) },
                        modifier = Modifier.weight(1f), label = { Text(strings.get("width")) },
                        suffix = { Text("px") }, singleLine = true, shape = RoundedCornerShape(14.dp)
                    )
                    OutlinedTextField(
                        value = height, onValueChange = { onHeight(it.filter(Char::isDigit)) },
                        modifier = Modifier.weight(1f), label = { Text(strings.get("height")) },
                        suffix = { Text("px") }, singleLine = true, shape = RoundedCornerShape(14.dp)
                    )
                }
            }
            "compress" -> {
                StudioSliderPanelV8(strings.get("quality"), quality, 10f..100f, tool.start, Icons.Default.Compress, { onQuality(it) }, quality.toInt().toString() + "%")
                StudioChoicePanelV8(strings.get("format"), listOf("JPEG" to "JPEG", "WEBP" to "WEBP"), format.name, tool.start, Icons.Default.Image) {
                    onFormat(OutputFormat.valueOf(it))
                }
            }
            "base64" -> {
                StudioChoicePanelV8(strings.get("format"), listOf("JPEG" to "JPEG", "PNG" to "PNG", "WEBP" to "WEBP"), format.name, tool.start, Icons.Default.Code) {
                    onFormat(OutputFormat.valueOf(it))
                }
                StudioSliderPanelV8(strings.get("quality"), quality, 10f..100f, tool.start, Icons.Default.Tune, { onQuality(it) }, quality.toInt().toString() + "%")
            }
            "convert", "metadata" -> StudioChoicePanelV8(
                strings.get("format"), listOf("JPEG" to "JPEG", "PNG" to "PNG", "WEBP" to "WEBP"),
                format.name, tool.start, Icons.Default.SwapHoriz
            ) { onFormat(OutputFormat.valueOf(it)) }

            "crop" -> StudioChoicePanelV8(
                strings.get("tool.crop.title"),
                listOf("Original" to "Original", "1:1" to "1:1", "4:5" to "4:5", "16:9" to "16:9", "9:16" to "9:16"),
                cropRatio, tool.start, Icons.Default.Crop
            ) { onCropRatio(it) }

            "rotate" -> {
                StudioChoicePanelV8(
                    strings.get("tool.rotate.title"), listOf("90" to "90°", "180" to "180°", "270" to "270°"),
                    angle.toString(), tool.start, Icons.Default.Rotate90DegreesCw
                ) { onAngle(it.toInt()) }
                StudioControlPanelV8(strings.get("mirror.h") + " / " + strings.get("mirror.v"), null, Icons.Default.Flip, tool.start) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StudioToggleV8(strings.get("mirror.h"), flipH, tool.start, Modifier.weight(1f)) { onFlipH(!flipH) }
                        StudioToggleV8(strings.get("mirror.v"), flipV, tool.start, Modifier.weight(1f)) { onFlipV(!flipV) }
                    }
                }
            }
            "filter" -> StudioChoicePanelV8(
                strings.get("tool.filter.title"),
                ImageFilter.entries.map { it.name to strings.filterTitle(it.name.lowercase()) },
                imageFilter.name, tool.start, Icons.Default.FilterVintage
            ) { selected -> onFilter(ImageFilter.valueOf(selected)) }

            "watermark" -> {
                StudioControlPanelV8(strings.get("text"), strings.get("tool.watermark.subtitle"), Icons.Default.TextFields, tool.start) {
                    OutlinedTextField(
                        value = watermark, onValueChange = onWatermark, modifier = Modifier.fillMaxWidth(),
                        label = { Text(strings.get("text")) }, singleLine = true, shape = RoundedCornerShape(14.dp)
                    )
                }
                StudioSliderPanelV8(strings.get("opacity"), opacity, 10f..100f, tool.start, Icons.Default.Opacity, { onOpacity(it) }, opacity.toInt().toString() + "%")
                StudioChoicePanelV8(
                    strings.get("position"),
                    listOf(
                        "top_left" to strings.get("position.top_left"),
                        "top_right" to strings.get("position.top_right"),
                        "center" to strings.get("position.center"),
                        "bottom_left" to strings.get("position.bottom_left"),
                        "bottom_right" to strings.get("position.bottom_right")
                    ),
                    position, tool.start, Icons.Default.ControlCamera
                ) { onPosition(it) }
            }

            "brightness", "contrast", "saturation", "warmth", "exposure", "tint" -> {
                val title = strings.get("tool." + tool.id + ".title")
                StudioSliderPanelV8(title, amount, -100f..100f, tool.start, tool.icon, { onAmount(it) }, amount.toInt().toString())
                StudioNoteV8(strings.get("tool." + tool.id + ".subtitle"), tool.start)
            }
            "blur", "sharpen", "noise_reduction", "face_blur", "vignette", "film_grain", "color_pop", "glitch", "outline", "scan_document" -> {
                StudioSliderPanelV8(
                    strings.get("strength"), amount, 5f..100f, tool.start, tool.icon, { onAmount(it) },
                    amount.toInt().toString() + "%"
                )
                StudioNoteV8(strings.toolSubtitle(tool.id), tool.start)
            }
            "posterize" -> StudioSliderPanelV8(strings.get("strength"), amount, 2f..12f, tool.start, tool.icon, { onAmount(it) }, amount.coerceIn(2f, 12f).toInt().toString() + " levels")
            "pixelate" -> StudioSliderPanelV8(strings.get("tool.pixelate.title"), pixelSize, 4f..48f, tool.start, tool.icon, { onPixelSize(it) }, pixelSize.toInt().toString())
            "border" -> StudioSliderPanelV8(strings.get("tool.border.title"), borderSize, 4f..120f, tool.start, tool.icon, { onBorderSize(it) }, borderSize.toInt().toString() + " px")
            "round" -> StudioSliderPanelV8(strings.get("tool.round.title"), cornerRadius, 8f..160f, tool.start, tool.icon, { onCornerRadius(it) }, cornerRadius.toInt().toString() + " px")
            "mirror" -> StudioChoicePanelV8(
                strings.get("tool.mirror.title"), listOf("horizontal" to strings.get("mirror.h"), "vertical" to strings.get("mirror.v")),
                mirrorAxis, tool.start, Icons.Default.Flip
            ) { onMirrorAxis(it) }
            "fit_canvas" -> {
                StudioControlPanelV8(strings.get("width") + " × " + strings.get("height"), strings.get("tool.fit_canvas.subtitle"), Icons.Default.AspectRatio, tool.start) {
                    Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        OutlinedTextField(width, { onWidth(it.filter(Char::isDigit)) }, Modifier.weight(1f), label = { Text(strings.get("width")) }, suffix = { Text("px") }, singleLine = true, shape = RoundedCornerShape(14.dp))
                        OutlinedTextField(height, { onHeight(it.filter(Char::isDigit)) }, Modifier.weight(1f), label = { Text(strings.get("height")) }, suffix = { Text("px") }, singleLine = true, shape = RoundedCornerShape(14.dp))
                    }
                }
                StudioChoicePanelV8(
                    strings.get("canvas.background"), listOf("white" to strings.get("background.white"), "black" to strings.get("background.black")),
                    fitBackground, tool.start, Icons.Default.Palette
                ) { onFitBackground(it) }
            }
            "target_size" -> {
                StudioSliderPanelV8(strings.get("target.kb"), targetKb, 50f..5000f, tool.start, Icons.Default.DataUsage, { onTargetKb(it) }, targetKb.toInt().toString() + " KB", steps = 48)
                StudioChoicePanelV8(strings.get("format"), listOf("JPEG" to "JPEG", "WEBP" to "WEBP"), format.name, tool.start, Icons.Default.Image) {
                    onFormat(OutputFormat.valueOf(it))
                }
            }
            "split_grid" -> {
                StudioChoicePanelV8(strings.get("grid.rows"), listOf("2" to "2", "3" to "3", "4" to "4"), gridRows.toString(), tool.start, Icons.Default.GridOn) { onGridRows(it.toInt()) }
                StudioChoicePanelV8(strings.get("grid.columns"), listOf("2" to "2", "3" to "3", "4" to "4"), gridColumns.toString(), tool.start, Icons.Default.GridOn) { onGridColumns(it.toInt()) }
            }
            "duotone" -> StudioChoicePanelV8(
                strings.get("tool.duotone.title"),
                listOf("ocean" to strings.get("duotone.ocean"), "sunset" to strings.get("duotone.sunset"), "ink" to strings.get("duotone.ink")),
                duotonePreset, tool.start, Icons.Default.ColorLens
            ) { onDuotonePreset(it) }

            "social_presets" -> StudioChoicePanelV8(
                strings.get("preset.social"),
                SocialImageProcessor.presets.map { it.id to it.title }, socialPreset, tool.start, Icons.Default.PhoneAndroid
            ) { onSocialPreset(it) }

            "smart_resize" -> {
                StudioControlPanelV8(strings.get("smart.resize"), strings.get("tool.smart_resize.subtitle"), Icons.Default.AspectRatio, tool.start) {
                    Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        OutlinedTextField(smartWidth, { onSmartWidth(it.filter(Char::isDigit)) }, Modifier.weight(1f), label = { Text(strings.get("width")) }, suffix = { Text("px") }, singleLine = true, shape = RoundedCornerShape(14.dp))
                        OutlinedTextField(smartHeight, { onSmartHeight(it.filter(Char::isDigit)) }, Modifier.weight(1f), label = { Text(strings.get("height")) }, suffix = { Text("px") }, singleLine = true, shape = RoundedCornerShape(14.dp))
                    }
                }
                StudioChoicePanelV8(strings.get("format"), listOf("JPEG" to "JPEG", "PNG" to "PNG", "WEBP" to "WEBP"), format.name, tool.start, Icons.Default.Image) {
                    onFormat(OutputFormat.valueOf(it))
                }
            }
            "heic_avif" -> {
                StudioChoicePanelV8(
                    strings.get("modern.formats"), listOf("HEIC" to "HEIC", "AVIF" to "AVIF"), modernFormat.name, tool.start, Icons.Default.Image
                ) { onModernFormat(ModernFormat.valueOf(it)) }
                StudioSliderPanelV8(strings.get("quality"), quality, 10f..100f, tool.start, Icons.Default.Tune, { onQuality(it) }, quality.toInt().toString() + "%")
            }
            "gif_creator" -> StudioSliderPanelV8(strings.get("quality"), gifDelay, 100f..1500f, tool.start, Icons.Default.Gif, { onGifDelay(it) }, gifDelay.toInt().toString() + " ms")
            "css_palette" -> StudioNoteV8(strings.get("tool.css_palette.subtitle"), tool.start)
            "palette" -> StudioNoteV8(strings.get("tool.palette.subtitle"), tool.start)
            "ocr" -> StudioNoteV8(strings.get("tool.ocr.subtitle"), tool.start)
            "exif" -> StudioNoteV8(strings.get("tool.exif.subtitle"), tool.start)
            "details" -> StudioNoteV8(strings.get("tool.details.subtitle"), tool.start)
            "negative", "auto_enhance", "pdf", "collage" -> StudioNoteV8(strings.toolSubtitle(tool.id), tool.start)
            else -> StudioNoteV8(strings.toolSubtitle(tool.id), tool.start)
        }
    }
}

@Composable
private fun StudioControlPanelV8(
    title: String, subtitle: String?, icon: ImageVector, accent: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    Card(
        modifier = Modifier.fillMaxWidth().border(BorderStroke(1.dp, accent.copy(alpha = 0.18f)), shape),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f))
    ) {
        Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(accent.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = accent, modifier = Modifier.size(19.dp))
                }
                Spacer(Modifier.width(9.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(title, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                    if (!subtitle.isNullOrBlank()) Text(subtitle, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 14.sp)
                }
            }
            content()
        }
    }
}

@Composable
private fun StudioSliderPanelV8(
    title: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    accent: Color,
    icon: ImageVector,
    onValue: (Float) -> Unit,
    valueLabel: String = value.toInt().toString(),
    steps: Int = 0
) {
    StudioControlPanelV8(title, null, icon, accent) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
            Surface(color = accent.copy(alpha = 0.13f), shape = RoundedCornerShape(10.dp)) {
                Text(valueLabel, Modifier.padding(horizontal = 10.dp, vertical = 6.dp), color = accent, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
            }
        }
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onValue,
            valueRange = range,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = accent,
                activeTrackColor = accent,
                inactiveTrackColor = accent.copy(alpha = 0.20f),
                activeTickColor = Color.White,
                inactiveTickColor = accent.copy(alpha = 0.28f)
            )
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(range.start.toInt().toString(), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
            Text(range.endInclusive.toInt().toString(), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
        }
    }
}

@Composable
private fun StudioChoicePanelV8(
    title: String,
    options: List<Pair<String, String>>,
    selected: String,
    accent: Color,
    icon: ImageVector,
    onSelect: (String) -> Unit
) {
    StudioControlPanelV8(title, null, icon, accent) {
        options.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (value, label) ->
                    val isSelected = value == selected
                    Surface(
                        onClick = { onSelect(value) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(13.dp),
                        color = if (isSelected) accent else MaterialTheme.colorScheme.surface,
                        contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                    ) {
                        Row(
                            Modifier.padding(horizontal = 9.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isSelected) {
                                Icon(Icons.Default.Check, null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(5.dp))
                            }
                            Text(label, fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium, fontSize = 11.sp, maxLines = 2, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StudioToggleV8(title: String, selected: Boolean, accent: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = if (selected) accent else MaterialTheme.colorScheme.surface,
        contentColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, if (selected) accent else MaterialTheme.colorScheme.outline.copy(alpha = 0.24f))
    ) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 13.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Icon(if (selected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked, null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun StudioNoteV8(text: String, accent: Color) {
    StudioControlPanelV8(
        title = text,
        subtitle = null,
        icon = Icons.Default.AutoAwesome,
        accent = accent
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Security, null, tint = accent, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(LocalUiText.current.get("local"), fontWeight = FontWeight.Bold, fontSize = 11.sp)
            Spacer(Modifier.weight(1f))
            Icon(Icons.Default.CheckCircle, null, tint = accent)
        }
    }
}

@Composable
private fun ChoicesV6(
    options: List<Pair<String,String>>,
    selectedId: String,
    onSelected: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        options.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { (id,label) ->
                    FilterChip(
                        selected = id == selectedId,
                        onClick = { onSelected(id) },
                        label = { Text(label, fontSize = 10.sp) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}


private fun decodeToolThumbnail(context: Context, uri: Uri, maxEdge: Int = 180): Bitmap? {
    val resolver = context.contentResolver
    val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
    val boundsStream = resolver.openInputStream(uri) ?: return null
    boundsStream.use { android.graphics.BitmapFactory.decodeStream(it, null, bounds) }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    var sampleSize = 1
    while (bounds.outWidth / sampleSize > maxEdge * 2 || bounds.outHeight / sampleSize > maxEdge * 2) {
        sampleSize *= 2
    }
    val options = android.graphics.BitmapFactory.Options().apply {
        inSampleSize = sampleSize
    }
    val stream = resolver.openInputStream(uri) ?: return null
    val decoded = stream.use { android.graphics.BitmapFactory.decodeStream(it, null, options) } ?: return null
    return createToolThumbnail(decoded, maxEdge)
}

private fun decodeToolPreview(bytes: ByteArray, maxEdge: Int = 180): Bitmap? {
    val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
    android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    var sampleSize = 1
    while (bounds.outWidth / sampleSize > maxEdge * 2 || bounds.outHeight / sampleSize > maxEdge * 2) {
        sampleSize *= 2
    }
    val options = android.graphics.BitmapFactory.Options().apply { inSampleSize = sampleSize }
    val decoded = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options) ?: return null
    return createToolThumbnail(decoded, maxEdge)
}

private fun createToolThumbnail(source: Bitmap, maxEdge: Int = 180): Bitmap {
    val longestEdge = maxOf(source.width, source.height)
    if (longestEdge <= maxEdge) return source
    val scale = maxEdge.toFloat() / longestEdge.toFloat()
    val thumbnail = Bitmap.createScaledBitmap(
        source,
        (source.width * scale).toInt().coerceAtLeast(1),
        (source.height * scale).toInt().coerceAtLeast(1),
        true
    )
    if (thumbnail !== source && !source.isRecycled) source.recycle()
    return thumbnail
}
