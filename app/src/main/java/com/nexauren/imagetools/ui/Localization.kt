package com.nexauren.imagetools.ui

import android.content.Context
import androidx.compose.runtime.*
import java.util.Locale

enum class AppLanguage(val code: String, val nativeName: String) {
    ENGLISH("en", "English"),
    PORTUGUESE("pt", "Português"),
    SPANISH("es", "Español"),
    FRENCH("fr", "Français"),
    ARABIC("ar", "العربية")
}

data class UiStrings(
    val home: String, val tools: String, val settings: String, val account: String,
    val premium: String, val save: String, val export: String, val openImage: String,
    val chooseImage: String, val chooseImages: String, val apply: String, val reset: String,
    val undo: String, val ready: String, val processing: String, val saved: String,
    val saveFailed: String, val defaultSaved: String, val outputFolder: String,
    val changeFolder: String, val language: String, val darkMode: String,
    val localProcessing: String, val signOut: String, val signIn: String,
    val createAccount: String, val email: String, val password: String, val forgotPassword: String,
    val displayName: String, val update: String, val verification: String,
    val resendVerification: String, val verified: String, val notVerified: String,
    val free: String, val pro: String, val included: String, val monthly: String,
    val subscribe: String, val cancel: String, val imageEditor: String, val quickActions: String,
    val creative: String, val utilities: String, val photo: String, val recent: String,
    val noRecent: String, val resize: String, val resizeSub: String,
    val compress: String, val compressSub: String, val convert: String, val convertSub: String,
    val crop: String, val cropSub: String, val rotate: String, val rotateSub: String,
    val filters: String, val filtersSub: String, val details: String, val detailsSub: String,
    val watermark: String, val watermarkSub: String, val adjust: String, val adjustSub: String,
    val collage: String, val collageSub: String, val frame: String, val frameSub: String,
    val meme: String, val memeSub: String, val pixelate: String, val pixelateSub: String,
    val outputFormat: String, val quality: String, val width: String, val height: String,
    val keepRatio: String, val original: String, val brightness: String, val contrast: String,
    val saturation: String, val ratio: String, val angle: String, val flip: String,
    val horizontal: String, val vertical: String, val filterStyle: String, val watermarkText: String,
    val position: String, val topLeft: String, val topRight: String, val center: String,
    val bottomLeft: String, val bottomRight: String, val border: String, val borderWidth: String,
    val background: String, val topText: String, val bottomText: String, val blockSize: String,
    val chooseRatio: String, val exportHint: String, val appName: String,
    val about: String, val version: String, val privacy: String, val accountStatus: String,
    val secureAccount: String, val resetPassword: String, val send: String, val success: String,
    val error: String
)

private val EN = UiStrings(
    "Home","Tools","Settings","Account","Premium","Save","Export","Open image","Choose image","Choose images",
    "Apply","Reset","Undo","Ready","Processing…","Saved","Save failed","Saved to Pictures / Image Tools","Output folder",
    "Change folder","Language","Dark mode","Local processing","Sign out","Sign in","Create account","Email","Password",
    "Forgot password?","Display name","Update","Email verification","Resend verification","Verified","Not verified",
    "Free","PRO","Included","US$5 / month","Subscribe","Cancel","Image editor","Quick actions","Creative","Utilities",
    "Photo","Recent","No recent edits yet","Resize Image","Set exact dimensions and export cleanly","Compress",
    "Reduce file size without leaving the app","Convert Format","Switch JPEG, PNG or WEBP","Smart Crop",
    "Crop for social, portraits and banners","Rotate & Flip","Straighten, rotate or mirror","Quick Filters",
    "Fast looks for everyday photos","Image Details","Inspect size, format and dimensions","Smart Watermark",
    "Add a clean brand mark to an image","Adjust","Tune light and color with live controls","Collage",
    "Combine multiple photos into one layout","Frame","Add borders, cards and backgrounds","Meme",
    "Build a simple captioned image","Pixelate","Create a pixel-art style effect","Output format","Quality",
    "Width","Height","Keep ratio","Original","Brightness","Contrast","Saturation","Ratio","Angle","Flip",
    "Horizontal","Vertical","Filter style","Watermark text","Position","Top left","Top right","Center",
    "Bottom left","Bottom right","Border","Border width","Background","Top text","Bottom text","Block size",
    "Choose a ratio","Tap Export when the preview is ready","Image Tools","About","Version","Privacy","Account status",
    "Your account and plan are synced securely","Secure account","Reset password","Send","Success","Error"
)

