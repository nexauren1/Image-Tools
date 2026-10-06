package com.nexauren.imagetools.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
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
import com.nexauren.imagetools.data.FirestoreRepository
import com.nexauren.imagetools.data.PaymentRepository
import com.nexauren.imagetools.media.ImageProcessor
import com.nexauren.imagetools.media.OutputFormat
import kotlinx.coroutines.launch

private data class Tool(val id: String, val title: String, val subtitle: String, val icon: ImageVector)

private val toolCatalog = listOf(
    Tool("resize", "Resize Image", "Pixel-perfect scaling", Icons.Default.PhotoSizeSelectLarge),
    Tool("compress", "Compress Image", "Smaller files, strong quality", Icons.Default.Compress),
    Tool("convert", "Convert Image", "JPEG, PNG and WEBP", Icons.Default.SwapHoriz)
)

@Composable
fun ImageToolsApp(
    auth: AuthRepository,
    firestore: FirestoreRepository,
    premium: Boolean,
    darkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    onStartPayment: (String) -> Unit
) {
    if (auth.currentUser == null) {
        AuthScreen(auth)
        return
    }

    var page by remember { mutableStateOf("home") }
    var toolId by remember { mutableStateOf<String?>(null) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(28.dp))
                Text(
                    "IMAGE TOOLS",
                    modifier = Modifier.padding(horizontal = 24.dp),
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp
                )
                Text(
                    if (premium) "Premium workspace" else "Free workspace",
                    modifier = Modifier.padding(24.dp, 4.dp),
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 12.sp
                )
                DrawerItem(Icons.Default.Home, "Home") {
                    page = "home"; toolId = null; scope.launch { drawerState.close() }
                }
                DrawerItem(Icons.Default.Build, "All tools") {
                    page = "tools"; toolId = null; scope.launch { drawerState.close() }
                }
                DrawerItem(Icons.Default.WorkspacePremium, "Premium") {
                    page = "premium"; toolId = null; scope.launch { drawerState.close() }
                }
                DrawerItem(Icons.Default.Person, "Account") {
                    page = "account"; toolId = null; scope.launch { drawerState.close() }
                }
                DrawerItem(Icons.Default.Settings, "Settings") {
                    page = "settings"; toolId = null; scope.launch { drawerState.close() }
                }
                DrawerItem(Icons.Default.Info, "About") {
                    page = "about"; toolId = null; scope.launch { drawerState.close() }
                }
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
                                if (premium) "Premium workspace" else "Local image power",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, "Menu")
                        }
                    },
                    actions = {
                        IconButton(onClick = { page = "premium"; toolId = null }) {
                            Icon(Icons.Default.AutoAwesome, "Premium")
                        }
                    }
                )
            },
            bottomBar = {
                NavigationBar {
                    NavItem("home", Icons.Default.Home, "Home", page) { page = "home"; toolId = null }
                    NavItem("tools", Icons.Default.Build, "Tools", page) { page = "tools"; toolId = null }
                    NavItem("account", Icons.Default.Person, "Account", page) { page = "account"; toolId = null }
                }
            }
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                AnimatedContent(targetState = toolId ?: page, label = "main-navigation") { target ->
                    when (target) {
                        "home" -> HomeScreen(
                            premium = premium,
                            openTool = { toolId = it },
                            openPremium = { page = "premium" }
                        )
                        "tools" -> ToolsScreen { toolId = it }
                        "account" -> AccountScreen(auth, firestore, premium) { page = "premium" }
                        "premium" -> PremiumScreen(auth, premium, onStartPayment)
                        "settings" -> SettingsScreen(darkMode, onDarkModeChange)
                        "about" -> AboutScreen()
                        else -> ToolScreen(target, premium) { toolId = null }
                    }
                }
            }
        }
    }
}

@Composable
private fun NavItem(id: String, icon: ImageVector, label: String, page: String, onClick: () -> Unit) {
    NavigationBarItem(selected = page == id, onClick = onClick, icon = { Icon(icon, label) }, label = { Text(label) })
}

@Composable
private fun DrawerItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    NavigationDrawerItem(
        label = { Text(label) },
        selected = false,
        onClick = onClick,
        icon = { Icon(icon, null) },
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
    )
}

