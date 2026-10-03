package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/*
 * Type scale for a Persian (RTL) reading surface.
 *
 * Two decisions that matter here:
 *  1. letterSpacing stays at 0 for every style. Positive tracking on Arabic-script
 *     text breaks the joined letterforms and reads as broken typography.
 *  2. lineHeight is generous (1.6x+ for body, 1.35x for titles) because the script
 *     has tall ascenders/descenders and dense stacked diacritics.
 *
 * Hierarchy is carried by weight (Normal / Medium / SemiBold / Bold) rather than
 * by colour alone, so the scale still reads on a monochrome palette.
 */
private val Sans = FontFamily.Default

// Display styles stay tight: large Persian headings need weight, not air.
private val displayLarge = TextStyle(
    fontFamily = Sans,
    fontWeight = FontWeight.Bold,
    fontSize = 40.sp,
    lineHeight = 50.sp,
    letterSpacing = 0.sp
)
private val displayMedium = TextStyle(
    fontFamily = Sans,
    fontWeight = FontWeight.Bold,
    fontSize = 34.sp,
    lineHeight = 44.sp,
    letterSpacing = 0.sp
)
private val displaySmall = TextStyle(
    fontFamily = Sans,
    fontWeight = FontWeight.SemiBold,
    fontSize = 28.sp,
    lineHeight = 38.sp,
    letterSpacing = 0.sp
)

private val headlineLarge = TextStyle(
    fontFamily = Sans,
    fontWeight = FontWeight.Bold,
    fontSize = 26.sp,
    lineHeight = 36.sp,
    letterSpacing = 0.sp
)
private val headlineMedium = TextStyle(
    fontFamily = Sans,
    fontWeight = FontWeight.Bold,
    fontSize = 22.sp,
    lineHeight = 31.sp,
    letterSpacing = 0.sp
)
private val headlineSmall = TextStyle(
    fontFamily = Sans,
    fontWeight = FontWeight.SemiBold,
    fontSize = 19.sp,
    lineHeight = 28.sp,
    letterSpacing = 0.sp
)

private val titleLarge = TextStyle(
    fontFamily = Sans,
    fontWeight = FontWeight.SemiBold,
    fontSize = 18.sp,
    lineHeight = 26.sp,
    letterSpacing = 0.sp
)
private val titleMedium = TextStyle(
    fontFamily = Sans,
    fontWeight = FontWeight.SemiBold,
    fontSize = 16.sp,
    lineHeight = 23.sp,
    letterSpacing = 0.sp
)
private val titleSmall = TextStyle(
    fontFamily = Sans,
    fontWeight = FontWeight.Medium,
    fontSize = 14.5.sp,
    lineHeight = 21.sp,
    letterSpacing = 0.sp
)

private val bodyLarge = TextStyle(
    fontFamily = Sans,
    fontWeight = FontWeight.Normal,
    fontSize = 15.5.sp,
    lineHeight = 26.sp,
    letterSpacing = 0.sp
)
private val bodyMedium = TextStyle(
    fontFamily = Sans,
    fontWeight = FontWeight.Normal,
    fontSize = 14.sp,
    lineHeight = 24.sp,
    letterSpacing = 0.sp
)
private val bodySmall = TextStyle(
    fontFamily = Sans,
    fontWeight = FontWeight.Normal,
    fontSize = 12.5.sp,
    lineHeight = 21.sp,
    letterSpacing = 0.sp
)

private val labelLarge = TextStyle(
    fontFamily = Sans,
    fontWeight = FontWeight.SemiBold,
    fontSize = 13.5.sp,
    lineHeight = 19.sp,
    letterSpacing = 0.sp
)
private val labelMedium = TextStyle(
    fontFamily = Sans,
    fontWeight = FontWeight.Medium,
    fontSize = 12.sp,
    lineHeight = 17.sp,
    letterSpacing = 0.sp
)
private val labelSmall = TextStyle(
    fontFamily = Sans,
    fontWeight = FontWeight.Medium,
    fontSize = 11.sp,
    lineHeight = 16.sp,
    letterSpacing = 0.sp
)

val Typography = Typography(
    displayLarge = displayLarge,
    displayMedium = displayMedium,
    displaySmall = displaySmall,
    headlineLarge = headlineLarge,
    headlineMedium = headlineMedium,
    headlineSmall = headlineSmall,
    titleLarge = titleLarge,
    titleMedium = titleMedium,
    titleSmall = titleSmall,
    bodyLarge = bodyLarge,
    bodyMedium = bodyMedium,
    bodySmall = bodySmall,
    labelLarge = labelLarge,
    labelMedium = labelMedium,
    labelSmall = labelSmall
)
