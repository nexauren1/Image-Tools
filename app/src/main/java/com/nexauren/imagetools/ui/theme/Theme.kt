package com.nexauren.imagetools.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ImageToolsTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    val colors = if (darkTheme) {
        darkColorScheme(
            primary = Color(0xFFB9A7FF),
            onPrimary = Color(0xFF2D1066),
            primaryContainer = Color(0xFF3C237B),
            onPrimaryContainer = Color(0xFFEADDFF),
            secondary = Color(0xFF7DD3FC),
            secondaryContainer = Color(0xFF164E63),
            tertiary = Color(0xFFF0ABFC),
            background = Color(0xFF070A12),
            surface = Color(0xFF0D1220),
            surfaceVariant = Color(0xFF1A2030),
            outline = Color(0xFF495166)
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF6750F5),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFE9DEFF),
            onPrimaryContainer = Color(0xFF25105E),
            secondary = Color(0xFF006A8B),
            secondaryContainer = Color(0xFFD0F1FF),
            tertiary = Color(0xFF9C2F7A),
            background = Color(0xFFF6F7FB),
            surface = Color.White,
            surfaceVariant = Color(0xFFEFF1F8),
            outline = Color(0xFF727888)
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
        extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
        small = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
        medium = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
        large = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
        extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(34.dp)
    )

    MaterialTheme(
        colorScheme = colors,
        typography = typography,
        shapes = shapes,
        content = content
    )
}
