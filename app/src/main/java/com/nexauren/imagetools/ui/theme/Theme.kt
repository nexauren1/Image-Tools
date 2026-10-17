package com.nexauren.imagetools.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val StudioViolet = Color(0xFF6548E8)
private val StudioCyan = Color(0xFF087F8C)
private val StudioPink = Color(0xFFBD477A)
private val StudioNight = Color(0xFF080C18)

@Composable
fun ImageToolsTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    val colors = if (darkTheme) {
        darkColorScheme(
            primary = Color(0xFFB7A7FF),
            onPrimary = Color(0xFF201052),
            primaryContainer = Color(0xFF36236F),
            onPrimaryContainer = Color(0xFFECE5FF),
            secondary = Color(0xFF66E8E1),
            onSecondary = Color(0xFF003737),
            secondaryContainer = Color(0xFF123E43),
            onSecondaryContainer = Color(0xFFBFF8F3),
            tertiary = Color(0xFFFFA6D1),
            onTertiary = Color(0xFF4A0C2D),
            background = StudioNight,
            surface = Color(0xFF11182A),
            surfaceVariant = Color(0xFF1A253A),
            onSurface = Color(0xFFF1F4FF),
            onSurfaceVariant = Color(0xFFB1BCD2),
            outline = Color(0xFF465570),
            error = Color(0xFFFF8A9A),
            errorContainer = Color(0xFF522331),
            onErrorContainer = Color(0xFFFFDADF)
        )
    } else {
        lightColorScheme(
            primary = StudioViolet,
            onPrimary = Color.White,
            primaryContainer = Color(0xFFE8E1FF),
            onPrimaryContainer = Color(0xFF241255),
            secondary = StudioCyan,
            onSecondary = Color.White,
            secondaryContainer = Color(0xFFD3F7F4),
            onSecondaryContainer = Color(0xFF063D40),
            tertiary = StudioPink,
            onTertiary = Color.White,
            background = Color(0xFFF5F6FC),
            surface = Color.White,
            surfaceVariant = Color(0xFFEEF0F8),
            onSurface = Color(0xFF14182B),
            onSurfaceVariant = Color(0xFF59647C),
            outline = Color(0xFFA5AEC3),
            error = Color(0xFFB4233F),
            errorContainer = Color(0xFFFFE1E6),
            onErrorContainer = Color(0xFF5D1021)
        )
    }

    val typography = Typography().run {
        copy(
            headlineSmall = headlineSmall.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold),
            titleLarge = titleLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold),
            titleMedium = titleMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
        )
    }

    val shapes = Shapes(
        extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(9.dp),
        small = androidx.compose.foundation.shape.RoundedCornerShape(13.dp),
        medium = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
        large = androidx.compose.foundation.shape.RoundedCornerShape(26.dp),
        extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(32.dp)
    )

    MaterialTheme(
        colorScheme = colors,
        typography = typography,
        shapes = shapes,
        content = content
    )
}
