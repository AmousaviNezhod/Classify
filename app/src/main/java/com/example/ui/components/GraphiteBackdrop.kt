package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * The one background every screen sits on.
 *
 * It replaces the old multi-hue gradient wash with a flat graphite plane plus two
 * very low-alpha highlights (top-trailing, bottom-leading) and a hairline grid.
 * The grid is the only texture in the app: drawn at ~2% alpha it reads as
 * engineered metal rather than decoration. Everything here is static — it never
 * repaints on scroll.
 */
@Composable
fun GraphiteBackdrop(modifier: Modifier = Modifier) {
    val base = MaterialTheme.colorScheme.background
    val ink = MaterialTheme.colorScheme.onBackground
    val gridStep = 30.dp

    Canvas(modifier.fillMaxSize()) {
        drawRect(color = base)

        // Key light: top-trailing, mimicking a single overhead source.
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(ink.copy(alpha = 0.055f), Color.Transparent),
                center = Offset(size.width * 0.86f, -size.height * 0.04f),
                radius = size.minDimension * 1.05f
            )
        )
        // Fill light: bottom-leading, a quarter as strong. Keeps the plane from
        // reading as a flat cut-out on tall screens.
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(ink.copy(alpha = 0.03f), Color.Transparent),
                center = Offset(size.width * 0.08f, size.height * 0.92f),
                radius = size.minDimension * 0.9f
            )
        )

        val step = gridStep.toPx()
        val line = ink.copy(alpha = 0.016f)
        var x = step
        while (x < size.width) {
            drawLine(line, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
            x += step
        }
        var y = step
        while (y < size.height) {
            drawLine(line, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
            y += step
        }
    }
}