@Composable
private fun AuthScreen(auth: AuthRepository) {
    var createMode by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFFF6F7FF), Color(0xFFE9E7FF)))
        )
    ) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier.size(88.dp).clip(RoundedCornerShape(28.dp)).background(
                    Brush.linearGradient(listOf(Color(0xFF6D5DFB), Color(0xFF00B9F2)))
                ),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.AutoFixHigh, null, tint = Color.White, modifier = Modifier.size(42.dp))
            }
            Spacer(Modifier.height(18.dp))
            Text("Image Tools", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
            Text("Fast. Local. Beautiful.", color = Color.Gray)
            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    busy = true; error = null
                    scope.launch {
                        auth.signInGoogle(BuildConfig.GOOGLE_WEB_CLIENT_ID)
                            .onFailure { error = it.message ?: "Google sign-in failed." }
                        busy = false
                    }
                },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Icon(Icons.Default.AccountCircle, null)
                Spacer(Modifier.width(8.dp))
                Text("Continue with Google", fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                HorizontalDivider(Modifier.weight(1f))
                Text("  OR  ", fontSize = 11.sp, color = Color.Gray)
                HorizontalDivider(Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Email") })
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                password,
                { password = it },
                Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation()
            )
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = {
                    busy = true; error = null
                    scope.launch {
                        val result = if (createMode) {
                            auth.registerEmail(email, password)
                        } else {
                            auth.signInEmail(email, password)
                        }
                        result.onFailure { error = it.message ?: "Authentication failed." }
                        busy = false
                    }
                },
                enabled = !busy && email.isNotBlank() && password.length >= 6,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text(if (createMode) "Create account" else "Sign in", fontWeight = FontWeight.Bold)
            }
            TextButton(onClick = { createMode = !createMode }) {
                Text(if (createMode) "Already have an account? Sign in" else "Create a new account")
            }
            if (error != null) {
                Text(error ?: "", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            }
            if (busy) CircularProgressIndicator(Modifier.padding(10.dp))
        }
    }
}

@Composable
private fun HomeScreen(premium: Boolean, openTool: (String) -> Unit, openPremium: () -> Unit) {
    var query by remember { mutableStateOf("") }
    val results = toolCatalog.filter {
        (it.title + " " + it.subtitle).contains(query, ignoreCase = true)
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(30.dp)).background(
                    Brush.linearGradient(
                        listOf(Color(0xFF121A3A), Color(0xFF6D5DFB), Color(0xFF00B9F2))
                    )
                ).padding(24.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Your image workspace.", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                    Text(
                        "Powerful editing tools that work directly on your phone.",
                        color = Color.White.copy(alpha = 0.86f)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AssistChip(onClick = {}, label = { Text("LOCAL PROCESSING") })
                        if (premium) AssistChip(onClick = {}, label = { Text("PREMIUM") })
                    }
                }
            }
        }
        item {
            OutlinedTextField(
                query,
                { query = it },
                Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                leadingIcon = { Icon(Icons.Default.Search, null) },
                placeholder = { Text("Search tools…") }
            )
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Quick tools", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                TextButton(onClick = { query = "" }) { Text("All") }
            }
        }
        items(results) { ToolCard(it, openTool) }
        if (!premium) {
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Unlock Premium", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text("One-time US$9. Future tools, batch workflows and more.")
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = openPremium) { Text("Upgrade") }
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolsScreen(openTool: (String) -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Tools", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
            Text("Start with three focused, production-ready image workflows.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        items(toolCatalog) { ToolCard(it, openTool) }
        item {
            Card(shape = RoundedCornerShape(24.dp)) {
                ListItem(
                    headlineContent = { Text("More tools are coming", fontWeight = FontWeight.Bold) },
                    supportingContent = { Text("Batch editing, watermarking, filters and PDF workflows are planned.") },
                    leadingContent = { Icon(Icons.Default.AddCircleOutline, null, tint = MaterialTheme.colorScheme.primary) }
                )
            }
        }
    }
}

@Composable
private fun ToolCard(tool: Tool, openTool: (String) -> Unit) {
    Card(
        onClick = { openTool(tool.id) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(56.dp).clip(RoundedCornerShape(18.dp)).background(
                    Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary))
                ),
                contentAlignment = Alignment.Center
            ) {
                Icon(tool.icon, null, tint = Color.White, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(tool.title, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Text(tool.subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            }
            Icon(Icons.Default.ChevronRight, null)
        }
    }
}

