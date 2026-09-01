package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = SportsCyan,
    onPrimary = Color(0xFF00363D),
    primaryContainer = Color(0xFF004F58),
    onPrimaryContainer = Color(0xFF80F3FF),
    secondary = SportsOrange,
    onSecondary = Color(0xFF4A1F00),
    secondaryContainer = Color(0xFF6C3000),
    onSecondaryContainer = Color(0xFFFFDCC5),
    tertiary = LiveRed,
    onTertiary = Color.White,
    background = StadiumDarkBg,
    onBackground = TextPrimaryDark,
    surface = StadiumDarkSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = StadiumDarkCard,
    onSurfaceVariant = TextSecondaryDark,
    outline = Color(0xFF334155),
    error = LiveRed
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0284C7),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = Color(0xFF0369A1),
    secondary = Color(0xFFEA580C),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFEDD5),
    onSecondaryContainer = Color(0xFFC2410C),
    tertiary = LiveRed,
    onTertiary = Color.White,
    background = StadiumLightBg,
    onBackground = TextPrimaryLight,
    surface = StadiumLightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = TextSecondaryLight,
    outline = Color(0xFFCBD5E1),
    error = LiveRed
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
