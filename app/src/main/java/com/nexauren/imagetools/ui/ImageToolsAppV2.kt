package com.nexauren.imagetools.ui

import android.content.Intent
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
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
import androidx.compose.ui.draw.rotate
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
import com.nexauren.imagetools.data.PaymentException
import com.nexauren.imagetools.data.PaymentRepository
import com.nexauren.imagetools.data.OutputFolderStore
import com.nexauren.imagetools.media.ImageFilter
import com.nexauren.imagetools.media.ImageProcessor
import com.nexauren.imagetools.media.OutputFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class Tool(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val start: Color,
    val end: Color,
    val soft: Color,
    val premiumOnly: Boolean = false
)

private val tools = listOf(
    Tool("resize", "Resize Image", "Exact dimensions for any workflow", Icons.Default.PhotoSizeSelectLarge, Color(0xFF007CF0), Color(0xFF00DFD8), Color(0xFFE9FBFB)),
    Tool("compress", "Compress", "Reduce file size with control", Icons.Default.Compress, Color(0xFF0BA360), Color(0xFF3CBA92), Color(0xFFEAFBF4)),
    Tool("convert", "Convert Format", "JPEG, PNG and WEBP", Icons.Default.SwapHoriz, Color(0xFF7B2FF7), Color(0xFFF107A3), Color(0xFFF7EEFF)),
    Tool("crop", "Smart Crop", "Social ratios, portraits and banners", Icons.Default.Crop, Color(0xFFFF8008), Color(0xFFFFC837), Color(0xFFFFF6E7)),
    Tool("rotate", "Rotate & Flip", "Straighten, rotate or mirror", Icons.Default.Rotate90DegreesCw, Color(0xFF4F46E5), Color(0xFFEC4899), Color(0xFFF1EFFF)),
    Tool("filter", "Quick Filters", "Fast looks for everyday photos", Icons.Default.FilterVintage, Color(0xFF11998E), Color(0xFF38EF7D), Color(0xFFE9FFF4)),
    Tool("info", "Image Details", "Inspect size, format and dimensions", Icons.Default.Info, Color(0xFF334155), Color(0xFF06B6D4), Color(0xFFEEF7F9)),
    Tool("watermark", "Smart Watermark", "Brand images with a clean custom mark", Icons.Default.TextFields, Color(0xFF7C3AED), Color(0xFFEC4899), Color(0xFFF7EEFF), premiumOnly = true),
    Tool("adjust", "Adjust", "Tune brightness, contrast and color", Icons.Default.Tune, Color(0xFFFF4D6D), Color(0xFFFF8A5B), Color(0xFFFFEEF1)),
    Tool("collage", "Collage", "Combine up to six photos", Icons.Default.GridView, Color(0xFF00A6FB), Color(0xFF38D9A9), Color(0xFFEAF9FF)),
    Tool("frame", "Frame", "Add borders and clean backgrounds", Icons.Default.CropFree, Color(0xFFB36BFF), Color(0xFF6C63FF), Color(0xFFF5F0FF)),
    Tool("meme", "Meme", "Build a captioned shareable image", Icons.Default.TextFields, Color(0xFFFFB703), Color(0xFFFF6B35), Color(0xFFFFF7DE)),
    Tool("pixelate", "Pixelate", "Create a controlled pixel effect", Icons.Default.GridOn, Color(0xFF8338EC), Color(0xFF3A86FF), Color(0xFFF1EDFF))
)



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageToolsAppV2(
    auth: AuthRepository,
    premium: Boolean,
    darkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    onStartPayment: (String) -> Unit,
    onCancelSubscription: () -> Unit
) {
    val context = LocalContext.current
    val language = rememberAppLanguage(context)
    val t = rememberStrings(language.value)

    if (auth.currentUser == null) {
        ModernAuthScreen(auth)
        return
    }

    var page by remember { mutableStateOf("home") }
    var selectedTool by remember { mutableStateOf<String?>(null) }
    val drawer = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawer,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(315.dp),
                drawerContainerColor = MaterialTheme.colorScheme.surface
            ) {
                Spacer(Modifier.height(22.dp))
                Row(
                    Modifier.padding(horizontal = 22.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(52.dp).clip(RoundedCornerShape(18.dp)).background(
                            Brush.linearGradient(listOf(Color(0xFF00C6FF), Color(0xFF7B2FF7), Color(0xFFFF4D6D)))
                        ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.PhotoLibrary, null, tint = Color.White, modifier = Modifier.size(27.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(t.appName, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                        Text(if (premium) t.premium else t.free, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }
                Spacer(Modifier.height(22.dp))
                DrawerEntry(Icons.Default.Home, t.home) { page = "home"; selectedTool = null; scope.launch { drawer.close() } }
                DrawerEntry(Icons.Default.Build, t.tools) { page = "tools"; selectedTool = null; scope.launch { drawer.close() } }
                DrawerEntry(Icons.Default.WorkspacePremium, t.premium) { page = "premium"; selectedTool = null; scope.launch { drawer.close() } }
                DrawerEntry(Icons.Default.Person, t.account) { page = "account"; selectedTool = null; scope.launch { drawer.close() } }
                DrawerEntry(Icons.Default.Settings, t.settings) { page = "settings"; selectedTool = null; scope.launch { drawer.close() } }
                DrawerEntry(Icons.Default.Info, t.about) { page = "about"; selectedTool = null; scope.launch { drawer.close() } }
                Spacer(Modifier.weight(1f))
                Surface(
                    Modifier.padding(16.dp).fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = .07f)
                ) {
                    Text(
                        "Local editing • private workflow • mobile first",
                        Modifier.padding(16.dp),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                if (selectedTool == null) {
                    TopAppBar(
                        title = {
                            Column {
                                Text(t.appName, fontWeight = FontWeight.ExtraBold)
                                Text(
                                    if (premium) t.premium else t.localProcessing,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawer.open() } }) {
                                Icon(Icons.Default.Menu, t.tools)
                            }
                        },
                        actions = {
                            IconButton(onClick = { page = "settings" }) { Icon(Icons.Default.Settings, t.settings) }
                        }
                    )
                } else {
                    TopAppBar(
                        title = { Text(selectedToolTitle(selectedTool ?: "", t), fontWeight = FontWeight.ExtraBold) },
                        navigationIcon = {
                            IconButton(onClick = { selectedTool = null }) {
                                Icon(Icons.Default.ArrowBack, t.home)
                            }
                        },
                        actions = {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color(0xFF10A37F).copy(alpha = .10f)
                            ) {
                                Text(
                                    if (premium && selectedTool == "watermark") t.premium else t.localProcessing,
                                    Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    color = if (premium && selectedTool == "watermark") Color(0xFF0B8F6B) else MaterialTheme.colorScheme.primary,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    )
                }
            },
            bottomBar = {
                if (selectedTool == null) {
                    NavigationBar {
                        BottomEntry("home", Icons.Default.Home, t.home, page) { page = "home" }
                        BottomEntry("tools", Icons.Default.Build, t.tools, page) { page = "tools" }
                        BottomEntry("account", Icons.Default.Person, t.account, page) { page = "account" }
                    }
                }
            }
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                AnimatedContent(targetState = selectedTool ?: page, label = "navigation") { target ->
                    when (target) {
                        "home" -> ModernHome(premium, { selectedTool = it }, t)
                        "tools" -> ModernTools(premium, { selectedTool = it }, t)
                        "account" -> ModernAccount(auth, premium) { page = "premium" }
                        "premium" -> ModernPremium(auth, premium, onStartPayment, onCancelSubscription)
                        "settings" -> ModernSettings(darkMode, onDarkModeChange, language, t)
                        "about" -> ModernAbout()
                        else -> ModernToolWorkspace(target, premium) { selectedTool = null }
                    }
                }
            }
        }
    }
}

@Composable
private fun DrawerEntry(icon: ImageVector, label: String, onClick: () -> Unit) {
    NavigationDrawerItem(
        label = { Text(label) },
        selected = false,
        onClick = onClick,
        icon = { Icon(icon, null) },
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
    )
}

@Composable
private fun RowScope.BottomEntry(id: String, icon: ImageVector, label: String, page: String, onClick: () -> Unit) {
    val selected = page == id
    Column(
        modifier = Modifier
            .weight(1f)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(3.dp))
        Text(
            label,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ModernAuthScreen(auth: AuthRepository) {
    var create by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Box(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF0B1022), Color(0xFF26306A))))
    ) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(22.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Box(
                    Modifier.size(92.dp).clip(RoundedCornerShape(28.dp)).background(
                        Brush.linearGradient(listOf(Color(0xFF7B2FF7), Color(0xFF00DFD8)))
                    ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.AutoFixHigh, null, tint = Color.White, modifier = Modifier.size(44.dp))
                }
                Spacer(Modifier.height(18.dp))
                Text(if (create) "Create your workspace" else "Welcome back", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(6.dp))
                Text("Simple, private image editing for your phone.", color = Color.White.copy(alpha = 0.75f))
                Spacer(Modifier.height(24.dp))
                Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(Modifier.padding(20.dp)) {
                        Button(
                            onClick = {
                                busy = true
                                error = null
                                notice = null
                                scope.launch {
                                    auth.signInGoogle(BuildConfig.GOOGLE_WEB_CLIENT_ID)
                                        .onFailure { error = authMessage(it) }
                                    busy = false
                                }
                            },
                            enabled = !busy,
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(17.dp)
                        ) {
                            Icon(Icons.Default.AccountCircle, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Continue with Google", fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(14.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            HorizontalDivider(Modifier.weight(1f))
                            Text("  OR  ", fontSize = 11.sp, color = Color.Gray)
                            HorizontalDivider(Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(14.dp))
                        OutlinedTextField(
                            email,
                            { email = it },
                            Modifier.fillMaxWidth(),
                            label = { Text("Email") },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp)
                        )
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            password,
                            { password = it },
                            Modifier.fillMaxWidth(),
                            label = { Text("Password") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            shape = RoundedCornerShape(16.dp)
                        )
                        if (!create) {
                            TextButton(
                                onClick = {
                                    error = null
                                    notice = null
                                    val target = email.trim()
                                    if (target.isBlank() || !target.contains("@")) {
                                        error = "Enter your email first."
                                    } else {
                                        busy = true
                                        scope.launch {
                                            auth.sendPasswordReset(target)
                                                .onSuccess {
                                                    notice = "Password reset email sent. Check your inbox."
                                                }
                                                .onFailure { error = authMessage(it) }
                                            busy = false
                                        }
                                    }
                                },
                                enabled = !busy,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Forgot password?")
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Button(
                            onClick = {
                                busy = true
                                error = null
                                notice = null
                                scope.launch {
                                    val result = if (create) {
                                        auth.registerEmail(email, password)
                                    } else {
                                        auth.signInEmail(email, password)
                                    }
                                    result
                                        .onFailure { error = authMessage(it) }
                                    busy = false
                                }
                            },
                            enabled = !busy && email.isNotBlank() && password.length >= 6,
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(17.dp)
                        ) {
                            Text(if (create) "Create account" else "Sign in", fontWeight = FontWeight.Bold)
                        }
                        TextButton(onClick = {
                            create = !create
                            error = null
                            notice = null
                        }, Modifier.fillMaxWidth()) {
                            Text(if (create) "Already have an account? Sign in" else "Create a new account")
                        }
                        Text("Your images are processed locally by the current tools.", fontSize = 12.sp, color = Color(0xFF64748B))
                        if (notice != null) {
                            Spacer(Modifier.height(8.dp))
                            Text(notice ?: "", color = Color(0xFF047857), fontSize = 12.sp)
                        }
                        if (error != null) {
                            Spacer(Modifier.height(8.dp))
                            Text(error ?: "", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                        }
                        if (busy) {
                            Spacer(Modifier.height(10.dp))
                            LinearProgressIndicator(Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        }
    }
}

private fun authMessage(error: Throwable): String {
    val text = error.message?.lowercase().orEmpty()
    return when {
        "already in use" in text -> "This email is already registered. Try signing in."
        "badly formatted" in text || "invalid email" in text -> "Please enter a valid email address."
        "wrong-password" in text || "invalid-credential" in text || "password is invalid" in text -> "The email or password is incorrect."
        "network" in text -> "Connection failed. Please check your internet and try again."
        "credential" in text -> "Google sign-in could not be completed. Please try again."
        else -> "We could not complete sign-in. Please try again."
    }
}

private fun selectedToolTitle(id: String, t: UiStrings): String {
    return when (id) {
        "resize" -> t.resize
        "compress" -> t.compress
        "convert" -> t.convert
        "crop" -> t.crop
        "rotate" -> t.rotate
        "filter" -> t.filters
        "info" -> t.details
        "watermark" -> t.watermark
        "adjust" -> t.adjust
        "collage" -> t.collage
        "frame" -> t.frame
        "meme" -> t.meme
        "pixelate" -> t.pixelate
        else -> id
    }
}

@Composable
private fun ModernHome(premium: Boolean, openTool: (String) -> Unit, t: UiStrings) {
    var query by remember { mutableStateOf("") }
    val results = tools.filter { (selectedToolTitle(it.id, t) + " " + it.subtitle).contains(query, true) }
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Box(
                Modifier.fillMaxWidth().height(205.dp).clip(RoundedCornerShape(30.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF07131D), Color(0xFF0D4156), Color(0xFF7B2FF7))))
                    .padding(22.dp)
            ) {
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = RoundedCornerShape(50), color = Color.White.copy(alpha = .14f)) {
                            Text(if (premium) t.premium else t.photo, color = Color.White, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        Surface(shape = RoundedCornerShape(50), color = Color.White.copy(alpha = .12f)) {
                            Text(tools.size.toString() + " tools", color = Color.White, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), fontSize = 10.sp)
                        }
                    }
                    Column {
                        Text(t.imageEditor, color = Color.White, fontSize = 29.sp, fontWeight = FontWeight.ExtraBold)
                        Text(
                            "A mobile editing workspace with real processing, previews and export.",
                            color = Color.White.copy(alpha = .80f),
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            OutlinedTextField(
                query,
                { query = it },
                Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                leadingIcon = { Icon(Icons.Default.Search, null) },
                placeholder = { Text("Search tools") }
            )
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column {
                Text(t.quickActions, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf("adjust","crop","compress","collage").forEach { id ->
                        val tool = tools.first { it.id == id }
                        Surface(
                            Modifier.weight(1f).height(92.dp).clickable { openTool(id) },
                            shape = RoundedCornerShape(20.dp),
                            color = tool.start.copy(alpha = .10f)
                        ) {
                            Column(Modifier.padding(11.dp), verticalArrangement = Arrangement.SpaceBetween) {
                                Icon(tool.icon, null, tint = tool.start)
                                Text(selectedToolTitle(id, t), maxLines = 1, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = tool.start)
                            }
                        }
                    }
                }
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(t.tools, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
        }
        if (results.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                EmptyCard("No tool found", "Try another search term.")
            }
        } else {
            gridItems(results) { ToolCard(it, openTool, premium, t) }
        }
        if (!premium) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFFF5EEFF)
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.WorkspacePremium, null, tint = Color(0xFF7C3AED))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Premium workspace", fontWeight = FontWeight.ExtraBold)
                            Text("Unlock advanced tools as the library grows.", fontSize = 12.sp, color = Color(0xFF6B7280))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModernTools(premium: Boolean, openTool: (String) -> Unit, t: UiStrings) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(t.tools, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
                Text("Choose a workflow. Each tool has its own editing workspace.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            }
        }
        gridItems(tools) { ToolCard(it, openTool, premium, t) }
    }
}

@Composable
private fun ToolCard(tool: Tool, openTool: (String) -> Unit, premium: Boolean, t: UiStrings) {
    val accent by animateColorAsState(tool.start, label = "toolAccent")
    val iconScale by animateFloatAsState(1f, animationSpec = spring(stiffness = 500f), label = "iconScale")
    val locked = tool.premiumOnly && !premium
    Card(
        onClick = { openTool(tool.id) },
        modifier = Modifier.fillMaxWidth().height(177.dp),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = tool.soft),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(Modifier.fillMaxSize().padding(15.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Box(
                    Modifier.size(58.dp).clip(RoundedCornerShape(19.dp))
                        .background(Brush.linearGradient(listOf(tool.start, tool.end))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(tool.icon, null, tint = Color.White, modifier = Modifier.size((28 * iconScale).dp))
                }
                if (locked) {
                    Surface(shape = RoundedCornerShape(50), color = Color(0xFF171126)) {
                        Row(Modifier.padding(horizontal = 8.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lock, null, tint = Color.White, modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("PRO", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
            Column {
                Text(selectedToolTitle(tool.id, t), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    tool.subtitle,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(50), color = accent.copy(alpha = .11f)) {
                    Text(
                        if (tool.premiumOnly) t.premium else t.free,
                        Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = accent
                    )
                }
                Icon(Icons.Default.ArrowForward, null, tint = accent)
            }
        }
    }
}

@Composable
private fun ModernToolWorkspace(id: String, premium: Boolean, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tool = tools.firstOrNull { it.id == id } ?: return
    val t = rememberStrings(rememberAppLanguage(context).value)

    if (tool.premiumOnly && !premium) {
        LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(30.dp),
                    color = tool.soft
                ) {
                    Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.size(76.dp).clip(RoundedCornerShape(24.dp)).background(tool.start), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Lock, null, tint = Color.White, modifier = Modifier.size(34.dp))
                        }
                        Spacer(Modifier.height(14.dp))
                        Text(selectedToolTitle(id, t), fontSize = 27.sp, fontWeight = FontWeight.ExtraBold)
                        Text(t.pro, color = Color(0xFFD81B42), fontWeight = FontWeight.ExtraBold)
                        Text("This tool is available with Premium.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text(t.home) }
                    }
                }
            }
        }
        return
    }

    var image by remember { mutableStateOf<Bitmap?>(null) }
    var sources by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var preview by remember { mutableStateOf<Bitmap?>(null) }
    var pendingBytes by remember { mutableStateOf<ByteArray?>(null) }
    var pendingFormat by remember { mutableStateOf(OutputFormat.JPEG) }
    var pendingWidth by remember { mutableIntStateOf(1) }
    var pendingHeight by remember { mutableIntStateOf(1) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }

    var width by remember { mutableStateOf("1080") }
    var height by remember { mutableStateOf("1080") }
    var keepRatio by remember { mutableStateOf(true) }
    var quality by remember { mutableFloatStateOf(82f) }
    var format by remember { mutableStateOf(OutputFormat.JPEG) }
    var cropRatio by remember { mutableStateOf("1:1") }
    var angle by remember { mutableIntStateOf(90) }
    var flipH by remember { mutableStateOf(false) }
    var flipV by remember { mutableStateOf(false) }
    var filter by remember { mutableStateOf(ImageFilter.ORIGINAL) }
    var brightness by remember { mutableFloatStateOf(0f) }
    var contrast by remember { mutableFloatStateOf(0f) }
    var saturation by remember { mutableFloatStateOf(1f) }
    var watermarkText by remember { mutableStateOf("IMAGE TOOLS") }
    var watermarkOpacity by remember { mutableFloatStateOf(72f) }
    var watermarkPosition by remember { mutableStateOf("Bottom right") }
    var border by remember { mutableFloatStateOf(24f) }
    var background by remember { mutableStateOf("White") }
    var topText by remember { mutableStateOf("") }
    var bottomText by remember { mutableStateOf("") }
    var pixelSize by remember { mutableFloatStateOf(12f) }
    var columns by remember { mutableIntStateOf(2) }

    fun processImage(transform: suspend () -> Bitmap, output: OutputFormat = format, outputQuality: Int = quality.toInt()) {
        scope.launch {
            busy = true
            status = t.processing
            try {
                val transformed = withContext(Dispatchers.Default) { transform() }
                val bytes = withContext(Dispatchers.Default) {
                    ImageProcessor.encode(
                        transformed,
                        output,
                        if (output == OutputFormat.PNG) 100 else outputQuality.coerceIn(1, 100)
                    )
                }
                preview = transformed
                pendingBytes = bytes
                pendingFormat = output
                pendingWidth = transformed.width
                pendingHeight = transformed.height
                status = t.ready + " • " + transformed.width + " × " + transformed.height
            } catch (e: Exception) {
                preview = null
                pendingBytes = null
                status = t.error
            } finally {
                busy = false
            }
        }
    }

    fun export() {
        val bytes = pendingBytes ?: return
        scope.launch {
            busy = true
            status = t.processing
            try {
                val saved = withContext(Dispatchers.IO) {
                    ImageProcessor.saveWithFallback(
                        context,
                        OutputFolderStore.getTreeUri(context),
                        bytes,
                        pendingFormat,
                        "image-tools-" + id,
                        pendingWidth,
                        pendingHeight
                    )
                }
                status = if (saved.usedDefaultGallery) t.defaultSaved else t.saved
            } catch (_: Exception) {
                status = t.saveFailed
            } finally {
                busy = false
            }
        }
    }

    val singlePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch(Dispatchers.IO) {
            val decoded = ImageProcessor.decode(context, uri)
            withContext(Dispatchers.Main) {
                image = decoded
                preview = null
                pendingBytes = null
                if (decoded != null) {
                    width = decoded.width.toString()
                    height = decoded.height.toString()
                    status = t.ready
                } else {
                    status = t.error
                }
            }
        }
    }

    val multiPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(6)) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        scope.launch(Dispatchers.IO) {
            val decoded = uris.mapNotNull { ImageProcessor.decode(context, it) }
            withContext(Dispatchers.Main) {
                sources = decoded
                image = decoded.firstOrNull()
                preview = null
                pendingBytes = null
                status = if (decoded.isEmpty()) t.error else t.ready
            }
        }
    }

    val current = preview ?: image

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Surface(shape = RoundedCornerShape(28.dp), color = tool.soft) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, t.home) }
                    Box(Modifier.size(56.dp).clip(RoundedCornerShape(20.dp)).background(Brush.linearGradient(listOf(tool.start, tool.end))), contentAlignment = Alignment.Center) {
                        Icon(tool.icon, null, tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(selectedToolTitle(id, t), fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
                        Text(tool.subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (premium && tool.premiumOnly) {
                        Surface(shape = RoundedCornerShape(50), color = Color(0xFF10A37F).copy(alpha = .12f)) {
                            Text(t.premium, Modifier.padding(horizontal = 9.dp, vertical = 6.dp), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0B8F6B))
                        }
                    }
                }
            }
        }

        item {
            when {
                current == null && id == "collage" -> {
                    Surface(
                        Modifier.fillMaxWidth().height(230.dp).clickable { multiPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        shape = RoundedCornerShape(30.dp),
                        color = Color(0xFF071C26)
                    ) {
                        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Icon(Icons.Default.Collections, null, tint = Color(0xFF00C6FF), modifier = Modifier.size(46.dp))
                            Spacer(Modifier.height(10.dp))
                            Text("Pick 2–6 images", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                            Text("Build one layout on the phone", color = Color.White.copy(alpha = .70f), fontSize = 12.sp)
                            Spacer(Modifier.height(13.dp))
                            Button(onClick = { multiPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) { Text(t.chooseImages) }
                        }
                    }
                }
                current == null -> {
                    Surface(
                        Modifier.fillMaxWidth().height(230.dp).clickable { singlePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        shape = RoundedCornerShape(30.dp),
                        color = Color(0xFF0E1426)
                    ) {
                        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Icon(Icons.Default.AddPhotoAlternate, null, tint = tool.start, modifier = Modifier.size(46.dp))
                            Spacer(Modifier.height(10.dp))
                            Text(t.chooseImage, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                            Text("Start with a photo and the editor will appear here.", color = Color.White.copy(alpha = .70f), fontSize = 12.sp)
                        }
                    }
                }
                else -> {
                    Surface(
                        shape = RoundedCornerShape(30.dp),
                        color = Color(0xFF090E19)
                    ) {
                        Box(Modifier.fillMaxWidth().heightIn(min = 250.dp, max = 390.dp).padding(10.dp), contentAlignment = Alignment.Center) {
                            Image(
                                current.asImageBitmap(),
                                null,
                                Modifier.fillMaxSize().clip(RoundedCornerShape(22.dp)),
                                contentScale = ContentScale.Fit
                            )
                            if (preview != null) {
                                Surface(
                                    Modifier.align(Alignment.TopStart).padding(10.dp),
                                    shape = RoundedCornerShape(50),
                                    color = Color(0xFF10A37F)
                                ) {
                                    Text(t.ready, Modifier.padding(horizontal = 10.dp, vertical = 6.dp), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            when (id) {
                "resize" -> ResizeStudio(width, height, keepRatio, { width = it }, { height = it }, { keepRatio = it })
                "compress" -> CompressStudio(quality, { quality = it })
                "convert" -> ConvertStudio(format, { format = it })
                "crop" -> CropStudio(cropRatio, { cropRatio = it })
                "rotate" -> RotateStudio(angle, { angle = it }, flipH, { flipH = it }, flipV, { flipV = it })
                "filter" -> FilterStudio(filter, { filter = it })
                "info" -> DetailsStudio(image, context)
                "watermark" -> WatermarkStudio(watermarkText, { watermarkText = it }, watermarkOpacity, { watermarkOpacity = it }, watermarkPosition, { watermarkPosition = it })
                "adjust" -> AdjustStudio(brightness, { brightness = it }, contrast, { contrast = it }, saturation, { saturation = it })
                "collage" -> CollageStudio(sources.size, columns, { columns = it }, background, { background = it }, { multiPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) })
                "frame" -> FrameStudio(border, { border = it }, background, { background = it })
                "meme" -> MemeStudio(topText, { topText = it }, bottomText, { bottomText = it })
                "pixelate" -> PixelateStudio(pixelSize, { pixelSize = it })
            }
        }

        item {
            when (id) {
                "info" -> Unit
                "collage" -> {
                    Button(
                        onClick = {
                            if (sources.size >= 2) {
                                processImage(
                                    { ImageProcessor.collage(sources, columns, background = frameColor(background)) },
                                    OutputFormat.JPEG,
                                    92
                                )
                            }
                        },
                        enabled = sources.size >= 2 && !busy,
                        modifier = Modifier.fillMaxWidth().height(57.dp),
                        shape = RoundedCornerShape(19.dp)
                    ) { Text(if (busy) t.processing else t.apply) }
                }
                else -> {
                    Button(
                        onClick = {
                            val source = image
                            if (source != null) {
                                when (id) {
                                    "resize" -> processImage({ ImageProcessor.resize(source, width.toIntOrNull() ?: source.width, height.toIntOrNull() ?: source.height) })
                                    "compress" -> processImage({ source }, OutputFormat.WEBP, quality.toInt())
                                    "convert" -> processImage({ source }, format, quality.toInt())
                                    "crop" -> processImage({ ImageProcessor.cropCenter(source, cropRatio) })
                                    "rotate" -> processImage({ ImageProcessor.rotate(source, angle, flipH, flipV) })
                                    "filter" -> processImage({ ImageProcessor.filter(source, filter) })
                                    "watermark" -> processImage({ ImageProcessor.watermark(source, watermarkText, watermarkOpacity.toInt(), watermarkPosition) })
                                    "adjust" -> processImage({ ImageProcessor.adjust(source, brightness, contrast, saturation) })
                                    "frame" -> processImage({ ImageProcessor.frame(source, border.toInt(), frameColor(background)) })
                                    "meme" -> processImage({ ImageProcessor.meme(source, topText, bottomText) })
                                    "pixelate" -> processImage({ ImageProcessor.pixelate(source, pixelSize.toInt()) })
                                }
                            }
                        },
                        enabled = image != null && !busy,
                        modifier = Modifier.fillMaxWidth().height(57.dp),
                        shape = RoundedCornerShape(19.dp)
                    ) {
                        if (busy) {
                            CircularProgressIndicator(Modifier.size(19.dp), strokeWidth = 2.dp, color = Color.White)
                            Spacer(Modifier.width(9.dp))
                        }
                        Text(if (busy) t.processing else t.apply)
                    }
                }
            }
        }

        if (status != null) {
            item {
                Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = .08f)) {
                    Text(status ?: "", Modifier.fillMaxWidth().padding(14.dp), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        if (preview != null && pendingBytes != null) {
            item {
                Surface(shape = RoundedCornerShape(25.dp), color = Color(0xFF0D1716)) {
                    Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text(t.export, color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
                                Text(
                                    pendingWidth.toString() + " × " + pendingHeight.toString() + " • " + pendingFormat.extension.uppercase(),
                                    color = Color.White.copy(alpha = .65f),
                                    fontSize = 12.sp
                                )
                            }
                            Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF10A37F))
                        }
                        Button(
                            onClick = { export() },
                            enabled = !busy,
                            modifier = Modifier.fillMaxWidth().height(55.dp),
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10A37F))
                        ) {
                            Icon(Icons.Default.SaveAlt, null)
                            Spacer(Modifier.width(8.dp))
                            Text(t.save, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResizeStudio(width: String, height: String, keep: Boolean, setWidth: (String) -> Unit, setHeight: (String) -> Unit, setKeep: (Boolean) -> Unit) {
    Surface(shape = RoundedCornerShape(26.dp), color = Color(0xFFEAF7FF)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Canvas size", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0067A7))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(width, { setWidth(it.filter(Char::isDigit)) }, Modifier.weight(1f), label = { Text("Width") }, singleLine = true)
                OutlinedTextField(height, { setHeight(it.filter(Char::isDigit)) }, Modifier.weight(1f), label = { Text("Height") }, singleLine = true)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(if (keep) Icons.Default.Link else Icons.Default.LinkOff, null, tint = Color(0xFF007CF0))
                Spacer(Modifier.width(9.dp))
                Text("Keep aspect ratio", Modifier.weight(1f), fontWeight = FontWeight.Bold)
                Switch(keep, setKeep)
            }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("720","1080","1440","2048").forEach {
                    AssistChip(
                        onClick = { setWidth(it) },
                        label = { Text(it + "px") }
                    )
                }
            }
        }
    }
}

@Composable
private fun CompressStudio(value: Float, setValue: (Float) -> Unit) {
    Surface(shape = RoundedCornerShape(30.dp), color = Color(0xFFEAFBF4)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Column { Text("Compression", fontSize = 21.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF087F5B)); Text("Smaller file, controlled quality", fontSize = 12.sp) }
                Text(value.toInt().toString() + "%", fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF087F5B))
            }
            Slider(value, setValue, valueRange = 20f..100f)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Smaller file", fontSize = 11.sp)
                Text("Better quality", fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun ConvertStudio(format: OutputFormat, setFormat: (OutputFormat) -> Unit) {
    Surface(shape = RoundedCornerShape(26.dp), color = Color(0xFFF4EEFF)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Choose output", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF7042C7))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutputFormat.entries.forEach {
                    Surface(
                        Modifier.weight(1f).height(92.dp).clickable { setFormat(it) },
                        shape = RoundedCornerShape(20.dp),
                        color = if (format == it) Color(0xFF7B2FF7) else Color.White
                    ) {
                        Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.SpaceBetween) {
                            Text(it.extension.uppercase(), color = if (format == it) Color.White else Color(0xFF7B2FF7), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                            Text(it.mime.removePrefix("image/"), color = if (format == it) Color.White.copy(alpha = .75f) else Color.Gray, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CropStudio(value: String, setValue: (String) -> Unit) {
    Surface(shape = RoundedCornerShape(26.dp), color = Color(0xFFFFF3E6)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
            Text("Crop for the moment", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFD96B00))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Original","1:1","4:5","3:4","4:3","16:9","9:16").forEach {
                    FilterChip(it == value, { setValue(it) }, label = { Text(it) })
                }
            }
            Text("1:1 square • 4:5 feed • 9:16 stories • 16:9 landscape", fontSize = 11.sp, color = Color(0xFF8A5A2B))
        }
    }
}

@Composable
private fun RotateStudio(angle: Int, setAngle: (Int) -> Unit, h: Boolean, setH: (Boolean) -> Unit, v: Boolean, setV: (Boolean) -> Unit) {
    Surface(shape = RoundedCornerShape(28.dp), color = Color(0xFFEEF0FF)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Transform", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF4C51BF))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                IconButton(onClick = { setAngle((angle + 90) % 360) }) { Icon(Icons.Default.Rotate90DegreesCw, null) }
                IconButton(onClick = { setH(!h) }) { Icon(Icons.Default.Flip, null) }
                IconButton(onClick = { setV(!v) }) { Icon(Icons.Default.SwapVert, null) }
            }
            Text("Angle " + angle.toString() + "°", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun FilterStudio(filter: ImageFilter, setFilter: (ImageFilter) -> Unit) {
    Surface(shape = RoundedCornerShape(26.dp), color = Color(0xFFE9FFF4)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
            Text("Looks", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF138A63))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ImageFilter.entries.forEach {
                    FilterChip(it == filter, { setFilter(it) }, label = { Text(it.label) })
                }
            }
        }
    }
}

@Composable
private fun DetailsStudio(image: Bitmap?, context: android.content.Context) {
    Surface(shape = RoundedCornerShape(28.dp), color = Color(0xFFF1F8FB)) {
        Column(Modifier.padding(19.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Image profile", fontSize = 21.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF236B7D))
            if (image == null) {
                Text("Choose an image to inspect it.")
            } else {
                Text(image.width.toString() + " × " + image.height.toString(), fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                Text((image.width * image.height / 1000000f).toString() + " MP", color = Color(0xFF236B7D))
                HorizontalDivider()
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Config"); Text(image.config?.name ?: "Unknown", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun WatermarkStudio(text: String, setText: (String) -> Unit, opacity: Float, setOpacity: (Float) -> Unit, position: String, setPosition: (String) -> Unit) {
    Surface(shape = RoundedCornerShape(28.dp), color = Color(0xFFFFEDF2)) {
        Column(Modifier.padding(19.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Brand mark", fontSize = 21.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFD6336C))
            OutlinedTextField(text, setText, Modifier.fillMaxWidth(), label = { Text("Watermark text") }, singleLine = true)
            Text("Opacity " + opacity.toInt().toString() + "%", fontWeight = FontWeight.Bold)
            Slider(opacity, setOpacity, valueRange = 15f..100f)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                listOf("Top left","Top right","Center","Bottom left","Bottom right").forEach {
                    FilterChip(it == position, { setPosition(it) }, label = { Text(it) })
                }
            }
        }
    }
}

@Composable
private fun AdjustStudio(brightness: Float, setBrightness: (Float) -> Unit, contrast: Float, setContrast: (Float) -> Unit, saturation: Float, setSaturation: (Float) -> Unit) {
    Surface(shape = RoundedCornerShape(30.dp), color = Color(0xFFFFEEF1)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Tune the image", fontSize = 21.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFD6405B))
            AdjustLine("Brightness", brightness, -1f..1f, setBrightness)
            AdjustLine("Contrast", contrast, -1f..1f, setContrast)
            AdjustLine("Saturation", saturation, 0f..2f, setSaturation)
        }
    }
}

@Composable
private fun AdjustLine(label: String, value: Float, range: ClosedFloatingPointRange<Float>, setValue: (Float) -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontWeight = FontWeight.Bold)
            Text("%.2f".format(value))
        }
        Slider(value, setValue, valueRange = range)
    }
}

@Composable
private fun CollageStudio(count: Int, columns: Int, setColumns: (Int) -> Unit, background: String, setBackground: (String) -> Unit, chooseMany: () -> Unit) {
    Surface(shape = RoundedCornerShape(30.dp), color = Color(0xFFEAF9FF)) {
        Column(Modifier.padding(19.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Collage board", fontSize = 21.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0078B7))
            Text(count.toString() + " images selected", color = Color(0xFF4C6A78))
            Button(onClick = chooseMany, modifier = Modifier.fillMaxWidth()) { Text("Choose 2–6 photos") }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (1..3).forEach { c -> FilterChip(c == columns, { setColumns(c) }, label = { Text(c.toString() + " columns") }) }
            }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("White","Black","Soft").forEach { b -> FilterChip(b == background, { setBackground(b) }, label = { Text(b) }) }
            }
        }
    }
}

@Composable
private fun FrameStudio(border: Float, setBorder: (Float) -> Unit, background: String, setBackground: (String) -> Unit) {
    Surface(shape = RoundedCornerShape(28.dp), color = Color(0xFFF5F0FF)) {
        Column(Modifier.padding(19.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
            Text("Frame maker", fontSize = 21.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF7752C5))
            Text("Border " + border.toInt().toString() + " px", fontWeight = FontWeight.Bold)
            Slider(border, setBorder, valueRange = 4f..120f)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("White","Black","Soft").forEach { b -> FilterChip(b == background, { setBackground(b) }, label = { Text(b) }) }
            }
        }
    }
}

@Composable
private fun MemeStudio(top: String, setTop: (String) -> Unit, bottom: String, setBottom: (String) -> Unit) {
    Surface(shape = RoundedCornerShape(27.dp), color = Color(0xFFFFF7DE)) {
        Column(Modifier.padding(19.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
            Text("Meme canvas", fontSize = 21.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFD38A00))
            OutlinedTextField(top, setTop, Modifier.fillMaxWidth(), label = { Text("Top text") })
            OutlinedTextField(bottom, setBottom, Modifier.fillMaxWidth(), label = { Text("Bottom text") })
            Text("Classic high-contrast caption layout.", fontSize = 11.sp, color = Color(0xFF7A6B49))
        }
    }
}

@Composable
private fun PixelateStudio(size: Float, setSize: (Float) -> Unit) {
    Surface(shape = RoundedCornerShape(30.dp), color = Color(0xFFF1EDFF)) {
        Column(Modifier.padding(19.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
            Text("Pixel lab", fontSize = 21.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF7138C6))
            Text("Block size " + size.toInt().toString(), fontWeight = FontWeight.Bold)
            Slider(size, setSize, valueRange = 2f..48f)
            Text("Higher blocks create a stronger pixel-art effect.", fontSize = 11.sp, color = Color(0xFF665E72))
        }
    }
}

private fun frameColor(value: String): Int {
    return when (value) {
        "Black" -> android.graphics.Color.BLACK
        "Soft" -> android.graphics.Color.rgb(245, 245, 250)
        else -> android.graphics.Color.WHITE
    }
}

@Composable
private fun WorkspaceHeader(tool: Tool, onBack: () -> Unit) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = tool.soft),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "Back")
            }
            Box(
                Modifier.size(56.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brush.linearGradient(listOf(tool.start, tool.end))),
                contentAlignment = Alignment.Center
            ) {
                Icon(tool.icon, null, tint = Color.White, modifier = Modifier.size(29.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(tool.title, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
                Text(tool.subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Surface(
                shape = RoundedCornerShape(50),
                color = if (tool.premiumOnly) Color(0xFF171126) else tool.start.copy(alpha = 0.10f)
            ) {
                Text(
                    if (tool.premiumOnly) "PRO" else "LOCAL",
                    Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                    color = if (tool.premiumOnly) Color.White else tool.start,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

@Composable
private fun ResizePanel(
    width: String,
    height: String,
    lock: Boolean,
    onWidth: (String) -> Unit,
    onHeight: (String) -> Unit,
    onLock: (Boolean) -> Unit
) {
    ControlPanel("Precision resize", "Tune the canvas while preserving proportions when needed.") {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            DimensionField("WIDTH", width, onWidth, Modifier.weight(1f))
            DimensionField("HEIGHT", height, onHeight, Modifier.weight(1f))
        }
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFFF4F6FA)
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (lock) Icons.Default.Link else Icons.Default.LinkOff,
                    null,
                    tint = if (lock) Color(0xFF2563EB) else Color(0xFF64748B)
                )
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text("Keep aspect ratio", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("Avoid stretched images", fontSize = 11.sp, color = Color(0xFF64748B))
                }
                Switch(lock, onLock)
            }
        }
        Surface(shape = RoundedCornerShape(14.dp), color = Color(0xFFEFF6FF)) {
            Text(
                "Output  •  " + width + " × " + height + " px",
                Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                color = Color(0xFF1D4ED8),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun DimensionField(
    label: String,
    value: String,
    onValue: (String) -> Unit,
    modifier: Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValue(it.filter(Char::isDigit)) },
        modifier = modifier,
        label = { Text(label) },
        suffix = { Text("px") },
        singleLine = true,
        shape = RoundedCornerShape(18.dp)
    )
}

@Composable
private fun CompressPanel(
    quality: Float,
    format: OutputFormat,
    onQuality: (Float) -> Unit,
    onFormat: (OutputFormat) -> Unit
) {
    ControlPanel("Compression lab", "Balance visual quality and a smaller final file.") {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Quality", fontSize = 12.sp, color = Color(0xFF64748B))
                Text(quality.toInt().toString() + "%", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
            }
            Surface(
                shape = RoundedCornerShape(50),
                color = when {
                    quality <= 40f -> Color(0xFFFEF2F2)
                    quality <= 70f -> Color(0xFFFFF7ED)
                    else -> Color(0xFFECFDF5)
                }
            ) {
                Text(
                    when {
                        quality <= 40f -> "Small file"
                        quality <= 70f -> "Balanced"
                        else -> "High quality"
                    },
                    Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        LinearProgressIndicator(
            progress = { quality / 100f },
            modifier = Modifier.fillMaxWidth().height(9.dp).clip(RoundedCornerShape(50))
        )
        Slider(quality, onQuality, valueRange = 10f..100f)
        Text("Output format", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            listOf(OutputFormat.JPEG, OutputFormat.WEBP).forEach { option ->
                FormatChoice(
                    option.label,
                    option.extension.uppercase(),
                    format == option,
                    Modifier.weight(1f)
                ) { onFormat(option) }
            }
        }
    }
}

@Composable
private fun FormatChoice(
    title: String,
    ext: String,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) Color(0xFFEEF2FF) else Color(0xFFF8FAFC)
        ),
        border = if (selected) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF6366F1)) else null
    ) {
        Column(Modifier.padding(13.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(ext, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = if (selected) Color(0xFF4F46E5) else Color(0xFF334155))
            Spacer(Modifier.height(4.dp))
            Text(title, fontSize = 11.sp, color = Color(0xFF64748B))
        }
    }
}

@Composable
private fun FormatPanel(format: OutputFormat, onFormat: (OutputFormat) -> Unit) {
    ControlPanel("Format studio", "Choose the right export format for your next destination.") {
        OutputFormat.values().forEach { option ->
            Card(
                onClick = { onFormat(option) },
                shape = RoundedCornerShape(19.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (format == option) Color(0xFFF5F3FF) else Color(0xFFF8FAFC)
                ),
                border = if (format == option) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF8B5CF6)) else null
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(44.dp).clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.linearGradient(
                                    when (option) {
                                        OutputFormat.JPEG -> listOf(Color(0xFF2563EB), Color(0xFF06B6D4))
                                        OutputFormat.PNG -> listOf(Color(0xFF7C3AED), Color(0xFFEC4899))
                                        OutputFormat.WEBP -> listOf(Color(0xFF059669), Color(0xFF22C55E))
                                    }
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(option.extension.uppercase(), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(option.label, fontWeight = FontWeight.ExtraBold)
                        Text(option.mime, fontSize = 11.sp, color = Color(0xFF64748B))
                    }
                    if (format == option) {
                        Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF7C3AED))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun CropPanel(ratio: String, onRatio: (String) -> Unit) {
    ControlPanel("Smart crop", "Pick a composition ratio before creating the result.") {
        val options = listOf("Original", "1:1", "4:5", "16:9", "9:16")
        options.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                row.forEach { option ->
                    CropChoice(option, ratio == option, Modifier.weight(1f)) { onRatio(option) }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun CropChoice(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = if (selected) Color(0xFFFFF7ED) else Color(0xFFF8FAFC)),
        border = if (selected) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B)) else null
    ) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            val w = when (label) { "9:16" -> 18.dp; "4:5" -> 28.dp; "1:1" -> 34.dp; else -> 42.dp }
            val h = when (label) { "16:9" -> 22.dp; "4:5" -> 34.dp; "9:16" -> 42.dp; "1:1" -> 34.dp; else -> 28.dp }
            Box(
                Modifier.size(width = w, height = h).clip(RoundedCornerShape(7.dp))
                    .background(if (selected) Color(0xFFF59E0B) else Color(0xFFCBD5E1))
            )
            Spacer(Modifier.height(8.dp))
            Text(label, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }
    }
}

@Composable
private fun RotatePanel(
    angle: Int,
    h: Boolean,
    v: Boolean,
    onAngle: (Int) -> Unit,
    onH: (Boolean) -> Unit,
    onV: (Boolean) -> Unit
) {
    ControlPanel("Transform studio", "Straighten, rotate or mirror before export.") {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Box(
                Modifier.size(92.dp).clip(RoundedCornerShape(24.dp)).background(Color(0xFFF1F5F9)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Image, null, modifier = Modifier.size(50.dp).rotate(angle.toFloat()), tint = Color(0xFF4F46E5))
                Text(angle.toString() + "°", Modifier.align(Alignment.BottomCenter).padding(bottom = 9.dp), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF4F46E5))
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(90, 180, 270).forEach { value ->
                FilterChip(angle == value, { onAngle(value) }, label = { Text(value.toString() + "°") }, modifier = Modifier.weight(1f))
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(h, { onH(!h) }, label = { Text("Mirror H") }, modifier = Modifier.weight(1f))
            FilterChip(v, { onV(!v) }, label = { Text("Mirror V") }, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun FilterPanel(filter: ImageFilter, onFilter: (ImageFilter) -> Unit) {
    ControlPanel("Quick filters", "Preview-ready presets for fast visual changes.") {
        val options = ImageFilter.values().toList()
        options.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                row.forEach { option ->
                    Card(
                        onClick = { onFilter(option) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = if (filter == option) Color(0xFFECFDF5) else Color(0xFFF8FAFC)),
                        border = if (filter == option) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981)) else null
                    ) {
                        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                Modifier.size(44.dp).clip(RoundedCornerShape(14.dp))
                                    .background(
                                        when (option) {
                                            ImageFilter.ORIGINAL -> Color(0xFFE2E8F0)
                                            ImageFilter.GRAYSCALE -> Color(0xFF64748B)
                                            ImageFilter.SEPIA -> Color(0xFFB7791F)
                                            ImageFilter.HIGH_CONTRAST -> Color(0xFF111827)
                                        }
                                    )
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(option.label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun WatermarkPanel(
    text: String,
    opacity: Float,
    position: String,
    onText: (String) -> Unit,
    onOpacity: (Float) -> Unit,
    onPosition: (String) -> Unit
) {
    ControlPanel("Smart Watermark", "A polished brand mark for creators and product images.") {
        OutlinedTextField(
            value = text,
            onValueChange = onText,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Watermark text") },
            singleLine = true,
            shape = RoundedCornerShape(16.dp)
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Opacity", fontWeight = FontWeight.Bold)
            Text(opacity.toInt().toString() + "%", color = Color(0xFF7C3AED), fontWeight = FontWeight.ExtraBold)
        }
        Slider(opacity, onOpacity, valueRange = 15f..100f)
        Text("Placement", fontWeight = FontWeight.Bold)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            listOf("Top left", "Center", "Bottom left", "Bottom right").forEach { item ->
                FilterChip(position == item, { onPosition(item) }, label = { Text(item) })
            }
        }
    }
}

@Composable
private fun ControlPanel(title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    Card(shape = RoundedCornerShape(26.dp), elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
            Text(title, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            HorizontalDivider()
            content()
        }
    }
}

@Composable
private fun InfoPanel(image: Bitmap?, bytes: Long?) {
    Card(shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Image details", fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
            if (image == null) {
                Text("Choose an image to inspect it.")
            } else {
                InfoLine("Resolution", image.width.toString() + " × " + image.height)
                InfoLine("File size", bytes?.let { ImageProcessor.humanBytes(it) } ?: "Not available")
                InfoLine("Processing", "On this device")
            }
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ModernAccount(auth: AuthRepository, premium: Boolean, openPremium: () -> Unit) {
    val user = auth.currentUser ?: return
    var name by remember(user.uid) { mutableStateOf(user.displayName ?: "") }
    var saving by remember { mutableStateOf(false) }
    var verified by remember(user.uid) { mutableStateOf(user.isEmailVerified) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val usesPassword = user.providerData.any { it.providerId == "password" }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Account", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
            Text("Your profile, plan and security in one place.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Card(shape = RoundedCornerShape(28.dp)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(64.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .background(Brush.linearGradient(listOf(Color(0xFF7B2FF7), Color(0xFF00B9F2)))),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                (name.ifBlank { user.email ?: "U" }).first().uppercase(),
                                color = Color.White,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(name.ifBlank { "Image Tools User" }, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                            Text(user.email ?: "", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    OutlinedTextField(
                        name,
                        { name = it },
                        Modifier.fillMaxWidth(),
                        label = { Text("Display name") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp)
                    )
                    Button(
                        onClick = {
                            saving = true
                            status = null
                            scope.launch {
                                auth.updateDisplayName(name)
                                    .onSuccess { status = "Profile updated." }
                                    .onFailure { status = authMessage(it) }
                                saving = false
                            }
                        },
                        enabled = !saving,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (saving) "Saving…" else "Save profile")
                    }
                    if (status != null) {
                        Text(
                            status ?: "",
                            color = if (status == "Profile updated.") Color(0xFF047857) else MaterialTheme.colorScheme.error,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        item {
            Card(shape = RoundedCornerShape(24.dp)) {
                ListItem(
                    headlineContent = { Text(if (premium) "Premium active" else "Free plan", fontWeight = FontWeight.Bold) },
                    supportingContent = {
                        Text(
                            if (premium) "Premium is active on this account."
                            else "Upgrade for US$5/month as the premium library grows."
                        )
                    },
                    leadingContent = { Icon(if (premium) Icons.Default.WorkspacePremium else Icons.Default.LockOpen, null) },
                    trailingContent = {
                        if (!premium) Button(onClick = openPremium) { Text("Upgrade") }
                    }
                )
            }
        }

        if (usesPassword) {
            item {
                Card(shape = RoundedCornerShape(24.dp)) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (verified) Icons.Default.MarkEmailRead else Icons.Default.MarkEmailUnread,
                                null,
                                tint = if (verified) Color(0xFF159A63) else Color(0xFFC2410C)
                            )
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    if (verified) "Email verified" else "Email not verified",
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    if (verified) "Your email address is confirmed."
                                    else "Confirm your email to keep your account secure.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (!verified) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        busy = true
                                        status = null
                                        scope.launch {
                                            auth.sendEmailVerification()
                                                .onSuccess { status = "Verification email sent." }
                                                .onFailure { status = authMessage(it) }
                                            busy = false
                                        }
                                    },
                                    enabled = !busy,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(if (busy) "Sending…" else "Send email")
                                }
                                OutlinedButton(
                                    onClick = {
                                        busy = true
                                        scope.launch {
                                            auth.reloadCurrentUser()
                                                .onSuccess {
                                                    verified = auth.currentUser?.isEmailVerified == true
                                                    status = if (verified) "Email verified." else "Email is still not verified."
                                                }
                                                .onFailure { status = authMessage(it) }
                                            busy = false
                                        }
                                    },
                                    enabled = !busy,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Refresh")
                                }
                            }
                        }
                    }
                }
            }

            item {
                Card(shape = RoundedCornerShape(24.dp)) {
                    ListItem(
                        headlineContent = { Text("Password recovery", fontWeight = FontWeight.Bold) },
                        supportingContent = { Text("Send a secure password reset link to " + (user.email ?: "your email") + ".") },
                        leadingContent = { Icon(Icons.Default.Password, null) },
                        trailingContent = {
                            TextButton(
                                onClick = {
                                    busy = true
                                    status = null
                                    scope.launch {
                                        auth.sendPasswordReset(user.email.orEmpty())
                                            .onSuccess { status = "Password reset email sent." }
                                            .onFailure { status = authMessage(it) }
                                        busy = false
                                    }
                                },
                                enabled = !busy
                            ) {
                                Text("Send")
                            }
                        }
                    )
                }
            }
        } else {
            item {
                Card(shape = RoundedCornerShape(24.dp)) {
                    ListItem(
                        headlineContent = { Text("Google account", fontWeight = FontWeight.Bold) },
                        supportingContent = { Text("Your identity is managed securely by Google Sign-In.") },
                        leadingContent = { Icon(Icons.Default.AccountCircle, null) },
                        trailingContent = { Icon(Icons.Default.VerifiedUser, null, tint = Color(0xFF159A63)) }
                    )
                }
            }
        }

        item {
            Card(shape = RoundedCornerShape(24.dp)) {
                ListItem(
                    headlineContent = { Text("Account security", fontWeight = FontWeight.Bold) },
                    supportingContent = { Text("Authentication is handled securely by Firebase Authentication.") },
                    leadingContent = { Icon(Icons.Default.Security, null) },
                    trailingContent = { Icon(Icons.Default.VerifiedUser, null, tint = Color(0xFF159A63)) }
                )
            }
        }

        item {
            OutlinedButton(
                onClick = { auth.signOut() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Sign out")
            }
        }
    }
}

@Composable
private fun ModernPremium(
    auth: AuthRepository,
    premium: Boolean,
    onStartPayment: (String) -> Unit,
    onCancelSubscription: () -> Unit
) {
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(30.dp)).background(Brush.linearGradient(listOf(Color(0xFF171126), Color(0xFF7B2FF7), Color(0xFFF107A3)))).padding(24.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Premium", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
                    Text("More power with a simple monthly plan.", color = Color.White.copy(alpha = 0.82f))
                    Text("US$5 / month", color = Color.White, fontSize = 38.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Recurring subscription • Cancel anytime", color = Color.White.copy(alpha = 0.82f), fontSize = 12.sp)
                }
            }
        }
        item {
            Card(shape = RoundedCornerShape(26.dp)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
                    PremiumLine("Growing library of advanced workflows")
                    PremiumLine("Premium features as the app expands")
                    PremiumLine("Secure monthly account entitlement")
                    PremiumLine("US$5 billed every month by PayPal")
                    Button(
                        onClick = {
                            busy = true
                            error = null
                            scope.launch {
                                val token = auth.idToken()
                                if (token.isNullOrBlank()) {
                                    error = "Please sign in again and retry."
                                } else {
                                    PaymentRepository.createSubscription(token)
                                        .onSuccess { (_, approveUrl) -> onStartPayment(approveUrl) }
                                        .onFailure { failure ->
                                            error = when (failure) {
                                                is PaymentException -> when (failure.code.uppercase()) {
                                                    "PERMISSION_DENIED", "NOT_AUTHORIZED" ->
                                                        "PayPal subscriptions are not enabled for this payment account yet."
                                                    "INVALID_RESOURCE_ID", "RESOURCE_NOT_FOUND" ->
                                                        "The premium subscription plan is not available yet. Please try again shortly."
                                                    "INVALID_REQUEST" ->
                                                        "PayPal rejected the subscription setup. Please try again."
                                                    else ->
                                                        "Subscription could not be started. Please try again."
                                                }
                                                else -> "Subscription could not be started. Please try again."
                                            }
                                        }
                                }
                                busy = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !premium && !busy
                    ) {
                        Text(if (premium) "Premium active" else if (busy) "Opening PayPal…" else "Subscribe for US$5/month")
                    }
                    if (premium) {
                        OutlinedButton(
                            onClick = onCancelSubscription,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Cancel subscription")
                        }
                    }
                    if (error != null) Text(error ?: "", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun PremiumLine(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF159A63), modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(text)
    }
}

@Composable
private fun ModernSettings(
    darkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    language: MutableState<AppLanguage>,
    t: UiStrings
) {
    val context = LocalContext.current
    var outputFolderUri by remember { mutableStateOf(OutputFolderStore.getTreeUri(context)) }
    val folderLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            OutputFolderStore.saveTreeUri(context, uri)
            outputFolderUri = uri
        }
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(t.settings, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
            Text("Make the app feel like yours.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Surface(shape = RoundedCornerShape(25.dp), color = Color(0xFFEAF7FF)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(50.dp).clip(RoundedCornerShape(17.dp)).background(Brush.linearGradient(listOf(Color(0xFF00C6FF), Color(0xFF7B2FF7)))), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.FolderSpecial, null, tint = Color.White)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(t.outputFolder, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                            Text(
                                OutputFolderStore.folderName(context, outputFolderUri) ?: t.defaultSaved,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Text(
                        "Exports automatically fall back to Pictures / Image Tools when a chosen folder is unavailable.",
                        fontSize = 12.sp,
                        color = Color(0xFF256B82)
                    )
                    Button(onClick = { folderLauncher.launch(outputFolderUri) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.FolderOpen, null)
                        Spacer(Modifier.width(8.dp))
                        Text(t.changeFolder)
                    }
                }
            }
        }
        item {
            Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .55f)) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DarkMode, null)
                    Spacer(Modifier.width(12.dp))
                    Text(t.darkMode, Modifier.weight(1f), fontWeight = FontWeight.Bold)
                    Switch(darkMode, onDarkModeChange)
                }
            }
        }
        item {
            Surface(shape = RoundedCornerShape(22.dp), color = Color(0xFF10A37F).copy(alpha = .08f)) {
                ListItem(
                    headlineContent = { Text(t.localProcessing, fontWeight = FontWeight.Bold) },
                    supportingContent = { Text("Image transformations run on the device with the current tools.") },
                    leadingContent = { Icon(Icons.Default.Security, null, tint = Color(0xFF10A37F)) }
                )
            }
        }
        item { Text(t.language, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold) }
        items(AppLanguage.entries.toList()) { item ->
            Surface(
                Modifier.fillMaxWidth().clickable { language.value = item },
                shape = RoundedCornerShape(18.dp),
                color = if (language.value == item) MaterialTheme.colorScheme.primary.copy(alpha = .10f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .45f)
            ) {
                Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(item.nativeName, fontWeight = if (language.value == item) FontWeight.ExtraBold else FontWeight.Medium)
                        Text(languageTag(item), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (language.value == item) Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun ModernAbout() {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text("About", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
            Text("Image Tools " + BuildConfig.VERSION_NAME, fontWeight = FontWeight.Bold)
            Text("A mobile-first image workspace with local processing and a safer export flow.")
        }
        item {
            Card(shape = RoundedCornerShape(24.dp)) {
                ListItem(
                    headlineContent = { Text("Local processing", fontWeight = FontWeight.Bold) },
                    supportingContent = { Text("Core transformations are performed on the device.") },
                    leadingContent = { Icon(Icons.Default.Security, null) }
                )
            }
        }
        item {
            Card(shape = RoundedCornerShape(24.dp)) {
                ListItem(
                    headlineContent = { Text("Export recovery", fontWeight = FontWeight.Bold) },
                    supportingContent = { Text("When a custom folder becomes unavailable, the app falls back to Pictures / Image Tools instead of stopping the export.") },
                    leadingContent = { Icon(Icons.Default.SaveAlt, null) }
                )
            }
        }
    }
}

@Composable
private fun EmptyCard(title: String, body: String) {
    Card(shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.SearchOff, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            Text(title, fontWeight = FontWeight.Bold)
            Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}