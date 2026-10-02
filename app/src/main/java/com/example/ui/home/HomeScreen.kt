package com.example.ui.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import com.example.UnitSelectionConfig
import com.example.domain.model.NormalizedSchedule
import com.example.domain.model.ScheduleClass
import com.example.domain.model.ScheduleOfferingIdentity
import com.example.domain.model.PdfAvailabilityStatus
import com.example.domain.model.ScheduleDayAvailability
import com.example.domain.normalizer.PersianTextNormalizer
import com.example.domain.parser.JsonScheduleParser

@Composable
fun HomeScreen(
    mySchedule: NormalizedSchedule?,
    universitySchedule: NormalizedSchedule?,
    selectedTab: Int,
    inputJson: String,
    searchQuery: String,
    isUpdating: Boolean,
    onTabChange: (Int) -> Unit,
    onUpdateInputJson: (String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onFetchSchedule: () -> Unit,
    dayAvailability: List<ScheduleDayAvailability> = emptyList(),
    modifier: Modifier = Modifier,
    onRemoveOffering: (ScheduleClass) -> Unit = {},
    onAddOffering: (ScheduleClass) -> Unit = {}
) {
    val context = LocalContext.current
    val unitUrl = LocalUriHandler.current
    var draftJson by remember { mutableStateOf(inputJson) }
    var importError by remember { mutableStateOf("") }
    var showHelp by remember { mutableStateOf(false) }
    var preview by remember { mutableStateOf<NormalizedSchedule?>(null) }
    val pane = selectedTab.coerceIn(0, 2)

    val jsonPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    ?: error("فایل قابل خواندن نیست.")
            }.onSuccess {
                draftJson = it
                importError = ""
            }.onFailure {
                importError = "خواندن فایل JSON انجام نشد. دوباره انتخاب کنید."
            }
        }
    }

    if (showHelp) AlertDialog(
        onDismissRequest = { showHelp = false },
        title = { Text("گرفتن خروجی JSON") },
        text = { Text("در پروژهٔ Unit Selection برنامهٔ انتخاب واحد را به‌صورت JSON خروجی بگیرید. سپس فایل را انتخاب کنید یا محتوایش را در کادر جای‌گذاری کنید.") },
        confirmButton = {
            TextButton(onClick = {
                unitUrl.openUri(UnitSelectionConfig.UNIT_SELECTION_URL)
                showHelp = false
            }) { Text("باز کردن Unit Selection") }
        },
        dismissButton = { TextButton(onClick = { showHelp = false }) { Text("بستن") } }
    )

    preview?.let { imported ->
        val previewItems = imported.classes.distinctBy(ScheduleOfferingIdentity::key)
        AlertDialog(
            onDismissRequest = { preview = null },
            title = { Text("پیش‌نمایش · ${PersianTextNormalizer.toPersianDigits(previewItems.size.toString())} کلاس") },
            text = {
                LazyColumn(Modifier.heightIn(max = 440.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(previewItems, key = { ScheduleOfferingIdentity.key(it) }) { OfferingCard(it) }
                }
            },
            confirmButton = {
                Button(onClick = {
                    onUpdateInputJson(imported.rawJson)
                    preview = null
                    onTabChange(0)
                }, modifier = Modifier.testTag("confirm_json_import")) { Text("تأیید و ذخیرهٔ برنامه") }
            },
            dismissButton = { TextButton(onClick = { preview = null }) { Text("بازگشت") } }
        )
    }

    Column(modifier.fillMaxSize()) {
        when (pane) {
            0 -> MyCoursesPane(
                schedule = mySchedule,
                universitySchedule = universitySchedule,
                isUpdating = isUpdating,
                onFetch = onFetchSchedule,
                onAdd = { onTabChange(2) },
                onBrowse = { onTabChange(1) },
                onRemove = onRemoveOffering,
                onEdit = {
                    draftJson = inputJson
                    importError = ""
                    onTabChange(2)
                }
            )
            1 -> CatalogPane(
                schedule = universitySchedule,
                dayAvailability = dayAvailability,
                searchQuery = searchQuery,
                isUpdating = isUpdating,
                onSearch = onSearchQueryChange,
                onFetch = onFetchSchedule,
                onAdd = onAddOffering,
                isSelected = { item -> mySchedule?.classes?.any { ScheduleOfferingIdentity.findCatalogMatch(item, listOf(it)) != null } == true }
            )
            else -> ImportPane(
                schedule = universitySchedule,
                draftJson = draftJson,
                error = importError,
                onDraftChange = { draftJson = it; importError = "" },
                onPickFile = { jsonPicker.launch(arrayOf("application/json", "text/plain", "*/*")) },
                onHelp = { showHelp = true },
                onPreview = {
                    val parsed = JsonScheduleParser.parse(draftJson)
                    if (parsed.isSuccess && parsed.getOrNull()?.classes?.isNotEmpty() == true) {
                        preview = parsed.getOrNull()
                        importError = ""
                    } else {
                        importError = parsed.exceptionOrNull()?.localizedMessage ?: "هیچ کلاسی در JSON پیدا نشد."
                    }
                },
                onManual = { onTabChange(1) },
                onFetch = onFetchSchedule
            )
        }
    }
}

