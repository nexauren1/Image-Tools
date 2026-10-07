package com.nexauren.imagetools.ui

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
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

private data class Tool(
    val id: String, val title: String, val subtitle: String, val icon: ImageVector,
    val a: Color, val b: Color, val pro: Boolean = false
)

private val CATALOG = listOf(
    Tool("resize","Resize","Exact dimensions + ratio lock",Icons.Default.PhotoSizeSelectLarge,Color(0xFF2563EB),Color(0xFF06B6D4)),
    Tool("compress","Compress","Smaller files with quality control",Icons.Default.Compress,Color(0xFF059669),Color(0xFF22C55E)),
    Tool("convert","Convert","JPEG, PNG and WEBP",Icons.Default.SwapHoriz,Color(0xFF7C3AED),Color(0xFFEC4899)),
    Tool("crop","Crop","1:1, 4:5, 16:9 and 9:16",Icons.Default.Crop,Color(0xFFF59E0B),Color(0xFFF97316)),
    Tool("rotate","Rotate & Flip","Straighten or mirror",Icons.Default.Rotate90DegreesCw,Color(0xFF4F46E5),Color(0xFFEC4899)),
    Tool("filter","Filters","Grayscale, sepia and contrast",Icons.Default.FilterVintage,Color(0xFF0F766E),Color(0xFF14B8A6)),
    Tool("watermark","Watermark","Add clean branding text",Icons.Default.TextFields,Color(0xFF7C3AED),Color(0xFFDB2777),true),
    Tool("details","Image Details","Resolution, ratio and size",Icons.Default.Info,Color(0xFF334155),Color(0xFF06B6D4)),
    Tool("brightness","Brightness","Lighten or darken",Icons.Default.LightMode,Color(0xFFF59E0B),Color(0xFFFBBF24)),
    Tool("contrast","Contrast","Increase or soften contrast",Icons.Default.Contrast,Color(0xFF0F172A),Color(0xFF475569)),
    Tool("saturation","Saturation","Boost or mute colors",Icons.Default.Palette,Color(0xFFDB2777),Color(0xFF8B5CF6)),
    Tool("warmth","Warmth","Cool ↔ warm tone control",Icons.Default.WbSunny,Color(0xFFEA580C),Color(0xFFF59E0B)),
    Tool("negative","Negative","Invert the image",Icons.Default.InvertColors,Color(0xFF111827),Color(0xFF6B7280)),
    Tool("blur","Blur","Quick privacy-friendly blur",Icons.Default.BlurOn,Color(0xFF0891B2),Color(0xFF6366F1),true),
    Tool("sharpen","Sharpen","Bring edges back",Icons.Default.AutoFixHigh,Color(0xFF16A34A),Color(0xFF0EA5E9),true),
    Tool("pixelate","Pixelate","Block effect for faces or design",Icons.Default.GridOn,Color(0xFF7C3AED),Color(0xFF4F46E5)),
    Tool("border","Border","Add a clean photo frame",Icons.Default.BorderStyle,Color(0xFF64748B),Color(0xFF0F172A)),
    Tool("round","Rounded Corners","Export profile-ready images",Icons.Default.RoundedCorner,Color(0xFF2563EB),Color(0xFF7C3AED),true),
    Tool("metadata","Remove Metadata","Strip common embedded metadata",Icons.Default.Security,Color(0xFF059669),Color(0xFF0F766E)),
    Tool("pdf","Image → PDF","Create a shareable PDF",Icons.Default.PictureAsPdf,Color(0xFFDC2626),Color(0xFFEA580C),true),
    Tool("palette","Color Palette","Extract five dominant colors",Icons.Default.ColorLens,Color(0xFF0EA5E9),Color(0xFF8B5CF6)),
    Tool("collage","Quick Collage","Combine 2–4 images",Icons.Default.Collections,Color(0xFFDB2777),Color(0xFFF97316),true),
    Tool("ocr","OCR","Extract text from an image",Icons.Default.TextSnippet,Color(0xFF2563EB),Color(0xFF8B5CF6)),
    Tool("background","Remove Background","Keep the main subject",Icons.Default.AutoFixNormal,Color(0xFF0EA5E9),Color(0xFF14B8A6),true),
    Tool("exif","EXIF","Read photo metadata",Icons.Default.DataObject,Color(0xFF475569),Color(0xFF06B6D4))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageToolsAppV3(
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
            AuthScreen(auth)
        } else {
            var page by remember { mutableStateOf("home") }
            var selected by remember { mutableStateOf<String?>(null) }
            var drawer by remember { mutableStateOf(false) }
            val drawerState = rememberDrawerState(if (drawer) DrawerValue.Open else DrawerValue.Closed)
            val scope = rememberCoroutineScope()

            LaunchedEffect(drawerState.currentValue) {
                drawer = drawerState.currentValue == DrawerValue.Open
            }

            fun go(p: String) {
                page = p
                selected = null
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
                        DrawerEntry(Icons.Default.Home, strings.get("home")) { go("home") }
                        DrawerEntry(Icons.Default.Build, strings.get("tools")) { go("tools") }
                        DrawerEntry(Icons.Default.DynamicFeed, strings.get("batch")) { go("batch") }
                        DrawerEntry(Icons.Default.WorkspacePremium, strings.get("premium")) { go("premium") }
                        DrawerEntry(Icons.Default.Person, strings.get("account")) { go("account") }
                        DrawerEntry(Icons.Default.Settings, strings.get("settings")) { go("settings") }
                        DrawerEntry(Icons.Default.Info, strings.get("about")) { go("about") }
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
                                IconButton(onClick = { go("premium") }) {
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
                            NavigationBarItem(
                                selected = page == "home" && selected == null,
                                onClick = { go("home") },
                                icon = { Icon(Icons.Default.Home, null) },
                                label = { Text(strings.get("home")) }
                            )
                            NavigationBarItem(
                                selected = page == "tools" && selected == null,
                                onClick = { go("tools") },
                                icon = { Icon(Icons.Default.Build, null) },
                                label = { Text(strings.get("tools")) }
                            )
                            NavigationBarItem(
                                selected = page == "batch" && selected == null,
                                onClick = { go("batch") },
                                icon = { Icon(Icons.Default.DynamicFeed, null) },
                                label = { Text(strings.get("batch")) }
                            )
                        }
                    }
                ) { pad ->
                    Box(Modifier.fillMaxSize().padding(pad)) {
                        when (selected ?: page) {
                            "home" -> HomeScreen(premium) { open(it) }
                            "tools" -> ToolsScreen(premium) { open(it) }
                            "batch" -> BatchScreen(strings)
                            "premium" -> PremiumScreen(auth, premium, onStartPayment, onCancelSubscription)
                            "account" -> AccountScreen(auth, premium) { go("premium") }
                            "settings" -> SettingsScreen(
                                darkMode = darkMode,
                                onDarkModeChange = onDarkModeChange,
                                language = language,
                                onLanguageChange = {
                                    language = it
                                    AppLanguageStore.save(context, it)
                                }
                            )
                            "about" -> AboutScreen()
                            else -> ToolWorkspace(
                                tool = CATALOG.first { it.id == (selected ?: "") },
                                premium = premium,
                                onBack = { selected = null },
                                onNeedPremium = { go("premium") }
                            )
                        }
                    }
                }
            }

            fun open(id: String) {
                val t = CATALOG.first { it.id == id }
                if (t.pro && !premium) go("premium") else selected = id
            }
        }
    }
}

