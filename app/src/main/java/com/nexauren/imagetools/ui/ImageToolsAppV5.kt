package com.nexauren.imagetools.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexauren.imagetools.BuildConfig
import com.nexauren.imagetools.auth.AuthRepository
import com.nexauren.imagetools.data.AppNotificationHelper
import com.nexauren.imagetools.data.AppNotificationSettings
import com.nexauren.imagetools.data.AppCenterStore
import com.nexauren.imagetools.data.FavoritesStore
import com.nexauren.imagetools.data.OutputFolderStore
import com.nexauren.imagetools.data.PaymentException
import com.nexauren.imagetools.data.ProcessingStatsStore
import com.nexauren.imagetools.data.PaymentRepository
import com.nexauren.imagetools.data.HistoryStore
import com.nexauren.imagetools.data.Recipe
import com.nexauren.imagetools.data.RecipeStore
import com.nexauren.imagetools.media.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val SITE = "https://steep-pine-34fe.nexaurenstore.workers.dev"
private const val PRIVACY = SITE + "/privacy"
private const val TERMS = SITE + "/terms"
private const val SUPPORT = SITE + "/support"

internal data class ToolDef(
    val id: String,
    val icon: ImageVector,
    val start: Color,
    val end: Color,
    val premium: Boolean = false
)

internal val TOOL_CATALOG = listOf(
    ToolDef("resize", Icons.Default.PhotoSizeSelectLarge, Color(0xFF2563EB), Color(0xFF06B6D4)),
    ToolDef("compress", Icons.Default.Compress, Color(0xFF059669), Color(0xFF22C55E)),
    ToolDef("convert", Icons.Default.SwapHoriz, Color(0xFF7C3AED), Color(0xFFEC4899)),
    ToolDef("crop", Icons.Default.Crop, Color(0xFFF59E0B), Color(0xFFF97316)),
    ToolDef("rotate", Icons.Default.Rotate90DegreesCw, Color(0xFF4F46E5), Color(0xFFEC4899)),
    ToolDef("filter", Icons.Default.FilterVintage, Color(0xFF0F766E), Color(0xFF14B8A6)),
    ToolDef("watermark", Icons.Default.TextFields, Color(0xFF7C3AED), Color(0xFFDB2777), true),
    ToolDef("details", Icons.Default.Info, Color(0xFF334155), Color(0xFF06B6D4)),
    ToolDef("brightness", Icons.Default.LightMode, Color(0xFFF59E0B), Color(0xFFFBBF24)),
    ToolDef("contrast", Icons.Default.Contrast, Color(0xFF0F172A), Color(0xFF475569)),
    ToolDef("saturation", Icons.Default.Palette, Color(0xFFDB2777), Color(0xFF8B5CF6)),
    ToolDef("warmth", Icons.Default.WbSunny, Color(0xFFEA580C), Color(0xFFF59E0B)),
    ToolDef("negative", Icons.Default.InvertColors, Color(0xFF111827), Color(0xFF6B7280)),
    ToolDef("blur", Icons.Default.BlurOn, Color(0xFF0891B2), Color(0xFF6366F1), true),
    ToolDef("sharpen", Icons.Default.AutoFixHigh, Color(0xFF16A34A), Color(0xFF0EA5E9), true),
    ToolDef("pixelate", Icons.Default.GridOn, Color(0xFF7C3AED), Color(0xFF4F46E5)),
    ToolDef("border", Icons.Default.BorderStyle, Color(0xFF64748B), Color(0xFF0F172A)),
    ToolDef("round", Icons.Default.RoundedCorner, Color(0xFF2563EB), Color(0xFF7C3AED), true),
    ToolDef("metadata", Icons.Default.Security, Color(0xFF059669), Color(0xFF0F766E)),
    ToolDef("pdf", Icons.Default.PictureAsPdf, Color(0xFFDC2626), Color(0xFFEA580C), true),
    ToolDef("palette", Icons.Default.ColorLens, Color(0xFF0EA5E9), Color(0xFF8B5CF6)),
    ToolDef("collage", Icons.Default.Collections, Color(0xFFDB2777), Color(0xFFF97316), true),
    ToolDef("ocr", Icons.Default.TextSnippet, Color(0xFF2563EB), Color(0xFF8B5CF6)),
    ToolDef("background", Icons.Default.AutoFixNormal, Color(0xFF0EA5E9), Color(0xFF14B8A6), true),
    ToolDef("exif", Icons.Default.DataObject, Color(0xFF475569), Color(0xFF06B6D4)),
    ToolDef("auto_enhance", Icons.Default.AutoAwesome, Color(0xFF7C3AED), Color(0xFF06B6D4), true),
    ToolDef("exposure", Icons.Default.Exposure, Color(0xFFF59E0B), Color(0xFFEF4444)),
    ToolDef("tint", Icons.Default.Tune, Color(0xFF14B8A6), Color(0xFF3B82F6)),
    ToolDef("vignette", Icons.Default.BlurOn, Color(0xFF111827), Color(0xFF7C3AED), true),
    ToolDef("posterize", Icons.Default.Palette, Color(0xFFEC4899), Color(0xFF8B5CF6)),
    ToolDef("duotone", Icons.Default.ColorLens, Color(0xFF0F766E), Color(0xFFEA580C), true),
    ToolDef("mirror", Icons.Default.Flip, Color(0xFF2563EB), Color(0xFF14B8A6)),
    ToolDef("noise_reduction", Icons.Default.AutoFixHigh, Color(0xFF6366F1), Color(0xFF0EA5E9), true),
    ToolDef("social_presets", Icons.Default.PhoneAndroid, Color(0xFFEC4899), Color(0xFF8B5CF6), false),
    ToolDef("smart_resize", Icons.Default.PhotoSizeSelectLarge, Color(0xFF0EA5E9), Color(0xFF2563EB), true),
    ToolDef("face_blur", Icons.Default.Face, Color(0xFF0F766E), Color(0xFF06B6D4), true),
    ToolDef("pdf_merge", Icons.Default.MergeType, Color(0xFFB91C1C), Color(0xFFF97316), true),
    ToolDef("gif_creator", Icons.Default.Gif, Color(0xFF7C3AED), Color(0xFFEC4899), true),
    ToolDef("heic_avif", Icons.Default.Image, Color(0xFF475569), Color(0xFF06B6D4), true),
    ToolDef("fit_canvas", Icons.Default.AspectRatio, Color(0xFF0EA5E9), Color(0xFF06B6D4)),
    ToolDef("target_size", Icons.Default.DataUsage, Color(0xFF059669), Color(0xFF0F766E), true),
    ToolDef("split_grid", Icons.Default.GridOn, Color(0xFFF59E0B), Color(0xFFEA580C), true),
    ToolDef("film_grain", Icons.Default.AutoFixHigh, Color(0xFF7C2D12), Color(0xFFF59E0B)),
    ToolDef("color_pop", Icons.Default.ColorLens, Color(0xFFDB2777), Color(0xFFF59E0B)),
    ToolDef("outline", Icons.Default.BorderStyle, Color(0xFF0F172A), Color(0xFF64748B)),
    ToolDef("glitch", Icons.Default.Bolt, Color(0xFF0891B2), Color(0xFFEC4899)),
    ToolDef("scan_document", Icons.Default.TextSnippet, Color(0xFF2563EB), Color(0xFF0F766E))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageToolsAppV5(
    auth: AuthRepository,
    premium: Boolean,
    darkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    onStartPayment: (String) -> Unit
) {
    val context = LocalContext.current
    var language by remember { mutableStateOf(AppLanguageStore.get(context)) }
    val strings = remember(language) { UiText(language) }

    CompositionLocalProvider(LocalUiText provides strings) {
        if (auth.currentUser == null) {
            AuthScreenV5(auth)
            return@CompositionLocalProvider
        }

        var page by remember { mutableStateOf("home") }
        var selectedTool by remember { mutableStateOf<String?>(null) }
        var centerUnread by remember { mutableIntStateOf(AppCenterStore.unreadCount(context)) }
        val scope = rememberCoroutineScope()
        val drawerState = rememberDrawerState(DrawerValue.Closed)

        fun navigate(target: String) {
            page = target
            selectedTool = null
            if (target == "center") {
                AppCenterStore.markAllSeen(context)
                centerUnread = 0
            }
            scope.launch { drawerState.close() }
        }

        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet {
                    Spacer(Modifier.height(18.dp))
                    Text(
                        "IMAGE TOOLS",
                        Modifier.padding(horizontal = 22.dp),
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 2.sp
                    )
                    Text(
                        if (premium) strings.get("pro") else strings.get("local"),
                        Modifier.padding(start = 22.dp, top = 4.dp, bottom = 16.dp),
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp
                    )
                    DrawerEntryV5(Icons.Default.Home, strings.get("home")) { navigate("home") }
                    DrawerEntryV5(Icons.Default.Build, strings.get("tools")) { navigate("tools") }
                    DrawerEntryV5(Icons.Default.DynamicFeed, strings.get("batch")) { navigate("batch") }
                    DrawerEntryV5(Icons.Default.WorkspacePremium, strings.get("premium")) { navigate("premium") }
                    DrawerEntryV5(Icons.Default.Person, strings.get("account")) { navigate("account") }
                    DrawerEntryV5(Icons.Default.Settings, strings.get("settings")) { navigate("settings") }
                    DrawerEntryV5(Icons.Default.History, strings.get("history")) { navigate("history") }
                    DrawerEntryV5(Icons.Default.AutoAwesome, strings.get("recipes")) { navigate("recipes") }
                    DrawerEntryV5(Icons.Default.Notifications, centerLabelV5(context)) { navigate("center") }
                    DrawerEntryV5(Icons.Default.Favorite, favoritesLabelV5(context)) { navigate("favorites") }
                    DrawerEntryV5(Icons.Default.Info, strings.get("about")) { navigate("about") }
                }
            }
        ) {
            Scaffold(
                topBar = {
                    CenterAlignedTopAppBar(
                        title = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Image Tools", fontWeight = FontWeight.ExtraBold)
                                Text(
                                    if (premium) strings.get("pro") else strings.get("home.subtitle"),
                                    fontSize = 8.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, strings.get("tools"))
                            }
                        },
                        actions = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                BadgedBox(
                                    badge = {
                                        if (centerUnread > 0) {
                                            Badge { Text(centerUnread.coerceAtMost(9).toString()) }
                                        }
                                    }
                                ) {
                                    IconButton(onClick = { navigate("center") }) {
                                        Icon(Icons.Default.Notifications, centerLabelV5(context))
                                    }
                                }
                                IconButton(onClick = { navigate("premium") }) {
                                    Icon(
                                        if (premium) Icons.Default.WorkspacePremium else Icons.Default.AutoAwesome,
                                        strings.get("premium")
                                    )
                                }
                            }
                        }
                    )
                },
                bottomBar = {
                    NavigationBar {
                        Row(Modifier.fillMaxWidth()) {
                            BottomTabV5(
                                page == "home",
                                { navigate("home") },
                                Icons.Default.Home,
                                strings.get("home")
                            )
                            BottomTabV5(
                                page == "tools",
                                { navigate("tools") },
                                Icons.Default.Build,
                                strings.get("tools")
                            )
                            BottomTabV5(
                                page == "batch",
                                { navigate("batch") },
                                Icons.Default.DynamicFeed,
                                strings.get("batch")
                            )
                        }
                    }
                }
            ) { padding ->
                Box(Modifier.fillMaxSize().padding(padding)) {
                    when {
                        selectedTool != null -> {
                            val tool = TOOL_CATALOG.first { it.id == selectedTool }
                            ToolWorkspaceV6(
                                tool = AnyToolDef(tool.id, tool.icon, tool.start, tool.end, tool.premium),
                                premium = premium,
                                onBack = { selectedTool = null },
                                onNeedPremium = { navigate("premium") }
                            )
                        }
                        page == "home" -> HomeScreenV5(strings, premium) { id ->
                            val tool = TOOL_CATALOG.first { it.id == id }
                            if (tool.premium && !premium) navigate("premium") else selectedTool = id
                        }
                        page == "tools" -> ToolsScreenV5(strings, premium) { id ->
                            val tool = TOOL_CATALOG.first { it.id == id }
                            if (tool.premium && !premium) navigate("premium") else selectedTool = id
                        }
                        page == "batch" -> BatchScreen(strings)
                        page == "premium" -> PremiumScreenV5(auth, strings, premium, onStartPayment)
                        page == "account" -> AccountScreenV5(auth, strings, premium) { navigate("premium") }
                        page == "history" -> HistoryScreenV5(strings)
                        page == "center" -> NotificationsCenterScreen(strings)
                        page == "favorites" -> FavoritesScreen(strings, premium) { id ->
                            val tool = TOOL_CATALOG.firstOrNull { it.id == id }
                            if (tool != null) {
                                if (tool.premium && !premium) navigate("premium") else selectedTool = id
                            }
                        }
                        page == "recipes" -> RecipesScreenV5(
                            auth = auth,
                            premium = premium,
                            strings = strings,
                            onApplyRecipe = { recipe ->
                                val tool = TOOL_CATALOG.firstOrNull { it.id == recipe.toolId }
                                if (tool != null) {
                                    if (tool.premium && !premium) {
                                        navigate("premium")
                                    } else {
                                        RecipeStore.setPending(context, recipe)
                                        page = "home"
                                        selectedTool = recipe.toolId
                                    }
                                }
                            }
                        )
                        page == "settings" -> SettingsScreenV5(
                            strings = strings,
                            darkMode = darkMode,
                            language = language,
                            onDarkModeChange = onDarkModeChange,
                            onLanguageChange = {
                                language = it
                                AppLanguageStore.save(context, it)
                            }
                        )
                        else -> AboutScreenV5(strings)
                    }
                }
            }
        }
    }
}