private val PT = EN.copy(
    home="Início", tools="Ferramentas", settings="Definições", account="Conta", premium="Premium", save="Guardar", export="Exportar",
    openImage="Abrir imagem", chooseImage="Escolher imagem", chooseImages="Escolher imagens", apply="Aplicar", reset="Repor", undo="Desfazer",
    ready="Pronto", processing="A processar…", saved="Guardado", saveFailed="Falha ao guardar", defaultSaved="Guardado em Imagens / Image Tools",
    outputFolder="Pasta de saída", changeFolder="Alterar pasta", language="Idioma", darkMode="Modo escuro", localProcessing="Processamento local",
    signOut="Terminar sessão", signIn="Iniciar sessão", createAccount="Criar conta", email="E-mail", password="Palavra-passe",
    forgotPassword="Esqueceu a palavra-passe?", displayName="Nome", update="Atualizar", verification="Verificação de e-mail",
    resendVerification="Reenviar verificação", verified="Verificado", notVerified="Não verificado", free="Grátis", pro="PRO", included="Incluído",
    monthly="US$5 / mês", subscribe="Subscrever", cancel="Cancelar", imageEditor="Editor de imagem", quickActions="Ações rápidas",
    creative="Criativo", utilities="Utilitários", photo="Foto", recent="Recentes", noRecent="Ainda não existem edições recentes",
    resize="Redimensionar", resizeSub="Defina dimensões exatas e exporte", compress="Comprimir", compressSub="Reduza o tamanho do ficheiro",
    convert="Converter formato", convertSub="Alterne entre JPEG, PNG ou WEBP", crop="Corte inteligente", cropSub="Cortes para redes sociais",
    rotate="Rodar e inverter", rotateSub="Endireite, rode ou espelhe", filters="Filtros rápidos", filtersSub="Estilos rápidos para fotos",
    details="Detalhes da imagem", detailsSub="Veja tamanho, formato e dimensões", watermark="Marca de água",
    watermarkSub="Adicione uma marca à imagem", adjust="Ajustar", adjustSub="Controle luz e cor", collage="Colagem",
    collageSub="Junte várias fotos num só layout", frame="Moldura", frameSub="Adicione bordas e fundos", meme="Meme",
    memeSub="Crie uma imagem com legendas", pixelate="Pixelizar", pixelateSub="Crie um efeito de pixel",
    outputFormat="Formato de saída", quality="Qualidade", width="Largura", height="Altura", keepRatio="Manter proporção",
    original="Original", brightness="Brilho", contrast="Contraste", saturation="Saturação", ratio="Proporção", angle="Ângulo",
    flip="Inverter", horizontal="Horizontal", vertical="Vertical", filterStyle="Estilo do filtro", watermarkText="Texto da marca",
    position="Posição", topLeft="Superior esquerdo", topRight="Superior direito", center="Centro", bottomLeft="Inferior esquerdo",
    bottomRight="Inferior direito", border="Borda", borderWidth="Largura da borda", background="Fundo", topText="Texto superior",
    bottomText="Texto inferior", blockSize="Tamanho do bloco", chooseRatio="Escolha uma proporção",
    exportHint="Toque em Exportar quando a pré-visualização estiver pronta", appName="Image Tools", about="Sobre",
    version="Versão", privacy="Privacidade", accountStatus="Estado da conta", secureAccount="Conta segura",
    resetPassword="Redefinir palavra-passe", send="Enviar", success="Sucesso", error="Erro"
)

