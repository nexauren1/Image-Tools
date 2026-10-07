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
import com.nexauren.imagetools.media.ImageFilter
import com.nexauren.imagetools.media.ImageProcessor
import com.nexauren.imagetools.media.OutputExporter
import com.nexauren.imagetools.media.OutputFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun BatchScreen(strings: UiText = LocalUiText.current) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var uris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var results by remember { mutableStateOf<List<Pair<Bitmap, ByteArray>>>(emptyList()) }
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
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Collections, null)
                        Spacer(Modifier.width(7.dp))
                        Text(strings.get("choose.images"))
                    }
                    if (uris.isNotEmpty()) {
                        Text(
                            uris.size.toString() + " selected",
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
                                            label = { Text(option.label, fontSize = 9.sp) }
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
                                busy = true
                                scope.launch {
                                    results = withContext(Dispatchers.Default) {
                                        uris.mapNotNull { ImageProcessor.decode(context, it) }.map { bitmap ->
                                            val preview = when (operation) {
                                                "resize" -> {
                                                    val w = width.toIntOrNull()?.coerceAtLeast(1) ?: bitmap.width
                                                    val h = (bitmap.height * (w.toFloat() / bitmap.width))
                                                        .toInt()
                                                        .coerceAtLeast(1)
                                                    ImageProcessor.resize(bitmap, w, h)
                                                }
                                                "filter" -> ImageProcessor.filter(bitmap, filter)
                                                "watermark" -> ImageProcessor.watermark(
                                                    bitmap,
                                                    watermark,
                                                    70,
                                                    "Bottom right"
                                                )
                                                else -> bitmap
                                            }
                                            val bytes = ImageProcessor.encode(
                                                preview,
                                                format,
                                                if (operation == "compress") quality.toInt() else 95
                                            )
                                            preview to bytes
                                        }
                                    }
                                    status = strings.get("batch.ready")
                                    busy = false
                                }
                            },
                            enabled = !busy,
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
                            results.size.toString() + " results",
                            fontWeight = FontWeight.ExtraBold
                        )
                        results.take(8).forEachIndexed { index, result ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Image(
                                    bitmap = result.first.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(RoundedCornerShape(10.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(Modifier.width(10.dp))
                                Text("Image " + (index + 1), Modifier.weight(1f))
                                Text(
                                    ImageProcessor.humanBytes(result.second.size.toLong()),
                                    fontSize = 10.sp
                                )
                            }
                        }
                        Button(
                            onClick = {
                                scope.launch {
                                    val tree = OutputFolderStore.getTreeUri(context)
                                    results.forEachIndexed { index, result ->
                                        withContext(Dispatchers.IO) {
                                            OutputExporter.saveImage(
                                                context,
                                                result.second,
                                                format,
                                                "batch_" + (index + 1),
                                                result.first.width,
                                                result.first.height,
                                                tree
                                            )
                                        }
                                    }
                                    status = strings.get("saved")
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Default.Save, null)
                            Spacer(Modifier.width(6.dp))
                            Text(strings.get("save.all"))
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
