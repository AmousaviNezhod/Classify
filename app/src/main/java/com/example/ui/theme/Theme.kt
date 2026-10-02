package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = AcademicBluePrimaryDark,
    onPrimary = AcademicBlueOnPrimaryDark,
    primaryContainer = AcademicBlueContainerDark,
    onPrimaryContainer = AcademicBlueOnContainerDark,
    secondary = AcademicSecondaryDark,
    secondaryContainer = AcademicSecondaryContainerDark,
    background = AcademicBackgroundDark,
    surface = AcademicSurfaceDark,
    surfaceVariant = AcademicSurfaceVariantDark,
    outline = AcademicOutlineDark
)

private val AmoledColorScheme = darkColorScheme(
    primary = AmoledPrimary,
    onPrimary = AmoledOnPrimary,
    primaryContainer = AmoledPrimaryContainer,
    onPrimaryContainer = AmoledOnPrimaryContainer,
    secondary = AcademicSecondaryDark,
    secondaryContainer = AcademicSecondaryContainerDark,
    background = AmoledBackground,
    surface = AmoledSurface,
    surfaceVariant = AmoledSurfaceVariant,
    outline = AmoledOutline
)

private val LightColorScheme = lightColorScheme(
    primary = AcademicBluePrimary,
    onPrimary = AcademicBlueOnPrimary,
    primaryContainer = AcademicBlueContainer,
    onPrimaryContainer = AcademicBlueOnContainer,
    secondary = AcademicSecondary,
    secondaryContainer = AcademicSecondaryContainer,
    background = AcademicBackground,
    surface = AcademicSurface,
    surfaceVariant = AcademicSurfaceVariant,
    outline = AcademicOutline
)

@Composable
fun MyApplicationTheme(
    themeMode: String = "SYSTEM",
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val isSystemDark = isSystemInDarkTheme()
    val colorScheme = when (themeMode) {
        "BLACK" -> AmoledColorScheme
        "DARK" -> DarkColorScheme
        "LIGHT" -> LightColorScheme
        else -> if (isSystemDark) DarkColorScheme else LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

