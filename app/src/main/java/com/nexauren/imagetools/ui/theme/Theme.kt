package com.nexauren.imagetools.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun ImageToolsTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    val colors = if (darkTheme) {
        darkColorScheme(
            primary = Color(0xFFB9A7FF),
            secondary = Color(0xFF8DD7FF),
            background = Color(0xFF080B14),
            surface = Color(0xFF111625)
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF6750F5),
            secondary = Color(0xFF006A8B),
            background = Color(0xFFF8F9FD),
            surface = Color.White
        )
    }
    MaterialTheme(colorScheme = colors, typography = Typography(), content = content)
}