@Composable private fun DrawerEntry(icon:ImageVector,label:String,onClick:()->Unit){
    NavigationDrawerItem(label={Text(label)},selected=false,onClick=onClick,icon={Icon(icon,null)},modifier=Modifier.padding(horizontal=12.dp,vertical=2.dp))
}

@Composable private fun AuthScreen(auth:AuthRepository){
    var create by remember{mutableStateOf(false)}
    var email by remember{mutableStateOf("")}; var pass by remember{mutableStateOf("")}
    var busy by remember{mutableStateOf(false)}; var error by remember{mutableStateOf<String?>(null)}
    val scope=rememberCoroutineScope()
    Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFF0B1022),Color(0xFF4C1D95),Color(0xFF0891B2))))){
        LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(22.dp),verticalArrangement=Arrangement.Center,horizontalAlignment=Alignment.CenterHorizontally){
            item{
                Box(Modifier.size(84.dp).clip(RoundedCornerShape(25.dp)).background(Brush.linearGradient(listOf(Color(0xFF7C3AED),Color(0xFF06B6D4)))),contentAlignment=Alignment.Center){Icon(Icons.Default.AutoFixHigh,null,tint=Color.White,modifier=Modifier.size(40.dp))}
                Spacer(Modifier.height(16.dp))
                Text(if(create)"Create your workspace" else "Welcome back",color=Color.White,fontSize=28.sp,fontWeight=FontWeight.ExtraBold)
                Text("A focused toolbox for everyday image jobs.",color=Color.White.copy(.78f))
                Spacer(Modifier.height(20.dp))
                Card(shape=RoundedCornerShape(28.dp)){Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
                    Button({busy=true;scope.launch{auth.signInGoogle(BuildConfig.GOOGLE_WEB_CLIENT_ID).onFailure{error=authMessage(it)};busy=false}},enabled=!busy,modifier=Modifier.fillMaxWidth().height(52.dp),shape=RoundedCornerShape(16.dp)){Icon(Icons.Default.AccountCircle,null);Spacer(Modifier.width(8.dp));Text("Continue with Google",fontWeight=FontWeight.Bold)}
                    Row(verticalAlignment=Alignment.CenterVertically){HorizontalDivider(Modifier.weight(1f));Text(" OR ",fontSize=10.sp);HorizontalDivider(Modifier.weight(1f))}
                    OutlinedTextField(email,{email=it},Modifier.fillMaxWidth(),label={Text("Email")},singleLine=true)
                    OutlinedTextField(pass,{pass=it},Modifier.fillMaxWidth(),label={Text("Password")},singleLine=true,visualTransformation=PasswordVisualTransformation())
                    Button({busy=true;error=null;scope.launch{(if(create)auth.registerEmail(email,pass) else auth.signInEmail(email,pass)).onFailure{error=authMessage(it)};busy=false}},enabled=!busy&&email.contains("@")&&pass.length>=6,modifier=Modifier.fillMaxWidth().height(52.dp),shape=RoundedCornerShape(16.dp)){Text(if(create)"Create account" else "Sign in",fontWeight=FontWeight.Bold)}
                    TextButton({create=!create},Modifier.fillMaxWidth()){Text(if(create)"I already have an account" else "Create an account")}
                    if(!create)TextButton({scope.launch{auth.sendPasswordReset(email).onFailure{error="Enter a valid email first."}}},Modifier.fillMaxWidth()){Text("Forgot password?")}
                    error?.let{Text(it,color=MaterialTheme.colorScheme.error,fontSize=12.sp)}
                    Text("Core image processing stays on the device.",fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
                }}
            }
        }
    }
}
private fun authMessage(e:Throwable):String{val t=e.message?.lowercase().orEmpty();return when{t.contains("already in use")->"This email is already registered.";t.contains("invalid email")||t.contains("badly formatted")->"Please enter a valid email address.";t.contains("wrong-password")||t.contains("invalid-credential")->"The email or password is incorrect.";t.contains("network")->"Connection failed. Check your internet.";else->"We could not complete sign-in. Please try again."}}

