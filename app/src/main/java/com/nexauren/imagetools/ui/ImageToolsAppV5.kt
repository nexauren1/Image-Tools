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
        val scope = rememberCoroutineScope()
        val drawerState = rememberDrawerState(DrawerValue.Closed)

        fun navigate(target: String) {
            page = target
            selectedTool = null
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
                    DrawerEntryV5(Icons.Default.Favorite, favoritesLabelV5(context)) { navigate("favorites") }
                    DrawerEntryV5(Icons.Default.History, strings.get("history")) { navigate("history") }
                    DrawerEntryV5(Icons.Default.AutoAwesome, strings.get("recipes")) { navigate("recipes") }
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
                            IconButton(onClick = { navigate("premium") }) {
                                Icon(
                                    if (premium) Icons.Default.WorkspacePremium else Icons.Default.AutoAwesome,
                                    strings.get("premium")
                                )
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
                        "Image Tools ${BuildConfig.VERSION_NAME}",
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
