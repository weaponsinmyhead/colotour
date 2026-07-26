package com.colotour.app.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = ColotourTeal,
    secondary = ColotourMapBlue,
    tertiary = ColotourCoral,
    background = ColotourDarkText,
    surface = Color(0xFF1E1C1A)
)

private val LightColorScheme = lightColorScheme(
    primary = ColotourTeal,
    onPrimary = Color.White,
    secondary = ColotourMapBlue,
    onSecondary = Color.White,
    tertiary = ColotourCoral,
    onTertiary = Color.White,
    background = ColotourCreamBg,
    surface = ColotourSurface,
    onBackground = ColotourDarkText,
    onSurface = ColotourDarkText,
    primaryContainer = ColotourPrimaryContainer,
    onPrimaryContainer = ColotourTeal
)

@Composable
fun TravelItineraryTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Desactivar para fijar nuestra propia paleta de diseño
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
