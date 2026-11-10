package com.audioscribe.poc.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF4FC3F7),
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFF006080),
    onPrimaryContainer = Color(0xFFD1E8FF),
    secondary = Color(0xFFB0E0F5),
    onSecondary = Color(0xFF003344),
    secondaryContainer = Color(0xFF004B60),
    onSecondaryContainer = Color(0xFFB0E0F5),
    tertiary = Color(0xFF81D4FA),
    onTertiary = Color(0xFF003646),
    tertiaryContainer = Color(0xFF004E62),
    onTertiaryContainer = Color(0xFF81D4FA),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF121212),
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF121212),
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF45494E),
    onSurfaceVariant = Color(0xFFC6CACC),
    outline = Color(0xFF909499),
    outlineVariant = Color(0xFF45494E),
    shadow = Color(0xFF000000),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFFE6E1E5),
    inverseOnSurface = Color(0xFF313033),
    inversePrimary = Color(0xFF006080),
    surfaceTint = Color(0xFF4FC3F7),
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF006080),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD1E8FF),
    onPrimaryContainer = Color(0xFF001D2D),
    secondary = Color(0xFF006174),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD1E8FF),
    onSecondaryContainer = Color(0xFF001D2D),
    tertiary = Color(0xFF00667A),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFD1E9FC),
    onTertiaryContainer = Color(0xFF001F2B),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFCFCFF),
    onBackground = Color(0xFF1C1B1F),
    surface = Color(0xFFFCFCFF),
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFF42474E),
    onSurfaceVariant = Color(0xFF42474E),
    outline = Color(0xFF767B80),
    outlineVariant = Color(0xFFC6CACC),
    shadow = Color(0xFF000000),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFF313033),
    inverseOnSurface = Color(0xFFE6E1E5),
    inversePrimary = Color(0xFF4FC3F7),
    surfaceTint = Color(0xFF006080),
)

@Composable
fun AppColorScheme(
    darkColorScheme: androidx.compose.material3.ColorScheme = DarkColorScheme,
    lightColorScheme: androidx.compose.material3.ColorScheme = LightColorScheme,
    isSystemInDarkTheme: Boolean
): androidx.compose.material3.ColorScheme {
    return if (isSystemInDarkTheme) darkColorScheme else lightColorScheme
}
