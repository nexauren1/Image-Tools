package com.nexauren.imagetools.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

@Composable
fun ImageToolsTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    val colors = if (darkTheme) {
        darkColorScheme(
            primary = Color(0xFF8B7CFF),
            onPrimary = Color(0xFF151225),
            secondary = Color(0xFF22D3EE),
            tertiary = Color(0xFF34D399),
            background = Color(0xFF070A12),
            surface = Color(0xFF0E1422),
            surfaceVariant = Color(0xFF171E2E),
            onSurface = Color(0xFFF5F7FB),
            onSurfaceVariant = Color(0xFFB5BED0)
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF5B46F6),
            onPrimary = Color.White,
            secondary = Color(0xFF0891B2),
            tertiary = Color(0xFF059669),
            background = Color(0xFFF5F7FC),
            surface = Color(0xFFFFFFFF),
            surfaceVariant = Color(0xFFEEF2F8),
            onSurface = Color(0xFF101828),
            onSurfaceVariant = Color(0xFF667085)
        )
    }

    val typography = Typography(
        displayLarge = Typography().displayLarge.copy(fontWeight = FontWeight.ExtraBold),
        displayMedium = Typography().displayMedium.copy(fontWeight = FontWeight.ExtraBold),
        headlineLarge = Typography().headlineLarge.copy(fontWeight = FontWeight.ExtraBold),
        headlineMedium = Typography().headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
        titleLarge = Typography().titleLarge.copy(fontWeight = FontWeight.ExtraBold)
    )

    val shapes = Shapes(
        small = RoundedCornerShape(12.dp),
        medium = RoundedCornerShape(18.dp),
        large = RoundedCornerShape(28.dp)
    )

    MaterialTheme(colorScheme = colors, typography = typography, shapes = shapes, content = content)
}