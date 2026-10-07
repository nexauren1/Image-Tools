package com.nexauren.imagetools.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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

    var batchMode by remember { mutableStateOf(tool.id == "collage" || tool.id == "gif_creator" || tool.id == "pdf_merge") }
    var sourceUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var previewBitmaps by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var pendingItems by remember { mutableStateOf<List<RawExportItem>>(emptyList()) }
    var exportedUri by remember { mutableStateOf<Uri?>(null) }
    var exportedMime by remember { mutableStateOf("image/*") }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }

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

    val singlePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            sourceUris = listOf(uri)
            previewBitmaps = emptyList()
            pendingItems = emptyList()
            status = strings.get("ready")
        }
    }

    val multiPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(20)
    ) { uris ->
        if (uris.isNotEmpty()) {
            sourceUris = uris.take(20)
            previewBitmaps = emptyList()
            pendingItems = emptyList()
            status = strings.get("ready")
        }
    }

    val pdfPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            sourceUris = uris.take(20)
            previewBitmaps = emptyList()
            pendingItems = emptyList()
            status = strings.get("ready")
        }
    }

    fun chooseInput() {
        if (tool.premium && !premium) {
            onNeedPremium()
            return
        }
        when (tool.id) {
            "pdf_merge" -> pdfPicker.launch(arrayOf("application/pdf"))
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
                    AdvancedImageProcessor.addBorder(bitmap, borderSize.toInt(), Color.WHITE.value.toInt()),
                    format, 100
                ),
                format.mime, format.extension, tool.id
            )
            "round" -> RawExportItem(
                ImageProcessor.encode(AdvancedImageProcessor.roundCorners(bitmap, cornerRadius), OutputFormat.PNG, 100),
                OutputFormat.PNG.mime, OutputFormat.PNG.extension, tool.id
            )
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
                ImageProcessor.encode(AdvancedImageProcessor.mirror(bitmap, flipH), format, 100),
                format.mime, format.extension, tool.id
            )
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
            else -> transformBitmap(bitmap, uri)
        }
    }

    fun recipeConfig(): Map<String, String> = mapOf(
        "width" to width,
        "height" to height,
        "quality" to quality.toInt().toString(),
        "format" to format.name,
        "cropRatio" to cropRatio,
        "angle" to angle.toString(),
        "amount" to amount.toInt().toString(),
        "pixelSize" to pixelSize.toInt().toString(),
        "borderSize" to borderSize.toInt().toString(),
        "cornerRadius" to cornerRadius.toInt().toString(),
        "socialPreset" to socialPreset,
        "modernFormat" to modernFormat.name,
        "smartWidth" to smartWidth,
        "smartHeight" to smartHeight
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
            } catch (e: Exception) {
                status = e.message ?: strings.get("save.error")
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
            exportedUri = null
            status = strings.get("processing")
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

                    if (tool.id == "gif_creator") {
                        val gif = withContext(Dispatchers.Default) {
                            GifProcessor.encode(bitmaps.map { it.second }, gifDelay.toLong())
                        }
                        pendingItems = listOf(RawExportItem(gif, "image/gif", "gif", "animation"))
                        previewBitmaps = bitmaps.take(4).map { it.second }
                    } else if (tool.id == "collage") {
                        val collage = withContext(Dispatchers.Default) {
                            AdvancedImageProcessor.collage(bitmaps.take(4).map { it.second })
                        }
                        pendingItems = listOf(
                            RawExportItem(
                                ImageProcessor.encode(collage, OutputFormat.PNG, 100),
                                OutputFormat.PNG.mime,
                                OutputFormat.PNG.extension,
                                "collage"
                            )
                        )
                        previewBitmaps = listOf(collage)
                    } else {
                        val items = bitmaps.map { (uri, bitmap) -> analyzeBitmap(bitmap, uri) }
                        pendingItems = items
                        previewBitmaps = bitmaps.take(6).map { it.second }
                    }

                    ProcessingStatsStore.recordProcessed(context, bitmaps.size)
                    status = strings.get("ready")
                }
            } catch (e: Exception) {
                status = e.message ?: strings.get("process.error")
            } finally {
                busy = false
            }
        }
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, strings.get("back"))
                }
                Box(
                    Modifier.size(50.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Brush.linearGradient(listOf(tool.start, tool.end))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(tool.icon, null, tint = Color.White)
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(strings.toolTitle(tool.id), fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
                    Text(strings.toolSubtitle(tool.id), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (tool.premium) {
                    Text(strings.get("pro"), color = Color(0xFF7C3AED), fontWeight = FontWeight.ExtraBold, fontSize = 9.sp)
                }
            }
        }

        item {
            Card(shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("1. " + strings.get("choose.image"), fontWeight = FontWeight.ExtraBold)
                    if (tool.id !in listOf("pdf_merge", "gif_creator", "collage")) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = !batchMode,
                                onClick = { batchMode = false },
                                label = { Text(strings.get("batch.single")) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = batchMode,
                                onClick = { batchMode = true },
                                label = { Text(strings.get("batch.multiple")) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Button(
                        onClick = { chooseInput() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(
                            when (tool.id) {
                                "pdf_merge" -> Icons.Default.PictureAsPdf
                                else -> if (batchMode) Icons.Default.Collections else Icons.Default.AddPhotoAlternate
                            },
                            null
                        )
                        Spacer(Modifier.width(7.dp))
                        Text(
                            when (tool.id) {
                                "pdf_merge" -> strings.get("choose.pdfs")
                                "gif_creator", "collage" -> strings.get("choose.frames")
                                else -> if (batchMode) strings.get("choose.images") else strings.get("choose.image")
                            }
                        )
                    }
                    if (sourceUris.isNotEmpty()) {
                        Text(
                            sourceUris.size.toString() + " " + strings.get("batch.selected"),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        if (sourceUris.isNotEmpty() && tool.id != "pdf_merge") {
            item {
                Card(shape = RoundedCornerShape(22.dp)) {
                    Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        Text("2. " + strings.get("config"), fontWeight = FontWeight.ExtraBold)
                        ToolControlsV6(
                            tool, strings, format, { format = it }, quality, { quality = it },
                            width, { width = it }, height, { height = it },
                            cropRatio, { cropRatio = it }, angle, { angle = it },
                            flipH, { flipH = it }, flipV, { flipV = it },
                            imageFilter, { imageFilter = it }, watermark, { watermark = it },
                            opacity, { opacity = it }, position, { position = it },
                            amount, { amount = it }, pixelSize, { pixelSize = it },
                            borderSize, { borderSize = it }, cornerRadius, { cornerRadius = it },
                            duotonePreset, { duotonePreset = it },
                            socialPreset, { socialPreset = it },
                            modernFormat, { modernFormat = it },
                            smartWidth, { smartWidth = it }, smartHeight, { smartHeight = it },
                            gifDelay, { gifDelay = it }
                        )
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { processAction() },
                        modifier = Modifier.weight(1f).height(54.dp),
                        shape = RoundedCornerShape(17.dp),
                        enabled = !busy
                    ) {
                        Icon(Icons.Default.PlayArrow, null)
                        Spacer(Modifier.width(7.dp))
                        Text("3. " + strings.get("preview.button"))
                    }
                    if (premium) {
                        OutlinedButton(onClick = { saveRecipe() }, modifier = Modifier.height(54.dp)) {
                            Icon(Icons.Default.AutoAwesome, null)
                        }
                    }
                }
            }
        } else if (sourceUris.isNotEmpty()) {
            item {
                Button(
                    onClick = { processAction() },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(17.dp),
                    enabled = !busy
                ) {
                    Icon(Icons.Default.PlayArrow, null)
                    Spacer(Modifier.width(7.dp))
                    Text("2. " + strings.get("preview.button"))
                }
            }
        }

        if (previewBitmaps.isNotEmpty() || pendingItems.isNotEmpty()) {
            item {
                Card(shape = RoundedCornerShape(22.dp)) {
                    Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        Text("4. " + strings.get("preview"), fontWeight = FontWeight.ExtraBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            previewBitmaps.take(4).forEach { bmp ->
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier.size(78.dp).clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                        Text(
                            pendingItems.size.toString() + " " + strings.get("batch.results"),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        status?.let { Text(it, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp) }
                    }
                }
            }
        }

        if (pendingItems.isNotEmpty()) {
            item {
                Card(shape = RoundedCornerShape(22.dp)) {
                    Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("5. " + strings.get("export"), fontWeight = FontWeight.ExtraBold)
                        Button(
                            onClick = { exportResults() },
                            enabled = !busy,
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Default.Save, null)
                            Spacer(Modifier.width(7.dp))
                            Text(strings.get("save"))
                        }
                    }
                }
            }
        }

        exportedUri?.let { uri ->
            item {
                Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5))) {
                    Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW).apply {
                                    data = uri
                                    type = exportedMime
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                })
                            }
                        }) { Text(strings.get("open")) }
                        OutlinedButton(onClick = {
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = exportedMime
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(send, strings.get("share")))
                        }) { Text(strings.get("share")) }
                    }
                }
            }
        }
    }

    if (busy) {
        ProcessingGearDialog(
            title = strings.get("processing.title"),
            subtitle = if (batchMode) strings.get("processing.batch") else strings.get("processing.subtitle")
        )
    }
}

