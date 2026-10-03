package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AppMotion
import com.example.ui.theme.tactileClick

/**
 * Week-day filter. Selection is communicated by three simultaneous changes —
 * fill, border and label weight — animated on one spring so the pill feels like a
 * physical key being pressed rather than a colour swap.
 */
@Composable
fun DaySelector(
    selectedDayIndex: Int?,
    onSelectDay: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    val days = listOf(
        null to "همه روزها",
        0 to "شنبه",
        1 to "یکشنبه",
        2 to "دوشنبه",
        3 to "سه‌شنبه",
        4 to "چهارشنبه",
        5 to "پنج‌شنبه",
        6 to "جمعه"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        days.forEach { (index, title) ->
            DayPill(
                title = title,
                selected = selectedDayIndex == index,
                onClick = { onSelectDay(index) },
                testTag = "day_chip_${index ?: -1}"
            )
        }
    }
}

@Composable
private fun DayPill(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val container by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.55f),
        animationSpec = tween(AppMotion.DURATION_BASE, easing = AppMotion.EaseOut),
        label = "dayChipContainer"
    )
    val border by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
        animationSpec = tween(AppMotion.DURATION_BASE, easing = AppMotion.EaseOut),
        label = "dayChipBorder"
    )
    val labelColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(AppMotion.DURATION_BASE, easing = AppMotion.EaseOut),
        label = "dayChipLabel"
    )
    val scale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.985f,
        animationSpec = AppMotion.springSettle,
        label = "dayChipScale"
    )

    Surface(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .tactileClick(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(50),
        color = container,
        tonalElevation = 0.dp,
        border = BorderStroke(1.dp, border)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = labelColor,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}
