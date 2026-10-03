package com.example.ui.theme

import androidx.compose.ui.graphics.Color

/*
 * Graphite — a single-family monochrome palette.
 *
 * Rules this file obeys:
 *  - One hue family (cool graphite, slight blue cast). No warm/cool mixing.
 *  - Surfaces are near-opaque solids, not translucent washes. Elevation is
 *    communicated by a lighter fill + a 1px hairline, never by drop shadows.
 *  - Exactly one accent: pure light (near-white on dark, near-black on light).
 *    All other chroma in the app survives only as functional semantics
 *    (event type, diff status, week parity) and is desaturated to < 80% sat.
 */

// ---------------------------------------------------------------- dark: graphite
val ModernDarkBackground = Color(0xFF08090B)
val ModernDarkSurface = Color(0xFF0D0F12)
val ModernDarkSurfaceElevated = Color(0xFF15181D)
val ModernDarkSurfaceVariant = Color(0xFF1B1F25)
val ModernDarkOutline = Color(0xFF2A2F37)

val ModernPrimaryDark = Color(0xFFF3F5F7)
val ModernOnPrimaryDark = Color(0xFF0A0C0E)
val ModernPrimaryContainerDark = Color(0xFF1C2027)
val ModernOnPrimaryContainerDark = Color(0xFFEDF0F4)

val ModernSecondaryDark = Color(0xFFB4BCC6)
val ModernOnSecondaryDark = Color(0xFF101317)
val ModernSecondaryContainerDark = Color(0xFF23282F)
val ModernOnSecondaryContainerDark = Color(0xFFE4E8ED)

val ModernTextPrimaryDark = Color(0xFFF3F5F7)
val ModernTextSecondaryDark = Color(0xFF9BA4AF)
val ModernTextTertiaryDark = Color(0xFF6D7681)

// ---------------------------------------------------------------- dark: black (true AMOLED)
val AmoledBackground = Color(0xFF000000)
val AmoledSurface = Color(0xFF07080A)
val AmoledSurfaceVariant = Color(0xFF14171C)
val AmoledOutline = Color(0xFF23272E)
val AmoledPrimary = Color(0xFFFFFFFF)
val AmoledOnPrimary = Color(0xFF000000)
val AmoledPrimaryContainer = Color(0xFF171A1F)
val AmoledOnPrimaryContainer = Color(0xFFF1F3F6)

// ---------------------------------------------------------------- light: paper
val ModernLightBackground = Color(0xFFF5F6F8)
val ModernLightSurface = Color(0xFFFFFFFF)
val ModernLightSurfaceElevated = Color(0xFFF0F2F4)
val ModernLightSurfaceVariant = Color(0xFFE7EAEE)
val ModernLightOutline = Color(0xFFD4D8DE)

val ModernPrimaryLight = Color(0xFF111418)
val ModernOnPrimaryLight = Color(0xFFFFFFFF)
val ModernPrimaryContainerLight = Color(0xFFECEEF1)
val ModernOnPrimaryContainerLight = Color(0xFF14171B)

val ModernSecondaryLight = Color(0xFF4A525C)
val ModernOnSecondaryLight = Color(0xFFFFFFFF)
val ModernSecondaryContainerLight = Color(0xFFE7EAEE)
val ModernOnSecondaryContainerLight = Color(0xFF252A31)

// ---------------------------------------------------------------- hairlines
/** Alpha used by every card border in the app. Keeps the whole UI on one weight. */
const val HairlineAlpha = 0.9f
const val CardFillAlpha = 0.96f
const val ChromeAlpha = 0.82f

// ---------------------------------------------------------------- text + error tokens
val Color_TextLight = Color(0xFF0C0E11)
val Color_TextSecondaryLight = Color(0xFF525B66)

val Color_ErrorDark = Color(0xFFD98B8E)
val Color_OnErrorDark = Color(0xFF1A0B0C)
val Color_ErrorContainerDark = Color(0xFF3A2124)
val Color_OnErrorContainerDark = Color(0xFFF4DBDC)

val Color_ErrorLight = Color(0xFF9E3B3F)
val Color_OnErrorLight = Color(0xFFFFFFFF)
val Color_ErrorContainerLight = Color(0xFFF6E3E3)
val Color_OnErrorContainerLight = Color(0xFF54191C)

// Backward compatibility aliases
val AcademicBluePrimary = ModernPrimaryLight
val AcademicBlueOnPrimary = ModernOnPrimaryLight
val AcademicBlueContainer = ModernPrimaryContainerLight
val AcademicBlueOnContainer = ModernOnPrimaryContainerLight
val AcademicSecondary = ModernSecondaryLight
val AcademicSecondaryContainer = ModernSecondaryContainerLight
val AcademicBackground = ModernLightBackground
val AcademicSurface = ModernLightSurface
val AcademicSurfaceVariant = ModernLightSurfaceVariant
val AcademicOutline = ModernLightOutline

val AcademicBluePrimaryDark = ModernPrimaryDark
val AcademicBlueOnPrimaryDark = ModernOnPrimaryDark
val AcademicBlueContainerDark = ModernPrimaryContainerDark
val AcademicBlueOnContainerDark = ModernOnPrimaryContainerDark
val AcademicSecondaryDark = ModernSecondaryDark
val AcademicSecondaryContainerDark = ModernSecondaryContainerDark
val AcademicBackgroundDark = ModernDarkBackground
val AcademicSurfaceDark = ModernDarkSurface
val AcademicSurfaceVariantDark = ModernDarkSurfaceVariant
val AcademicOutlineDark = ModernDarkOutline

// ---------------------------------------------------------------- functional semantics
// Desaturated on purpose: they must stay distinguishable from each other without
// fighting the monochrome chrome for attention.
val EventExamColor = Color(0xFFC97A7E)
val EventMidtermColor = Color(0xFFC79A6A)
val EventPresentationColor = Color(0xFF9A8CC4)
val EventAssignmentColor = Color(0xFF7A9BC9)
val EventQuizColor = Color(0xFF6FB2AC)
val EventOtherColor = Color(0xFF9BA4AF)

val DiffUnchanged = Color(0xFF6FAE93)
val DiffModified = Color(0xFFC9A468)
val DiffAdded = Color(0xFF7A9BC9)
val DiffRemoved = Color(0xFFC97A7E)

// Week parity: same neutral family, different weight. Text carries the meaning.
val ParityEvenColor = Color(0xFFD6DCE5)
val ParityOddColor = Color(0xFF98A2AE)