private data class AnyToolDef(
    val id: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val start: Color,
    val end: Color,
    val premium: Boolean
)

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
    gifDelay: Float, onGifDelay: (Float) -> Unit
) {
    when (tool.id) {
        "resize" -> {
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                OutlinedTextField(width, { onWidth(it.filter(Char::isDigit)) }, Modifier.weight(1f), label = { Text(strings.get("width")) }, singleLine = true)
                OutlinedTextField(height, { onHeight(it.filter(Char::isDigit)) }, Modifier.weight(1f), label = { Text(strings.get("height")) }, singleLine = true)
            }
        }
        "compress" -> {
            Text(strings.get("quality") + " " + quality.toInt() + "%", fontWeight = FontWeight.Bold)
            Slider(quality, onQuality, valueRange = 10f..100f)
            FormatChipsV5(format, onFormat, listOf(OutputFormat.JPEG, OutputFormat.WEBP))
        }
        "convert", "metadata", "smart_resize", "social_presets" -> {
            FormatChipsV5(format, onFormat, listOf(OutputFormat.JPEG, OutputFormat.PNG, OutputFormat.WEBP))
        }
        "crop" -> ChoicesV5(listOf("1:1", "4:5", "16:9", "9:16"), cropRatio, onCropRatio)
        "rotate" -> {
            ChoicesV5(listOf("90", "180", "270"), angle.toString()) { onAngle(it.toInt()) }
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                FilterChip(flipH, { onFlipH(!flipH) }, label = { Text(strings.get("mirror.h")) }, modifier = Modifier.weight(1f))
                FilterChip(flipV, { onFlipV(!flipV) }, label = { Text(strings.get("mirror.v")) }, modifier = Modifier.weight(1f))
            }
        }
        "filter" -> ChoicesV5(
            ImageFilter.entries.map { strings.filterTitle(it.name.lowercase()) },
            strings.filterTitle(imageFilter.name.lowercase())
        ) { selected -> onFilter(ImageFilter.entries.first { strings.filterTitle(it.name.lowercase()) == selected }) }
        "watermark" -> {
            OutlinedTextField(watermark, onWatermark, Modifier.fillMaxWidth(), label = { Text(strings.get("text")) }, singleLine = true)
            Slider(opacity, onOpacity, valueRange = 10f..100f)
        }
        "brightness", "contrast", "saturation", "warmth", "blur", "sharpen", "exposure", "tint", "vignette", "noise_reduction", "face_blur" -> {
            Text(strings.get("strength") + " " + amount.toInt())
            Slider(
                amount,
                onAmount,
                valueRange = if (tool.id in listOf("brightness", "contrast", "saturation", "warmth", "exposure", "tint")) -100f..100f else 5f..100f
            )
        }
        "pixelate" -> Slider(pixelSize, onPixelSize, valueRange = 4f..48f)
        "border" -> Slider(borderSize, onBorderSize, valueRange = 4f..120f)
        "round" -> Slider(cornerRadius, onCornerRadius, valueRange = 8f..160f)
        "duotone" -> ChoicesV5(listOf(strings.get("duotone.ocean"), strings.get("duotone.sunset"), strings.get("duotone.ink")),
            when (duotonePreset) {
                "sunset" -> strings.get("duotone.sunset")
                "ink" -> strings.get("duotone.ink")
                else -> strings.get("duotone.ocean")
            }
        ) { selected ->
            onDuotonePreset(when (selected) {
                strings.get("duotone.sunset") -> "sunset"
                strings.get("duotone.ink") -> "ink"
                else -> "ocean"
            })
        }
        "social_presets" -> ChoicesV6(
            SocialImageProcessor.presets.map { it.id to it.title },
            socialPreset,
            onSocialPreset
        )
        "smart_resize" -> {
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                OutlinedTextField(smartWidth, { onSmartWidth(it.filter(Char::isDigit)) }, Modifier.weight(1f), label = { Text(strings.get("width")) }, singleLine = true)
                OutlinedTextField(smartHeight, { onSmartHeight(it.filter(Char::isDigit)) }, Modifier.weight(1f), label = { Text(strings.get("height")) }, singleLine = true)
            }
        }
        "heic_avif" -> ChoicesV5(
            listOf(ModernFormat.HEIC.name, ModernFormat.AVIF.name),
            modernFormat.name,
            { onModernFormat(ModernFormat.valueOf(it)) }
        )
        "gif_creator" -> {
            Text(strings.get("quality") + " " + gifDelay.toInt() + " ms")
            Slider(gifDelay, onGifDelay, valueRange = 100f..1500f)
        }
        else -> Text(strings.toolSubtitle(tool.id), fontSize = 12.sp)
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
