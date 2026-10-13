package com.seagull.clip.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// 靛蓝主色，和 htv(视频) / cili(薄荷青) / paper(学术) 区分开
private val Indigo = Color(0xFF3B5BDB)
private val IndigoDark = Color(0xFF9DB2FF)
private val Mint = Color(0xFF2F9E8F)

private val Light = lightColorScheme(
    primary = Indigo,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE3FF),
    onPrimaryContainer = Color(0xFF0A1B52),
    secondary = Mint,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD3F2EC),
    onSecondaryContainer = Color(0xFF00332C),
    background = Color(0xFFF6F7FB),
    onBackground = Color(0xFF1A1C21),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1C21),
    surfaceVariant = Color(0xFFEDEFF6),
    onSurfaceVariant = Color(0xFF5A5F6E),
    outline = Color(0xFFC3C7D4),
    outlineVariant = Color(0xFFE2E5EE)
)

private val Dark = darkColorScheme(
    primary = IndigoDark,
    onPrimary = Color(0xFF0A1B52),
    primaryContainer = Color(0xFF27408E),
    onPrimaryContainer = Color(0xFFDCE3FF),
    secondary = Color(0xFF7AD8CB),
    onSecondary = Color(0xFF00332C),
    background = Color(0xFF111318),
    onBackground = Color(0xFFE3E4E9),
    surface = Color(0xFF181B21),
    onSurface = Color(0xFFE3E4E9),
    surfaceVariant = Color(0xFF262A33),
    onSurfaceVariant = Color(0xFFB8BDCA),
    outline = Color(0xFF4A4F5C),
    outlineVariant = Color(0xFF2E323C)
)

@Composable
fun ClipTheme(
    dark: Boolean = isSystemInDarkTheme(),
    dynamic: Boolean = false,
    content: @Composable () -> Unit
) {
    val scheme = when {
        dynamic && Build.VERSION.SDK_INT >= 31 -> {
            val ctx = LocalContext.current
            if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        dark -> Dark
        else -> Light
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