private val ES = EN.copy(
    home="Inicio", tools="Herramientas", settings="Ajustes", account="Cuenta", premium="Premium", save="Guardar", export="Exportar",
    openImage="Abrir imagen", chooseImage="Elegir imagen", chooseImages="Elegir imágenes", apply="Aplicar", reset="Restablecer", undo="Deshacer",
    ready="Listo", processing="Procesando…", saved="Guardado", saveFailed="No se pudo guardar", defaultSaved="Guardado en Imágenes / Image Tools",
    outputFolder="Carpeta de salida", changeFolder="Cambiar carpeta", language="Idioma", darkMode="Modo oscuro", localProcessing="Procesamiento local",
    signOut="Cerrar sesión", signIn="Iniciar sesión", createAccount="Crear cuenta", email="Correo", password="Contraseña",
    forgotPassword="¿Olvidaste la contraseña?", displayName="Nombre", update="Actualizar", verification="Verificación de correo",
    resendVerification="Reenviar verificación", verified="Verificado", notVerified="No verificado", free="Gratis", pro="PRO", included="Incluido",
    monthly="US$5 / mes", subscribe="Suscribirse", cancel="Cancelar", imageEditor="Editor de imágenes", quickActions="Acciones rápidas",
    creative="Creativo", utilities="Utilidades", photo="Foto", recent="Recientes", noRecent="Aún no hay ediciones recientes",
    resize="Redimensionar", resizeSub="Define dimensiones exactas y exporta", compress="Comprimir", compressSub="Reduce el tamaño del archivo",
    convert="Convertir formato", convertSub="Cambia entre JPEG, PNG o WEBP", crop="Recorte inteligente", cropSub="Recorta para redes, retratos y banners",
    rotate="Girar y voltear", rotateSub="Endereza, gira o refleja", filters="Filtros rápidos", filtersSub="Estilos rápidos para fotos",
    details="Detalles de imagen", detailsSub="Mira tamaño, formato y dimensiones", watermark="Marca de agua",
    watermarkSub="Añade una marca limpia a la imagen", adjust="Ajustar", adjustSub="Controla luz y color", collage="Collage",
    collageSub="Combina varias fotos en un diseño", frame="Marco", frameSub="Añade bordes y fondos", meme="Meme",
    memeSub="Crea una imagen con texto", pixelate="Pixelar", pixelateSub="Crea un efecto de píxeles",
    outputFormat="Formato de salida", quality="Calidad", width="Ancho", height="Alto", keepRatio="Mantener proporción",
    original="Original", brightness="Brillo", contrast="Contraste", saturation="Saturación", ratio="Proporción", angle="Ángulo",
    flip="Voltear", horizontal="Horizontal", vertical="Vertical", filterStyle="Estilo del filtro", watermarkText="Texto de marca",
    position="Posición", topLeft="Arriba izquierda", topRight="Arriba derecha", center="Centro", bottomLeft="Abajo izquierda",
    bottomRight="Abajo derecha", border="Borde", borderWidth="Ancho del borde", background="Fondo", topText="Texto superior",
    bottomText="Texto inferior", blockSize="Tamaño del bloque", chooseRatio="Elige una proporción",
    exportHint="Toca Exportar cuando la vista previa esté lista", appName="Image Tools", about="Acerca de",
    version="Versión", privacy="Privacidad", accountStatus="Estado de la cuenta", secureAccount="Cuenta segura",
    resetPassword="Restablecer contraseña", send="Enviar", success="Éxito", error="Error"
)

