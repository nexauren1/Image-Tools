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
import com.nexauren.imagetools.data.FirestoreRepository
import com.nexauren.imagetools.data.PaymentException
import com.nexauren.imagetools.data.PaymentRepository
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
    Tool("crop", "Smart Crop", "Common ratios for social and web", Icons.Default.Crop, Color(0xFFFF8008), Color(0xFFFFC837), Color(0xFFFFF6E7)),
    Tool("rotate", "Rotate & Flip", "Straighten or mirror instantly", Icons.Default.Rotate90DegreesCw, Color(0xFF4F46E5), Color(0xFFEC4899), Color(0xFFF1EFFF)),
    Tool("filter", "Quick Filters", "Clean presets for everyday photos", Icons.Default.FilterVintage, Color(0xFF11998E), Color(0xFF38EF7D), Color(0xFFE9FFF4)),
    Tool("info", "Image Details", "Check dimensions and file size", Icons.Default.Info, Color(0xFF334155), Color(0xFF06B6D4), Color(0xFFEEF7F9)),
    Tool("watermark", "Smart Watermark", "Brand images with a clean custom mark", Icons.Default.TextFields, Color(0xFF7C3AED), Color(0xFFEC4899), Color(0xFFF7EEFF), premiumOnly = true)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageToolsAppV2(
    auth: AuthRepository,
    firestore: FirestoreRepository,
    premium: Boolean,
    darkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    onStartPayment: (String) -> Unit,
    onCancelSubscription: () -> Unit
) {
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
            ModalDrawerSheet {
                Spacer(Modifier.height(24.dp))
                Text("IMAGE TOOLS", Modifier.padding(horizontal = 24.dp), fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
                Text(
                    if (premium) "Premium workspace" else "Free workspace",
                    Modifier.padding(start = 24.dp, top = 4.dp, bottom = 18.dp),
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 12.sp
                )
                DrawerEntry(Icons.Default.Home, "Home") { page = "home"; selectedTool = null; scope.launch { drawer.close() } }
                DrawerEntry(Icons.Default.Build, "All tools") { page = "tools"; selectedTool = null; scope.launch { drawer.close() } }
                DrawerEntry(Icons.Default.WorkspacePremium, "Premium") { page = "premium"; selectedTool = null; scope.launch { drawer.close() } }
                DrawerEntry(Icons.Default.Person, "Account") { page = "account"; selectedTool = null; scope.launch { drawer.close() } }
                DrawerEntry(Icons.Default.Settings, "Settings") { page = "settings"; selectedTool = null; scope.launch { drawer.close() } }
                DrawerEntry(Icons.Default.Info, "About") { page = "about"; selectedTool = null; scope.launch { drawer.close() } }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("Image Tools", fontWeight = FontWeight.ExtraBold)
                            Text(
                                if (premium) "Premium workspace" else "Private image workspace",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawer.open() } }) {
                            Icon(Icons.Default.Menu, "Menu")
                        }
                    },
                    actions = {
                        IconButton(onClick = { page = "premium"; selectedTool = null }) {
                            Icon(Icons.Default.AutoAwesome, "Premium")
                        }
                    }
                )
            },
            bottomBar = {
                NavigationBar {
                    BottomEntry("home", Icons.Default.Home, "Home", page) { page = "home"; selectedTool = null }
                    BottomEntry("tools", Icons.Default.Build, "Tools", page) { page = "tools"; selectedTool = null }
                    BottomEntry("account", Icons.Default.Person, "Account", page) { page = "account"; selectedTool = null }
                }
            }
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                AnimatedContent(targetState = selectedTool ?: page, label = "navigation") { target ->
                    when (target) {
                        "home" -> ModernHome(premium) {
                            val selected = tools.firstOrNull { tool -> tool.id == it }
                            if (selected?.premiumOnly == true && !premium) page = "premium" else selectedTool = it
                        }
                        "tools" -> ModernTools {
                            val selected = tools.firstOrNull { tool -> tool.id == it }
                            if (selected?.premiumOnly == true && !premium) page = "premium" else selectedTool = it
                        }
                        "account" -> ModernAccount(auth, firestore, premium) { page = "premium" }
                        "premium" -> ModernPremium(auth, premium, onStartPayment, onCancelSubscription)
                        "settings" -> ModernSettings(darkMode, onDarkModeChange)
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
                                scope.launch {
                                    auth.signInGoogle(BuildConfig.GOOGLE_WEB_CLIENT_ID).onFailure { error = authMessage(it) }
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
                        OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth(), label = { Text("Email") }, singleLine = true, shape = RoundedCornerShape(16.dp))
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
                        Spacer(Modifier.height(14.dp))
                        Button(
                            onClick = {
                                busy = true
                                error = null
                                scope.launch {
                                    val result = if (create) auth.registerEmail(email, password) else auth.signInEmail(email, password)
                                    result.onFailure { error = authMessage(it) }
                                    busy = false
                                }
                            },
                            enabled = !busy && email.isNotBlank() && password.length >= 6,
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(17.dp)
                        ) {
                            Text(if (create) "Create account" else "Sign in", fontWeight = FontWeight.Bold)
                        }
                        TextButton(onClick = { create = !create; error = null }, Modifier.fillMaxWidth()) {
                            Text(if (create) "Already have an account? Sign in" else "Create a new account")
                        }
                        Text("Your images are processed locally by the current tools.", fontSize = 12.sp, color = Color(0xFF64748B))
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

@Composable
private fun ModernHome(premium: Boolean, openTool: (String) -> Unit) {
    var query by remember { mutableStateOf("") }
    val results = tools.filter { (it.title + " " + it.subtitle).contains(query, true) }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(30.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF0B1022), Color(0xFF6D28D9), Color(0xFF06B6D4))
                        )
                    )
                    .padding(22.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text("Your creative toolbox", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                    Text(
                        "Fast local editing with a sharper, more visual workflow.",
                        color = Color.White.copy(alpha = 0.82f)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(shape = RoundedCornerShape(50), color = Color.White.copy(alpha = 0.14f)) {
                            Text("ON-DEVICE", Modifier.padding(horizontal = 10.dp, vertical = 6.dp), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        Surface(shape = RoundedCornerShape(50), color = Color.White.copy(alpha = 0.14f)) {
                            Text("${tools.size} TOOLS", Modifier.padding(horizontal = 10.dp, vertical = 6.dp), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
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
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Column {
                    Text("Tools", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Pick a workflow and start editing.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (premium) {
                    AssistChip(onClick = {}, label = { Text("PREMIUM ACTIVE") }, leadingIcon = { Icon(Icons.Default.WorkspacePremium, null) })
                }
            }
        }
        if (results.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                EmptyCard("No tool found", "Try another search term.")
            }
        } else {
            gridItems(results) { ToolCard(it, openTool) }
        }
        if (!premium) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5EEFF)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Row(Modifier.padding(17.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(
                                Brush.linearGradient(listOf(Color(0xFF7C3AED), Color(0xFFEC4899)))
                            ),
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Default.WorkspacePremium, null, tint = Color.White) }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Unlock Smart Watermark", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                            Text("Brand your images with a polished custom mark.", fontSize = 12.sp, color = Color(0xFF6B7280))
                        }
                        Text("US$5", fontWeight = FontWeight.ExtraBold, color = Color(0xFF7C3AED))
                    }
                }
            }
        }
    }
}