@Composable
private fun ToolScreen(id: String, premium: Boolean, onBack: () -> Unit) {
    val context = LocalContext.current
    val selectedTool = toolCatalog.firstOrNull { it.id == id } ?: return
    var sourceUri by remember { mutableStateOf<Uri?>(null) }
    var bitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var result by remember { mutableStateOf<com.nexauren.imagetools.media.ImageResult?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var working by remember { mutableStateOf(false) }
    var width by remember { mutableStateOf("1920") }
    var height by remember { mutableStateOf("1080") }
    var quality by remember { mutableFloatStateOf(78f) }
    var format by remember { mutableStateOf(OutputFormat.JPEG) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        sourceUri = uri
        bitmap = uri?.let { ImageProcessor.decode(context, it) }
        result = null
        message = null
        val image = bitmap
        if (id == "resize" && image != null) {
            width = image.width.toString()
            height = image.height.toString()
        }
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
                Icon(selectedTool.icon, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Text(selectedTool.title, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
        item {
            Card(
                onClick = {
                    picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                shape = RoundedCornerShape(26.dp)
            ) {
                Column(Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    val image = bitmap
                    if (image != null) {
                        androidx.compose.foundation.Image(
                            bitmap = image.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(20.dp)),
                            contentScale = ContentScale.Fit
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(image.width.toString() + " × " + image.height.toString(), fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.AddPhotoAlternate, null, modifier = Modifier.size(54.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(8.dp))
                        Text("Select an image", fontWeight = FontWeight.Bold)
                        Text("Processing stays on-device.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        if (id == "resize") {
            item {
                Text("Target size", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(width, { width = it.filter(Char::isDigit) }, Modifier.weight(1f), label = { Text("Width") }, singleLine = true)
                    OutlinedTextField(height, { height = it.filter(Char::isDigit) }, Modifier.weight(1f), label = { Text("Height") }, singleLine = true)
                }
            }
        }
        if (id == "compress") {
            item {
                Text("Quality", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(quality.toInt().toString() + "%", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Slider(value = quality, onValueChange = { quality = it }, valueRange = 10f..100f)
            }
        }
        if (id == "convert") {
            item {
                Text("Output format", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutputFormat.values().forEach { item ->
                        FilterChip(selected = format == item, onClick = { format = item }, label = { Text(item.label) })
                    }
                }
            }
        }
        item {
            Button(
                onClick = {
                    val image = bitmap ?: return@Button
                    working = true
                    message = null
                    try {
                        val output = when (id) {
                            "resize" -> {
                                val resized = ImageProcessor.resize(image, width.toInt(), height.toInt())
                                val bytes = ImageProcessor.encode(resized, OutputFormat.PNG, 100)
                                Triple(bytes, OutputFormat.PNG, Pair(resized.width, resized.height))
                            }
                            "compress" -> {
                                val bytes = ImageProcessor.encode(image, OutputFormat.JPEG, quality.toInt())
                                Triple(bytes, OutputFormat.JPEG, Pair(image.width, image.height))
                            }
                            else -> {
                                val bytes = ImageProcessor.encode(image, format, 92)
                                Triple(bytes, format, Pair(image.width, image.height))
                            }
                        }
                        result = ImageProcessor.save(
                            context,
                            output.first,
                            output.second,
                            id,
                            output.third.first,
                            output.third.second
                        )
                        message = "Saved to Pictures / Image Tools"
                    } catch (e: Exception) {
                        message = e.message ?: "Processing failed."
                    } finally {
                        working = false
                    }
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                enabled = bitmap != null && !working,
                shape = RoundedCornerShape(18.dp)
            ) {
                if (working) {
                    CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Process image", fontWeight = FontWeight.Bold)
                }
            }
        }
        result?.let { done ->
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Ready", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                        Text(
                            done.width.toString() + " × " + done.height.toString() +
                                " • " + ImageProcessor.humanBytes(done.bytes) +
                                " • " + done.format.label
                        )
                        OutlinedButton(onClick = {
                            val share = Intent(Intent.ACTION_SEND).apply {
                                type = done.format.mime
                                putExtra(Intent.EXTRA_STREAM, done.uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(share, "Share image"))
                        }) {
                            Icon(Icons.Default.Share, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Share result")
                        }
                    }
                }
            }
        }
        message?.let { item { Text(it, color = if (result != null) Color(0xFF168A5B) else MaterialTheme.colorScheme.error) } }
        if (!premium) {
            item {
                Text("Premium roadmap", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("Batch processing and advanced image workflows will be available as Premium features.")
            }
        }
    }
}

@Composable
private fun AccountScreen(
    auth: AuthRepository,
    firestore: FirestoreRepository,
    premium: Boolean,
    openPremium: () -> Unit
) {
    val user = auth.currentUser ?: return
    var name by remember(user.uid) { mutableStateOf(user.displayName ?: "") }
    var saving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text("Account", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
            Card(shape = RoundedCornerShape(28.dp)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(64.dp).clip(RoundedCornerShape(22.dp)).background(MaterialTheme.colorScheme.primary),
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
                            Text(name.ifBlank { "Image Tools User" }, fontWeight = FontWeight.Bold, fontSize = 19.sp)
                            Text(user.email ?: "", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Display name") }, singleLine = true)
                    Button(
                        onClick = {
                            saving = true
                            firestore.updateDisplayName(name)
                            scope.launch {
                                kotlinx.coroutines.delay(350)
                                saving = false
                            }
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
                    supportingContent = {
                        Text(if (premium) "This account has an active Premium entitlement." else "Upgrade once for US$9 to unlock the Premium roadmap.")
                    },
                    leadingContent = { Icon(if (premium) Icons.Default.WorkspacePremium else Icons.Default.LockOpen, null) },
                    trailingContent = { if (!premium) Button(onClick = openPremium) { Text("Upgrade") } }
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
private fun PremiumScreen(auth: AuthRepository, premium: Boolean, onStartPayment: (String) -> Unit) {
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(30.dp)).background(
                    Brush.linearGradient(listOf(Color(0xFF121829), Color(0xFF6D5DFB), Color(0xFF00B9F2)))
                ).padding(24.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Premium", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
                    Text("One upgrade. More power.", color = Color.White.copy(alpha = 0.86f))
                    Text("US$9", color = Color.White, fontSize = 44.sp, fontWeight = FontWeight.ExtraBold)
                    Text("one-time purchase", color = Color.White.copy(alpha = 0.76f))
                }
            }
        }
        item {
            Card(shape = RoundedCornerShape(26.dp)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    PremiumLine("More powerful image workflows")
                    PremiumLine("Batch processing roadmap")
                    PremiumLine("Premium tools as the app grows")
                    PremiumLine("Account entitlement stored in Firestore")
                    PremiumLine("No recurring subscription in v1")
                    Button(
                        onClick = {
                            busy = true
                            error = null
                            scope.launch {
                                val token = auth.idToken()
                                if (token.isNullOrBlank()) {
                                    error = "Please sign in again."
                                    busy = false
                                } else {
                                    PaymentRepository.createOrder(token)
                                        .onSuccess { (_, approveUrl) ->
                                            onStartPayment(approveUrl)
                                        }
                                        .onFailure { error = it.message ?: "Unable to start checkout." }
                                    busy = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !premium && !busy
                    ) {
                        Text(if (premium) "Premium active" else if (busy) "Opening PayPal…" else "Pay with PayPal")
                    }
                    if (error != null) Text(error ?: "", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    if (BuildConfig.WORKER_URL.contains("YOUR-IMAGE-TOOLS-WORKER")) {
                        Text(
                            "Worker is not connected yet. Add WORKER_URL before testing checkout.",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PremiumLine(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF18A56A), modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(text)
    }
}

@Composable
private fun SettingsScreen(darkMode: Boolean, onDarkModeChange: (Boolean) -> Unit) {
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
                    headlineContent = { Text("Privacy model", fontWeight = FontWeight.Bold) },
                    supportingContent = { Text("Image processing is local in the first release.") },
                    leadingContent = { Icon(Icons.Default.Security, null) },
                    trailingContent = { Icon(Icons.Default.VerifiedUser, null, tint = Color(0xFF18A56A)) }
                )
            }
        }
        item {
            Card(shape = RoundedCornerShape(22.dp)) {
                ListItem(
                    headlineContent = { Text("Payment backend", fontWeight = FontWeight.Bold) },
                    supportingContent = { Text(BuildConfig.WORKER_URL) },
                    leadingContent = { Icon(Icons.Default.CloudQueue, null) }
                )
            }
        }
    }
}

@Composable
private fun AboutScreen() {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text("About", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
            Text("Image Tools 1.0", fontWeight = FontWeight.Bold)
            Text("A modern mobile image workspace built for quick, private workflows.")
        }
        item {
            Card(shape = RoundedCornerShape(24.dp)) {
                ListItem(
                    headlineContent = { Text("Architecture", fontWeight = FontWeight.Bold) },
                    supportingContent = { Text("Firebase account + Firestore entitlement + Cloudflare Worker payment backend + local image processing.") },
                    leadingContent = { Icon(Icons.Default.Layers, null) }
                )
            }
        }
    }
}