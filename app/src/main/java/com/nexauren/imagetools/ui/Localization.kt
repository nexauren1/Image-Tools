package com.nexauren.imagetools.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import java.util.Locale

enum class AppLanguage(val code: String, val nativeName: String) {
    ENGLISH("en", "English"),
    PORTUGUESE("pt", "Português"),
    SPANISH("es", "Español"),
    FRENCH("fr", "Français"),
    ARABIC("ar", "العربية")
}

data class UiStrings(
    val appName: String,
    val home: String,
    val tools: String,
    val settings: String,
    val account: String,
    val premium: String,
    val save: String,
    val export: String,
    val chooseImage: String,
    val chooseImages: String,
    val apply: String,
    val ready: String,
    val processing: String,
    val saved: String,
    val saveFailed: String,
    val defaultSaved: String,
    val outputFolder: String,
    val changeFolder: String,
    val language: String,
    val darkMode: String,
    val localProcessing: String,
    val free: String,
    val pro: String,
    val imageEditor: String,
    val quickActions: String,
    val photo: String,
    val resize: String,
    val compress: String,
    val convert: String,
    val crop: String,
    val rotate: String,
    val filters: String,
    val details: String,
    val watermark: String,
    val adjust: String,
    val collage: String,
    val frame: String,
    val meme: String,
    val pixelate: String,
    val about: String,
    val error: String
)

private val EN = UiStrings(
    appName = "Image Tools",
    home = "Home",
    tools = "Tools",
    settings = "Settings",
    account = "Account",
    premium = "Premium",
    save = "Save",
    export = "Export",
    chooseImage = "Choose image",
    chooseImages = "Choose images",
    apply = "Apply",
    ready = "Ready",
    processing = "Processing…",
    saved = "Saved",
    saveFailed = "Save failed",
    defaultSaved = "Saved to Pictures / Image Tools",
    outputFolder = "Output folder",
    changeFolder = "Change folder",
    language = "Language",
    darkMode = "Dark mode",
    localProcessing = "Local processing",
    free = "Free",
    pro = "PRO",
    imageEditor = "Image editor",
    quickActions = "Quick actions",
    photo = "Photo",
    resize = "Resize Image",
    compress = "Compress",
    convert = "Convert Format",
    crop = "Smart Crop",
    rotate = "Rotate & Flip",
    filters = "Quick Filters",
    details = "Image Details",
    watermark = "Smart Watermark",
    adjust = "Adjust",
    collage = "Collage",
    frame = "Frame",
    meme = "Meme",
    pixelate = "Pixelate",
    about = "About",
    error = "Something went wrong"
)

private val PT = EN.copy(
    home = "Início", tools = "Ferramentas", settings = "Definições", account = "Conta",
    premium = "Premium", save = "Guardar", export = "Exportar", chooseImage = "Escolher imagem",
    chooseImages = "Escolher imagens", apply = "Aplicar", ready = "Pronto", processing = "A processar…",
    saved = "Guardado", saveFailed = "Falha ao guardar", defaultSaved = "Guardado em Imagens / Image Tools",
    outputFolder = "Pasta de saída", changeFolder = "Alterar pasta", language = "Idioma", darkMode = "Modo escuro",
    localProcessing = "Processamento local", free = "Grátis", imageEditor = "Editor de imagem",
    quickActions = "Ações rápidas", photo = "Foto", resize = "Redimensionar", compress = "Comprimir",
    convert = "Converter formato", crop = "Corte inteligente", rotate = "Rodar e inverter",
    filters = "Filtros rápidos", details = "Detalhes da imagem", watermark = "Marca de água",
    adjust = "Ajustar", collage = "Colagem", frame = "Moldura", meme = "Meme", pixelate = "Pixelizar",
    about = "Sobre", error = "Algo correu mal"
)

