package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.normalizer.PersianTextNormalizer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
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
    val rotation by if (isUpdating) {
        val transition = rememberInfiniteTransition(label = "refresh")
        transition.animateFloat(0f, 360f, infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Restart), label = "refresh_rotation")
    } else {
        remember { mutableFloatStateOf(0f) }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        TopAppBar(
            title = {
                Column {
                    Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    if (lastUpdatedTimestamp != null && lastUpdatedTimestamp > 0) {
                        Text("آخرین دریافت · ${formatPersianTimestamp(lastUpdatedTimestamp)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Text("برنامهٔ هفتگی شما", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            actions = {
                IconButton(onClick = onToggleTheme, modifier = Modifier.testTag("theme_toggle_button")) {
                    val (icon, description) = when (themeMode) {
                        "BLACK" -> Icons.Default.LightMode to "تغییر به حالت روشن"
                        "DARK" -> Icons.Default.Nightlight to "تغییر پوسته"
                        else -> Icons.Default.DarkMode to "تغییر پوسته"
                    }
                    Icon(icon, contentDescription = description, tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onRefresh, enabled = !isUpdating, modifier = Modifier.testTag("manual_refresh_button")) {
                    Icon(Icons.Default.Refresh, contentDescription = "دریافت داده‌های دانشگاه", modifier = Modifier.size(23.dp).rotate(rotation), tint = MaterialTheme.colorScheme.onSurface)
                }
                if (showSettings) IconButton(onClick = onOpenSettings, modifier = Modifier.testTag("settings_top_button")) {
                    Icon(Icons.Default.Settings, contentDescription = "تنظیمات", tint = MaterialTheme.colorScheme.onSurface)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
        )
        if (isUpdating) LinearProgressIndicator(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
        )
    }
}

private fun formatPersianTimestamp(timestamp: Long): String {
    val formatted = SimpleDateFormat("yyyy/MM/dd - HH:mm", Locale.getDefault()).format(Date(timestamp))
    return PersianTextNormalizer.toPersianDigits(formatted)
}
