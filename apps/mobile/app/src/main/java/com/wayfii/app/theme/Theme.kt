package com.wayfii.app.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape

private val LightColorScheme = lightColorScheme(
    // Primario: Teal (Menta)
    primary            = WayfiiTeal,
    onPrimary          = Color.White,
    primaryContainer   = WayfiiPrimaryContainer,
    onPrimaryContainer = WayfiiTeal,

    // Secundario: Sky Blue (Cielo)
    secondary            = WayfiiSkyBlue,
    onSecondary          = Color.White,
    secondaryContainer   = WayfiiSecondaryContainer,
    onSecondaryContainer = WayfiiSkyBlue,

    // Terciario: Coral (Acento)
    tertiary            = WayfiiCoral,
    onTertiary          = Color.White,
    tertiaryContainer   = WayfiiCoralLight,
    onTertiaryContainer = WayfiiCoral,

    // Fondos y superficies
    background    = WayfiiCreamBg,
    onBackground  = WayfiiDarkText,
    surface       = WayfiiSurface,
    onSurface     = WayfiiDarkText,

    // Variantes de superficie
    surfaceVariant   = WayfiiSurfaceVariant,
    onSurfaceVariant = WayfiiMediumText,

    // Bordes
    outline        = WayfiiOutline,
    outlineVariant = WayfiiOutlineVariant,

    // Estados de error
    error            = WayfiiError,
    onError          = Color.White,
    errorContainer   = WayfiiErrorContainer,
    onErrorContainer = WayfiiError,
)

private val DarkColorScheme = darkColorScheme(
    primary            = WayfiiTealLight,
    onPrimary          = WayfiiDarkBg,
    primaryContainer   = WayfiiTeal,
    onPrimaryContainer = Color.White,

    secondary            = Color(0xFF7DD3FC), // Sky light
    onSecondary          = WayfiiDarkBg,

    tertiary            = Color(0xFFFDA4AF), // Rose light
    onTertiary          = WayfiiDarkBg,
    tertiaryContainer   = Color(0xFF881337),
    onTertiaryContainer = Color(0xFFFFF1F2),

    background   = WayfiiDarkBg,
    onBackground = Color(0xFFF1F5F9),
    surface      = WayfiiDarkSurface,
    onSurface    = Color(0xFFF1F5F9),

    surfaceVariant   = Color(0xFF334155),
    onSurfaceVariant = Color(0xFF94A3B8),

    outline        = Color(0xFF475569),
    outlineVariant = Color(0xFF1E293B),

    error          = Color(0xFFFCA5A5),
    onError        = Color(0xFF7F1D1D),
    errorContainer = Color(0xFF991B1B),
    onErrorContainer = Color(0xFFFEE2E2),
)

/**
 * Shapes de Wayfii — Corners más amplios para sensación lúdica y moderna.
 */
val WayfiiShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),   // Chips compactos
    small      = RoundedCornerShape(12.dp),  // Botones, inputs
    medium     = RoundedCornerShape(18.dp),  // Cards secundarias
    large      = RoundedCornerShape(24.dp),  // Cards principales, overlays
    extraLarge = RoundedCornerShape(32.dp),  // Bottom panels, Containers grandes
)

@Composable
fun WayfiiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    // Forzamos LightColorScheme para mantener la estética "Cielo/Clara" del requerimiento
    // pero permitimos dark si el usuario realmente lo desea (opcional). 
    // Siguiendo el prompt, priorizamos la estética clara.
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography,
        shapes      = WayfiiShapes,
        content     = content
    )
}