private fun centerLabelV5(context: Context): String = when (AppLanguageStore.get(context)) {
    AppLanguage.PT -> "Novidades"
    AppLanguage.ES -> "Novedades"
    AppLanguage.FR -> "Nouveautés"
    else -> "What's new"
}

private fun favoritesLabelV5(context: Context): String = when (AppLanguageStore.get(context)) {
    AppLanguage.PT -> "Favoritos"
    AppLanguage.ES -> "Favoritos"
    AppLanguage.FR -> "Favoris"
    else -> "Favorites"
}

private fun toolCategoryV5(id: String): String = when {
    id in setOf("details", "metadata", "exif", "face_blur", "background") -> "privacy"
    id in setOf("pdf", "pdf_merge", "gif_creator", "ocr", "scan_document") -> "docs"
    id in setOf(
        "filter", "watermark", "brightness", "contrast", "saturation", "warmth",
        "negative", "blur", "sharpen", "pixelate", "border", "round", "auto_enhance",
        "exposure", "tint", "vignette", "noise_reduction", "resize", "compress",
        "convert", "crop", "rotate", "mirror", "smart_resize", "fit_canvas", "target_size"
    ) -> "edit"
    else -> "creative"
}

@Composable
private fun DrawerEntryV5(icon: ImageVector, label: String, onClick: () -> Unit) {
    NavigationDrawerItem(
        label = { Text(label) },
        selected = false,
        onClick = onClick,
        icon = { Icon(icon, null) },
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
    )
}

