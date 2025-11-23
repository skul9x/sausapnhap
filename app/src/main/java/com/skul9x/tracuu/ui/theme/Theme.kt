package com.skul9x.tracuu.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val PrimaryBlue = Color(0xFF2563EB)
val PrimaryHover = Color(0xFF1D4ED8)
val BgColor = Color(0xFFF1F5F9)
val CardBg = Color(0xFFFFFFFF)
val TextMain = Color(0xFF1E293B)
val TextSecondary = Color(0xFF64748B)
val BorderColor = Color(0xFFE2E8F0)
val HighlightOld = Color(0xFFEF4444)
val HighlightNew = Color(0xFF10B981)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = Color.White,
    secondary = TextSecondary,
    background = BgColor,
    surface = CardBg,
    onSurface = TextMain,
    outline = BorderColor
)

@Composable
fun TraCuuTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}