package com.nexauren.imagetools.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val StudioViolet = Color(0xFF7653F6)
private val StudioCyan = Color(0xFF16B9F4)
private val StudioPink = Color(0xFFE945A8)
private val StudioNight = Color(0xFF070A14)

@Composable
fun ImageToolsTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    val colors = if (darkTheme) {
        darkColorScheme(
            primary = Color(0xFF43C8FF),
            onPrimary = Color(0xFF002A42),
            primaryContainer = Color(0xFF123E68),
            onPrimaryContainer = Color(0xFFD5F2FF),
            secondary = Color(0xFFB69AFF),
            onSecondary = Color(0xFF2C155C),
            secondaryContainer = Color(0xFF39236B),
            onSecondaryContainer = Color(0xFFEBDDFF),
            tertiary = Color(0xFFFF75C2),
            onTertiary = Color(0xFF4C1233),
            background = StudioNight,
            surface = Color(0xFF101629),
            surfaceVariant = Color(0xFF19223A),
            onSurface = Color(0xFFF4F6FF),
            onSurfaceVariant = Color(0xFFB6C2D9),
            outline = Color(0xFF465570),
            error = Color(0xFFFF8A9A),
            errorContainer = Color(0xFF522331),
            onErrorContainer = Color(0xFFFFDADF)
        )
    } else {
        lightColorScheme(
            primary = StudioViolet,
            onPrimary = Color.White,
            primaryContainer = Color(0xFFE9E2FF),
            onPrimaryContainer = Color(0xFF241255),
            secondary = Color(0xFF087FAD),
            onSecondary = Color.White,
            secondaryContainer = Color(0xFFD9F2FF),
            onSecondaryContainer = Color(0xFF06364C),
            tertiary = StudioPink,
            onTertiary = Color.White,
            background = Color(0xFFF4F6FC),
            surface = Color.White,
            surfaceVariant = Color(0xFFEDF0F8),
            onSurface = Color(0xFF11182B),
            onSurfaceVariant = Color(0xFF58647C),
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
