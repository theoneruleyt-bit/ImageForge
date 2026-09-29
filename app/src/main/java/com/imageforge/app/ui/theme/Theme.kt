package com.imageforge.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF5B4FE9), onPrimary = Color.White,
    primaryContainer = Color(0xFFE7E3FF), onPrimaryContainer = Color(0xFF17104D),
    secondary = Color(0xFF565D78), secondaryContainer = Color(0xFFDEE2FF),
    background = Color(0xFFF9F9FD), surface = Color(0xFFF9F9FD), surfaceVariant = Color(0xFFE5E1EC)
)
private val DarkColors = darkColorScheme(
    primary = Color(0xFFC5BFFF), primaryContainer = Color(0xFF4238C6),
    secondary = Color(0xFFBEC5E5), secondaryContainer = Color(0xFF3E455E),
    background = Color(0xFF111116), surface = Color(0xFF111116), surfaceVariant = Color(0xFF47464F)
)

@Composable
fun ImageForgeTheme(dark: Boolean = false, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) DarkColors else LightColors, typography = Typography(), content = content)
}