@Composable private fun HomeScreen(premium:Boolean,open:(String)->Unit){
    val strings = LocalUiText.current
    var q by remember{mutableStateOf("")}
    val popular=CATALOG.filter{it.id in listOf("resize","compress","convert","crop","brightness","metadata")}
    val filtered=CATALOG.filter{(strings.toolTitle(it.id)+" "+strings.toolSubtitle(it.id)).contains(q,true)}
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(14.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
        item{Card(shape=RoundedCornerShape(30.dp)){Box(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFF0B1022),Color(0xFF4C1D95),Color(0xFF0891B2)))).padding(22.dp)){Column(verticalArrangement=Arrangement.spacedBy(8.dp)){Text(if(premium)"PRO WORKSPACE" else "IMAGE TOOLBOX",color=Color.White,fontWeight=FontWeight.ExtraBold,fontSize=10.sp,letterSpacing=1.4.sp);Text("Do more with every image.",color=Color.White,fontSize=28.sp,fontWeight=FontWeight.ExtraBold);Text("Resize, compress, convert, transform and clean images without a complicated editor.",color=Color.White.copy(.82f));Row(horizontalArrangement=Arrangement.spacedBy(7.dp)){Pill("20+","tools");Pill("Local","processing");Pill("PRO","optional")}}}}}
        item{OutlinedTextField(q,{q=it},Modifier.fillMaxWidth(),placeholder={Text("Search tools…")},leadingIcon={Icon(Icons.Default.Search,null)},singleLine=true,shape=RoundedCornerShape(18.dp))}
        item{Header(strings.get("popular"),strings.get("home.subtitle"))}
        item{LazyVerticalGrid(GridCells.Fixed(2),Modifier.height(250.dp),userScrollEnabled=false,horizontalArrangement=Arrangement.spacedBy(10.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){items(popular){ToolCard(it,premium,open)}}}
        item{Header(strings.get("all.tools"),filtered.size.toString()+" "+strings.get("tools"))}
        items(filtered){CompactTool(it){open(it.id)}}
        item{Feature(strings.get("local"),strings.get("private"),"Lock")}
    }
}
@Composable private fun Pill(v:String,l:String){Surface(shape=RoundedCornerShape(14.dp),color=Color.White.copy(.1f)){Column(Modifier.padding(horizontal=10.dp,vertical=6.dp)){Text(v,color=Color.White,fontWeight=FontWeight.ExtraBold,fontSize=13.sp);Text(l,color=Color.White.copy(.7f),fontSize=8.sp)}}}
@Composable private fun Header(a:String,b:String){Column{Text(a,fontSize=18.sp,fontWeight=FontWeight.ExtraBold);Text(b,fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}}
@Composable private fun ToolCard(t:Tool,premium:Boolean,open:(String)->Unit){
    val strings = LocalUiText.current
    Card(onClick={open(t.id)},shape=RoundedCornerShape(21.dp)){
        Column(Modifier.padding(13.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
            Box(Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(Brush.linearGradient(listOf(t.a,t.b))),contentAlignment=Alignment.Center){Icon(t.icon,null,tint=Color.White)}
            Text(strings.toolTitle(t.id),fontWeight=FontWeight.ExtraBold,fontSize=15.sp)
            Text(strings.toolSubtitle(t.id),fontSize=10.sp,color=MaterialTheme.colorScheme.onSurfaceVariant,minLines=2)
            if(t.pro)Text(strings.get("pro"),fontSize=9.sp,color=Color(0xFF7C3AED),fontWeight=FontWeight.ExtraBold)
        }
    }
}
@Composable private fun CompactTool(t:Tool,click:()->Unit){
    val strings = LocalUiText.current
    ListItem(headlineContent={Text(strings.toolTitle(t.id),fontWeight=FontWeight.Bold)},supportingContent={Text(strings.toolSubtitle(t.id),fontSize=11.sp)},leadingContent={Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(Brush.linearGradient(listOf(t.a,t.b))),contentAlignment=Alignment.Center){Icon(t.icon,null,tint=Color.White)}} ,trailingContent={Text(if(t.pro)strings.get("pro") else "›",fontSize=12.sp,fontWeight=FontWeight.ExtraBold,color=MaterialTheme.colorScheme.primary)},modifier=Modifier.clip(RoundedCornerShape(18.dp)).clickable{click()})
}
@Composable private fun ToolsScreen(premium:Boolean,open:(String)->Unit){LazyVerticalGrid(GridCells.Fixed(2),Modifier.fillMaxSize(),contentPadding=PaddingValues(14.dp),horizontalArrangement=Arrangement.spacedBy(12.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){items(CATALOG){ToolCard(it,premium,open)}}}
@Composable private fun Feature(title:String,text:String,icon:String){Card(shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surfaceVariant)){Row(Modifier.padding(16.dp)){Icon(if(icon=="Lock")Icons.Default.Lock else Icons.Default.Info,null,tint=MaterialTheme.colorScheme.primary);Spacer(Modifier.width(10.dp));Column{Text(title,fontWeight=FontWeight.Bold);Text(text,fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}}}}

@Composable private fun PremiumScreen(auth:AuthRepository,premium:Boolean,start:(String)->Unit,cancel:()->Unit){
    var busy by remember{mutableStateOf(false)}; var error by remember{mutableStateOf<String?>(null)}; val scope=rememberCoroutineScope()
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
        item{Card(shape=RoundedCornerShape(30.dp)){Box(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFF17102D),Color(0xFF6D28D9),Color(0xFF0E7490)))).padding(24.dp)){Column(verticalArrangement=Arrangement.spacedBy(9.dp)){Text("IMAGE TOOLS PRO",color=Color.White.copy(.7f),fontSize=10.sp,fontWeight=FontWeight.ExtraBold);Text(if(premium)"Your PRO workspace is active." else "Unlock the advanced toolbox.",color=Color.White,fontSize=26.sp,fontWeight=FontWeight.ExtraBold);Text("Secure PayPal checkout. Subscription status is verified by the backend.",color=Color.White.copy(.82f),fontSize=12.sp)}}}}
        item{Card(shape=RoundedCornerShape(22.dp)){Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(9.dp)){FeatureLine("Advanced blur and sharpen");FeatureLine("Rounded corners, PDF export and collage");FeatureLine("Priority creative workflows");FeatureLine("Cancel from the app")}}}
        item{if(!premium)Button({busy=true;error=null;scope.launch{val token=auth.idToken();if(token.isNullOrBlank())error="Please sign in again." else PaymentRepository.createSubscription(token).onSuccess{start(it.approveUrl)}.onFailure{error=if(it is PaymentException)it.message else "Could not start PayPal checkout."};busy=false}},enabled=!busy,modifier=Modifier.fillMaxWidth().height(54.dp),shape=RoundedCornerShape(17.dp)){Icon(Icons.Default.Payment,null);Spacer(Modifier.width(8.dp));Text(if(busy)"Connecting to PayPal…" else "Continue with PayPal",fontWeight=FontWeight.ExtraBold)}else OutlinedButton(cancel,modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(17.dp)){Text("Cancel subscription")}}
        error?.let{item{Text(it,color=MaterialTheme.colorScheme.error,fontSize=12.sp)}}
        item{Feature("Secure entitlement","PayPal secrets stay on the Cloudflare Worker; the APK only receives safe checkout links.","Lock")}
    }
}
@Composable private fun FeatureLine(t:String){Row(verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.CheckCircle,null,tint=Color(0xFF10B981),modifier=Modifier.size(20.dp));Spacer(Modifier.width(9.dp));Text(t,fontWeight=FontWeight.Medium)}}

