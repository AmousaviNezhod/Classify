package com.example.ui.settings

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import com.example.AppLinks
import com.example.domain.normalizer.PersianTextNormalizer
import com.example.ui.theme.AmoledBackground
import com.example.ui.theme.AmoledOutline
import com.example.ui.theme.AppMotion
import com.example.ui.theme.DiffRemoved
import com.example.ui.theme.ModernDarkOutline
import com.example.ui.theme.ModernDarkSurface
import com.example.ui.theme.ModernLightOutline
import com.example.ui.theme.ModernLightSurface
import com.example.ui.theme.tactileClick

@Composable
fun SettingsScreen(
    sourceUrl: String,
    themeMode: String,
    storageSize: String,
    pdfCount: Int,
    inputJson: String,
    onUpdateSourceUrl: (String) -> Unit,
    onResetSourceUrl: () -> Unit,
    onSetThemeMode: (String) -> Unit,
    onUpdateInputJson: (String) -> Unit,
    onClearCache: () -> Unit,
    onClearPdfs: () -> Unit,
    onForceRefetchAll: () -> Unit,
    onResetAllData: () -> Unit,
    onOpenPdfs: () -> Unit = {},
    onOpenComparison: () -> Unit = {},
    onExportFullBackup: () -> Unit = {},
    onRestoreFullBackup: (String) -> Unit = {},
    onExportEvents: () -> Unit = {},
    onImportEvents: (String) -> Unit = {},
    onExportSchedule: () -> Unit = {},
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }
    val uriHandler = LocalUriHandler.current
    val context = androidx.compose.ui.platform.LocalContext.current

    val restoreBackupPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            }.getOrNull()?.let { content ->
                onRestoreFullBackup(content)
            }
        }
    }

    val importEventsPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            }.getOrNull()?.let { content ->
                onImportEvents(content)
            }
        }
    }

    var showUrlDialog by remember { mutableStateOf(false) }
    var tempUrl by remember(sourceUrl) { mutableStateOf(sourceUrl) }

    var showJsonDialog by remember { mutableStateOf(false) }
    var tempJson by remember(inputJson) { mutableStateOf(inputJson) }

    var showResetConfirmDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Screen Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("back_from_settings")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "بازگشت"
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "تنظیمات برنامه",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        // 1. Appearance Section
        SectionTitle(title = "ظاهر و پوسته", icon = Icons.Default.Brightness4)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.75f))
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                ThemeOption(
                    title = "گرافیت",
                    subtitle = "خاکستری تیرهٔ پیش‌فرض",
                    selected = themeMode != "BLACK" && themeMode != "LIGHT",
                    swatch = ModernDarkSurface,
                    swatchBorder = ModernDarkOutline,
                    onClick = { onSetThemeMode("DARK") }
                )
                ThemeOption(
                    title = "مشکی مطلق",
                    subtitle = "AMOLED، کم‌مصرف‌ترین حالت",
                    selected = themeMode == "BLACK",
                    swatch = AmoledBackground,
                    swatchBorder = AmoledOutline,
                    onClick = { onSetThemeMode("BLACK") }
                )
                ThemeOption(
                    title = "روشن",
                    subtitle = "کاغذی، برای نور روز",
                    selected = themeMode == "LIGHT",
                    swatch = ModernLightSurface,
                    swatchBorder = ModernLightOutline,
                    onClick = { onSetThemeMode("LIGHT") }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Data Source Section
        SectionTitle(title = "منبع داده دانشگاه", icon = Icons.Default.Language)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.75f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "آدرس صفحه برنامه دانشگاه:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = sourceUrl,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            tempUrl = sourceUrl
                            showUrlDialog = true
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("تغییر آدرس")
                    }

                    OutlinedButton(
                        onClick = onResetSourceUrl,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("پیش‌فرض")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. User Registered Schedule (Input JSON)
        SectionTitle(title = "برنامه انتخابی من (JSON)", icon = Icons.Default.Code)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.75f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "برنامه کلاسی ثبت شده شما که با برنامه دانشگاه مقایسه می‌شود.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        tempJson = inputJson
                        showJsonDialog = true
                    },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("مشاهده و ویرایش داده‌های برنامه (JSON)")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. Storage & Cache
        SectionTitle(title = "حافظه و کش", icon = Icons.Default.Storage)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.75f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onOpenPdfs, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp)) { Text("مدیریت PDFها") }
                    OutlinedButton(onClick = onOpenComparison, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp)) { Text("تغییرات برنامه") }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "تعداد فایل‌های PDF دانلود شده:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${PersianTextNormalizer.toPersianDigits(pdfCount.toString())} فایل",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "حجم کل فایل‌های محلی:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = storageSize,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action buttons
                OutlinedButton(
                    onClick = onClearCache,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("پاک کردن کش برنامه کلاسی")
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onClearPdfs,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("حذف فایل‌های PDF دانلود شده")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onForceRefetchAll,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("پاک کردن کش و دریافت مجدد همه فایل‌ها")
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = { showResetConfirmDialog = true },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = DiffRemoved
                    ),
                    border = BorderStroke(1.dp, DiffRemoved.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("بازنشانی کامل اطلاعات برنامه")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. Backup & Sharing
        SectionTitle(title = "پشتیبان‌گیری و اشتراک‌گذاری داده‌ها", icon = Icons.Default.Backup)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.75f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "می‌توانید تمام اطلاعات برنامه (درس‌ها، رویدادها، وظایف، یادداشت‌ها و تنظیمات) را ذخیره و بازیابی کنید.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onExportFullBackup,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.FileUpload, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("تهیه پشتیبان")
                    }
                    OutlinedButton(
                        onClick = { restoreBackupPicker.launch(arrayOf("application/json", "text/plain", "*/*")) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.FileDownload, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("بازیابی پشتیبان")
                    }
                }

                Spacer(Modifier.height(4.dp))
                Text(
                    text = "اشتراک‌گذاری رویدادها و برنامه با دیگران:",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onExportEvents,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Share, null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("ارسال رویدادها")
                    }
                    OutlinedButton(
                        onClick = { importEventsPicker.launch(arrayOf("application/json", "text/plain", "*/*")) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Event, null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("ورود رویدادها")
                    }
                }

                OutlinedButton(
                    onClick = onExportSchedule,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Share, null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("اشتراک‌گذاری فایل برنامهٔ درسی")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 5. About
        SectionTitle(title = "درباره سازنده", icon = Icons.Default.Info)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.75f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Classify · برنامه‌ریز کلاس‌های دانشگاه",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "ساخته‌شده توسط AmousaviNezhod",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "اگر مشکلی در برنامه دیدید یا پیشنهادی دارید، از یکی از راه‌های زیر پیام بدهید.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = { uriHandler.openUri(AppLinks.GITHUB_URL) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) { Text("گیت‌هاب سازنده") }
                    Button(
                        onClick = { uriHandler.openUri(AppLinks.TELEGRAM_URL) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) { Text("پیام در تلگرام") }
                }
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }

    // Dialog: Edit Source URL
    if (showUrlDialog) {
        AlertDialog(
            onDismissRequest = { showUrlDialog = false },
            title = { Text("ویرایش آدرس صفحه دانشگاه") },
            text = {
                OutlinedTextField(
                    value = tempUrl,
                    onValueChange = { tempUrl = it },
                    label = { Text("آدرس URL") },
                    singleLine = false,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateSourceUrl(tempUrl)
                        showUrlDialog = false
                    }
                ) {
                    Text("ذخیره")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUrlDialog = false }) {
                    Text("انصراف")
                }
            }
        )
    }

    // Dialog: Edit Input JSON
    if (showJsonDialog) {
        AlertDialog(
            onDismissRequest = { showJsonDialog = false },
            title = { Text("ویرایش ساختار برنامه انتخابی (JSON)") },
            text = {
                OutlinedTextField(
                    value = tempJson,
                    onValueChange = { tempJson = it },
                    label = { Text("کد JSON") },
                    singleLine = false,
                    minLines = 8,
                    maxLines = 14,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateInputJson(tempJson)
                        showJsonDialog = false
                    }
                ) {
                    Text("ذخیره و اعمال")
                }
            },
            dismissButton = {
                TextButton(onClick = { showJsonDialog = false }) {
                    Text("انصراف")
                }
            }
        )
    }

    // Dialog: Reset Confirmation
    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = { Text("بازنشانی کلی اطلاعات") },
            text = {
                Text("آیا از پاک کردن تمامی فایل‌های برنامه، فایل‌های PDF ذخیره شده و تنظیمات اطمینان دارید؟")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onResetAllData()
                        showResetConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DiffRemoved)
                ) {
                    Text("بله، بازنشانی شود")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmDialog = false }) {
                    Text("انصراف")
                }
            }
        )
    }
}

@Composable
private fun SectionTitle(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun ThemeOption(
    title: String,
    subtitle: String,
    selected: Boolean,
    swatch: Color,
    swatchBorder: Color,
    onClick: () -> Unit
) {
    val container by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        animationSpec = tween(AppMotion.DURATION_BASE, easing = AppMotion.EaseOut),
        label = "themeOptionContainer"
    )

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = container,
        tonalElevation = 0.dp,
        border = if (selected) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
        } else {
            null
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .tactileClick(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Live preview of the actual palette instead of an abstract radio dot:
            // you pick the theme by its colour, which is the thing you are choosing.
            Surface(
                shape = MaterialTheme.shapes.extraSmall,
                color = swatch,
                border = BorderStroke(1.dp, swatchBorder),
                modifier = Modifier.size(width = 34.dp, height = 26.dp)
            ) {}
            Spacer(modifier = Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            AnimatedVisibility(
                visible = selected,
                enter = fadeIn(tween(AppMotion.DURATION_BASE)) + scaleIn(initialScale = 0.7f),
                exit = fadeOut(tween(AppMotion.DURATION_QUICK))
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(19.dp)
                )
            }
        }
    }
}
