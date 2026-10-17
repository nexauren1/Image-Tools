package com.nexauren.imagetools.ui

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.nexauren.imagetools.data.OutputFolderStore
import com.nexauren.imagetools.data.ProcessingStatsStore
import com.nexauren.imagetools.media.ImageFilter
import com.nexauren.imagetools.media.ImageProcessor
import com.nexauren.imagetools.media.OutputExporter
import com.nexauren.imagetools.media.OutputFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class BatchResultItem(
    val preview: Bitmap,
    val bytes: ByteArray,
    val width: Int,
    val height: Int,
    val format: OutputFormat
)

@Composable
fun BatchScreen(strings: UiText = LocalUiText.current) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var uris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var results by remember { mutableStateOf<List<BatchResultItem>>(emptyList()) }
    var operation by remember { mutableStateOf("resize") }
    var width by remember { mutableStateOf("1280") }
    var quality by remember { mutableFloatStateOf(80f) }
    var format by remember { mutableStateOf(OutputFormat.JPEG) }
    var filter by remember { mutableStateOf(ImageFilter.ORIGINAL) }
    var watermark by remember { mutableStateOf("IMAGE TOOLS") }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(20)
    ) { picked ->
        uris = picked
        results = emptyList()
        status = null
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(shape = RoundedCornerShape(28.dp)) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    Text(
                        strings.get("batch"),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(strings.get("batch.select"), fontSize = 12.sp)
                    Button(
                        onClick = {
                            picker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Collections, null)
                        Spacer(Modifier.width(7.dp))
                        Text(strings.get("choose.images"))
                    }
                    if (uris.isNotEmpty()) {
                        Text(
                            uris.size.toString() + " " + strings.get("batch.selected"),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        if (uris.isNotEmpty()) {
            item {
                Card(shape = RoundedCornerShape(22.dp)) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(strings.get("batch.operation"), fontWeight = FontWeight.ExtraBold)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf(
                                "resize" to strings.get("batch.resize"),
                                "compress" to strings.get("batch.compress"),
                                "convert" to strings.get("batch.convert"),
                                "filter" to strings.get("batch.filter"),
                                "watermark" to strings.get("batch.watermark")
                            ).forEach { (id, label) ->
                                FilterChip(
                                    selected = operation == id,
                                    onClick = { operation = id },
                                    label = { Text(label, fontSize = 9.sp) }
                                )
                            }
                        }

                        when (operation) {
                            "resize" -> {
                                OutlinedTextField(
                                    value = width,
                                    onValueChange = { width = it.filter(Char::isDigit) },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text(strings.get("width")) },
                                    singleLine = true
                                )
                            }
                            "compress" -> {
                                Text(strings.get("quality") + " " + quality.toInt() + "%")
                                Slider(
                                    value = quality,
                                    onValueChange = { quality = it },
                                    valueRange = 10f..100f
                                )
                            }
                            "convert" -> {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    OutputFormat.entries.forEach { option ->
                                        FilterChip(
                                            selected = format == option,
                                            onClick = { format = option },
                                            label = { Text(option.extension.uppercase()) }
                                        )
                                    }
                                }
                            }
                            "filter" -> {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    ImageFilter.entries.forEach { option ->
                                        FilterChip(
                                            selected = filter == option,
                                            onClick = { filter = option },
                                            label = { Text(strings.filterTitle(option.name.lowercase()), fontSize = 9.sp) }
                                        )
                                    }
                                }
                            }
                            "watermark" -> {
                                OutlinedTextField(
                                    value = watermark,
                                    onValueChange = { watermark = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text(strings.get("text")) },
                                    singleLine = true
                                )
                            }
                        }

                        Button(
                            onClick = {
                                val selectedUris = uris.take(20).toList()
                                val selectedOperation = operation
                                val selectedWidth = width
                                val selectedQuality = quality.toInt()
                                val selectedFormat = format
                                val selectedFilter = filter
                                val selectedWatermark = watermark
                                busy = true
                                status = strings.get("processing.batch")
                                results = emptyList()
                                scope.launch {
                                    val batchResults = mutableListOf<BatchResultItem>()
                                    try {
                                        for (uri in selectedUris) {
                                            val bitmap = withContext(Dispatchers.IO) {
                                                ImageProcessor.decode(context, uri)
                                            } ?: continue
                                            try {
                                                val output = withContext(Dispatchers.Default) {
                                                    when (selectedOperation) {
                                                        "resize" -> {
                                                            val targetWidth = selectedWidth.toIntOrNull()
                                                                ?: error("Enter a valid image width.")
                                                            require(targetWidth in 1..12_000) {
                                                                "Width must be between 1 and 12,000 pixels."
                                                            }
                                                            val targetHeight = (bitmap.height *
                                                                (targetWidth.toFloat() / bitmap.width))
                                                                .toInt()
                                                                .coerceAtLeast(1)
                                                            ImageProcessor.resize(bitmap, targetWidth, targetHeight)
                                                        }
                                                        "filter" -> ImageProcessor.filter(bitmap, selectedFilter)
                                                        "watermark" -> ImageProcessor.watermark(
                                                            bitmap,
                                                            selectedWatermark,
                                                            70,
                                                            "Bottom right"
                                                        )
                                                        else -> bitmap
                                                    }
                                                }

                                                try {
                                                    val bytes = withContext(Dispatchers.Default) {
                                                        ImageProcessor.encode(
                                                            output,
                                                            selectedFormat,
                                                            if (selectedOperation == "compress") selectedQuality else 95
                                                        )
                                                    }
                                                    val outputBytes = batchResults.sumOf { it.bytes.size.toLong() } +
                                                        bytes.size.toLong()
                                                    require(outputBytes <= 96L * 1024L * 1024L) {
                                                        "Batch output exceeds 96 MB. Process fewer images at a time."
                                                    }

                                                    val preview = withContext(Dispatchers.Default) {
                                                        val scale = minOf(
                                                            1f,
                                                            420f / maxOf(output.width, output.height)
                                                        )
                                                        val thumbWidth = (output.width * scale).toInt().coerceAtLeast(1)
                                                        val thumbHeight = (output.height * scale).toInt().coerceAtLeast(1)
                                                        val scaled = Bitmap.createScaledBitmap(
                                                            output,
                                                            thumbWidth,
                                                            thumbHeight,
                                                            true
                                                        )
                                                        if (scaled === output) output.copy(Bitmap.Config.ARGB_8888, false)
                                                        ?: error("Could not create preview image.")
                                                        else scaled
                                                    }
                                                    batchResults += BatchResultItem(
                                                        preview = preview,
                                                        bytes = bytes,
                                                        width = output.width,
                                                        height = output.height,
                                                        format = selectedFormat
                                                    )
                                                } finally {
                                                    if (output !== bitmap && !output.isRecycled) output.recycle()
                                                }
                                            } finally {
                                                if (!bitmap.isRecycled) bitmap.recycle()
                                            }
                                        }
                                        require(batchResults.isNotEmpty()) {
                                            strings.get("process.error")
                                        }
                                        ProcessingStatsStore.recordProcessed(context, batchResults.size)
                                        results = batchResults.toList()
                                        status = strings.get("batch.ready")
                                    } catch (error: Exception) {
                                        batchResults.forEach { item ->
                                            if (!item.preview.isRecycled) item.preview.recycle()
                                        }
                                        results = emptyList()
                                        status = error.message ?: strings.get("process.error")
                                    } finally {
                                        busy = false
                                    }
                                }
                            },
                            enabled = !busy && uris.isNotEmpty(),
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, null)
                            Spacer(Modifier.width(6.dp))
                            Text(if (busy) strings.get("processing") else strings.get("batch.start"))
                        }
                    }
                }
            }
        }

        if (results.isNotEmpty()) {
            item {
                Card(shape = RoundedCornerShape(22.dp)) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            results.size.toString() + " " + strings.get("batch.results"),
                            fontWeight = FontWeight.ExtraBold
                        )
                        results.take(8).forEachIndexed { index, result ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Image(
                                    bitmap = result.preview.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(RoundedCornerShape(10.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(strings.get("batch.image") + " " + (index + 1), Modifier.weight(1f))
                                Text(
                                    ImageProcessor.humanBytes(result.bytes.size.toLong()),
                                    fontSize = 10.sp
                                )
                            }
                        }
                        Button(
                            onClick = {
                                val exportResults = results.toList()
                                busy = true
                                scope.launch {
                                    try {
                                        val tree = OutputFolderStore.getTreeUri(context)
                                        exportResults.forEachIndexed { index, result ->
                                            withContext(Dispatchers.IO) {
                                                OutputExporter.saveImage(
                                                    context,
                                                    result.bytes,
                                                    result.format,
                                                    "batch_" + (index + 1),
                                                    result.width,
                                                    result.height,
                                                    tree
                                                )
                                            }
                                        }
                                        ProcessingStatsStore.recordExported(context, exportResults.size)
                                        status = strings.get("saved")
                                    } catch (error: Exception) {
                                        status = error.message ?: strings.get("save.error")
                                    } finally {
                                        busy = false
                                    }
                                }
                            },
                            enabled = !busy,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Default.Save, null)
                            Spacer(Modifier.width(6.dp))
                            Text(if (busy) strings.get("working") else strings.get("save.all"))
                        }
                    }
                }
            }
        }

        status?.let { message ->
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(message, modifier = Modifier.padding(12.dp), fontSize = 12.sp)
                }
            }
        }
    }
}