@Composable private fun AccountScreen(auth:AuthRepository,premium:Boolean,openPremium:()->Unit){
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
        item{Card(shape=RoundedCornerShape(25.dp)){Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){Text("Account",fontSize=23.sp,fontWeight=FontWeight.ExtraBold);Text(auth.currentUser?.email.orEmpty(),color=MaterialTheme.colorScheme.onSurfaceVariant);Text(if(premium)"PRO ACTIVE" else "FREE PLAN",color=if(premium)Color(0xFF047857) else MaterialTheme.colorScheme.primary,fontSize=10.sp,fontWeight=FontWeight.ExtraBold)}}}
        item{Button(openPremium,Modifier.fillMaxWidth(),shape=RoundedCornerShape(16.dp)){Text(if(premium)"Manage Premium" else "See Premium")}}
        item{OutlinedButton({auth.signOut()},Modifier.fillMaxWidth(),shape=RoundedCornerShape(16.dp)){Text("Sign out")}}
        item{Feature("Account deletion","Use Support in Settings to request deletion of your account and associated server-side account data.","Info")}}
    }
}

@Composable
private fun SettingsScreen(
    darkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    language: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit
) {
    val strings = LocalUiText.current
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
            Header(strings.get("settings"), strings.get("language.help"))
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
            Setting(strings.get("settings.dark"), "Use a darker workspace", Icons.Default.DarkMode) {
                Switch(darkMode, onDarkModeChange)
            }
        }
        item {
            Setting(
                strings.get("settings.output"),
                folder ?: "Pictures/Image Tools",
                Icons.Default.Folder
            ) {
                TextButton(onClick = { picker.launch(null) }) {
                    Text(strings.get("choose.image"))
                }
            }
        }
        item { LinkCard("Privacy Policy", PRIVACY, Icons.Default.PrivacyTip) }
        item { LinkCard("Terms of Service", TERMS, Icons.Default.Gavel) }
        item { LinkCard("Support & account deletion", SUPPORT, Icons.Default.SupportAgent) }
    }
}

