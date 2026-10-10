package com.example.ui.theme

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

private val DarkColorScheme = darkColorScheme(
    primary = ArkinPrimaryLight,
    onPrimary = Color.White,
    primaryContainer = ArkinPrimaryHover,
    onPrimaryContainer = Color.White,
    secondary = Color(0xFFA5B4FC),
    onSecondary = Color(0xFF1E1E2D),
    background = ArkinBackgroundDark,
    onBackground = ArkinTextDark,
    surface = ArkinSurfaceDark,
    onSurface = ArkinTextDark,
    surfaceVariant = ArkinSurfaceVariantDark,
    onSurfaceVariant = ArkinTextMutedDark,
    outline = Color(0xFF383A52)
)

private val LightColorScheme = lightColorScheme(
    primary = ArkinPrimary,
    onPrimary = Color.White,
    primaryContainer = ArkinPrimaryContainer,
    onPrimaryContainer = ArkinPrimaryHover,
    secondary = AccentBlueText,
    onSecondary = Color.White,
    background = ArkinBackgroundLight,
    onBackground = ArkinTextLight,
    surface = ArkinSurfaceLight,
    onSurface = ArkinTextLight,
    surfaceVariant = Color(0xFFF1F3FB),
    onSurfaceVariant = ArkinTextMutedLight,
    outline = Color(0xFFE5E7EB)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep consistent Arkin branding
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
