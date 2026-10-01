package com.projeto.wellio.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Sem cores dinâmicas (Material You): o app usa a própria paleta (SPEC §2.5)
private val LightColorScheme = lightColorScheme(
    primary = Navy,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDDE1FF),
    onPrimaryContainer = Navy,
    secondary = Color(0xFF4A5A9C),
    background = Color(0xFFF5F5F7),
    onBackground = Color(0xFF1B1C22),
    surface = Color.White,
    onSurface = Color(0xFF1B1C22),
    surfaceVariant = Color(0xFFE6E7EF),
    onSurfaceVariant = Color(0xFF5B5D6B)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF9FAEFF),
    onPrimary = Color(0xFF0E1640),
    primaryContainer = Color(0xFF2C3E80),
    onPrimaryContainer = Color(0xFFDDE1FF),
    secondary = Color(0xFFB7C1F2),
    background = Color(0xFF101218),
    onBackground = Color(0xFFE3E3EA),
    surface = Color(0xFF181A22),
    onSurface = Color(0xFFE3E3EA),
    surfaceVariant = Color(0xFF2A2C36),
    onSurfaceVariant = Color(0xFFB4B6C4)
)

private val LocalWellioColors = staticCompositionLocalOf { LightWellioColors }

@Composable
fun WellioTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalWellioColors provides if (darkTheme) DarkWellioColors else LightWellioColors
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
            typography = Typography,
            content = content
        )
    }
}

/** Acesso às cores dos módulos: `WellioTheme.colors.water.accent`. */
object WellioTheme {
    val colors: WellioColors
        @Composable @ReadOnlyComposable
        get() = LocalWellioColors.current
}