@Composable private fun SettingsScreen(dark:Boolean,toggle:(Boolean)->Unit){
    val c=LocalContext.current
    var folder by remember{mutableStateOf(OutputFolderStore.folderName(c))}
    val picker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()){u->if(u!=null){runCatching{c.contentResolver.takePersistableUriPermission(u,Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)};OutputFolderStore.saveTreeUri(c,u);folder=OutputFolderStore.folderName(c,u)}}
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
        item{Header("Settings","Tune the app before your next export")}
        item{Setting("Dark mode","Use a darker workspace",Icons.Default.DarkMode){Switch(dark,toggle)}}
        item{Setting("Output folder",folder?:"Pictures/Image Tools",Icons.Default.Folder){TextButton({picker.launch(null)}){Text("Choose")}}}
        item{LinkCard("Privacy Policy",PRIVACY,Icons.Default.PrivacyTip)}
        item{LinkCard("Terms of Service",TERMS,Icons.Default.Gavel)}
        item{LinkCard("Support & account deletion",SUPPORT,Icons.Default.SupportAgent)}
        item{Setting("App","Image Tools 1.5.0 • Android 10+",Icons.Default.Info){Text("OK",color=MaterialTheme.colorScheme.primary,fontWeight=FontWeight.Bold)}}
    }
}
@Composable private fun Setting(a:String,b:String,i:ImageVector,end:@Composable()->Unit){Card(shape=RoundedCornerShape(20.dp)){Row(Modifier.fillMaxWidth().padding(16.dp),verticalAlignment=Alignment.CenterVertically){Icon(i,null,tint=MaterialTheme.colorScheme.primary);Spacer(Modifier.width(11.dp));Column(Modifier.weight(1f)){Text(a,fontWeight=FontWeight.Bold);Text(b,fontSize=10.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};end()}}}
@Composable private fun LinkCard(t:String,url:String,i:ImageVector){val c=LocalContext.current;Card({c.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(url)))},shape=RoundedCornerShape(20.dp)){ListItem(leadingContent={Icon(i,null,tint=MaterialTheme.colorScheme.primary)},headlineContent={Text(t,fontWeight=FontWeight.Bold)},trailingContent={Icon(Icons.Default.OpenInNew,null)})}}

@Composable private fun AboutScreen(){LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(18.dp)){item{Card(shape=RoundedCornerShape(28.dp)){Column(Modifier.padding(22.dp),verticalArrangement=Arrangement.spacedBy(9.dp)){Text("About Image Tools",fontSize=25.sp,fontWeight=FontWeight.ExtraBold);Text("A focused Android toolbox for resizing, compression, conversion and image finishing.");Text("Version 1.5.0",fontWeight=FontWeight.ExtraBold,color=MaterialTheme.colorScheme.primary);Text("The refreshed design takes cues from successful image toolboxes: strong card hierarchy, grouped utilities, privacy/metadata actions, palette extraction, collage and PDF export. It keeps the implementation lightweight and local rather than copying another product.",fontSize=12.sp)}}}}}

