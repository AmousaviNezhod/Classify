package com.example.ui.theme

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

/*
 * Motion tokens.
 *
 * Every animation in the app is gated by one of these values so the whole product
 * moves with the same physics. Only `alpha` and `transform` (scale/translate) are
 * ever animated — never size or position properties that force relayout.
 *
 * Compose honours the system "remove animations" setting automatically through
 * MotionDurationScale, so no separate reduced-motion branch is needed here.
 */
object AppMotion {
    /** Micro feedback: press, toggle, selection. */
    val springSnap: AnimationSpec<Float> = spring(dampingRatio = 0.9f, stiffness = 1200f)

    /** Standard UI movement: pills, indicators, colour and size changes. */
    val springSettle: AnimationSpec<Float> = spring(dampingRatio = 0.82f, stiffness = 620f)

    /** Same physics as [springSettle], for dimension-based animations. */
    val springSettleDp: AnimationSpec<Dp> = spring(dampingRatio = 0.82f, stiffness = 620f)

    /** Larger travel: panels, screen transitions, hero reveals. */
    val springGlide: AnimationSpec<Float> = spring(dampingRatio = 0.88f, stiffness = 340f)

    /** Spring for integer-pixel travel (slides, screen transitions). */
    val springGlideOffset: FiniteAnimationSpec<IntOffset> = spring(
        dampingRatio = 0.86f,
        stiffness = 420f,
        visibilityThreshold = IntOffset(1, 1)
    )

    const val DURATION_QUICK = 150
    const val DURATION_BASE = 240
    const val DURATION_SLOW = 380
    const val DURATION_REVEAL = 420

    /** Enter easing: fast start, long tail. */
    val EaseOut: Easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)

    /** Exit easing: leaves quickly, never lingers. */
    val EaseIn: Easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
}

/**
 * Tactile press feedback. Shrinks the target a hair while it is held and springs
 * back on release, so every tappable surface feels physical.
 */
fun Modifier.pressFeedback(
    enabled: Boolean = true,
    pressedScale: Float = 0.975f,
    interactionSource: MutableInteractionSource? = null
): Modifier = composed {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) pressedScale else 1f,
        animationSpec = AppMotion.springSnap,
        label = "pressScale"
    )
    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * Clickable with the ripple replaced by the press physics above. Use for cards and
 * large tappable rows; keep real ripple for the platform-y small controls.
 */
fun Modifier.tactileClick(
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier = composed {
    val source = remember { MutableInteractionSource() }
    this
        .pressFeedback(enabled = enabled, interactionSource = source)
        .clickable(
            interactionSource = source,
            indication = null,
            enabled = enabled,
            onClick = onClick
        )
}

/**
 * Entrance reveal: fade + short rise, staggered by [index]. Runs once per
 * composition, transform-only, and costs nothing once finished.
 */
fun Modifier.revealOnEnter(
    index: Int = 0,
    distance: Dp = 16.dp,
    startVisible: Boolean = false,
    staggerMillis: Int = 45
): Modifier = composed {
    var shown by remember { mutableStateOf(startVisible) }
    LaunchedEffect(Unit) { shown = true }
    val progress by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = tween(
            durationMillis = AppMotion.DURATION_REVEAL,
            delayMillis = minOf(index, 10) * staggerMillis,
            easing = AppMotion.EaseOut
        ),
        label = "reveal"
    )
    this.graphicsLayer {
        alpha = progress
        translationY = (1f - progress) * distance.toPx()
    }
}

/** Solid colour swatch used by the theme pickers and diff legends. */
@Composable
fun Swatch(color: androidx.compose.ui.graphics.Color, shape: Shape, size: Dp = 14.dp) {
    Box(Modifier.size(size).clip(shape).background(color))
}
