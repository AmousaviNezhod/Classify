package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
                enter = slideInVertically(tween(220), initialOffsetY = { it / 2 }) + fadeIn(tween(220)),
                exit = slideOutVertically(tween(180), targetOffsetY = { it / 2 }) + fadeOut(tween(180))
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
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 5.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 14.dp, end = 4.dp, top = 5.dp, bottom = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(21.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                toast.message,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "بستن پیام", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