@Composable private fun ToolWorkspace(tool:Tool,premium:Boolean,onBack:()->Unit,onNeedPremium:()->Unit){
    val c=LocalContext.current; val scope=rememberCoroutineScope()
    var src by remember{mutableStateOf<Uri?>(null)}; var bmp by remember{mutableStateOf<Bitmap?>(null)}; var preview by remember{mutableStateOf<Bitmap?>(null)}
    var pendingBytes by remember{mutableStateOf<ByteArray?>(null)}; var pendingFormat by remember{mutableStateOf<OutputFormat?>(null)}; var pendingPdf by remember{mutableStateOf(false)}
    var outUri by remember{mutableStateOf<Uri?>(null)}; var outMime by remember{mutableStateOf("image/*")}; var busy by remember{mutableStateOf(false)}
    var status by remember{mutableStateOf<String?>(null)}; var details by remember{mutableStateOf<String?>(null)}; var palette by remember{mutableStateOf(emptyList<Int>())}
    var width by remember{mutableStateOf("")}; var height by remember{mutableStateOf("")}; var keep by remember{mutableStateOf(true)}; var quality by remember{mutableFloatStateOf(82f)}
    var format by remember{mutableStateOf(OutputFormat.JPEG)}; var ratio by remember{mutableStateOf("Original")}; var angle by remember{mutableIntStateOf(90)}
    var fh by remember{mutableStateOf(false)}; var fv by remember{mutableStateOf(false)}; var filter by remember{mutableStateOf(ImageFilter.ORIGINAL)}
    var text by remember{mutableStateOf("IMAGE TOOLS")}; var opacity by remember{mutableFloatStateOf(65f)}; var position by remember{mutableStateOf("Bottom right")}
    var amount by remember{mutableFloatStateOf(0f)}; var px by remember{mutableFloatStateOf(18f)}; var border by remember{mutableFloatStateOf(24f)}; var radius by remember{mutableFloatStateOf(36f)}

    val pick=rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()){u->src=u;if(u!=null)scope.launch{bmp=withContext(Dispatchers.IO){ImageProcessor.decode(c,u)};preview=bmp;width=bmp?.width?.toString().orEmpty();height=bmp?.height?.toString().orEmpty();status=if(bmp!=null)"Image ready." else "Could not read the image.";details=null;palette=emptyList()}}
    val multi=rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(4)){us->if(us.isNotEmpty())scope.launch{busy=true;val bs=withContext(Dispatchers.IO){us.mapNotNull{ImageProcessor.decode(c,it)}};if(bs.size>=2){val x=withContext(Dispatchers.Default){AdvancedImageProcessor.collage(bs)};bmp=bs.first();src=us.first();preview=x;status="Collage ready. Tap Save."}else status="Select at least two images.";busy=false}}

    fun choose(){if(tool.pro&&!premium){onNeedPremium();return};if(tool.id=="collage")multi.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) else pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))}
    suspend fun prepareBitmap(b:Bitmap,out:OutputFormat=if(tool.id=="round" || tool.id=="background")OutputFormat.PNG else format,q:Int=if(tool.id=="compress")quality.toInt() else 100){
        pendingBytes=withContext(Dispatchers.Default){ImageProcessor.encode(b,out,q)}
        pendingFormat=out
        pendingPdf=false
        preview=b
        status="Preview ready."
    }
    suspend fun process(){
        if(tool.pro&&!premium){onNeedPremium();return}
        val b=bmp?:run{choose();return}
        busy=true;status="Processing…";details=null;palette=emptyList()
        try{
            when(tool.id){
                "details"->{val n=src?.let{withContext(Dispatchers.IO){ImageProcessor.sourceBytes(c,it)}};details="Resolution: "+b.width+" × "+b.height+"\\nAspect ratio: "+String.format("%.3f",b.width.toFloat()/b.height)+"\\n"+(if(n!=null)"Source size: "+ImageProcessor.humanBytes(n) else "Source size: unavailable");preview=b}
                "palette"->{palette=withContext(Dispatchers.Default){AdvancedImageProcessor.palette(b,5)};preview=b}
                "pdf"->{pendingBytes=withContext(Dispatchers.Default){AdvancedImageProcessor.pdfBytes(b)};pendingFormat=null;pendingPdf=true;preview=b;status="PDF preview ready."}
                "metadata"->{prepareBitmap(b,format,100);status="Preview ready. Exporting re-encodes the image and strips common metadata."}
                "resize"->{val w=width.toIntOrNull()?.coerceAtLeast(1)?:b.width;val h=if(keep)(b.height*(w.toFloat()/b.width)).toInt().coerceAtLeast(1) else height.toIntOrNull()?.coerceAtLeast(1)?:b.height;preview=ImageProcessor.resize(b,w,h)}
                "compress"->{prepareBitmap(b,format,quality.toInt())}
                "convert"->{prepareBitmap(b,format,95)}
                "crop"->{preview=ImageProcessor.cropCenter(b,ratio)}
                "rotate"->{preview=ImageProcessor.rotate(b,angle,fh,fv)}
                "filter"->{preview=ImageProcessor.filter(b,filter)}
                "watermark"->{preview=ImageProcessor.watermark(b,text,opacity.toInt(),position)}
                "brightness"->{preview=AdvancedImageProcessor.adjustColor(b,amount.toInt(),0,0,0)}
                "contrast"->{preview=AdvancedImageProcessor.adjustColor(b,0,amount.toInt(),0,0)}
                "saturation"->{preview=AdvancedImageProcessor.adjustColor(b,0,0,amount.toInt(),0)}
                "warmth"->{preview=AdvancedImageProcessor.adjustColor(b,0,0,0,amount.toInt())}
                "negative"->{preview=AdvancedImageProcessor.negative(b)}
                "blur"->{preview=AdvancedImageProcessor.blur(b,(amount/25f).toInt().coerceIn(1,4))}
                "sharpen"->{preview=AdvancedImageProcessor.sharpen(b,(amount/25f).toInt().coerceIn(1,3))}
                "pixelate"->{preview=AdvancedImageProcessor.pixelate(b,px.toInt())}
                "border"->{preview=AdvancedImageProcessor.addBorder(b,border.toInt(),android.graphics.Color.WHITE)}
                "round"->{preview=AdvancedImageProcessor.roundCorners(b,radius)}
                "ocr"->{ocr=withContext(Dispatchers.Default){OcrProcessor.recognize(b)};preview=b;status="OCR ready."}
                "exif"->{exif=withContext(Dispatchers.IO){ExifProcessor.read(c,src ?: error("Select an image first."))};preview=b;status="EXIF ready."}
                "background"->{val fg=withContext(Dispatchers.Default){BackgroundRemovalProcessor.removeBackground(b)};prepareBitmap(fg,OutputFormat.PNG,100)}
                "collage"->{status=status?: "Collage ready."}
            }
            if(tool.id in listOf("resize","crop","rotate","filter","watermark","brightness","contrast","saturation","warmth","negative","blur","sharpen","pixelate","border","round"))status="Preview ready. Tap Save."
        }catch(e:Exception){status="Could not process this image: "+(e.message?:"unknown error")}finally{busy=false}
    }
    fun exportPending(){
        val bytes=pendingBytes ?: return
        scope.launch {
            busy=true
            try{
                val tree=OutputFolderStore.getTreeUri(c)
                outUri=if(pendingPdf){
                    OutputExporter.savePdf(c,bytes,"image-tools",tree)
                }else{
                    OutputExporter.saveImage(c,bytes,pendingFormat ?: format,tool.id,preview?.width ?: 1,preview?.height ?: 1,tree).uri
                }
                outMime=if(pendingPdf)"application/pdf" else pendingFormat?.mime ?: "image/*"
                status="Saved successfully."
            }catch(e:Exception){
                status="Could not save the result."
            }finally{busy=false}
        }
    }

    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(14.dp),verticalArrangement=Arrangement.spacedBy(11.dp)){
        item{Row(verticalAlignment=Alignment.CenterVertically){IconButton(onBack){Icon(Icons.Default.ArrowBack,"Back")};Box(Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(Brush.linearGradient(listOf(tool.a,tool.b))),contentAlignment=Alignment.Center){Icon(tool.icon,null,tint=Color.White)};Spacer(Modifier.width(10.dp));Column(Modifier.weight(1f)){Text(tool.title,fontSize=21.sp,fontWeight=FontWeight.ExtraBold);Text(tool.subtitle,fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};if(tool.pro)Text("PRO",fontSize=9.sp,color=Color(0xFF7C3AED),fontWeight=FontWeight.ExtraBold)}}
        item{Card(shape=RoundedCornerShape(23.dp)){Column(Modifier.padding(15.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){if(preview!=null)Image(preview!!.asImageBitmap(),null,Modifier.fillMaxWidth().heightIn(min=180.dp,max=320.dp).clip(RoundedCornerShape(17.dp)),contentScale=ContentScale.Fit) else Box(Modifier.fillMaxWidth().height(190.dp).clip(RoundedCornerShape(17.dp)).background(MaterialTheme.colorScheme.surfaceVariant),contentAlignment=Alignment.Center){Column(horizontalAlignment=Alignment.CenterHorizontally){Icon(tool.icon,null,tint=MaterialTheme.colorScheme.primary,modifier=Modifier.size(40.dp));Text("Choose an image to start",fontWeight=FontWeight.Bold);Text("Core editing is processed locally.",fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}};Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Button({choose()},Modifier.weight(1f),shape=RoundedCornerShape(15.dp)){Text(if(src==null)"Choose image" else "Choose another")};if(preview!=null&&tool.id!in listOf("details","palette","pdf"))OutlinedButton({saveNow()},enabled=!busy,shape=RoundedCornerShape(15.dp)){Text("Save")}}}}
        if (tool.id !in listOf("details", "palette", "pdf", "collage")) {
            item {
                Controls(
                    tool, width, height,
                    { width = it }, { height = it },
                    keep, { keep = it },
                    quality, { quality = it },
                    format, { format = it },
                    ratio, { ratio = it },
                    angle, { angle = it },
                    fh, { fh = it },
                    fv, { fv = it },
                    filter, { filter = it },
                    text, { text = it },
                    opacity, { opacity = it },
                    position, { position = it },
                    amount, { amount = it },
                    px, { px = it },
                    border, { border = it },
                    radius, { radius = it }
                )
            }
        }
        if(tool.id=="collage")item{Card(shape=RoundedCornerShape(20.dp)){Column(Modifier.padding(16.dp)){Text("Quick collage",fontWeight=FontWeight.ExtraBold);Text("Choose 2–4 images and the app builds a clean grid.",fontSize=11.sp);Spacer(Modifier.height(8.dp));Button({choose()},Modifier.fillMaxWidth(),shape=RoundedCornerShape(15.dp)){Text("Choose 2–4 images")}}}}
        item{Button({scope.launch{process()}},enabled=!busy,modifier=Modifier.fillMaxWidth().height(52.dp),shape=RoundedCornerShape(17.dp)){Icon(Icons.Default.AutoAwesome,null);Spacer(Modifier.width(8.dp));Text(if(busy) "Working…" else when(tool.id){"pdf"->"Create PDF";"palette"->"Extract palette";"details"->"Read details";else->"Preview result"},fontWeight=FontWeight.ExtraBold)}}
        details?.let{item{Card{Column(Modifier.padding(16.dp)){Text("Image details",fontWeight=FontWeight.ExtraBold);Spacer(Modifier.height(6.dp));Text(it,fontSize=12.sp)}}}}
        if(palette.isNotEmpty())item{Card(shape=RoundedCornerShape(20.dp)){Column(Modifier.padding(15.dp)){Text("Dominant palette",fontWeight=FontWeight.ExtraBold);Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(7.dp)){palette.forEach{col->Column(Modifier.weight(1f),horizontalAlignment=Alignment.CenterHorizontally){Box(Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(12.dp)).background(Color(col)));Text("#%06X".format(col and 0xFFFFFF),fontSize=8.sp,fontWeight=FontWeight.Bold)}}}}}}
        outUri?.let{u->item{Card(shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=Color(0xFFECFDF5))){Column(Modifier.padding(15.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){Text("Export ready",fontSize=18.sp,fontWeight=FontWeight.ExtraBold);Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Button({runCatching{c.startActivity(Intent(Intent.ACTION_VIEW).apply{data=u;type=outMime;addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)})}}){Text("Open")};OutlinedButton({c.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply{type=outMime;putExtra(Intent.EXTRA_STREAM,u);addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)},"Share"))}){Text("Share")}}}}}}
        status?.let{item{Text(it,fontSize=12.sp,color=if(it.startsWith("Could")||it.startsWith("Select"))MaterialTheme.colorScheme.error else Color(0xFF047857),modifier=Modifier.padding(horizontal=4.dp))}}
        if(tool.pro&&!premium)item{Feature("PRO tool","Subscribe from Premium to unlock this workflow.","Info")}
    }
}

