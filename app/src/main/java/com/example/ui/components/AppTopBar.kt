package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.domain.normalizer.PersianTextNormalizer
import com.example.ui.theme.AppMotion
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AppTopBar(
    title: String,
    lastUpdatedTimestamp: Long?,
    isUpdating: Boolean,
    themeMode: String = "SYSTEM",
    onToggleTheme: () -> Unit = {},
    onRefresh: () -> Unit,
    onOpenSettings: () -> Unit,
    showSettings: Boolean = true,
    modifier: Modifier = Modifier
) {
    // Idle rotation is a fixed value, so the spin never runs when nothing is happening.
    val rotation by if (isUpdating) {
        val transition = rememberInfiniteTransition(label = "refresh")
        transition.animateFloat(
            0f, 360f,
            infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Restart),
            label = "refresh_rotation"
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }
    val refreshing by animateFloatAsState(
        targetValue = if (isUpdating) 1f else 0f,
        animationSpec = tween(AppMotion.DURATION_BASE, easing = AppMotion.EaseOut),
        label = "refreshTint"
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 10.dp, top = 10.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                // The title slides itself in whenever the destination changes; the
                // motion is the signal that you moved, no separate indicator needed.
                AnimatedContent(
                    targetState = title,
                    transitionSpec = {
                        (slideInVertically(
                            animationSpec = tween(AppMotion.DURATION_BASE, easing = AppMotion.EaseOut),
                            initialOffsetY = { it / 2 }
                        ) + fadeIn(tween(AppMotion.DURATION_BASE))) togetherWith
                            (slideOutVertically(
                                animationSpec = tween(AppMotion.DURATION_QUICK, easing = AppMotion.EaseIn),
                                targetOffsetY = { -it / 2 }
                            ) + fadeOut(tween(AppMotion.DURATION_QUICK)))
                    },
                    label = "topBarTitle"
                ) { animatedTitle ->
                    Text(
                        text = animatedTitle,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(Modifier.height(2.dp))

                Text(
                    text = if (lastUpdatedTimestamp != null && lastUpdatedTimestamp > 0) {
                        "آخرین دریافت · ${formatPersianTimestamp(lastUpdatedTimestamp)}"
                    } else {
                        "برنامهٔ هفتگی شما"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onToggleTheme,
                    modifier = Modifier.testTag("theme_toggle_button")
                ) {
                    AnimatedContent(
                        targetState = themeMode == "BLACK",
                        transitionSpec = {
                            (fadeIn(tween(AppMotion.DURATION_BASE)) + slideInVertically { it / 2 }) togetherWith
                                (fadeOut(tween(AppMotion.DURATION_QUICK)) + slideOutVertically { -it / 2 })
                        },
                        label = "themeIcon"
                    ) { isBlack ->
                        Icon(
                            imageVector = if (isBlack) Icons.Default.Bolt else Icons.Default.Contrast,
                            contentDescription = if (isBlack) "تغییر به تم گرافیت" else "تغییر به مشکی مطلق",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onRefresh,
                    enabled = !isUpdating,
                    modifier = Modifier.testTag("manual_refresh_button")
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f * refreshing),
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "دریافت داده‌های دانشگاه",
                                modifier = Modifier.size(19.dp).rotate(rotation),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                if (showSettings) {
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.testTag("settings_top_button")
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "تنظیمات",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Loading is signalled by a 2dp slider at 24% opacity instead of a full
        // progress row: quiet enough to leave the content readable underneath.
        AnimatedVisibility(
            visible = isUpdating,
            enter = fadeIn(tween(AppMotion.DURATION_BASE)),
            exit = fadeOut(tween(AppMotion.DURATION_QUICK))
        ) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(2.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f),
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
            )
        }
    }
}

private fun formatPersianTimestamp(timestamp: Long): String {
    val formatted = SimpleDateFormat("yyyy/MM/dd - HH:mm", Locale.getDefault()).format(Date(timestamp))
    return PersianTextNormalizer.toPersianDigits(formatted)
}
