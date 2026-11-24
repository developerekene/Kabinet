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

private val DarkColorScheme =
    darkColorScheme(
        primary = RedPrimary,
        onPrimary = Color.White,
        primaryContainer = RedPrimaryDark,
        onPrimaryContainer = Color.White,
        secondary = Color(0xFFFF5252),
        background = DarkBackground,
        surface = DarkSurface,
        surfaceVariant = DarkSurfaceVariant,
        onBackground = TextPrimaryDark,
        onSurface = TextPrimaryDark,
        onSurfaceVariant = TextSecondaryDark
    )

private val LightColorScheme =
    lightColorScheme(
        primary = RedPrimary,
        onPrimary = Color.White,
        primaryContainer = RedContainer,
        onPrimaryContainer = OnRedContainer,
        secondary = Color(0xFFD32F2F),
        background = LightBackground,
        surface = LightSurface,
        surfaceVariant = LightSurfaceVariant,
        onBackground = TextPrimaryLight,
        onSurface = TextPrimaryLight,
        onSurfaceVariant = TextSecondaryLight
    )

@Composable
fun KabinetTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Default false to ensure bold Red accent is prominent
    content: @Composable () -> Unit,
) {
    val colorScheme =
        when {
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

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    KabinetTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
}