@Composable private fun Controls(t:Tool,w:String,h:String,ow:(String)->Unit,oh:(String)->Unit,keep:Boolean,ok:(Boolean)->Unit,q:Float,oq:(Float)->Unit,f:OutputFormat,of:(OutputFormat)->Unit,ratio:String,oratio:(String)->Unit,angle:Int,oangle:(Int)->Unit,fh:Boolean,ofh:(Boolean)->Unit,fv:Boolean,ofv:(Boolean)->Unit,filter:ImageFilter,ofilter:(ImageFilter)->Unit,text:String,otext:(String)->Unit,op:Float,oop:(Float)->Unit,pos:String,opos:(String)->Unit,amt:Float,oamt:(Float)->Unit,px:Float,opx:(Float)->Unit,border:Float,ob:(Float)->Unit,radius:Float,orad:(Float)->Unit){
    Card(shape=RoundedCornerShape(21.dp)){Column(Modifier.padding(15.dp),verticalArrangement=Arrangement.spacedBy(9.dp)){
        when(t.id){
            "resize"->{Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){OutlinedTextField(w,ow,Modifier.weight(1f),label={Text("Width")},singleLine=true);OutlinedTextField(h,oh,Modifier.weight(1f),label={Text("Height")},singleLine=true)};Row(verticalAlignment=Alignment.CenterVertically){Text("Keep aspect ratio",Modifier.weight(1f),fontWeight=FontWeight.Bold);Switch(keep,ok)}}
            "compress"->{Text("Quality "+q.toInt()+"%",fontWeight=FontWeight.ExtraBold);Slider(q,oq,valueRange=10f..100f);FormatChips(f,of,listOf(OutputFormat.JPEG,OutputFormat.WEBP))}
            "convert","metadata"->{Text(if(t.id=="metadata")"Re-encode to strip common metadata." else "Choose the export format.");FormatChips(f,of,OutputFormat.values().toList())}
            "crop"->{Text("Crop ratio");Choices(listOf("Original","1:1","4:5","16:9","9:16"),ratio,oratio)}
            "rotate"->{Choices(listOf("90","180","270"),angle.toString()){oangle(it.toInt())};Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){FilterChip(fh,{ofh(!fh)},label={Text("Mirror H")},modifier=Modifier.weight(1f));FilterChip(fv,{ofv(!fv)},label={Text("Mirror V")},modifier=Modifier.weight(1f))}}
            "filter"->{Choices(ImageFilter.values().map{it.label},filter.label){selected->ofilter(ImageFilter.values().first{it.label==selected})}}
            "watermark"->{OutlinedTextField(text,otext,Modifier.fillMaxWidth(),label={Text("Watermark text")},singleLine=true);Text("Opacity "+op.toInt()+"%");Slider(op,oop,valueRange=10f..100f);Choices(listOf("Top left","Top right","Center","Bottom left","Bottom right"),pos,opos)}
            "brightness","contrast","saturation","warmth"->{Text("Amount "+amt.toInt(),fontWeight=FontWeight.ExtraBold);Slider(amt,oamt,valueRange=-100f..100f)}
            "blur","sharpen"->{Text("Strength "+amt.toInt(),fontWeight=FontWeight.ExtraBold);Slider(amt,oamt,valueRange=0f..100f)}
            "pixelate"->{Text("Block size "+px.toInt()+" px",fontWeight=FontWeight.ExtraBold);Slider(px,opx,valueRange=4f..48f)}
            "border"->{Text("Border "+border.toInt()+" px",fontWeight=FontWeight.ExtraBold);Slider(border,ob,valueRange=4f..120f)}
            "round"->{Text("Radius "+radius.toInt()+" px",fontWeight=FontWeight.ExtraBold);Slider(radius,orad,valueRange=8f..160f)}
        }
    }}
}
@Composable
private fun FormatChips(
    sel: OutputFormat,
    on: (OutputFormat) -> Unit,
    opts: List<OutputFormat>
) {
    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        opts.forEach { option ->
            FilterChip(
                selected = sel == option,
                onClick = { on(option) },
                label = { Text(option.extension.uppercase()) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun Choices(
    opts: List<String>,
    sel: String,
    on: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        opts.forEach { value ->
            FilterChip(
                selected = sel == value,
                onClick = { on(value) },
                label = { Text(value, fontSize = 10.sp) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}