private val ES = EN.copy(
    home = "Inicio", tools = "Herramientas", settings = "Ajustes", account = "Cuenta",
    premium = "Premium", save = "Guardar", export = "Exportar", chooseImage = "Elegir imagen",
    chooseImages = "Elegir imágenes", apply = "Aplicar", ready = "Listo", processing = "Procesando…",
    saved = "Guardado", saveFailed = "No se pudo guardar", defaultSaved = "Guardado en Imágenes / Image Tools",
    outputFolder = "Carpeta de salida", changeFolder = "Cambiar carpeta", language = "Idioma", darkMode = "Modo oscuro",
    localProcessing = "Procesamiento local", free = "Gratis", imageEditor = "Editor de imágenes",
    quickActions = "Acciones rápidas", photo = "Foto", resize = "Redimensionar", compress = "Comprimir",
    convert = "Convertir formato", crop = "Recorte inteligente", rotate = "Girar y voltear",
    filters = "Filtros rápidos", details = "Detalles de imagen", watermark = "Marca de agua",
    adjust = "Ajustar", collage = "Collage", frame = "Marco", meme = "Meme", pixelate = "Pixelar",
    about = "Acerca de", error = "Algo salió mal"
)

private val FR = EN.copy(
    home = "Accueil", tools = "Outils", settings = "Réglages", account = "Compte",
    premium = "Premium", save = "Enregistrer", export = "Exporter", chooseImage = "Choisir une image",
    chooseImages = "Choisir des images", apply = "Appliquer", ready = "Prêt", processing = "Traitement…",
    saved = "Enregistré", saveFailed = "Échec de l’enregistrement", defaultSaved = "Enregistré dans Images / Image Tools",
    outputFolder = "Dossier de sortie", changeFolder = "Changer de dossier", language = "Langue", darkMode = "Mode sombre",
    localProcessing = "Traitement local", free = "Gratuit", imageEditor = "Éditeur d’image",
    quickActions = "Actions rapides", photo = "Photo", resize = "Redimensionner", compress = "Compresser",
    convert = "Convertir", crop = "Recadrage intelligent", rotate = "Rotation et miroir",
    filters = "Filtres rapides", details = "Détails de l’image", watermark = "Filigrane",
    adjust = "Ajuster", collage = "Collage", frame = "Cadre", meme = "Mème", pixelate = "Pixeliser",
    about = "À propos", error = "Une erreur est survenue"
)

private val AR = EN.copy(
    home = "الرئيسية", tools = "الأدوات", settings = "الإعدادات", account = "الحساب",
    premium = "Premium", save = "حفظ", export = "تصدير", chooseImage = "اختيار صورة",
    chooseImages = "اختيار صور", apply = "تطبيق", ready = "جاهز", processing = "جارٍ المعالجة…",
    saved = "تم الحفظ", saveFailed = "تعذر الحفظ", defaultSaved = "تم الحفظ في الصور / Image Tools",
    outputFolder = "مجلد الإخراج", changeFolder = "تغيير المجلد", language = "اللغة", darkMode = "الوضع الداكن",
    localProcessing = "المعالجة المحلية", free = "مجاني", imageEditor = "محرر الصور",
    quickActions = "إجراءات سريعة", photo = "صورة", resize = "تغيير الحجم", compress = "ضغط",
    convert = "تحويل الصيغة", crop = "قص ذكي", rotate = "تدوير وقلب", filters = "فلاتر سريعة",
    details = "تفاصيل الصورة", watermark = "علامة مائية", adjust = "ضبط", collage = "كولاج",
    frame = "إطار", meme = "ميم", pixelate = "بكسلة", about = "حول التطبيق", error = "حدث خطأ"
)

private fun stringsFor(language: AppLanguage): UiStrings = when (language) {
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
        runCatching {
            AppLanguage.valueOf(
                prefs.getString(LANG_KEY, AppLanguage.ENGLISH.name) ?: AppLanguage.ENGLISH.name
            )
        }.getOrDefault(AppLanguage.ENGLISH)
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