@Composable
private fun MyCoursesPane(
    schedule: NormalizedSchedule?,
    universitySchedule: NormalizedSchedule?,
    isUpdating: Boolean,
    onFetch: () -> Unit,
    onAdd: () -> Unit,
    onBrowse: () -> Unit,
    onRemove: (ScheduleClass) -> Unit,
    onEdit: () -> Unit
) {
    val courses = remember(schedule, universitySchedule) {
        val catalogClasses = universitySchedule?.classes.orEmpty()
        schedule?.classes.orEmpty().map { own ->
            own to ScheduleOfferingIdentity.enrichFromCatalog(own, catalogClasses)
        }.distinctBy { ScheduleOfferingIdentity.key(it.first) }
            .sortedWith(compareBy({ it.second.dayIndex }, { it.second.startTime }))
    }

    Column(Modifier.fillMaxSize()) {
        if (courses.isEmpty()) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f), modifier = Modifier.size(72.dp)) {
                    Box(contentAlignment = Alignment.Center) { Icon(Icons.AutoMirrored.Filled.EventNote, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(33.dp)) }
                }
                Text("هنوز درسی اضافه نشده", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Text("داده‌های دانشگاه را دریافت کنید، یا برنامهٔ خودتان را از Unit Selection وارد کنید.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                Button(onClick = onFetch, enabled = !isUpdating, modifier = Modifier.fillMaxWidth().testTag("empty_state_fetch_schedule"), shape = RoundedCornerShape(14.dp)) {
                    if (isUpdating) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Icon(Icons.Default.Download, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (isUpdating) "در حال دریافت داده‌ها…" else "دریافت برنامهٔ دانشگاه")
                }
                OutlinedButton(onClick = onAdd, modifier = Modifier.fillMaxWidth().testTag("empty_state_json_entry"), shape = RoundedCornerShape(14.dp)) {
                    Icon(Icons.Default.UploadFile, null)
                    Spacer(Modifier.width(8.dp))
                    Text("افزودن / وارد کردن برنامه")
                }
                if (universitySchedule?.classes?.isNotEmpty() == true) TextButton(onClick = onBrowse) { Text("یا مرور همهٔ کلاس‌ها") }
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onAdd, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                            Icon(Icons.Default.Add, null); Spacer(Modifier.width(4.dp)); Text("افزودن درس")
                        }
                        OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                            Icon(Icons.Default.Code, null); Spacer(Modifier.width(4.dp)); Text("ویرایش برنامه")
                        }
                    }
                }
                items(courses, key = { ScheduleOfferingIdentity.key(it.first) }) { (original, display) ->
                    OfferingCard(display, actionLabel = "حذف", onAction = { onRemove(original) })
                }
            }
        }
    }
}

