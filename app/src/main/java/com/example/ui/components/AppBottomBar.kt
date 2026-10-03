package com.example.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AppMotion

data class BottomTab(
    val label: String,
    val icon: ImageVector,
    val testTag: String
)

/**
 * Floating tab bar with a single pill that glides between destinations.
 *
 * Compared to the stock NavigationBar this drops the per-item ripple and the
 * icon/label cross-fade in favour of one continuous object that travels — the
 * movement is what tells the eye where you went, so nothing else has to.
 */
@Composable
fun AppBottomBar(
    tabs: List<BottomTab>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    val layoutDirection = LocalLayoutDirection.current

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.94f),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.75f))
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp)
                .padding(6.dp)
        ) {
            val slotWidth = if (tabs.isEmpty()) maxWidth else maxWidth / tabs.size
            val slotWidthPx = with(LocalDensity.current) { slotWidth.toPx() }
            val directionSign = if (layoutDirection == LayoutDirection.Rtl) -1f else 1f
            val targetX = directionSign * slotWidthPx * selectedIndex.coerceIn(0, (tabs.size - 1).coerceAtLeast(0))
            val pillX by animateFloatAsState(
                targetValue = targetX,
                animationSpec = AppMotion.springGlide,
                label = "tabPillX"
            )

            // Traveling pill: one object, continuously repositioned.
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(slotWidth)
                    .graphicsLayer { translationX = pillX }
                    .padding(horizontal = 3.dp)
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    tonalElevation = 0.dp,
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                    )
                ) {}
            }

            Row(
                modifier = Modifier.fillMaxSize().testTag("main_bottom_nav")
            ) {
                tabs.forEachIndexed { index, tab ->
                    val selected = index == selectedIndex
                    val labelAlpha by animateFloatAsState(
                        targetValue = if (selected) 1f else 0.62f,
                        animationSpec = tween(AppMotion.DURATION_BASE, easing = AppMotion.EaseOut),
                        label = "tabLabelAlpha"
                    )
                    val iconScale by animateFloatAsState(
                        targetValue = if (selected) 1.06f else 1f,
                        animationSpec = AppMotion.springSettle,
                        label = "tabIconScale"
                    )
                    val iconLift by animateDpAsState(
                        targetValue = if (selected) (-1).dp else 0.dp,
                        animationSpec = AppMotion.springSettleDp,
                        label = "tabIconLift"
                    )
                    val interaction = remember { MutableInteractionSource() }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .clip(RoundedCornerShape(20.dp))
                            .clickable(
                                interactionSource = interaction,
                                indication = null
                            ) {
                                if (!selected) {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onSelect(index)
                                }
                            }
                            .testTag(tab.testTag),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.label,
                            tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                // offset, not padding: the selected icon lifts by a
                                // negative value and padding rejects negatives.
                                .offset(y = iconLift)
                                .size(21.dp)
                                .graphicsLayer {
                                    scaleX = iconScale
                                    scaleY = iconScale
                                }
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.graphicsLayer { alpha = labelAlpha }
                        )
                    }
                }
            }
        }
    }
}
