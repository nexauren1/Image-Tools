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
import com.nexauren.imagetools.data.OutputFolderStore
import com.nexauren.imagetools.data.PaymentException
import com.nexauren.imagetools.data.PaymentRepository
import com.nexauren.imagetools.media.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val SITE = "https://steep-pine-34fe.nexaurenstore.workers.dev"
private const val PRIVACY = SITE + "/privacy"
private const val TERMS = SITE + "/terms"
private const val SUPPORT = SITE + "/support"

private data class ToolDef(
    val id: String,
    val icon: ImageVector,
    val start: Color,
    val end: Color,
    val premium: Boolean = false
)

private val TOOL_CATALOG = listOf(
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
    ToolDef("exif", Icons.Default.DataObject, Color(0xFF475569), Color(0xFF06B6D4))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageToolsAppV5(
    auth: AuthRepository,
    premium: Boolean,
    darkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    onStartPayment: (String) -> Unit,
    onCancelSubscription: () -> Unit
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
                        BottomNavV5(page == "home", { navigate("home") }, Icons.Default.Home, strings.get("home"))
                        BottomNavV5(page == "tools", { navigate("tools") }, Icons.Default.Build, strings.get("tools"))
                        BottomNavV5(page == "batch", { navigate("batch") }, Icons.Default.DynamicFeed, strings.get("batch"))
                    }
                }
            ) { padding ->
                Box(Modifier.fillMaxSize().padding(padding)) {
                    when {
                        selectedTool != null -> {
                            val tool = TOOL_CATALOG.first { it.id == selectedTool }
                            ToolWorkspaceV5(
                                tool = tool,
                                premium = premium,
                                onBack = { selectedTool = null },
                                onNeedPremium = { navigate("premium") }
                            )
                        }
                        page == "home" -> HomeScreenV5(strings, premium) { id ->
                            val tool = TOOL_CATALOG.first { it.id == id }
                            if (tool.premium && !premium) navigate("premium") else selectedTool = id
                        }
                        page == "tools" -> ToolsScreenV5(strings) { id ->
                            val tool = TOOL_CATALOG.first { it.id == id }
                            if (tool.premium && !premium) navigate("premium") else selectedTool = id
                        }
                        page == "batch" -> BatchScreen(strings)
                        page == "premium" -> PremiumScreenV5(auth, strings, premium, onStartPayment, onCancelSubscription)
                        page == "account" -> AccountScreenV5(auth, strings, premium) { navigate("premium") }
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
private fun BottomNavV5(selected: Boolean, onClick: () -> Unit, icon: ImageVector, label: String) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = { Icon(icon, null) },
        label = { Text(label) }
    )
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
                                        .onFailure { error = authMessageV5(it) }
                                    busy = false
                                }
                            },
                            enabled = !busy,
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Default.AccountCircle, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Continue with Google")
                        }
                        HorizontalDivider()
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Email") },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Password") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation()
                        )
                        Button(
                            onClick = {
                                busy = true
                                error = null
                                scope.launch {
                                    val result = if (create) auth.registerEmail(email, password) else auth.signInEmail(email, password)
                                    result.onFailure { error = authMessageV5(it) }
                                    busy = false
                                }
                            },
                            enabled = !busy && email.contains("@") && password.length >= 6,
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(if (create) "Create account" else "Sign in")
                        }
                        TextButton(
                            onClick = { create = !create },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (create) "I already have an account" else "Create an account")
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
                            Text("Forgot password?")
                        }
                        error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
                    }
                }
            }
        }
    }
}

private fun authMessageV5(error: Throwable): String {
    val text = error.message?.lowercase().orEmpty()
    return when {
        "already in use" in text -> "This email is already registered."
        "invalid email" in text || "badly formatted" in text -> "Please enter a valid email address."
        "wrong-password" in text || "invalid-credential" in text -> "The email or password is incorrect."
        "network" in text -> "Connection failed. Check your internet."
        else -> "We could not complete sign-in. Please try again."
    }
}

@Composable
private fun HomeScreenV5(strings: UiText, premium: Boolean, openTool: (String) -> Unit) {
    var query by remember { mutableStateOf("") }
    val filtered = TOOL_CATALOG.filter {
        (strings.toolTitle(it.id) + " " + strings.toolSubtitle(it.id)).contains(query, true)
    }
    val popularIds = listOf("resize", "compress", "convert", "crop", "ocr", "background")
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
                        .background(Brush.linearGradient(listOf(Color(0xFF0B1022), Color(0xFF4C1D95), Color(0xFF0891B2))))
                        .padding(22.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(if (premium) strings.get("pro") else "IMAGE TOOLBOX", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 10.sp)
                        Text(strings.get("home"), color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                        Text(strings.get("home.subtitle"), color = Color.White.copy(alpha = .82f), fontSize = 12.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            MiniPillV5("25+", strings.get("tools"))
                            MiniPillV5("Local", "processing")
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
        item { SectionTitleV5(strings.get("popular"), strings.get("home.subtitle")) }
        items(popular) { tool -> ToolRowV5(tool, strings, openTool) }
        item { SectionTitleV5(strings.get("all.tools"), filtered.size.toString()) }
        items(filtered) { tool -> ToolRowV5(tool, strings, openTool) }
        item { FeatureCardV5(Icons.Default.Lock, strings.get("local"), strings.get("private")) }
    }
}

@Composable
private fun ToolsScreenV5(strings: UiText, openTool: (String) -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(TOOL_CATALOG) { tool -> ToolRowV5(tool, strings, openTool) }
    }
}

@Composable
private fun ToolRowV5(tool: ToolDef, strings: UiText, openTool: (String) -> Unit) {
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
            if (tool.premium) {
                Text(strings.get("pro"), color = Color(0xFF7C3AED), fontWeight = FontWeight.ExtraBold, fontSize = 9.sp)
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
private fun FeatureCardV5(icon: ImageVector, title: String, text: String) {
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
