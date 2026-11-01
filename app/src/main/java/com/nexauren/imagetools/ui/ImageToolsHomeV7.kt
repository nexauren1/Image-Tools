package com.nexauren.imagetools.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun ImageToolsHomeV7(
    strings: UiText,
    premium: Boolean,
    onOpenTool: (String) -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenPremium: () -> Unit
) {
    val context = LocalContext.current
    val language = AppLanguageStore.get(context)
    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("all") }

    val categoryLabels = listOf(
        "all" to homeCopyV7(language, "Todas", "All tools", "Todas", "Tous"),
        "essentials" to homeCopyV7(language, "Essenciais", "Essentials", "Esenciales", "Essentiels"),
        "creative" to homeCopyV7(language, "Criativas", "Creative", "Creativas", "Créatif"),
        "documents" to homeCopyV7(language, "Documentos", "Documents", "Documentos", "Documents"),
        "privacy" to homeCopyV7(language, "Privacidade", "Privacy", "Privacidad", "Confidentialité")
    )

    val visibleTools = TOOL_CATALOG.filter { tool ->
        val categoryMatches = category == "all" || homeToolCategoryV7(tool.id) == category
        val queryMatches = query.isBlank() ||
            (strings.toolTitle(tool.id) + " " + strings.toolSubtitle(tool.id)).contains(query.trim(), ignoreCase = true)
        categoryMatches && queryMatches
    }

    val quickTools = listOf("compress", "resize", "convert", "crop")
        .mapNotNull { id -> TOOL_CATALOG.firstOrNull { it.id == id } }

    val highlighted = if (query.isBlank()) visibleTools.take(12) else visibleTools
    val toolRows = highlighted.chunked(3)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 14.dp, top = 8.dp, end = 14.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        item {
            StudioHomeHeroV7(
                language = language,
                premium = premium,
                toolCount = TOOL_CATALOG.size,
                onCompress = { onOpenTool("compress") },
                onPremium = onOpenPremium
            )
        }

        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(homeCopyV7(language, "Pesquisar ferramentas", "Search tools", "Buscar herramientas", "Rechercher des outils")) },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (query.isNotBlank()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Default.Close, homeCopyV7(language, "Limpar pesquisa", "Clear search", "Borrar búsqueda", "Effacer la recherche"))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
                )
            )
        }

        if (query.isBlank()) {
            item {
                HomeSectionHeadingV7(
                    title = homeCopyV7(language, "Acesso rápido", "Quick actions", "Acceso rápido", "Accès rapide"),
                    subtitle = homeCopyV7(language, "As ferramentas que mais vai usar", "Your everyday image tools", "Tus herramientas de imagen", "Vos outils d’image")
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    quickTools.forEach { tool ->
                        HomeQuickActionV7(
                            tool = tool,
                            title = strings.toolTitle(tool.id),
                            modifier = Modifier.weight(1f),
                            onClick = { onOpenTool(tool.id) }
                        )
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                HomeSectionHeadingV7(
                    title = if (query.isBlank()) homeCopyV7(language, "Ferramentas em destaque", "Featured tools", "Herramientas destacadas", "Outils à la une") else homeCopyV7(language, "Resultados", "Results", "Resultados", "Résultats"),
                    subtitle = if (query.isBlank()) homeCopyV7(language, "Escolha uma ferramenta para começar", "Choose a tool to get started", "Elige una herramienta", "Choisissez un outil") else "${visibleTools.size} " + homeCopyV7(language, "encontradas", "found", "encontradas", "trouvés")
                )
                if (query.isBlank()) {
                    TextButton(onClick = onOpenLibrary, contentPadding = PaddingValues(horizontal = 6.dp)) {
                        Text(homeCopyV7(language, "Ver todas", "View all", "Ver todas", "Tout voir"), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        Icon(Icons.Default.ArrowForward, null, modifier = Modifier.size(15.dp))
                    }
                }
            }
        }

        if (visibleTools.isEmpty()) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.SearchOff, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp))
                        Text(homeCopyV7(language, "Nenhuma ferramenta encontrada", "No tools found", "No se encontraron herramientas", "Aucun outil trouvé"), fontWeight = FontWeight.Bold)
                        Text(homeCopyV7(language, "Experimente outro termo ou categoria.", "Try another search or category.", "Prueba otra búsqueda o categoría.", "Essayez une autre recherche ou catégorie."), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, textAlign = TextAlign.Center)
                    }
                }
            }
        } else {
            if (query.isBlank()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        categoryLabels.forEach { (id, label) ->
                            FilterChip(
                                selected = category == id,
                                onClick = { category = id },
                                label = { Text(label, fontSize = 10.sp, fontWeight = FontWeight.SemiBold) },
                                leadingIcon = if (category == id) {
                                    { Icon(Icons.Default.Check, null, modifier = Modifier.size(14.dp)) }
                                } else null
                            )
                        }
                    }
                }
            }

            items(toolRows) { rowTools ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    rowTools.forEach { tool ->
                        HomeToolTileV7(
                            tool = tool,
                            strings = strings,
                            premium = premium,
                            modifier = Modifier.weight(1f),
                            onClick = { onOpenTool(tool.id) }
                        )
                    }
                    repeat(3 - rowTools.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }

        item {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(15.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        Modifier.size(43.dp).clip(RoundedCornerShape(14.dp))
                            .background(Brush.linearGradient(listOf(Color(0xFF0EA5E9), Color(0xFF6548E8)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Security, null, tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(homeCopyV7(language, "Privacidade em primeiro lugar", "Privacy comes first", "Privacidad primero", "La confidentialité d’abord"), fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                        Text(homeCopyV7(language, "O processamento das ferramentas de imagem é feito no dispositivo.", "Image processing runs on your device.", "El procesamiento de imágenes se realiza en tu dispositivo.", "Le traitement des images s’effectue sur votre appareil."), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 15.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun StudioHomeHeroV7(
    language: AppLanguage,
    premium: Boolean,
    toolCount: Int,
    onCompress: () -> Unit,
    onPremium: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(29.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF080D1C))
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
                .background(Brush.linearGradient(listOf(Color(0xFF080D1C), Color(0xFF19265A), Color(0xFF5627A8), Color(0xFF075C91))))
                .padding(19.dp)
        ) {
            Box(
                Modifier.align(Alignment.TopEnd).offset(x = 16.dp, y = (-8).dp)
                    .size(124.dp).clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Color(0xFF18B9FF).copy(alpha = 0.28f), Color(0xFFB02BFF).copy(alpha = 0.13f)))),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Image, null, tint = Color.White.copy(alpha = 0.86f), modifier = Modifier.size(65.dp))
            }

            Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
                Surface(color = Color.White.copy(alpha = 0.12f), shape = RoundedCornerShape(9.dp)) {
                    Text(
                        "IMAGE STUDIO  •  ${toolCount} " + homeCopyV7(language, "ferramentas", "tools", "herramientas", "outils"),
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                        color = Color(0xFFB8E9FF),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.7.sp
                    )
                }
                Text(
                    "Image Tools",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.7).sp
                )
                Text(
                    homeCopyV7(language, "Edite, comprima e transforme as suas imagens.", "Edit, compress and transform your images.", "Edita, comprime y transforma tus imágenes.", "Modifiez, compressez et transformez vos images."),
                    color = Color.White.copy(alpha = 0.88f),
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    modifier = Modifier.fillMaxWidth(0.82f)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    HeroBadgeV7(Icons.Default.Bolt, homeCopyV7(language, "Rápido", "Fast", "Rápido", "Rapide"))
                    HeroBadgeV7(Icons.Default.Security, homeCopyV7(language, "Local", "On-device", "Local", "Local"))
                    HeroBadgeV7(Icons.Default.WorkspacePremium, if (premium) "PRO" else homeCopyV7(language, "Opcional", "Optional", "Opcional", "Optionnel"))
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    Button(
                        onClick = onCompress,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF168BFA), contentColor = Color.White),
                        contentPadding = PaddingValues(horizontal = 15.dp, vertical = 10.dp)
                    ) {
                        Icon(Icons.Default.Compress, null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(7.dp))
                        Text(homeCopyV7(language, "Começar", "Get started", "Comenzar", "Commencer"), fontWeight = FontWeight.ExtraBold)
                    }
                    if (!premium) {
                        TextButton(onClick = onPremium, contentPadding = PaddingValues(horizontal = 5.dp)) {
                            Text("PRO", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                            Icon(Icons.Default.ArrowForward, null, tint = Color.White, modifier = Modifier.size(15.dp))
                        }
                    } else {
                        Text(homeCopyV7(language, "PRO ativo", "PRO active", "PRO activo", "PRO actif"), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroBadgeV7(icon: ImageVector, label: String) {
    Surface(color = Color.White.copy(alpha = 0.12f), shape = RoundedCornerShape(9.dp)) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(12.dp))
            Text(label, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun HomeSectionHeadingV7(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
        Text(subtitle, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun HomeQuickActionV7(
    tool: ToolDef,
    title: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(84.dp),
        shape = RoundedCornerShape(17.dp),
        colors = CardDefaults.cardColors(containerColor = tool.start)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
                .background(Brush.linearGradient(listOf(tool.start, tool.end)))
                .padding(horizontal = 5.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(tool.icon, null, tint = Color.White, modifier = Modifier.size(24.dp))
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 9.sp, maxLines = 2, textAlign = TextAlign.Center, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun HomeToolTileV7(
    tool: ToolDef,
    strings: UiText,
    premium: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val locked = tool.premium && !premium
    Card(
        onClick = onClick,
        modifier = modifier.height(112.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = tool.start)
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
                .background(Brush.linearGradient(listOf(tool.start, tool.end)))
                .padding(11.dp)
        ) {
            Box(
                Modifier.align(Alignment.TopEnd).size(34.dp).clip(RoundedCornerShape(11.dp))
                    .background(Color.White.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(tool.icon, null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            if (locked) {
                Icon(Icons.Default.Lock, null, tint = Color.White.copy(alpha = 0.82f), modifier = Modifier.align(Alignment.TopStart).size(13.dp))
            }
            Column(
                modifier = Modifier.align(Alignment.BottomStart),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    strings.toolTitle(tool.id),
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 2,
                    lineHeight = 13.sp,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    if (locked) "PRO" else strings.toolSubtitle(tool.id),
                    color = Color.White.copy(alpha = 0.78f),
                    fontSize = 8.sp,
                    fontWeight = if (locked) FontWeight.ExtraBold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun homeToolCategoryV7(id: String): String = when (id) {
    "resize", "compress", "convert", "crop", "rotate", "mirror", "fit_canvas", "smart_resize", "target_size" -> "essentials"
    "logo_watermark", "color_replace" -> "creative"
    "pdf", "pdf_merge", "ocr", "scan_document", "base64" -> "documents"
    "details", "metadata", "exif", "face_blur" -> "privacy"
    else -> "creative"
}

private fun homeCopyV7(
    language: AppLanguage,
    pt: String,
    en: String,
    es: String,
    fr: String
): String = when (language) {
    AppLanguage.PT -> pt
    AppLanguage.EN -> en
    AppLanguage.ES -> es
    AppLanguage.FR -> fr
}