@Composable
private fun CatalogPane(
    schedule: NormalizedSchedule?,
    dayAvailability: List<ScheduleDayAvailability>,
    searchQuery: String,
    isUpdating: Boolean,
    onSearch: (String) -> Unit,
    onFetch: () -> Unit,
    onAdd: (ScheduleClass) -> Unit,
    isSelected: (ScheduleClass) -> Boolean
) {
    val catalogClasses = schedule?.classes.orEmpty()
    if (catalogClasses.isEmpty()) {
        Column(Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.AccountBalance, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(44.dp))
                Spacer(Modifier.height(12.dp))
                Text("فهرست کلاس‌ها هنوز آماده نیست", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text("دریافت داده‌ها PDFهای دانشگاه را می‌خواند و همهٔ ارائه‌ها را می‌سازد.", textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (dayAvailability.isNotEmpty()) {
                    Spacer(Modifier.height(18.dp))
                    DayPdfStatusList(dayAvailability)
                }
                Spacer(Modifier.height(18.dp))
                Button(onClick = onFetch, enabled = !isUpdating) { Text(if (isUpdating) "در حال دریافت…" else "دریافت داده‌های دانشگاه") }
            }
        }
        return
    }
    Column(Modifier.fillMaxSize()) {
        if (dayAvailability.isNotEmpty()) DayPdfStatusList(dayAvailability, Modifier.padding(top = 4.dp))
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearch,
            placeholder = { Text("نام، کد، گروه، روز، ساعت، استاد یا مکان") },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.primary) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = MaterialTheme.colorScheme.surface, unfocusedContainerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp).testTag("search_classes_input")
        )
        val offerings = remember(catalogClasses, searchQuery) {
            val query = PersianTextNormalizer.toAsciiDigits(PersianTextNormalizer.normalizeText(searchQuery)).lowercase(Locale.ROOT)
            catalogClasses.distinctBy(ScheduleOfferingIdentity::key)
                .filter { query.isBlank() || PersianTextNormalizer.toAsciiDigits(ScheduleOfferingIdentity.searchText(it)).contains(query) }
                .sortedWith(compareBy({ it.courseName }, { it.groupCode }, { it.dayIndex }, { it.startTime }))
        }
        Text("${PersianTextNormalizer.toPersianDigits(offerings.size.toString())} نتیجه", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 22.dp, vertical = 2.dp))
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(offerings, key = { ScheduleOfferingIdentity.key(it) }) { course ->
                val selected = isSelected(course)
                OfferingCard(course, actionLabel = if (selected) "افزوده شد" else "افزودن", onAction = { if (!selected) onAdd(course) }, isSelected = selected)
            }
            if (offerings.isEmpty()) item {
                Text("کلاسی با این عبارت پیدا نشد.", Modifier.fillMaxWidth().padding(32.dp), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ImportPane(
    schedule: NormalizedSchedule?,
    draftJson: String,
    error: String,
    onDraftChange: (String) -> Unit,
    onPickFile: () -> Unit,
    onHelp: () -> Unit,
    onPreview: () -> Unit,
    onManual: () -> Unit,
    onFetch: () -> Unit
) {
    val hasCatalog = !schedule?.classes.isNullOrEmpty()
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("افزودن / وارد کردن", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                IconButton(onClick = onHelp, modifier = Modifier.testTag("json_import_help")) {
                    Text("?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
            Text("ورود از Unit Selection", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("خروجی JSON را انتخاب کنید یا محتوایش را در کادر جای‌گذاری کنید.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = draftJson,
                onValueChange = onDraftChange,
                placeholder = { Text("{  \"format\": \"unit-selection-schedule\", … }", fontFamily = FontFamily.Monospace) },
                minLines = 7,
                maxLines = 11,
                shape = RoundedCornerShape(16.dp),
                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                modifier = Modifier.fillMaxWidth().testTag("json_editor_input")
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onPickFile, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Default.FolderOpen, null)
                    Spacer(Modifier.width(5.dp))
                    Text("انتخاب فایل")
                }
                Button(onClick = onPreview, enabled = draftJson.isNotBlank(), modifier = Modifier.weight(1f).testTag("preview_json_button"), shape = RoundedCornerShape(12.dp)) { Text("بررسی و پیش‌نمایش") }
            }
            if (error.isNotBlank()) {
                Spacer(Modifier.height(10.dp)); Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            if (hasCatalog) {
                Spacer(Modifier.height(10.dp))
                Text(
                    "پس از انتخاب کلاس‌ها از فهرست PDF، نام استاد و محل برگزاری به برنامهٔ واردشده افزوده می‌شود.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(24.dp)); DividerLabel("یا انتخاب دستی")
            Spacer(Modifier.height(12.dp))
            Text("فهرست دستی از فایل‌های PDF دانشگاه ساخته می‌شود، نه از یک لیست ثابت.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            if (hasCatalog) {
                Button(onClick = onManual, modifier = Modifier.fillMaxWidth().testTag("empty_state_pick_courses"), shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Default.Search, null); Spacer(Modifier.width(8.dp)); Text("جستجو و انتخاب از همهٔ کلاس‌ها")
                }
            } else {
                OutlinedButton(onClick = onFetch, modifier = Modifier.fillMaxWidth().testTag("import_load_catalog"), shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Default.Download, null); Spacer(Modifier.width(8.dp)); Text("دریافت داده‌های دانشگاه")
                }
            }
            Spacer(Modifier.height(10.dp)); Text("قالب سازگار: unit-selection-schedule", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
private fun DayPdfStatusList(items: List<ScheduleDayAvailability>, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        items.sortedBy(ScheduleDayAvailability::dayIndex).forEach { day ->
            val label = when (day.status) {
                PdfAvailabilityStatus.AVAILABLE -> "✓ برنامه استخراج شد (${PersianTextNormalizer.toPersianDigits(day.normalClassCount.toString())} کلاس، ${PersianTextNormalizer.toPersianDigits(day.workshopClassCount.toString())} کارگاه)"
                PdfAvailabilityStatus.NOT_FOUND -> "○ فایل هنوز بارگذاری نشده"
                PdfAvailabilityStatus.DOWNLOAD_FAILED -> "⚠ دانلود فایل ناموفق بود"
                PdfAvailabilityStatus.PARSE_FAILED -> "⚠ فایل پیدا شد، اما استخراج جدول ناموفق بود"
                PdfAvailabilityStatus.CHECK_FAILED -> "؟ بررسی سایت انجام نشد"
                PdfAvailabilityStatus.UNKNOWN -> if (day.discoveredPdfCount > 0) "… فایل پیدا شده، در انتظار دانلود یا استخراج" else "— هنوز بررسی نشده"
            }
            val color = when (day.status) {
                PdfAvailabilityStatus.AVAILABLE -> MaterialTheme.colorScheme.primary
                PdfAvailabilityStatus.PARSE_FAILED, PdfAvailabilityStatus.DOWNLOAD_FAILED, PdfAvailabilityStatus.CHECK_FAILED -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(day.dayName, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                Text(label, style = MaterialTheme.typography.labelSmall, color = color, textAlign = TextAlign.End)
            }
        }
    }
}

@Composable
private fun OfferingCard(
    course: ScheduleClass,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    isSelected: Boolean = false
) {
    Card(
        shape = RoundedCornerShape(17.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth().testTag("offering_${ScheduleOfferingIdentity.key(course).hashCode()}")
    ) {
        Column(Modifier.fillMaxWidth().padding(15.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(course.courseName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(buildList {
                        if (course.courseCode.isNotBlank()) add("کد ${PersianTextNormalizer.toPersianDigits(course.courseCode)}")
                        if (course.groupCode.isNotBlank()) add("گروه ${PersianTextNormalizer.toPersianDigits(course.groupCode)}")
                        if (course.units > 0) add("${PersianTextNormalizer.toPersianDigits(course.units.toString())} واحد")
                    }.joinToString("  ·  "), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (onAction != null) TextButton(
                    onClick = onAction,
                    enabled = !isSelected,
                    modifier = Modifier.testTag("offering_action_${ScheduleOfferingIdentity.key(course).hashCode()}")
                ) {
                    Icon(if (isSelected) Icons.Default.Check else if (actionLabel == "حذف") Icons.Default.DeleteOutline else Icons.Default.Add, null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(4.dp)); Text(actionLabel ?: "افزودن")
                }
            }
            Spacer(Modifier.height(10.dp))
            Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 11.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(course.dayOfWeek, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.weight(1f))
                    Text("${PersianTextNormalizer.toPersianDigits(course.startTime)} – ${PersianTextNormalizer.toPersianDigits(course.endTime)}", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.MeetingRoom, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(5.dp)); Text(course.classroom.ifBlank { "مکان ثبت نشده" }, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                }
                if (course.teacher.isNotBlank()) Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, null, modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(4.dp))
                    Text(course.teacher, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun DividerLabel(label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(Modifier.weight(1f).height(1.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)) {}
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(horizontal = 10.dp))
        Surface(Modifier.weight(1f).height(1.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)) {}
    }
}
