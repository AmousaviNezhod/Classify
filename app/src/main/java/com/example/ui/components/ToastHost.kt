package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AppMotion
import kotlinx.coroutines.delay

enum class ToastType { SUCCESS, INFO, WARNING, ERROR }

data class AppToast(val id: Long, val message: String, val type: ToastType)

@Composable
fun ToastHost(
    toasts: List<AppToast>,
    onDismiss: (AppToast) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        toasts.takeLast(3).forEach { toast ->
            var visible by remember(toast.id) { mutableStateOf(true) }
            LaunchedEffect(toast.id) {
                delay(5_000)
                visible = false
            }
            LaunchedEffect(visible, toast.id) {
                if (!visible) {
                    delay(220)
                    onDismiss(toast)
                }
            }
            AnimatedVisibility(
                visible = visible,
                enter = slideInVertically(
                    animationSpec = spring(
                        dampingRatio = 0.78f,
                        stiffness = 480f,
                        visibilityThreshold = IntOffset(1, 1)
                    ),
                    initialOffsetY = { it }
                ) + fadeIn(tween(AppMotion.DURATION_BASE, easing = AppMotion.EaseOut)),
                exit = slideOutVertically(
                    animationSpec = tween(AppMotion.DURATION_QUICK, easing = AppMotion.EaseIn),
                    targetOffsetY = { it / 2 }
                ) + fadeOut(tween(AppMotion.DURATION_QUICK))
            ) {
                ToastCard(toast) { visible = false }
            }
        }
    }
}

@Composable
private fun ToastCard(toast: AppToast, onDismiss: () -> Unit) {
    val (icon, tint) = when (toast.type) {
        ToastType.SUCCESS -> Icons.Default.CheckCircle to MaterialTheme.colorScheme.primary
        ToastType.INFO -> Icons.Default.Info to MaterialTheme.colorScheme.primary
        ToastType.WARNING -> Icons.Default.Info to MaterialTheme.colorScheme.secondary
        ToastType.ERROR -> Icons.Default.ErrorOutline to MaterialTheme.colorScheme.error
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        tonalElevation = 0.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.9f))
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(end = 4.dp)
                // Status rail replaces the drop shadow the old toast relied on.
                // Drawn behind the row so it always matches the real height.
                .drawBehind {
                    val railWidth = 3.dp.toPx()
                    drawRect(
                        color = tint.copy(alpha = 0.9f),
                        topLeft = Offset(size.width - railWidth, 0f),
                        size = Size(railWidth, size.height)
                    )
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.width(14.dp))
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(19.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                toast.message,
                modifier = Modifier.weight(1f).padding(vertical = 12.dp),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "بستن پیام",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
