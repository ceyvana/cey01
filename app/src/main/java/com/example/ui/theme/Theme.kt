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
    primary = BentoPrimaryContainer, // Elevated bright containers in dark mode
    onPrimary = BentoOnPrimaryContainer,
    primaryContainer = BentoPrimary,
    onPrimaryContainer = BentoOnPrimary,
    secondary = BentoSecondary,
    onSecondary = BentoOnSecondary,
    secondaryContainer = Color(0xFF2E3033),
    onSecondaryContainer = Color(0xFFE1E2EC),
    tertiary = BentoTertiary,
    onTertiary = BentoOnTertiary,
    tertiaryContainer = Color(0xFF3B2D54),
    onTertiaryContainer = Color(0xFFF7EFFF),
    background = Color(0xFF1A1C1E),
    onBackground = Color(0xFFE1E2EC),
    surface = Color(0xFF202225),
    onSurface = Color(0xFFE1E2EC),
    outline = BentoOutline,
    outlineVariant = BentoOutlineVariant,
    error = BentoError,
    onError = BentoOnError
)

private val LightColorScheme = lightColorScheme(
    primary = BentoPrimary,
    onPrimary = BentoOnPrimary,
    primaryContainer = BentoPrimaryContainer,
    onPrimaryContainer = BentoOnPrimaryContainer,
    secondary = BentoSecondary,
    onSecondary = BentoOnSecondary,
    secondaryContainer = BentoSecondaryContainer,
    onSecondaryContainer = BentoOnSecondaryContainer,
    tertiary = BentoTertiary,
    onTertiary = BentoOnTertiary,
    tertiaryContainer = BentoTertiaryContainer,
    onTertiaryContainer = BentoOnTertiaryContainer,
    background = BentoBackground,
    onBackground = BentoOnBackground,
    surface = BentoSurface,
    onSurface = BentoOnSurface,
    outline = BentoOutline,
    outlineVariant = BentoOutlineVariant,
    error = BentoError,
    onError = BentoOnError
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Allow dynamic color on Android 12+ or use our customized theme
    dynamicColor: Boolean = false, // Set to false to force our beautiful customized brand design
    content: @Composable () -> Unit,
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
