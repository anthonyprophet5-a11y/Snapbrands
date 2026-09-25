package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SnapDarkColorScheme = darkColorScheme(
    primary = SnapIndigoLight,
    onPrimary = SnapIndigoDark,
    primaryContainer = SnapIndigoDark,
    onPrimaryContainer = SnapIndigoContainer,
    secondary = SnapTealLight,
    onSecondary = SnapTealDark,
    secondaryContainer = SnapTealDark,
    onSecondaryContainer = SnapTealContainer,
    tertiary = SnapAmberAccent,
    onTertiary = Color.Black,
    background = SnapBackgroundDark,
    onBackground = SnapTextPrimaryDark,
    surface = SnapSurfaceDark,
    onSurface = SnapTextPrimaryDark,
    surfaceVariant = SnapSurfaceElevatedDark,
    onSurfaceVariant = SnapTextSecondaryDark,
    outline = SnapBorderDark,
    outlineVariant = Color(0xFF1E293B),
    error = SnapCrimsonError,
    onError = Color.White
)

private val SnapLightColorScheme = lightColorScheme(
    primary = SnapIndigoPrimary,
    onPrimary = Color.White,
    primaryContainer = SnapIndigoContainer,
    onPrimaryContainer = SnapIndigoOnContainer,
    secondary = SnapTealSecondary,
    onSecondary = Color.White,
    secondaryContainer = SnapTealContainer,
    onSecondaryContainer = SnapTealOnContainer,
    tertiary = SnapAmberAccent,
    onTertiary = Color.White,
    background = SnapBackgroundLight,
    onBackground = SnapTextPrimaryLight,
    surface = SnapSurfaceLight,
    onSurface = SnapTextPrimaryLight,
    surfaceVariant = SnapSurfaceElevatedLight,
    onSurfaceVariant = SnapTextSecondaryLight,
    outline = SnapBorderLight,
    outlineVariant = SnapBorderSubtle,
    error = SnapCrimsonError,
    onError = Color.White
)

@Composable
fun SnapBrandTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) SnapDarkColorScheme else SnapLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

