package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

/*
 * One accent per theme, used on the whole surface area of the app:
 *   dark  -> light (near-white) accent on graphite
 *   light -> ink (near-black) accent on paper
 * Nothing in the chrome is allowed to introduce a second hue.
 */

private val DarkColorScheme = darkColorScheme(
    primary = ModernPrimaryDark,
    onPrimary = ModernOnPrimaryDark,
    primaryContainer = ModernPrimaryContainerDark,
    onPrimaryContainer = ModernOnPrimaryContainerDark,
    secondary = ModernSecondaryDark,
    onSecondary = ModernOnSecondaryDark,
    secondaryContainer = ModernSecondaryContainerDark,
    onSecondaryContainer = ModernOnSecondaryContainerDark,
    tertiary = ModernSecondaryDark,
    onTertiary = ModernOnSecondaryDark,
    tertiaryContainer = ModernSecondaryContainerDark,
    onTertiaryContainer = ModernOnSecondaryContainerDark,
    background = ModernDarkBackground,
    onBackground = ModernTextPrimaryDark,
    surface = ModernDarkSurface,
    onSurface = ModernTextPrimaryDark,
    surfaceVariant = ModernDarkSurfaceVariant,
    onSurfaceVariant = ModernTextSecondaryDark,
    surfaceContainerLowest = ModernDarkBackground,
    surfaceContainerLow = ModernDarkSurface,
    surfaceContainer = ModernDarkSurface,
    surfaceContainerHigh = ModernDarkSurfaceElevated,
    surfaceContainerHighest = ModernDarkSurfaceVariant,
    outline = ModernDarkOutline,
    outlineVariant = ModernDarkOutline.copy(alpha = 0.55f),
    inverseSurface = ModernTextPrimaryDark,
    inverseOnSurface = ModernDarkBackground,
    error = Color_ErrorDark,
    onError = Color_OnErrorDark,
    errorContainer = Color_ErrorContainerDark,
    onErrorContainer = Color_OnErrorContainerDark
)

private val AmoledColorScheme = darkColorScheme(
    primary = AmoledPrimary,
    onPrimary = AmoledOnPrimary,
    primaryContainer = AmoledPrimaryContainer,
    onPrimaryContainer = AmoledOnPrimaryContainer,
    secondary = ModernSecondaryDark,
    onSecondary = ModernOnSecondaryDark,
    secondaryContainer = ModernSecondaryContainerDark,
    onSecondaryContainer = ModernOnSecondaryContainerDark,
    tertiary = ModernSecondaryDark,
    onTertiary = ModernOnSecondaryDark,
    tertiaryContainer = ModernSecondaryContainerDark,
    onTertiaryContainer = ModernOnSecondaryContainerDark,
    background = AmoledBackground,
    onBackground = ModernTextPrimaryDark,
    surface = AmoledSurface,
    onSurface = ModernTextPrimaryDark,
    surfaceVariant = AmoledSurfaceVariant,
    onSurfaceVariant = ModernTextSecondaryDark,
    surfaceContainerLowest = AmoledBackground,
    surfaceContainerLow = AmoledSurface,
    surfaceContainer = AmoledSurface,
    surfaceContainerHigh = AmoledSurfaceVariant,
    surfaceContainerHighest = ModernDarkSurfaceVariant,
    outline = AmoledOutline,
    outlineVariant = AmoledOutline.copy(alpha = 0.55f),
    inverseSurface = ModernTextPrimaryDark,
    inverseOnSurface = AmoledBackground,
    error = Color_ErrorDark,
    onError = Color_OnErrorDark,
    errorContainer = Color_ErrorContainerDark,
    onErrorContainer = Color_OnErrorContainerDark
)

private val LightColorScheme = lightColorScheme(
    primary = ModernPrimaryLight,
    onPrimary = ModernOnPrimaryLight,
    primaryContainer = ModernPrimaryContainerLight,
    onPrimaryContainer = ModernOnPrimaryContainerLight,
    secondary = ModernSecondaryLight,
    onSecondary = ModernOnSecondaryLight,
    secondaryContainer = ModernSecondaryContainerLight,
    onSecondaryContainer = ModernOnSecondaryContainerLight,
    tertiary = ModernSecondaryLight,
    onTertiary = ModernOnSecondaryLight,
    tertiaryContainer = ModernSecondaryContainerLight,
    onTertiaryContainer = ModernOnSecondaryContainerLight,
    background = ModernLightBackground,
    onBackground = Color_TextLight,
    surface = ModernLightSurface,
    onSurface = Color_TextLight,
    surfaceVariant = ModernLightSurfaceVariant,
    onSurfaceVariant = Color_TextSecondaryLight,
    surfaceContainerLowest = ModernLightSurface,
    surfaceContainerLow = ModernLightBackground,
    surfaceContainer = ModernLightSurface,
    surfaceContainerHigh = ModernLightSurfaceElevated,
    surfaceContainerHighest = ModernLightSurfaceVariant,
    outline = ModernLightOutline,
    outlineVariant = ModernLightOutline.copy(alpha = 0.6f),
    inverseSurface = Color_TextLight,
    inverseOnSurface = ModernLightSurface,
    error = Color_ErrorLight,
    onError = Color_OnErrorLight,
    errorContainer = Color_ErrorContainerLight,
    onErrorContainer = Color_OnErrorContainerLight
)

/**
 * One radius language for the whole app: soft, graphite-hardware feel.
 *  - extraSmall/small -> chips, badges, inline pills
 *  - medium           -> inputs, small cards
 *  - large            -> standard cards
 *  - extraLarge       -> hero panels, dialogs
 */
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun MyApplicationTheme(
    themeMode: String = "SYSTEM",
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when (themeMode) {
        "LIGHT" -> LightColorScheme
        "BLACK" -> AmoledColorScheme
        else -> DarkColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