@Composable
private fun ModernTools(openTool: (String) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("All tools", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
                Text("Modern image workflows, ready when you are.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            }
        }
        gridItems(tools) { ToolCard(it, openTool) }
    }
}

@Composable
private fun ToolCard(tool: Tool, openTool: (String) -> Unit) {
    val accent by animateColorAsState(tool.start, label = "toolAccent")
    val iconScale by animateFloatAsState(1f, animationSpec = spring(stiffness = 500f), label = "iconScale")

    Card(
        onClick = { openTool(tool.id) },
        modifier = Modifier.fillMaxWidth().height(184.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = tool.soft),
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
    ) {
        Box(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 18.dp, y = (-22).dp)
                    .size(96.dp)
                    .clip(RoundedCornerShape(40.dp))
                    .background(Brush.linearGradient(listOf(tool.end.copy(alpha = 0.18f), Color.Transparent)))
            )
            Column(
                Modifier.fillMaxSize().padding(14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        Modifier.size(60.dp).clip(RoundedCornerShape(20.dp))
                            .background(Brush.linearGradient(listOf(tool.start, tool.end))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(tool.icon, null, tint = Color.White, modifier = Modifier.size((29 * iconScale).dp))
                    }
                    if (tool.premiumOnly) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color(0xFF171126).copy(alpha = 0.95f)
                        ) {
                            Row(
                                Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Lock, null, tint = Color.White, modifier = Modifier.size(13.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("PRO", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
                Column {
                    Text(tool.title, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        tool.subtitle,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = accent.copy(alpha = 0.10f)
                    ) {
                        Text(
                            if (tool.premiumOnly) "Premium" else "Free",
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = accent
                        )
                    }
                    Icon(Icons.Default.ArrowForward, null, tint = accent)
                }
            }
        }
    }
}

@Composable
private fun ModernToolWorkspace(id: String, premium: Boolean, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tool = tools.firstOrNull { it.id == id } ?: return
    var image by remember { mutableStateOf<Bitmap?>(null) }
    var sourceBytes by remember { mutableStateOf<Long?>(null) }
    var result by remember { mutableStateOf<com.nexauren.imagetools.media.ImageResult?>(null) }
    var status by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var pendingBytes by remember { mutableStateOf<ByteArray?>(null) }
    var pendingFormat by remember { mutableStateOf(OutputFormat.JPEG) }
    var pendingWidth by remember { mutableIntStateOf(1) }
    var pendingHeight by remember { mutableIntStateOf(1) }
    var pendingPrefix by remember { mutableStateOf("image-tools") }

    var width by remember { mutableStateOf("1920") }
    var height by remember { mutableStateOf("1080") }
    var lockRatio by remember { mutableStateOf(true) }
    var quality by remember { mutableFloatStateOf(78f) }
    var format by remember { mutableStateOf(OutputFormat.JPEG) }
    var compressFormat by remember { mutableStateOf(OutputFormat.WEBP) }
    var cropRatio by remember { mutableStateOf("1:1") }
    var angle by remember { mutableIntStateOf(90) }
    var mirrorH by remember { mutableStateOf(false) }
    var mirrorV by remember { mutableStateOf(false) }
    var filter by remember { mutableStateOf(ImageFilter.ORIGINAL) }
    var watermarkText by remember { mutableStateOf("IMAGE TOOLS") }
    var watermarkOpacity by remember { mutableFloatStateOf(72f) }
    var watermarkPosition by remember { mutableStateOf("Bottom right") }

    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument()) { uri ->
        val bytes = pendingBytes
        if (uri == null || bytes == null) {
            pendingBytes = null
            status = "Save cancelled. Your image was not changed."
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            try {
                result = withContext(Dispatchers.IO) {
                    ImageProcessor.saveToUri(context, uri, bytes, pendingFormat, pendingWidth, pendingHeight)
                }
                status = "Saved successfully. You chose where the file goes."
            } catch (_: Exception) {
                status = "We could not save the file. Please choose another location."
            } finally {
                pendingBytes = null
            }
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        image = uri?.let { ImageProcessor.decode(context, it) }
        sourceBytes = uri?.let { ImageProcessor.sourceBytes(context, it) }
        result = null
        status = null
        image?.let {
            if (id == "resize") {
                width = it.width.toString()
                height = it.height.toString()
            }
        }
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            WorkspaceHeader(tool, onBack)
        }
        item {
            Card(
                onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                shape = RoundedCornerShape(30.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF101426)),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(Modifier.fillMaxWidth().padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = RoundedCornerShape(50), color = tool.start.copy(alpha = 0.16f)) {
                            Row(Modifier.padding(horizontal = 9.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(tool.icon, null, tint = tool.end, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(5.dp))
                                Text(tool.title.uppercase(), color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                        Text("LOCAL", color = Color.White.copy(alpha = 0.55f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(12.dp))
                    if (image == null) {
                        Box(
                            Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(22.dp))
                                .background(Brush.linearGradient(listOf(Color(0xFF171D35), Color(0xFF0D1120)))),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    Modifier.size(72.dp).clip(RoundedCornerShape(24.dp))
                                        .background(Brush.linearGradient(listOf(tool.start, tool.end))),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(tool.icon, null, tint = Color.White, modifier = Modifier.size(34.dp))
                                }
                                Spacer(Modifier.height(12.dp))
                                Text("Choose an image", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                                Text("Tap to open your photo picker.", color = Color.White.copy(alpha = 0.62f), fontSize = 12.sp)
                            }
                        }
                    } else {
                        Box(
                            Modifier.fillMaxWidth().height(260.dp).clip(RoundedCornerShape(22.dp))
                                .background(Color(0xFF080B14)),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                image!!.asImageBitmap(),
                                "Selected image",
                                Modifier.fillMaxSize().padding(10.dp),
                                contentScale = ContentScale.Fit
                            )
                            Surface(
                                Modifier.align(Alignment.BottomStart).padding(12.dp),
                                shape = RoundedCornerShape(50),
                                color = Color.Black.copy(alpha = 0.72f)
                            ) {
                                Text(
                                    image!!.width.toString() + " × " + image!!.height,
                                    Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(Modifier.height(9.dp))
                        Text("Tap the preview to replace the image.", color = Color.White.copy(alpha = 0.62f), fontSize = 12.sp)
                    }
                }
            }
        }

        if (id == "info") {
            item {
                InfoPanel(image, sourceBytes)
            }
        } else {
            item {
                when (id) {
                    "resize" -> ResizePanel(width, height, lockRatio,
                        onWidth = {
                            val v = it.filter(Char::isDigit)
                            width = v
                            if (lockRatio && image != null && v.isNotBlank()) {
                                val ratio = image!!.width.toFloat() / image!!.height.toFloat()
                                height = (v.toInt().coerceAtLeast(1) / ratio).toInt().coerceAtLeast(1).toString()
                            }
                        },
                        onHeight = { height = it.filter(Char::isDigit) },
                        onLock = { lockRatio = it }
                    )
                    "compress" -> CompressPanel(quality, compressFormat, { quality = it }, { compressFormat = it })
                    "convert" -> FormatPanel(format) { format = it }
                    "crop" -> CropPanel(cropRatio) { cropRatio = it }
                    "rotate" -> RotatePanel(angle, mirrorH, mirrorV, { angle = it }, { mirrorH = it }, { mirrorV = it })
                    "filter" -> FilterPanel(filter) { filter = it }
                    "watermark" -> WatermarkPanel(watermarkText, watermarkOpacity, watermarkPosition,
                        onText = { watermarkText = it },
                        onOpacity = { watermarkOpacity = it },
                        onPosition = { watermarkPosition = it }
                    )
                }
            }
            item {
                Button(
                    onClick = {
                        val source = image ?: return@Button
                        busy = true
                        status = null
                        scope.launch {
                            try {
                                val transformed = withContext(Dispatchers.Default) {
                                    when (id) {
                                        "resize" -> ImageProcessor.resize(source, width.toIntOrNull()?.coerceAtLeast(1) ?: source.width, height.toIntOrNull()?.coerceAtLeast(1) ?: source.height)
                                        "compress", "convert" -> source
                                        "crop" -> ImageProcessor.cropCenter(source, cropRatio)
                                        "rotate" -> ImageProcessor.rotate(source, angle, mirrorH, mirrorV)
                                        "filter" -> ImageProcessor.filter(source, filter)
                                        "watermark" -> ImageProcessor.watermark(source, watermarkText, watermarkOpacity.toInt(), watermarkPosition)
                                        else -> source
                                    }
                                }
                                val outputFormat = if (id == "compress") compressFormat else format
                                val outputQuality = if (outputFormat == OutputFormat.PNG) 100 else quality.toInt().coerceIn(1, 100)
                                val bytes = withContext(Dispatchers.Default) { ImageProcessor.encode(transformed, outputFormat, outputQuality) }
                                pendingBytes = bytes
                                pendingFormat = outputFormat
                                pendingWidth = transformed.width
                                pendingHeight = transformed.height
                                pendingPrefix = id
                                status = "Choose where to save your result."
                                saveLauncher.launch(id + "_" + System.currentTimeMillis() + "." + outputFormat.extension)
                            } catch (_: Exception) {
                                status = "We could not create the result. Try another image or a smaller output."
                            } finally {
                                busy = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    enabled = image != null && !busy,
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = tool.start)
                ) {
                    if (busy) {
                        CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = Color.White)
                    } else {
                        Text("Create & save…", fontWeight = FontWeight.Bold)
                    }
                }
            }
            result?.let { saved ->
                item {
                    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFE9FFF4))) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Ready", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                            Text(saved.width.toString() + " × " + saved.height.toString() + " • " + ImageProcessor.humanBytes(saved.bytes) + " • " + saved.format.label)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = {
                                    val open = Intent(Intent.ACTION_VIEW).apply {
                                        data = saved.uri
                                        type = saved.format.mime
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(open)
                                }) { Text("Open") }
                                OutlinedButton(onClick = {
                                    val share = Intent(Intent.ACTION_SEND).apply {
                                        type = saved.format.mime
                                        putExtra(Intent.EXTRA_STREAM, saved.uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(share, "Share image"))
                                }) {
                                    Icon(Icons.Default.Share, null)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Share")
                                }
                            }
                        }
                    }
                }
            }
            status?.let { text ->
                item {
                    Text(text, color = if (result != null) Color(0xFF159A63) else MaterialTheme.colorScheme.error, fontWeight = FontWeight.Medium)
                }
            }
            if (!premium) {
                item {
                    Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Premium growth", fontWeight = FontWeight.Bold)
                            Text("More batch and advanced workflows can be added as the premium library expands.", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkspaceHeader(tool: Tool, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
        Box(Modifier.size(52.dp).clip(RoundedCornerShape(17.dp)).background(Brush.linearGradient(listOf(tool.start, tool.end))), contentAlignment = Alignment.Center) {
            Icon(tool.icon, null, tint = Color.White)
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(tool.title, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
            Text(tool.subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ResizePanel(width: String, height: String, lock: Boolean, onWidth: (String) -> Unit, onHeight: (String) -> Unit, onLock: (Boolean) -> Unit) {
    ControlPanel("Precision resize", "Set the exact output size.") {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(width, onWidth, Modifier.weight(1f), label = { Text("Width") }, singleLine = true)
            OutlinedTextField(height, onHeight, Modifier.weight(1f), label = { Text("Height") }, singleLine = true)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(lock, onLock)
            Spacer(Modifier.width(8.dp))
            Text("Keep aspect ratio")
        }
    }
}

@Composable
private fun CompressPanel(quality: Float, format: OutputFormat, onQuality: (Float) -> Unit, onFormat: (OutputFormat) -> Unit) {
    ControlPanel("Compression lab", "Find your balance between quality and file size.") {
        Text(quality.toInt().toString() + "% quality", fontWeight = FontWeight.ExtraBold)
        Slider(quality, onQuality, valueRange = 10f..100f)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(OutputFormat.JPEG, OutputFormat.WEBP).forEach {
                FilterChip(format == it, { onFormat(it) }, label = { Text(it.label) })
            }
        }
    }
}

@Composable
private fun FormatPanel(format: OutputFormat, onFormat: (OutputFormat) -> Unit) {
    ControlPanel("Format studio", "Pick the output type for the next app or website.") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutputFormat.values().forEach {
                FilterChip(format == it, { onFormat(it) }, label = { Text(it.label) })
            }
        }
    }
}

@Composable
private fun CropPanel(ratio: String, onRatio: (String) -> Unit) {
    ControlPanel("Crop presets", "Fast centered crops for common formats.") {
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            listOf("Original", "1:1", "4:5").forEach { FilterChip(ratio == it, { onRatio(it) }, label = { Text(it) }) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            listOf("16:9", "9:16").forEach { FilterChip(ratio == it, { onRatio(it) }, label = { Text(it) }) }
        }
    }
}

@Composable
private fun RotatePanel(angle: Int, h: Boolean, v: Boolean, onAngle: (Int) -> Unit, onH: (Boolean) -> Unit, onV: (Boolean) -> Unit) {
    ControlPanel("Transform studio", "Rotate or mirror before export.") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(90, 180, 270).forEach { FilterChip(angle == it, { onAngle(it) }, label = { Text(it.toString() + "°") }) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(h, { onH(!h) }, label = { Text("Mirror H") })
            FilterChip(v, { onV(!v) }, label = { Text("Mirror V") })
        }
    }
}

@Composable
private fun FilterPanel(filter: ImageFilter, onFilter: (ImageFilter) -> Unit) {
    ControlPanel("Filter presets", "Clean, simple looks without leaving the app.") {
        ImageFilter.values().toList().chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { FilterChip(filter == it, { onFilter(it) }, label = { Text(it.label) }) }
            }
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
    ControlPanel("Smart Watermark", "Add a clean brand mark without uploading your photo.") {
        OutlinedTextField(
            value = text,
            onValueChange = onText,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Watermark text") },
            singleLine = true,
            shape = RoundedCornerShape(16.dp)
        )
        Text("Opacity " + opacity.toInt() + "%", fontWeight = FontWeight.Bold)
        Slider(opacity, onOpacity, valueRange = 15f..100f)
        Text("Position", fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            listOf("Top left", "Center", "Bottom left", "Bottom right").forEach { item ->
                FilterChip(position == item, { onPosition(item) }, label = { Text(item) })
            }
        }
    }
}

@Composable
private fun ControlPanel(title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    Card(shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
            Text(title, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
private fun ModernAccount(auth: AuthRepository, firestore: FirestoreRepository, premium: Boolean, openPremium: () -> Unit) {
    val user = auth.currentUser ?: return
    var name by remember(user.uid) { mutableStateOf(user.displayName ?: "") }
    var saving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text("Account", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
            Text("Your profile, plan and security in one place.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Card(shape = RoundedCornerShape(28.dp)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(64.dp).clip(RoundedCornerShape(22.dp)).background(Brush.linearGradient(listOf(Color(0xFF7B2FF7), Color(0xFF00B9F2)))), contentAlignment = Alignment.Center) {
                            Text((name.ifBlank { user.email ?: "U" }).first().uppercase(), color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(name.ifBlank { "Image Tools User" }, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                            Text(user.email ?: "", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Display name") }, singleLine = true, shape = RoundedCornerShape(16.dp))
                    Button(
                        onClick = {
                            saving = true
                            firestore.updateDisplayName(name)
                            scope.launch { kotlinx.coroutines.delay(300); saving = false }
                        },
                        enabled = !saving,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(if (saving) "Saving…" else "Save profile") }
                }
            }
        }
        item {
            Card(shape = RoundedCornerShape(24.dp)) {
                ListItem(
                    headlineContent = { Text(if (premium) "Premium active" else "Free plan", fontWeight = FontWeight.Bold) },
                    supportingContent = { Text(if (premium) "Premium is active on this account." else "Upgrade for US$5/month as the premium library grows.") },
                    leadingContent = { Icon(if (premium) Icons.Default.WorkspacePremium else Icons.Default.LockOpen, null) },
                    trailingContent = { if (!premium) Button(onClick = openPremium) { Text("Upgrade") } }
                )
            }
        }
        item {
            Card(shape = RoundedCornerShape(24.dp)) {
                ListItem(
                    headlineContent = { Text("Account security", fontWeight = FontWeight.Bold) },
                    supportingContent = { Text("Sign-in is handled securely by your authentication provider.") },
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
            ) { Text("Sign out") }
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
private fun ModernSettings(darkMode: Boolean, onDarkModeChange: (Boolean) -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Settings", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold) }
        item {
            Card(shape = RoundedCornerShape(22.dp)) {
                ListItem(
                    headlineContent = { Text("Dark mode", fontWeight = FontWeight.Bold) },
                    supportingContent = { Text("Use a darker editing workspace.") },
                    leadingContent = { Icon(Icons.Default.DarkMode, null) },
                    trailingContent = { Switch(darkMode, onDarkModeChange) }
                )
            }
        }
        item {
            Card(shape = RoundedCornerShape(22.dp)) {
                ListItem(
                    headlineContent = { Text("Local processing", fontWeight = FontWeight.Bold) },
                    supportingContent = { Text("Current image editing happens on your device.") },
                    leadingContent = { Icon(Icons.Default.Security, null) },
                    trailingContent = { Icon(Icons.Default.VerifiedUser, null, tint = Color(0xFF159A63)) }
                )
            }
        }
        item {
            Card(shape = RoundedCornerShape(22.dp)) {
                ListItem(
                    headlineContent = { Text("Account sync", fontWeight = FontWeight.Bold) },
                    supportingContent = { Text("Profile and plan status sync securely to your account.") },
                    leadingContent = { Icon(Icons.Default.CloudDone, null) }
                )
            }
        }
    }
}

@Composable
private fun ModernAbout() {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text("About", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
            Text("Image Tools 1.3.0", fontWeight = FontWeight.Bold)
            Text("A focused image workspace built for fast, private editing.")
        }
        item {
            Card(shape = RoundedCornerShape(24.dp)) {
                ListItem(
                    headlineContent = { Text("Privacy first", fontWeight = FontWeight.Bold) },
                    supportingContent = { Text("The current editing tools process images locally.") },
                    leadingContent = { Icon(Icons.Default.Shield, null) }
                )
            }
        }
        item {
            Card(shape = RoundedCornerShape(24.dp)) {
                ListItem(
                    headlineContent = { Text("What is new", fontWeight = FontWeight.Bold) },
                    supportingContent = { Text("New crop, rotate, filter and image-details workflows plus a redesigned account experience.") },
                    leadingContent = { Icon(Icons.Default.AutoAwesome, null) }
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