@Composable
private fun RowScope.BottomTabV5(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .clickable(onClick = onClick)
            .padding(vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            null,
            tint = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            label,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AuthScreenV5(auth: AuthRepository) {
    val context = LocalContext.current
    val strings = remember { UiText(AppLanguageStore.get(context)) }
    var create by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Box(
        Modifier.fillMaxSize().background(
            Brush.linearGradient(listOf(Color(0xFF0B1022), Color(0xFF4C1D95), Color(0xFF0891B2)))
        )
    ) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(22.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Box(
                    Modifier.size(86.dp).clip(RoundedCornerShape(26.dp))
                        .background(Brush.linearGradient(listOf(Color(0xFF7C3AED), Color(0xFF06B6D4)))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.AutoFixHigh, null, tint = Color.White, modifier = Modifier.size(42.dp))
                }
                Spacer(Modifier.height(16.dp))
                Text("Image Tools", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                Text(strings.get("local"), color = Color.White.copy(alpha = .78f))
                Spacer(Modifier.height(20.dp))
                Card(shape = RoundedCornerShape(28.dp)) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                busy = true
                                scope.launch {
                                    auth.signInGoogle(BuildConfig.GOOGLE_WEB_CLIENT_ID)
                                        .onFailure { error = authMessageV5(it, strings) }
                                    busy = false
                                }
                            },
                            enabled = !busy,
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Default.AccountCircle, null)
                            Spacer(Modifier.width(8.dp))
                            Text(strings.get("auth.google"))
                        }
                        HorizontalDivider()
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(strings.get("auth.email")) },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(strings.get("auth.password")) },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation()
                        )
                        Button(
                            onClick = {
                                busy = true
                                error = null
                                scope.launch {
                                    val result = if (create) auth.registerEmail(email, password) else auth.signInEmail(email, password)
                                    result.onFailure { error = authMessageV5(it, strings) }
                                    busy = false
                                }
                            },
                            enabled = !busy && email.contains("@") && password.length >= 6,
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(if (create) strings.get("auth.create") else strings.get("auth.signin"))
                        }
                        TextButton(
                            onClick = { create = !create },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (create) strings.get("auth.already") else strings.get("auth.new"))
                        }
                        TextButton(
                            onClick = {
                                scope.launch {
                                    auth.sendPasswordReset(email)
                                        .onFailure { error = "Enter a valid email first." }
                                }
                            },
                            enabled = !create,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(strings.get("auth.forgot"))
                        }
                        error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
                    }
                }
            }
        }
    }
}

private fun authMessageV5(error: Throwable, strings: UiText): String {
    val text = error.message?.lowercase().orEmpty()
    return when {
        "already in use" in text -> strings.get("auth.exists")
        "invalid email" in text || "badly formatted" in text -> strings.get("auth.invalid")
        "wrong-password" in text || "invalid-credential" in text -> strings.get("auth.generic")
        "network" in text -> strings.get("auth.network")
        else -> strings.get("auth.generic")
    }
}

@Composable
private fun HomeScreenV5(strings: UiText, premium: Boolean, openTool: (String) -> Unit) {
    var query by remember { mutableStateOf("") }
    val filtered = TOOL_CATALOG.filter {
        (strings.toolTitle(it.id) + " " + strings.toolSubtitle(it.id)).contains(query, true)
    }
    val popularIds = listOf("resize", "compress", "auto_enhance", "background", "ocr", "collage")
    val popular = popularIds.mapNotNull { id -> filtered.firstOrNull { it.id == id } }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(shape = RoundedCornerShape(30.dp)) {
                Box(
                    Modifier.fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFF0B1022),
                                    Color(0xFF4C1D95),
                                    Color(0xFF0891B2)
                                )
                            )
                        )
                        .padding(22.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    if (premium) strings.get("pro") else "IMAGE TOOLBOX",
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 10.sp
                                )
                                Text(
                                    strings.get("home"),
                                    color = Color.White,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                            Box(
                                Modifier.size(54.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(Color.White.copy(alpha = .12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    if (premium) Icons.Default.WorkspacePremium else Icons.Default.AutoAwesome,
                                    null,
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        Text(
                            strings.get("home.subtitle"),
                            color = Color.White.copy(alpha = .82f),
                            fontSize = 12.sp
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            MiniPillV5(
                                TOOL_CATALOG.size.toString() + "+",
                                strings.get("tools")
                            )
                            MiniPillV5(strings.get("local"), strings.get("processing"))
                            MiniPillV5("PRO", "optional")
                        }
                    }
                }
            }
        }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(strings.get("search")) },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true,
                shape = RoundedCornerShape(18.dp)
            )
        }
        item {
            SectionTitleV5(strings.get("popular"), strings.get("home.subtitle"))
        }
        items(popular) { tool -> ToolRowV5(tool, strings, premium, openTool) }
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionTitleV5(strings.get("all.tools"), filtered.size.toString())
                if (premium) {
                    AssistChip(
                        onClick = {},
                        label = { Text(strings.get("pro")) },
                        leadingIcon = { Icon(Icons.Default.WorkspacePremium, null) }
                    )
                }
            }
        }
        items(filtered.take(10)) { tool -> ToolRowV5(tool, strings, premium, openTool) }
        item {
            FeatureCardV5(
                if (premium) Icons.Default.Verified else Icons.Default.Lock,
                if (premium) strings.get("premium.active") else strings.get("local"),
                if (premium) strings.get("premium.active") + " • " + strings.get("tools") else strings.get("private")
            )
        }
    }
}