private val FR = EN.copy(
    home="Accueil", tools="Outils", settings="Réglages", account="Compte", premium="Premium", save="Enregistrer", export="Exporter",
    openImage="Ouvrir une image", chooseImage="Choisir une image", chooseImages="Choisir des images", apply="Appliquer", reset="Réinitialiser",
    undo="Annuler", ready="Prêt", processing="Traitement…", saved="Enregistré", saveFailed="Échec de l’enregistrement",
    defaultSaved="Enregistré dans Images / Image Tools", outputFolder="Dossier de sortie", changeFolder="Changer de dossier",
    language="Langue", darkMode="Mode sombre", localProcessing="Traitement local", signOut="Se déconnecter", signIn="Se connecter",
    createAccount="Créer un compte", email="E-mail", password="Mot de passe", forgotPassword="Mot de passe oublié ?", displayName="Nom",
    update="Mettre à jour", verification="Vérification de l’e-mail", resendVerification="Renvoyer la vérification", verified="Vérifié",
    notVerified="Non vérifié", free="Gratuit", pro="PRO", included="Inclus", monthly="5 $US / mois", subscribe="S’abonner",
    cancel="Annuler", imageEditor="Éditeur d’image", quickActions="Actions rapides", creative="Créatif", utilities="Utilitaires",
    photo="Photo", recent="Récents", noRecent="Aucune modification récente", resize="Redimensionner", resizeSub="Dimensions précises et export",
    compress="Compresser", compressSub="Réduire la taille du fichier", convert="Convertir", convertSub="Passer de JPEG à PNG ou WEBP",
    crop="Recadrage intelligent", cropSub="Recadrages pour réseaux et portraits", rotate="Rotation et miroir", rotateSub="Redresser, tourner ou refléter",
    filters="Filtres rapides", filtersSub="Styles photo instantanés", details="Détails de l’image", detailsSub="Taille, format et dimensions",
    watermark="Filigrane", watermarkSub="Ajouter une marque propre", adjust="Ajuster", adjustSub="Lumière et couleur",
    collage="Collage", collageSub="Combiner plusieurs photos", frame="Cadre", frameSub="Bordures et arrière-plans", meme="Mème",
    memeSub="Créer une image légendée", pixelate="Pixeliser", pixelateSub="Créer un effet pixel",
    outputFormat="Format de sortie", quality="Qualité", width="Largeur", height="Hauteur", keepRatio="Conserver les proportions",
    original="Original", brightness="Luminosité", contrast="Contraste", saturation="Saturation", ratio="Proportion", angle="Angle",
    flip="Miroir", horizontal="Horizontal", vertical="Vertical", filterStyle="Style de filtre", watermarkText="Texte du filigrane",
    position="Position", topLeft="Haut gauche", topRight="Haut droit", center="Centre", bottomLeft="Bas gauche", bottomRight="Bas droit",
    border="Bordure", borderWidth="Largeur de bordure", background="Arrière-plan", topText="Texte haut", bottomText="Texte bas",
    blockSize="Taille du bloc", chooseRatio="Choisir un ratio", exportHint="Touchez Exporter lorsque l’aperçu est prêt",
    appName="Image Tools", about="À propos", version="Version", privacy="Confidentialité", accountStatus="État du compte",
    secureAccount="Compte sécurisé", resetPassword="Réinitialiser le mot de passe", send="Envoyer", success="Succès", error="Erreur"
)

