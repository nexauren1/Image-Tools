package com.nexauren.imagetools.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ProcessingGearDialog(title: String, subtitle: String) {
    val transition = rememberInfiniteTransition(label = "processing-gear")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gear-rotation"
    )

    AlertDialog(
        onDismissRequest = {},
        confirmButton = {},
        icon = {
            Icon(
                Icons.Default.Settings,
                contentDescription = null,
                modifier = Modifier
                    .size(52.dp)
                    .graphicsLayer { rotationZ = rotation },
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(title, fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold)
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(subtitle, fontSize = 12.sp)
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    )
}