@Composable
private fun ToolsScreenV5(strings: UiText, premium: Boolean, openTool: (String) -> Unit) {
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("all") }
    val filtered = TOOL_CATALOG.filter {
        (category == "all" || toolCategoryV5(it.id) == category) &&
            (strings.toolTitle(it.id) + " " + strings.toolSubtitle(it.id)).contains(query, true)
    }
    val categories = listOf(
        "all" to "Todos",
        "edit" to "Editar",
        "creative" to "Criativo",
        "privacy" to "Privacidade",
        "docs" to "Documentos"
    )

    Column(Modifier.fillMaxSize()) {
        Surface(
            tonalElevation = 2.dp,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(
                Modifier.fillMaxWidth().padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Build, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(strings.get("tools"), fontWeight = FontWeight.ExtraBold)
                    Text(
                        filtered.size.toString() + " " + strings.get("tools"),
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (filtered.any { it.premium }) {
                    Text(
                        strings.get("pro"),
                        color = Color(0xFF7C3AED),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 10.sp
                    )
                }
            }
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.padding(horizontal = 14.dp).fillMaxWidth(),
            placeholder = { Text(strings.get("search")) },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            singleLine = true,
            shape = RoundedCornerShape(18.dp)
        )

        Row(
            Modifier.padding(horizontal = 14.dp).fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            categories.forEach { (id, label) ->
                FilterChip(
                    selected = category == id,
                    onClick = { category = id },
                    label = { Text(label, fontSize = 10.sp) }
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            gridItems(filtered) { tool ->
                ToolGridCardV5(tool, strings, premium, openTool)
            }
        }
    }
}

@Composable
internal fun ToolGridCardV5(tool: ToolDef, strings: UiText, premium: Boolean, openTool: (String) -> Unit) {
    Card(
        onClick = { openTool(tool.id) },
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            Modifier.padding(13.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            val context = LocalContext.current
            var favorite by remember(tool.id) { mutableStateOf(FavoritesStore.isFavorite(context, tool.id)) }

            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    Modifier.size(44.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(Brush.linearGradient(listOf(tool.start, tool.end))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(tool.icon, null, tint = Color.White)
                }
                Spacer(Modifier.weight(1f))
                if (tool.premium && !premium) {
                    Surface(
                        shape = RoundedCornerShape(9.dp),
                        color = Color(0xFFEDE9FE)
                    ) {
                        Text(
                            strings.get("pro"),
                            color = Color(0xFF6D28D9),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 8.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
                IconButton(
                    onClick = { favorite = FavoritesStore.toggle(context, tool.id) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        if (favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        null,
                        tint = if (favorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                strings.toolTitle(tool.id),
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1
            )
            Text(
                strings.toolSubtitle(tool.id),
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                minLines = 2
            )
        }
    }
}

@Composable
private fun ToolRowV5(tool: ToolDef, strings: UiText, premium: Boolean, openTool: (String) -> Unit) {
    Card(onClick = { openTool(tool.id) }, shape = RoundedCornerShape(21.dp)) {
        Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(50.dp).clip(RoundedCornerShape(17.dp))
                    .background(Brush.linearGradient(listOf(tool.start, tool.end))),
                contentAlignment = Alignment.Center
            ) {
                Icon(tool.icon, null, tint = Color.White)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(strings.toolTitle(tool.id), fontWeight = FontWeight.ExtraBold)
                Text(strings.toolSubtitle(tool.id), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (tool.premium && !premium) {
                Text(strings.get("pro"), color = Color(0xFF7C3AED), fontWeight = FontWeight.ExtraBold, fontSize = 9.sp)
            }
            val context = LocalContext.current
            var favorite by remember(tool.id) { mutableStateOf(FavoritesStore.isFavorite(context, tool.id)) }
            IconButton(
                onClick = { favorite = FavoritesStore.toggle(context, tool.id) },
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    if (favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    null,
                    tint = if (favorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SectionTitleV5(title: String, subtitle: String) {
    Column {
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
        Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun MiniPillV5(value: String, label: String) {
    Surface(shape = RoundedCornerShape(14.dp), color = Color.White.copy(alpha = .10f)) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
            Text(value, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
            Text(label, color = Color.White.copy(alpha = .70f), fontSize = 8.sp)
        }
    }
}

@Composable
internal fun FeatureCardV5(icon: ImageVector, title: String, text: String) {
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(10.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold)
                Text(text, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}


@Composable
private fun ToolWorkspaceV5(
    tool: ToolDef,
    premium: Boolean,
    onBack: () -> Unit,
    onNeedPremium: () -> Unit
) {
    val context = LocalContext.current
    val strings = LocalUiText.current
    val scope = rememberCoroutineScope()

    var sourceUri by remember { mutableStateOf<Uri?>(null) }
    var sourceBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var pendingBytes by remember { mutableStateOf<ByteArray?>(null) }
    var pendingFormat by remember { mutableStateOf<OutputFormat?>(null) }
    var pendingPdf by remember { mutableStateOf(false) }
    var exportedUri by remember { mutableStateOf<Uri?>(null) }
    var exportedMime by remember { mutableStateOf("image/*") }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    var details by remember { mutableStateOf<String?>(null) }
    var ocrText by remember { mutableStateOf<String?>(null) }
    var exifText by remember { mutableStateOf<String?>(null) }
    var palette by remember { mutableStateOf<List<Int>>(emptyList()) }

    var width by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var keepRatio by remember { mutableStateOf(true) }
    var quality by remember { mutableFloatStateOf(82f) }
    var format by remember { mutableStateOf(OutputFormat.JPEG) }
    var cropRatio by remember { mutableStateOf("Original") }
    var angle by remember { mutableIntStateOf(90) }
    var flipH by remember { mutableStateOf(false) }
    var flipV by remember { mutableStateOf(false) }
    var imageFilter by remember { mutableStateOf(ImageFilter.ORIGINAL) }
    var watermark by remember { mutableStateOf("IMAGE TOOLS") }
    var opacity by remember { mutableFloatStateOf(65f) }
    var position by remember { mutableStateOf("bottom_right") }
    var amount by remember { mutableFloatStateOf(0f) }
    var pixelSize by remember { mutableFloatStateOf(18f) }
    var borderSize by remember { mutableFloatStateOf(24f) }
    var cornerRadius by remember { mutableFloatStateOf(36f) }
    var duotonePreset by remember { mutableStateOf("ocean") }

    LaunchedEffect(tool.id) {
        amount = when (tool.id) {
            "film_grain" -> 32f
            "color_pop" -> 78f
            "outline" -> 68f
            "glitch" -> 36f
            "scan_document" -> 72f
            else -> amount
        }
    }

    val singlePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            sourceUri = uri
            scope.launch {
                val bitmap = withContext(Dispatchers.IO) {
                    ImageProcessor.decode(context, uri)
                }
                sourceBitmap = bitmap
                previewBitmap = null
                pendingBytes = null
                pendingFormat = null
                pendingPdf = false
                exportedUri = null
                details = null
                ocrText = null
                exifText = null
                palette = emptyList()
                width = bitmap?.width?.toString().orEmpty()
                height = bitmap?.height?.toString().orEmpty()
                status = if (bitmap == null) "Could not read the image." else strings.get("ready")
            }
        }
    }

    val multiPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(4)
    ) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch {
                busy = true
                val bitmaps = withContext(Dispatchers.IO) {
                    uris.mapNotNull { ImageProcessor.decode(context, it) }
                }
                if (bitmaps.size >= 2) {
                    val collage = withContext(Dispatchers.Default) {
                        AdvancedImageProcessor.collage(bitmaps)
                    }
                    sourceBitmap = bitmaps.first()
                    sourceUri = uris.first()
                    previewBitmap = collage
                    pendingBytes = withContext(Dispatchers.Default) {
                        ImageProcessor.encode(collage, OutputFormat.PNG, 100)
                    }
                    pendingFormat = OutputFormat.PNG
                    pendingPdf = false
                    status = strings.get("ready")
                } else {
                    status = strings.get("collage.help")
                }
                busy = false
            }
        }
    }

    fun chooseInput() {
        if (tool.premium && !premium) {
            onNeedPremium()
            return
        }
        if (tool.id == "collage") {
            multiPicker.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        } else {
            singlePicker.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }
    }

    suspend fun prepareImage(bitmap: Bitmap, output: OutputFormat = format, qualityValue: Int = 100) {
        pendingBytes = withContext(Dispatchers.Default) {
            ImageProcessor.encode(bitmap, output, qualityValue)
        }
        pendingFormat = output
        pendingPdf = false
        previewBitmap = bitmap
        status = strings.get("ready")
    }

    suspend fun processAction() {
        val bitmap = sourceBitmap
        if (bitmap == null) {
            status = strings.get("select.first")
            chooseInput()
            return
        }
        if (tool.premium && !premium) {
            onNeedPremium()
            return
        }

        busy = true
        details = null
        ocrText = null
        exifText = null
        palette = emptyList()
        pendingBytes = null
        pendingFormat = null
        pendingPdf = false
        exportedUri = null
        status = strings.get("processing")

        try {
            when (tool.id) {
                "resize" -> {
                    val targetWidth = width.toIntOrNull()?.coerceAtLeast(1) ?: bitmap.width
                    val targetHeight = if (keepRatio) {
                        (bitmap.height * (targetWidth.toFloat() / bitmap.width)).toInt().coerceAtLeast(1)
                    } else {
                        height.toIntOrNull()?.coerceAtLeast(1) ?: bitmap.height
                    }
                    prepareImage(
                        ImageProcessor.resize(bitmap, targetWidth, targetHeight),
                        format,
                        100
                    )
                }

                "compress" -> {
                    prepareImage(bitmap, format, quality.toInt())
                }

                "convert" -> {
                    prepareImage(bitmap, format, 95)
                }

                "crop" -> {
                    prepareImage(ImageProcessor.cropCenter(bitmap, cropRatio), format, 100)
                }

                "rotate" -> {
                    prepareImage(ImageProcessor.rotate(bitmap, angle, flipH, flipV), format, 100)
                }

                "filter" -> {
                    prepareImage(ImageProcessor.filter(bitmap, imageFilter), format, 100)
                }

                "watermark" -> {
                    prepareImage(
                        ImageProcessor.watermark(
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
                        ),
                        format,
                        100
                    )
                }

                "brightness" -> {
                    prepareImage(
                        AdvancedImageProcessor.adjustColor(bitmap, amount.toInt(), 0, 0, 0),
                        format,
                        100
                    )
                }

                "contrast" -> {
                    prepareImage(
                        AdvancedImageProcessor.adjustColor(bitmap, 0, amount.toInt(), 0, 0),
                        format,
                        100
                    )
                }

                "saturation" -> {
                    prepareImage(
                        AdvancedImageProcessor.adjustColor(bitmap, 0, 0, amount.toInt(), 0),
                        format,
                        100
                    )
                }

                "warmth" -> {
                    prepareImage(
                        AdvancedImageProcessor.adjustColor(bitmap, 0, 0, 0, amount.toInt()),
                        format,
                        100
                    )
                }

                "negative" -> {
                    prepareImage(AdvancedImageProcessor.negative(bitmap), format, 100)
                }

                "blur" -> {
                    prepareImage(
                        AdvancedImageProcessor.blur(
                            bitmap,
                            (amount / 25f).toInt().coerceIn(1, 4)
                        ),
                        format,
                        100
                    )
                }

                "sharpen" -> {
                    prepareImage(
                        AdvancedImageProcessor.sharpen(
                            bitmap,
                            (amount / 25f).toInt().coerceIn(1, 3)
                        ),
                        format,
                        100
                    )
                }

                "pixelate" -> {
                    prepareImage(AdvancedImageProcessor.pixelate(bitmap, pixelSize.toInt()), format, 100)
                }

                "border" -> {
                    prepareImage(
                        AdvancedImageProcessor.addBorder(
                            bitmap,
                            borderSize.toInt(),
                            android.graphics.Color.WHITE
                        ),
                        format,
                        100
                    )
                }

                "round" -> {
                    prepareImage(
                        AdvancedImageProcessor.roundCorners(bitmap, cornerRadius),
                        OutputFormat.PNG,
                        100
                    )
                }

                "metadata" -> {
                    prepareImage(
                        bitmap,
                        format,
                        100
                    )
                    status = "Preview ready. Export will re-encode the image and remove common metadata."
                }

                "auto_enhance" -> {
                    prepareImage(AdvancedImageProcessor.autoEnhance(bitmap), format, 100)
                }

                "exposure" -> {
                    prepareImage(
                        AdvancedImageProcessor.exposure(bitmap, amount.toInt()),
                        format,
                        100
                    )
                }

                "tint" -> {
                    prepareImage(
                        AdvancedImageProcessor.tint(bitmap, amount.toInt()),
                        format,
                        100
                    )
                }

                "vignette" -> {
                    prepareImage(
                        AdvancedImageProcessor.vignette(bitmap, amount.toInt()),
                        format,
                        100
                    )
                }

                "posterize" -> {
                    prepareImage(
                        AdvancedImageProcessor.posterize(bitmap, amount.toInt()),
                        format,
                        100
                    )
                }

                "duotone" -> {
                    val colors = when (duotonePreset) {
                        "sunset" -> android.graphics.Color.rgb(60, 12, 50) to android.graphics.Color.rgb(255, 182, 92)
                        "ink" -> android.graphics.Color.rgb(15, 23, 42) to android.graphics.Color.rgb(241, 245, 249)
                        else -> android.graphics.Color.rgb(8, 47, 73) to android.graphics.Color.rgb(103, 232, 249)
                    }
                    prepareImage(
                        AdvancedImageProcessor.duotone(bitmap, colors.first, colors.second),
                        format,
                        100
                    )
                }

                "mirror" -> {
                    prepareImage(
                        AdvancedImageProcessor.mirror(bitmap, flipH),
                        format,
                        100
                    )
                }

                "noise_reduction" -> {
                    prepareImage(
                        AdvancedImageProcessor.denoise(bitmap, amount.toInt()),
                        format,
                        100
                    )
                }

                "film_grain" -> {
                    prepareImage(
                        AdvancedImageProcessor.filmGrain(bitmap, amount.toInt()),
                        format,
                        100
                    )
                }

                "color_pop" -> {
                    prepareImage(
                        AdvancedImageProcessor.colorPop(bitmap, amount.toInt()),
                        format,
                        100
                    )
                }

                "outline" -> {
                    prepareImage(
                        AdvancedImageProcessor.outline(bitmap, amount.toInt()),
                        format,
                        100
                    )
                }

                "glitch" -> {
                    prepareImage(
                        AdvancedImageProcessor.glitch(bitmap, amount.toInt()),
                        format,
                        100
                    )
                }

                "scan_document" -> {
                    prepareImage(
                        AdvancedImageProcessor.scanDocument(bitmap, amount.toInt()),
                        OutputFormat.PNG,
                        100
                    )
                }

                "details" -> {
                    val sourceBytes = sourceUri?.let {
                        withContext(Dispatchers.IO) {
                            ImageProcessor.sourceBytes(context, it)
                        }
                    }
                    details = buildString {
                        append("Resolution: ")
                        append(bitmap.width)
                        append(" × ")
                        append(bitmap.height)
                        append("\nAspect ratio: ")
                        append("%.3f".format(bitmap.width.toFloat() / bitmap.height))
                        if (sourceBytes != null) {
                            append("\nSource size: ")
                            append(ImageProcessor.humanBytes(sourceBytes))
                        }
                    }
                    previewBitmap = bitmap
                    status = strings.get("ready")
                }

                "palette" -> {
                    palette = withContext(Dispatchers.Default) {
                        AdvancedImageProcessor.palette(bitmap, 5)
                    }
                    previewBitmap = bitmap
                    status = strings.get("ready")
                }

                "ocr" -> {
                    ocrText = withContext(Dispatchers.Default) {
                        OcrProcessor.recognize(bitmap)
                    }
                    previewBitmap = bitmap
                    status = strings.get("ocr.title")
                }

                "exif" -> {
                    exifText = withContext(Dispatchers.IO) {
                        ExifProcessor.read(
                            context,
                            sourceUri ?: error("Select an image first.")
                        )
                    }
                    previewBitmap = bitmap
                    status = strings.get("exif.title")
                }

                "background" -> {
                    val foreground = withContext(Dispatchers.Default) {
                        BackgroundRemovalProcessor.removeBackground(bitmap)
                    }
                    prepareImage(foreground, OutputFormat.PNG, 100)
                }

                "pdf" -> {
                    pendingBytes = withContext(Dispatchers.Default) {
                        AdvancedImageProcessor.pdfBytes(bitmap)
                    }
                    pendingFormat = null
                    pendingPdf = true
                    previewBitmap = bitmap
                    status = strings.get("pdf.ready")
                }

                "collage" -> {
                    if (pendingBytes == null) {
                        chooseInput()
                    }
                }
            }
            if (previewBitmap != null || pendingBytes != null || details != null || ocrText != null || exifText != null || palette.isNotEmpty()) {
                ProcessingStatsStore.recordProcessed(context)
            }
        } catch (error: Exception) {
            status = error.message ?: strings.get("process.error")
        } finally {
            busy = false
        }
    }

    fun exportResult() {
        val bytes = pendingBytes ?: run {
            status = strings.get("no.result")
            return
        }
        scope.launch {
            busy = true
            try {
                val tree = OutputFolderStore.getTreeUri(context)
                exportedUri = if (pendingPdf) {
                    OutputExporter.savePdf(context, bytes, "image-tools", tree)
                } else {
                    OutputExporter.saveImage(
                        context,
                        bytes,
                        pendingFormat ?: OutputFormat.JPEG,
                        tool.id,
                        previewBitmap?.width ?: 1,
                        previewBitmap?.height ?: 1,
                        tree
                    ).uri
                }
                exportedMime = if (pendingPdf) "application/pdf" else pendingFormat?.mime ?: "image/*"
                ProcessingStatsStore.recordExported(context)
                status = strings.get("saved")
            } catch (error: Exception) {
                status = strings.get("save.error")
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
            ToolHeaderV5(tool, strings, premium, onBack)
        }

        item {
            WorkflowStepV5(
                1,
                strings.get("choose.image"),
                strings.toolSubtitle(tool.id)
            ) {
                Button(
                    onClick = { chooseInput() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, null)
                    Spacer(Modifier.width(7.dp))
                    Text(
                        if (sourceBitmap == null) strings.get("choose.image")
                        else strings.get("choose.another")
                    )
                }
            }
        }

        if (sourceBitmap != null) {
            item {
                WorkflowStepV5(
                    2,
                    strings.get("config"),
                    strings.toolSubtitle(tool.id)
                ) {
                    ToolControlsV5(
                        tool = tool,
                        strings = strings,
                        width = width,
                        height = height,
                        onWidth = { width = it },
                        onHeight = { height = it },
                        keepRatio = keepRatio,
                        onKeepRatio = { keepRatio = it },
                        quality = quality,
                        onQuality = { quality = it },
                        format = format,
                        onFormat = { format = it },
                        cropRatio = cropRatio,
                        onCropRatio = { cropRatio = it },
                        angle = angle,
                        onAngle = { angle = it },
                        flipH = flipH,
                        onFlipH = { flipH = it },
                        flipV = flipV,
                        onFlipV = { flipV = it },
                        imageFilter = imageFilter,
                        onFilter = { imageFilter = it },
                        watermark = watermark,
                        onWatermark = { watermark = it },
                        opacity = opacity,
                        onOpacity = { opacity = it },
                        position = position,
                        onPosition = { position = it },
                        amount = amount,
                        onAmount = { amount = it },
                        pixelSize = pixelSize,
                        onPixelSize = { pixelSize = it },
                        borderSize = borderSize,
                        onBorderSize = { borderSize = it },
                        cornerRadius = cornerRadius,
                        onCornerRadius = { cornerRadius = it },
                        duotonePreset = duotonePreset,
                        onDuotonePreset = { duotonePreset = it }
                    )
                }
            }

            item {
                WorkflowStepV5(
                    3,
                    strings.get("action"),
                    strings.get("preview.button")
                ) {
                    Button(
                        onClick = { scope.launch { processAction() } },
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(17.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, null)
                        Spacer(Modifier.width(7.dp))
                        Text(
                            if (busy) strings.get("processing")
                            else strings.get("preview.button")
                        )
                    }
                }
            }
        }

        if (previewBitmap != null || details != null || ocrText != null || exifText != null || palette.isNotEmpty()) {
            item {
                WorkflowStepV5(
                    4,
                    strings.get("preview"),
                    strings.get("ready")
                ) {
                    previewBitmap?.let {
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 170.dp, max = 330.dp)
                                .clip(RoundedCornerShape(18.dp)),
                            contentScale = ContentScale.Fit
                        )
                    }

                    details?.let {
                        ResultTextCardV5(strings.get("details.result"), it)
                    }

                    exifText?.let {
                        ResultTextCardV5(strings.get("exif.title"), it)
                    }

                    ocrText?.let {
                        ResultTextCardV5(
                            strings.get("ocr.title"),
                            if (it.isBlank()) strings.get("ocr.empty") else it
                        )
                        if (it.isNotBlank()) {
                            TextButton(
                                onClick = {
                                    val clipboard =
                                        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(
                                        ClipData.newPlainText("Image Tools OCR", it)
                                    )
                                    status = strings.get("ocr.copy")
                                }
                            ) {
                                Text(strings.get("ocr.copy"))
                            }
                        }
                    }

                    if (palette.isNotEmpty()) {
                        Text(strings.get("palette.title"), fontWeight = FontWeight.ExtraBold)
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            palette.forEach { colorInt ->
                                Column(
                                    Modifier.weight(1f),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        Modifier
                                            .fillMaxWidth()
                                            .height(54.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(colorInt))
                                    )
                                    Text(
                                        "#%06X".format(colorInt and 0xFFFFFF),
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    status?.let {
                        Text(
                            it,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        if (pendingBytes != null) {
            item {
                WorkflowStepV5(
                    5,
                    strings.get("export"),
                    strings.get("ready")
                ) {
                    Button(
                        onClick = { exportResult() },
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

        exportedUri?.let { uri ->
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5))
                ) {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(strings.get("saved"), fontWeight = FontWeight.ExtraBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    runCatching {
                                        context.startActivity(
                                            Intent(Intent.ACTION_VIEW).apply {
                                                data = uri
                                                type = exportedMime
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                        )
                                    }
                                }
                            ) {
                                Text(strings.get("open"))
                            }
                            OutlinedButton(
                                onClick = {
                                    val send = Intent(Intent.ACTION_SEND).apply {
                                        type = exportedMime
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(
                                        Intent.createChooser(send, strings.get("share"))
                                    )
                                }
                            ) {
                                Text(strings.get("share"))
                            }
                        }
                    }
                }
            }
        }
    }

    if (busy) {
        ProcessingGearDialog(
            title = strings.get("processing.title"),
            subtitle = strings.get("processing.subtitle")
        )
    }
}

@Composable
private fun ToolHeaderV5(tool: ToolDef, strings: UiText, premium: Boolean, onBack: () -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
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
            Text(
                strings.toolTitle(tool.id),
                fontSize = 21.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                strings.toolSubtitle(tool.id),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (tool.premium && !premium) {
            Text(
                strings.get("pro"),
                color = Color(0xFF7C3AED),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 9.sp
            )
        }
    }
}

@Composable
private fun WorkflowStepV5(
    number: Int,
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(shape = RoundedCornerShape(22.dp)) {
        Column(
            Modifier.padding(15.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        number.toString(),
                        Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Spacer(Modifier.width(9.dp))
                Column {
                    Text(title, fontWeight = FontWeight.ExtraBold)
                    Text(
                        subtitle,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            content()
        }
    }
}

@Composable
private fun ResultTextCardV5(title: String, text: String) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            Modifier.padding(13.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Text(title, fontWeight = FontWeight.ExtraBold)
            Text(text, fontSize = 12.sp)
        }
    }
}



@Composable
private fun ToolControlsV5(
    tool: ToolDef,
    strings: UiText,
    width: String,
    height: String,
    onWidth: (String) -> Unit,
    onHeight: (String) -> Unit,
    keepRatio: Boolean,
    onKeepRatio: (Boolean) -> Unit,
    quality: Float,
    onQuality: (Float) -> Unit,
    format: OutputFormat,
    onFormat: (OutputFormat) -> Unit,
    cropRatio: String,
    onCropRatio: (String) -> Unit,
    angle: Int,
    onAngle: (Int) -> Unit,
    flipH: Boolean,
    onFlipH: (Boolean) -> Unit,
    flipV: Boolean,
    onFlipV: (Boolean) -> Unit,
    imageFilter: ImageFilter,
    onFilter: (ImageFilter) -> Unit,
    watermark: String,
    onWatermark: (String) -> Unit,
    opacity: Float,
    onOpacity: (Float) -> Unit,
    position: String,
    onPosition: (String) -> Unit,
    amount: Float,
    onAmount: (Float) -> Unit,
    pixelSize: Float,
    onPixelSize: (Float) -> Unit,
    borderSize: Float,
    onBorderSize: (Float) -> Unit,
    cornerRadius: Float,
    onCornerRadius: (Float) -> Unit,
    duotonePreset: String,
    onDuotonePreset: (String) -> Unit
) {
    when (tool.id) {
        "resize" -> {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = width,
                    onValueChange = { onWidth(it.filter(Char::isDigit)) },
                    modifier = Modifier.weight(1f),
                    label = { Text(strings.get("width")) },
                    singleLine = true
                )
                OutlinedTextField(
                    value = height,
                    onValueChange = { onHeight(it.filter(Char::isDigit)) },
                    modifier = Modifier.weight(1f),
                    label = { Text(strings.get("height")) },
                    singleLine = true
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(strings.get("aspect"), Modifier.weight(1f), fontWeight = FontWeight.Bold)
                Switch(keepRatio, onKeepRatio)
            }
        }

        "compress" -> {
            Text(
                strings.get("quality") + " " + quality.toInt() + "%",
                fontWeight = FontWeight.ExtraBold
            )
            Slider(quality, onQuality, valueRange = 10f..100f)
            FormatChipsV5(format, onFormat, listOf(OutputFormat.JPEG, OutputFormat.WEBP))
        }

        "convert", "metadata" -> {
            Text(strings.get("format"), fontWeight = FontWeight.Bold)
            FormatChipsV5(format, onFormat, OutputFormat.entries.toList())
        }

        "crop" -> {
            ChoicesV5(
                listOf("Original", "1:1", "4:5", "16:9", "9:16"),
                cropRatio,
                onCropRatio
            )
        }

        "rotate" -> {
            ChoicesV5(listOf("90", "180", "270"), angle.toString()) {
                onAngle(it.toInt())
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    flipH,
                    { onFlipH(!flipH) },
                    label = { Text(strings.get("mirror.h")) },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    flipV,
                    { onFlipV(!flipV) },
                    label = { Text(strings.get("mirror.v")) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        "filter" -> {
            ChoicesV5(
                ImageFilter.entries.map { strings.filterTitle(it.name.lowercase()) },
                strings.filterTitle(imageFilter.name.lowercase())
            ) { selected ->
                onFilter(ImageFilter.entries.first { strings.filterTitle(it.name.lowercase()) == selected })
            }
        }

        "watermark" -> {
            OutlinedTextField(
                value = watermark,
                onValueChange = onWatermark,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(strings.get("text")) },
                singleLine = true
            )
            Text(strings.get("opacity") + " " + opacity.toInt() + "%")
            Slider(opacity, onOpacity, valueRange = 10f..100f)
            val positions = listOf(
                "top_left" to strings.get("position.top_left"),
                "top_right" to strings.get("position.top_right"),
                "center" to strings.get("position.center"),
                "bottom_left" to strings.get("position.bottom_left"),
                "bottom_right" to strings.get("position.bottom_right")
            )
            ChoicesV5(
                positions.map { it.second },
                positions.first { it.first == position }.second
            ) { selected ->
                onPosition(positions.first { it.second == selected }.first)
            }
        }

        "brightness", "contrast", "saturation", "warmth" -> {
            Text(strings.get("strength") + " " + amount.toInt())
            Slider(amount, onAmount, valueRange = -100f..100f)
        }

        "blur", "sharpen" -> {
            Text(strings.get("strength") + " " + amount.toInt())
            Slider(amount, onAmount, valueRange = 0f..100f)
        }

        "pixelate" -> {
            Text(strings.get("strength") + " " + pixelSize.toInt() + " px")
            Slider(pixelSize, onPixelSize, valueRange = 4f..48f)
        }

        "border" -> {
            Text(strings.get("strength") + " " + borderSize.toInt() + " px")
            Slider(borderSize, onBorderSize, valueRange = 4f..120f)
        }

        "round" -> {
            Text(strings.get("strength") + " " + cornerRadius.toInt() + " px")
            Slider(cornerRadius, onCornerRadius, valueRange = 8f..160f)
        }

        "auto_enhance" -> {
            Text(strings.toolSubtitle(tool.id), fontSize = 12.sp)
        }

        "exposure", "tint", "vignette", "noise_reduction",
        "film_grain", "color_pop", "outline", "glitch", "scan_document" -> {
            Text(strings.get("strength") + " " + amount.toInt())
            Slider(
                amount,
                onAmount,
                valueRange = if (tool.id == "exposure") -100f..100f else 0f..100f
            )
        }

        "posterize" -> {
            Text(strings.get("strength") + " " + amount.toInt())
            Slider(
                amount,
                onAmount,
                valueRange = 2f..12f,
                steps = 9
            )
        }

        "duotone" -> {
            Text(strings.toolSubtitle(tool.id), fontSize = 12.sp)
            ChoicesV5(
                listOf(
                    strings.get("duotone.ocean"),
                    strings.get("duotone.sunset"),
                    strings.get("duotone.ink")
                ),
                when (duotonePreset) {
                    "sunset" -> strings.get("duotone.sunset")
                    "ink" -> strings.get("duotone.ink")
                    else -> strings.get("duotone.ocean")
                }
            ) { selected ->
                onDuotonePreset(
                    when (selected) {
                        strings.get("duotone.sunset") -> "sunset"
                        strings.get("duotone.ink") -> "ink"
                        else -> "ocean"
                    }
                )
            }
        }

        "mirror" -> {
            Text(strings.get("position"), fontWeight = FontWeight.Bold)
            ChoicesV5(
                listOf("H", "V"),
                if (flipH) "H" else "V"
            ) { selected ->
                onFlipH(selected == "H")
            }
        }

        else -> {
            Text(strings.toolSubtitle(tool.id), fontSize = 12.sp)
        }
    }
}

@Composable
fun FormatChipsV5(
    selected: OutputFormat,
    onSelected: (OutputFormat) -> Unit,
    options: List<OutputFormat>
) {
    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        options.forEach { option ->
            FilterChip(
                selected = selected == option,
                onClick = { onSelected(option) },
                label = { Text(option.extension.uppercase()) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun ChoicesV5(
    options: List<String>,
    selected: String,
    onSelected: (String) -> Unit
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        options.forEach { option ->
            FilterChip(
                selected = selected == option,
                onClick = { onSelected(option) },
                label = { Text(option, fontSize = 10.sp) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun PremiumScreenV5(
    auth: AuthRepository,
    strings: UiText,
    premium: Boolean,
    onStartPayment: (String) -> Unit
) {
    val context = LocalContext.current
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(shape = RoundedCornerShape(30.dp)) {
                Box(
                    Modifier.fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF17102D), Color(0xFF6D28D9), Color(0xFF0E7490))
                            )
                        )
                        .padding(24.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        Text(
                            "IMAGE TOOLS PRO",
                            color = Color.White.copy(alpha = .72f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            if (premium) strings.get("premium.active") else strings.get("premium.unlock"),
                            color = Color.White,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            strings.get("paypal"),
                            color = Color.White.copy(alpha = .82f),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        item {
            FeatureCardV5(
                Icons.Default.CheckCircle,
                strings.get("pro"),
                TOOL_CATALOG.filter { it.premium }.joinToString(" • ") { strings.toolTitle(it.id) }
            )
        }

        item {
            if (!premium) {
                Button(
                    onClick = {
                        busy = true
                        error = null
                        scope.launch {
                            val token = auth.idToken()
                            if (token.isNullOrBlank()) {
                                error = "Please sign in again."
                            } else {
                                PaymentRepository.createSubscription(token)
                                    .onSuccess {
                                        com.nexauren.imagetools.data.SubscriptionStore.save(context, auth.currentUser?.uid, it.subscriptionId)
                                        onStartPayment(it.approveUrl)
                                    }
                                    .onFailure {
                                        error = if (it is PaymentException) {
                                            it.message
                                        } else {
                                            "Could not start PayPal checkout."
                                        }
                                    }
                            }
                            busy = false
                        }
                    },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(17.dp)
                ) {
                    Icon(Icons.Default.Payment, null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (busy) strings.get("working") else strings.get("paypal"),
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        error?.let {
            item {
                Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun AccountScreenV5(
    auth: AuthRepository,
    strings: UiText,
    premium: Boolean,
    openPremium: () -> Unit
) {
    val context = LocalContext.current
    val processed = ProcessingStatsStore.processed(context)
    val exported = ProcessingStatsStore.exported(context)

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(shape = RoundedCornerShape(28.dp)) {
                Box(
                    Modifier.fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFF111827),
                                    Color(0xFF4C1D95),
                                    Color(0xFF0E7490)
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            strings.get("account"),
                            color = Color.White,
                            fontSize = 25.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            auth.currentUser?.email.orEmpty(),
                            color = Color.White.copy(alpha = .78f),
                            fontSize = 12.sp
                        )
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = .12f)
                        ) {
                            Text(
                                if (premium) strings.get("premium.active") else strings.get("free"),
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AccountStatCardV5(
                    processed.toString(),
                    strings.get("account.processed"),
                    Icons.Default.AutoFixHigh,
                    Modifier.weight(1f)
                )
                AccountStatCardV5(
                    exported.toString(),
                    strings.get("account.exported"),
                    Icons.Default.FileDownload,
                    Modifier.weight(1f)
                )
            }
        }

        item {
            Card(shape = RoundedCornerShape(22.dp)) {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    Text(
                        strings.get("account.subscription"),
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        if (premium) strings.get("premium.active") else strings.get("free"),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Button(
                        onClick = openPremium,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(15.dp)
                    ) {
                        Icon(Icons.Default.WorkspacePremium, null)
                        Spacer(Modifier.width(7.dp))
                        Text(strings.get("premium"))
                    }
                }
            }
        }

        item {
            FeatureCardV5(
                Icons.Default.Security,
                strings.get("account.security"),
                auth.currentUser?.email.orEmpty()
            )
        }

        item {
            Card(shape = RoundedCornerShape(22.dp)) {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Text(
                        strings.get("settings.legal"),
                        fontWeight = FontWeight.ExtraBold
                    )
                    LegalLinkV5("Privacy Policy", PRIVACY, Icons.Default.PrivacyTip)
                    LegalLinkV5("Terms of Service", TERMS, Icons.Default.Gavel)
                    LegalLinkV5(strings.get("account.support"), SUPPORT, Icons.Default.SupportAgent)
                }
            }
        }

        item {
            OutlinedButton(
                onClick = { auth.signOut() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Logout, null)
                Spacer(Modifier.width(7.dp))
                Text(strings.get("account.signout"))
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text(
                        strings.get("account.delete"),
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        strings.get("account.delete.help"),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    TextButton(
                        onClick = {
                            context.startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse(SUPPORT + "/account-deletion")
                                )
                            )
                        }
                    ) {
                        Icon(Icons.Default.DeleteOutline, null)
                        Spacer(Modifier.width(5.dp))
                        Text(strings.get("account.delete"))
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountStatCardV5(
    value: String,
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Text(
                value,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                label,
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SettingsScreenV5(
    strings: UiText,
    darkMode: Boolean,
    language: AppLanguage,
    onDarkModeChange: (Boolean) -> Unit,
    onLanguageChange: (AppLanguage) -> Unit
) {
    val context = LocalContext.current
    var folder by remember { mutableStateOf(OutputFolderStore.folderName(context)) }
    var notificationsEnabled by remember { mutableStateOf(AppNotificationSettings.isEnabled(context)) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationsEnabled = granted
        AppNotificationSettings.setEnabled(context, granted)
        if (granted) {
            AppNotificationHelper.ensureChannel(context)
            AppNotificationHelper.show(
                context,
                strings.get("notification.premium.title"),
                strings.get("settings.notifications.help")
            )
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            }
            OutputFolderStore.saveTreeUri(context, uri)
            folder = OutputFolderStore.folderName(context, uri)
        }
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionTitleV5(strings.get("settings"), strings.get("language.help"))
        }

        item {
            Card(shape = RoundedCornerShape(20.dp)) {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(strings.get("language"), fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        AppLanguage.entries.forEach { option ->
                            FilterChip(
                                selected = option == language,
                                onClick = { onLanguageChange(option) },
                                label = { Text(option.label, fontSize = 9.sp) }
                            )
                        }
                    }
                }
            }
        }

        item {
            SettingRowV5(
                strings.get("settings.dark"),
                strings.get("settings.dark.help"),
                Icons.Default.DarkMode
            ) {
                Switch(darkMode, onDarkModeChange)
            }
        }

        item {
            SettingRowV5(
                strings.get("settings.notifications"),
                strings.get("settings.notifications.help"),
                Icons.Default.Notifications
            ) {
                Switch(
                    checked = notificationsEnabled,
                    onCheckedChange = { enabled ->
                        if (!enabled) {
                            notificationsEnabled = false
                            AppNotificationSettings.setEnabled(context, false)
                        } else if (Build.VERSION.SDK_INT >= 33 &&
                            !AppNotificationHelper.hasPermission(context)
                        ) {
                            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            notificationsEnabled = true
                            AppNotificationSettings.setEnabled(context, true)
                            AppNotificationHelper.ensureChannel(context)
                        }
                    }
                )
            }
        }

        if (notificationsEnabled && AppNotificationHelper.hasPermission(context)) {
            item {
                OutlinedButton(
                    onClick = {
                        AppNotificationHelper.show(
                            context,
                            strings.get("notification.premium.title"),
                            strings.get("settings.notifications.help")
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.NotificationsActive, null)
                    Spacer(Modifier.width(7.dp))
                    Text(strings.get("settings.notifications.test"))
                }
            }
        }

        item {
            SettingRowV5(
                strings.get("settings.output"),
                folder ?: "Pictures/Image Tools",
                Icons.Default.Folder
            ) {
                TextButton(onClick = { picker.launch(null) }) {
                    Text(strings.get("choose.image"))
                }
            }
        }

        item {
            LegalLinkV5("Privacy Policy", PRIVACY, Icons.Default.PrivacyTip)
        }
        item {
            LegalLinkV5("Terms of Service", TERMS, Icons.Default.Gavel)
        }
        item {
            LegalLinkV5("Support & account deletion", SUPPORT, Icons.Default.SupportAgent)
        }
    }
}

@Composable
private fun SettingRowV5(
    title: String,
    subtitle: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Card(shape = RoundedCornerShape(20.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(subtitle, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            content()
        }
    }
}

@Composable
private fun LegalLinkV5(title: String, url: String, icon: ImageVector) {
    val context = LocalContext.current
    Card(
        onClick = {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        },
        shape = RoundedCornerShape(20.dp)
    ) {
        ListItem(
            leadingContent = { Icon(icon, null, tint = MaterialTheme.colorScheme.primary) },
            headlineContent = { Text(title, fontWeight = FontWeight.Bold) },
            trailingContent = { Icon(Icons.Default.OpenInNew, null) }
        )
    }
}

@Composable
private fun AboutScreenV5(strings: UiText) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(shape = RoundedCornerShape(26.dp)) {
                Column(
                    Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    Text(strings.get("about"), fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
                    Text(strings.get("about.text"))
                    Text(
                        "Image Tools 1.11.4",
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Input → Configure → Action → Preview → Export",
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