private val AR = EN.copy(
    home="الرئيسية", tools="الأدوات", settings="الإعدادات", account="الحساب", premium="Premium", save="حفظ", export="تصدير",
    openImage="فتح صورة", chooseImage="اختيار صورة", chooseImages="اختيار صور", apply="تطبيق", reset="إعادة ضبط", undo="تراجع",
    ready="جاهز", processing="جارٍ المعالجة…", saved="تم الحفظ", saveFailed="تعذر الحفظ",
    defaultSaved="تم الحفظ في الصور / Image Tools", outputFolder="مجلد الإخراج", changeFolder="تغيير المجلد", language="اللغة",
    darkMode="الوضع الداكن", localProcessing="معالجة محلية", signOut="تسجيل الخروج", signIn="تسجيل الدخول", createAccount="إنشاء حساب",
    email="البريد الإلكتروني", password="كلمة المرور", forgotPassword="نسيت كلمة المرور؟", displayName="الاسم", update="تحديث",
    verification="التحقق من البريد", resendVerification="إعادة إرسال التحقق", verified="تم التحقق", notVerified="غير متحقق",
    free="مجاني", pro="PRO", included="مشمول", monthly="5$ / شهر", subscribe="اشتراك", cancel="إلغاء",
    imageEditor="محرر الصور", quickActions="إجراءات سريعة", creative="إبداعي", utilities="أدوات مساعدة", photo="صورة", recent="الأخيرة",
    noRecent="لا توجد تعديلات حديثة", resize="تغيير الحجم", resizeSub="أبعاد دقيقة وتصدير", compress="ضغط", compressSub="تقليل حجم الملف",
    convert="تحويل الصيغة", convertSub="JPEG وPNG وWEBP", crop="قص ذكي", cropSub="قص للمنشورات والصور", rotate="تدوير وقلب",
    rotateSub="تدوير أو عكس الصورة", filters="فلاتر سريعة", filtersSub="أنماط سريعة للصور", details="تفاصيل الصورة",
    detailsSub="الحجم والصيغة والأبعاد", watermark="علامة مائية", watermarkSub="إضافة علامة للصور", adjust="ضبط",
    adjustSub="الضوء والألوان", collage="كولاج", collageSub="دمج عدة صور", frame="إطار", frameSub="حدود وخلفيات", meme="ميم",
    memeSub="إنشاء صورة بنص", pixelate="بكسلة", pixelateSub="تأثير بكسل",
    outputFormat="صيغة الإخراج", quality="الجودة", width="العرض", height="الارتفاع", keepRatio="الحفاظ على النسبة",
    original="الأصل", brightness="السطوع", contrast="التباين", saturation="التشبع", ratio="النسبة", angle="الزاوية",
    flip="قلب", horizontal="أفقي", vertical="عمودي", filterStyle="نمط الفلتر", watermarkText="نص العلامة",
    position="الموضع", topLeft="أعلى اليسار", topRight="أعلى اليمين", center="الوسط", bottomLeft="أسفل اليسار", bottomRight="أسفل اليمين",
    border="الحدود", borderWidth="عرض الحدود", background="الخلفية", topText="النص العلوي", bottomText="النص السفلي",
    blockSize="حجم البكسل", chooseRatio="اختر النسبة", exportHint="اضغط تصدير عندما تصبح المعاينة جاهزة",
    appName="Image Tools", about="حول التطبيق", version="الإصدار", privacy="الخصوصية", accountStatus="حالة الحساب",
    secureAccount="حساب آمن", resetPassword="إعادة تعيين كلمة المرور", send="إرسال", success="نجاح", error="خطأ"
)

private fun stringsFor(language: AppLanguage): UiStrings = when(language) {
    AppLanguage.ENGLISH -> EN
    AppLanguage.PORTUGUESE -> PT
    AppLanguage.SPANISH -> ES
    AppLanguage.FRENCH -> FR
    AppLanguage.ARABIC -> AR
}

private const val PREFS = "image_tools_app"
private const val LANG_KEY = "language"

@Composable
fun rememberAppLanguage(context: Context): MutableState<AppLanguage> {
    val prefs = remember { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
    val stored = remember {
        runCatching { AppLanguage.valueOf(prefs.getString(LANG_KEY, AppLanguage.ENGLISH.name) ?: AppLanguage.ENGLISH.name) }
            .getOrDefault(AppLanguage.ENGLISH)
    }
    val state = remember { mutableStateOf(stored) }
    LaunchedEffect(state.value) {
        prefs.edit().putString(LANG_KEY, state.value.name).apply()
    }
    return state
}

@Composable
fun rememberStrings(language: AppLanguage): UiStrings = remember(language) { stringsFor(language) }

fun languageLabel(language: AppLanguage): String = language.nativeName

fun languageTag(language: AppLanguage): String = Locale(language.code).displayLanguage
