package com.imageforge.app.ui.theme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Light = lightColorScheme(primary=Color(0xFF6558E8), secondary=Color(0xFF5D5FEF), background=Color(0xFFF7F7FA), surface=Color.White, surfaceVariant=Color(0xFFF0F0F5))
private val Dark = darkColorScheme(primary=Color(0xFFB8AEFF), background=Color(0xFF111116), surface=Color(0xFF19191F), surfaceVariant=Color(0xFF24242C))
@Composable fun ImageForgeTheme(dark:Boolean=false, content:@Composable()->Unit){ MaterialTheme(colorScheme=if(dark) Dark else Light, typography=Typography(), content=content) }
