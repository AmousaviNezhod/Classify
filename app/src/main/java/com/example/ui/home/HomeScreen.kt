package com.example.ui.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MeetingRoom
import com.example.domain.calendar.PersianCalendarHelper
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import com.example.ui.components.DaySelector
import com.example.ui.components.SegmentedTabs
import com.example.ui.theme.AppMotion
import com.example.ui.theme.tactileClick

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    mySchedule: NormalizedSchedule?,
    universitySchedule: NormalizedSchedule?,
    selectedTab: Int = 0,
    inputJson: String,
    searchQuery: String,
    isUpdating: Boolean,
    onTabChange: (Int) -> Unit = {},
    onUpdateInputJson: (String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onFetchSchedule: () -> Unit,
    dayAvailability: List<ScheduleDayAvailability> = emptyList(),
    modifier: Modifier = Modifier,
    onRemoveOffering: (ScheduleClass) -> Unit = {},
    onAddOffering: (ScheduleClass) -> Unit = {},
    onOpenCourseDetail: (ScheduleClass) -> Unit = {}
) {
    val context = LocalContext.current
    val unitUrl = LocalUriHandler.current
    var draftJson by remember { mutableStateOf(inputJson) }
    var importError by remember { mutableStateOf("") }
    var showHelp by remember { mutableStateOf(false) }
    var preview by remember { mutableStateOf<NormalizedSchedule?>(null) }

    var internalTab by remember { mutableIntStateOf(selectedTab) }

    // Sync if caller pushes an explicit tab change
    LaunchedEffect(selectedTab) {
        internalTab = selectedTab
    }

    val pane = internalTab.coerceIn(0, 2)
    val selectedCatalogOfferingKeys = remember(mySchedule?.classes, universitySchedule?.classes) {
        val catalog = universitySchedule?.classes.orEmpty()
        if (catalog.isEmpty()) emptySet()
        else {
            val catalogByDay = catalog.groupBy { it.dayIndex }
            mySchedule?.classes.orEmpty().mapNotNull { selected ->
                val dayCatalog = catalogByDay[selected.dayIndex].orEmpty()
                ScheduleOfferingIdentity.findCatalogMatch(selected, dayCatalog)?.let(ScheduleOfferingIdentity::key)
            }.toSet()
        }
    }
    val changeTab: (Int) -> Unit = { index ->
        internalTab = index
        onTabChange(index)
    }

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
        val catalog = universitySchedule?.classes.orEmpty()
        val previewItems = remember(imported, catalog) {
            imported.classes
                .map { ScheduleOfferingIdentity.enrichFromCatalog(it, catalog) }
                .distinctBy(ScheduleOfferingIdentity::key)
        }
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
                    changeTab(0)
                }, modifier = Modifier.testTag("confirm_json_import")) { Text("تأیید و ذخیرهٔ برنامه") }
            },
            dismissButton = { TextButton(onClick = { preview = null }) { Text("بازگشت") } }
        )
    }

    Column(modifier.fillMaxSize()) {
        // Top Tab Navigation Bar
        val myCoursesCount = remember(mySchedule?.classes) {
            mySchedule?.classes.orEmpty().map { it.semanticKey }.distinct().size
        }
        val catalogCount = remember(universitySchedule?.classes) {
            universitySchedule?.classes.orEmpty().size
        }

        // Counts moved from the tab labels into the panes: three Persian labels of
        // varying length inside one segmented control only stay readable if they
        // keep a similar width.
        SegmentedTabs(
            labels = listOf("درس‌های من", "همهٔ کلاس‌ها", "افزودن"),
            selectedIndex = pane,
            onSelect = { changeTab(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            testTagPrefix = "courses_pane"
        )

        val myCoursesLabel = if (myCoursesCount > 0) {
            "${PersianTextNormalizer.toPersianDigits(myCoursesCount.toString())} درس در برنامهٔ شما"
        } else {
            null
        }
        val catalogLabel = if (catalogCount > 0) {
            "${PersianTextNormalizer.toPersianDigits(catalogCount.toString())} ارائه در فهرست دانشگاه"
        } else {
            null
        }
        val paneSummary = when (pane) {
            0 -> myCoursesLabel
            1 -> catalogLabel
            else -> null
        }
        if (paneSummary != null) {
            Text(
                text = paneSummary,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 4.dp)
            )
        }

        when (pane) {
            0 -> MyCoursesPane(
                schedule = mySchedule,
                universitySchedule = universitySchedule,
                dayAvailability = dayAvailability,
                isUpdating = isUpdating,
                onFetch = onFetchSchedule,
                onAdd = { changeTab(2) },
                onBrowse = { changeTab(1) },
                onRemove = onRemoveOffering,
                onOpenCourseDetail = onOpenCourseDetail,
                onEdit = {
                    draftJson = inputJson
                    importError = ""
                    changeTab(2)
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
                onOpenCourseDetail = onOpenCourseDetail,
                selectedOfferingKeys = selectedCatalogOfferingKeys
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
                onManual = { changeTab(1) },
                onFetch = onFetchSchedule
            )
        }
    }
}

@Composable
private fun MyCoursesPane(
    schedule: NormalizedSchedule?,
    universitySchedule: NormalizedSchedule?,
    dayAvailability: List<ScheduleDayAvailability>,
    isUpdating: Boolean,
    onFetch: () -> Unit,
    onAdd: () -> Unit,
    onBrowse: () -> Unit,
    onRemove: (ScheduleClass) -> Unit,
    onOpenCourseDetail: (ScheduleClass) -> Unit,
    onEdit: () -> Unit
) {
    val courses = remember(schedule, universitySchedule) {
        val catalogClasses = universitySchedule?.classes.orEmpty()
        val catalogByDay = catalogClasses.groupBy { it.dayIndex }
        schedule?.classes.orEmpty().map { own ->
            own to ScheduleOfferingIdentity.enrichFromCatalog(own, catalogByDay[own.dayIndex].orEmpty())
        }.distinctBy { it.first.semanticKey }
            .sortedWith(compareBy({ it.second.dayIndex }, { it.second.startTime }))
    }

    val hasCatalog = universitySchedule?.classes?.isNotEmpty() == true

    Column(Modifier.fillMaxSize()) {
        if (courses.isEmpty()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 22.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.38f),
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.AutoMirrored.Filled.EventNote,
                            null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(33.dp)
                        )
                    }
                }

                Text("هنوز درسی به برنامهٔ شما اضافه نشده", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)

                if (hasCatalog) {
                    Text(
                        "فهرست ${PersianTextNormalizer.toPersianDigits(universitySchedule!!.classes.size.toString())} کلاس دانشگاه دریافت شده است. می‌توانید از بخش «همهٔ کلاس‌ها» درس‌های خود را انتخاب کنید یا برنامهٔ کامل را وارد کنید.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Button(
                        onClick = onBrowse,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Search, null)
                        Spacer(Modifier.width(8.dp))
                        Text("مشاهده و انتخاب از همهٔ کلاس‌ها")
                    }

                    OutlinedButton(
                        onClick = onAdd,
                        modifier = Modifier.fillMaxWidth().testTag("empty_state_json_entry"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.UploadFile, null)
                        Spacer(Modifier.width(8.dp))
                        Text("وارد کردن برنامه از فایل JSON")
                    }
                } else {
                    Text(
                        "داده‌های دانشگاه را دریافت کنید، یا برنامهٔ خودتان را از Unit Selection وارد کنید.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Button(
                        onClick = onFetch,
                        enabled = !isUpdating,
                        modifier = Modifier.fillMaxWidth().testTag("empty_state_fetch_schedule"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        if (isUpdating) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        else Icon(Icons.Default.Download, null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (isUpdating) "در حال دریافت داده‌ها…" else "دریافت برنامهٔ دانشگاه")
                    }

                    OutlinedButton(
                        onClick = onAdd,
                        modifier = Modifier.fillMaxWidth().testTag("empty_state_json_entry"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.UploadFile, null)
                        Spacer(Modifier.width(8.dp))
                        Text("افزودن / وارد کردن برنامه")
                    }
                }

                if (dayAvailability.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    DayPdfStatusCard(dayAvailability)
                }
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onBrowse,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("افزودن درس")
                        }
                        OutlinedButton(
                            onClick = onEdit,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Code, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("ویرایش برنامه")
                        }
                    }
                }

                items(courses, key = { ScheduleOfferingIdentity.key(it.first) }) { (original, display) ->
                    // animateItem keeps the list readable while a course is added or
                    // removed: the remaining cards slide instead of jumping.
                    Box(Modifier.animateItem()) {
                        OfferingCard(
                            course = display,
                            actionLabel = "حذف",
                            onAction = { onRemove(original) },
                            onClick = { onOpenCourseDetail(original) }
                        )
                    }
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
    onOpenCourseDetail: (ScheduleClass) -> Unit = {},
    selectedOfferingKeys: Set<String>
) {
    val catalogClasses = schedule?.classes.orEmpty()
    val searchableCatalog = remember(catalogClasses) {
        catalogClasses
            .map { course -> Triple(course, ScheduleOfferingIdentity.key(course), ScheduleOfferingIdentity.searchText(course)) }
            .sortedWith(compareBy({ it.first.dayIndex }, { it.first.startTime }, { it.first.courseName }, { it.first.groupCode }))
    }
    var selectedDayFilter by remember { mutableStateOf<Int?>(null) }
    var selectedParityFilter by remember { mutableStateOf<String?>(null) }
    var settledSearchQuery by remember { mutableStateOf(searchQuery) }

    LaunchedEffect(searchQuery) {
        kotlinx.coroutines.delay(120)
        settledSearchQuery = searchQuery
    }

    if (catalogClasses.isEmpty()) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.AccountBalance, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(44.dp))
                Spacer(Modifier.height(12.dp))
                Text("فهرست کلاس‌ها هنوز آماده نیست", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text("دریافت داده‌ها PDFهای دانشگاه را می‌خواند و همهٔ ارائه‌ها را می‌سازد.", textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (dayAvailability.isNotEmpty()) {
                    Spacer(Modifier.height(18.dp))
                    DayPdfStatusCard(dayAvailability)
                }
                Spacer(Modifier.height(18.dp))
                Button(onClick = onFetch, enabled = !isUpdating) {
                    if (isUpdating) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    else Icon(Icons.Default.Download, null)
                    Spacer(Modifier.width(6.dp))
                    Text(if (isUpdating) "در حال دریافت…" else "دریافت داده‌های دانشگاه")
                }
            }
        }
        return
    }

    Column(Modifier.fillMaxSize()) {
        // Status of Days' PDFs
        if (dayAvailability.isNotEmpty()) {
            DayPdfStatusCard(dayAvailability, Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
        }

        // Search Input
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearch,
            placeholder = { Text("جستجوی نام درس، استاد، کد، گروه، مکان یا روز...") },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.primary) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .testTag("search_classes_input")
        )

        // Day & Parity Filter Rows
        DaySelector(
            selectedDayIndex = selectedDayFilter,
            onSelectDay = { selectedDayFilter = it }
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val parityOptions = listOf<Pair<String?, String>>(
                null to "همهٔ هفته‌ها",
                "زوج" to "فقط هفته زوج",
                "فرد" to "فقط هفته فرد"
            )
            parityOptions.forEach { (key, label) ->
                val isSelected = selectedParityFilter == key
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                    modifier = Modifier.clickable { selectedParityFilter = key }
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Filter Offerings
        val offerings = remember(searchableCatalog, settledSearchQuery, selectedDayFilter, selectedParityFilter) {
            val query = PersianTextNormalizer.toAsciiDigits(PersianTextNormalizer.normalizeText(settledSearchQuery)).lowercase(Locale.ROOT)
            searchableCatalog.filter { (course, _, searchableText) ->
                val matchesQuery = query.isBlank() || searchableText.contains(query)
                val matchesDay = selectedDayFilter == null || course.dayIndex == selectedDayFilter
                val matchesParity = when (selectedParityFilter) {
                    "زوج" -> course.parity.contains("زوج") || course.parity.isBlank()
                    "فرد" -> course.parity.contains("فرد") || course.parity.isBlank()
                    else -> true
                }
                matchesQuery && matchesDay && matchesParity
            }
        }

        Text(
            text = "${PersianTextNormalizer.toPersianDigits(offerings.size.toString())} نتیجه یافت شد",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
        )

        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(offerings, key = { it.second }) { (course, offeringKey, _) ->
                val selected = offeringKey in selectedOfferingKeys
                OfferingCard(
                    course = course,
                    actionLabel = if (selected) "افزوده شد" else "افزودن",
                    onAction = { if (!selected) onAdd(course) },
                    isSelected = selected,
                    onClick = { onOpenCourseDetail(course) }
                )
            }
            if (offerings.isEmpty()) {
                item {
                    Text(
                        "کلاسی با این مشخصات پیدا نشد.",
                        Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 96.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("افزودن و وارد کردن برنامه", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
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
                Button(onClick = onPreview, enabled = draftJson.isNotBlank(), modifier = Modifier.weight(1f).testTag("preview_json_button"), shape = RoundedCornerShape(12.dp)) {
                    Text("بررسی و پیش‌نمایش")
                }
            }
            if (error.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            if (hasCatalog) {
                Spacer(Modifier.height(10.dp))
                Text(
                    "پس از انتخاب کلاس‌ها از فهرست PDF، نام استاد و محل برگزاری به برنامهٔ واردشده افزوده می‌شود.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(24.dp))
            DividerLabel("یا انتخاب دستی")
            Spacer(Modifier.height(12.dp))
            Text("می‌توانید کلاس‌ها را مستقیماً از برنامه استخراج‌شدهٔ PDF دانشگاه انتخاب کنید.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            if (hasCatalog) {
                Button(onClick = onManual, modifier = Modifier.fillMaxWidth().testTag("empty_state_pick_courses"), shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Default.Search, null)
                    Spacer(Modifier.width(8.dp))
                    Text("جستجو و انتخاب از همهٔ کلاس‌ها")
                }
            } else {
                OutlinedButton(onClick = onFetch, modifier = Modifier.fillMaxWidth().testTag("import_load_catalog"), shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Default.Download, null)
                    Spacer(Modifier.width(8.dp))
                    Text("دریافت داده‌های دانشگاه")
                }
            }
            Spacer(Modifier.height(10.dp))
            Text("قالب سازگار: unit-selection-schedule", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
fun DayPdfStatusCard(items: List<ScheduleDayAvailability>, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    val readyCount = items.count { it.status == PdfAvailabilityStatus.AVAILABLE }

    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.75f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.DateRange,
                        null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "وضعیت دریافت برنامهٔ روزها: ${PersianTextNormalizer.toPersianDigits(readyCount.toString())} از ${PersianTextNormalizer.toPersianDigits(items.size.toString())} روز آماده است",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.padding(top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items.sortedBy(ScheduleDayAvailability::dayIndex).forEach { day ->
                        val label = when (day.status) {
                            PdfAvailabilityStatus.AVAILABLE -> "✓ استخراج شد (${PersianTextNormalizer.toPersianDigits(day.normalClassCount.toString())} کلاس، ${PersianTextNormalizer.toPersianDigits(day.workshopClassCount.toString())} کارگاه)"
                            PdfAvailabilityStatus.NOT_FOUND -> "○ هنوز بارگذاری نشده"
                            PdfAvailabilityStatus.DOWNLOAD_FAILED -> "⚠ دانلود فایل ناموفق بود"
                            PdfAvailabilityStatus.PARSE_FAILED -> "⚠ جدول برنامه خوانده نشد"
                            PdfAvailabilityStatus.CHECK_FAILED -> "؟ نیاز به همگام‌سازی"
                            PdfAvailabilityStatus.UNKNOWN -> if (day.discoveredPdfCount > 0) "… در انتظار دانلود یا استخراج" else "— هنوز بررسی نشده"
                        }
                        val color = when (day.status) {
                            PdfAvailabilityStatus.AVAILABLE -> MaterialTheme.colorScheme.primary
                            PdfAvailabilityStatus.PARSE_FAILED, PdfAvailabilityStatus.DOWNLOAD_FAILED, PdfAvailabilityStatus.CHECK_FAILED -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        val weekTag = if (day.weekNumber != null) "هفته ${PersianTextNormalizer.toPersianDigits(day.weekNumber.toString())}" else ""
                        val dateTag = if (day.checkedAt > 0) "به‌روزرسانی ${PersianCalendarHelper.formatShortDate(day.checkedAt)}" else ""
                        val metaTag = listOf(weekTag, dateTag).filter(String::isNotBlank).joinToString(" · ")
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(day.dayName, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                                if (metaTag.isNotBlank()) {
                                    Text(metaTag, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
                                }
                            }
                            Text(label, style = MaterialTheme.typography.labelSmall, color = color, textAlign = TextAlign.End)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OfferingCard(
    course: ScheduleClass,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    isSelected: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    // Selected state animates rather than snapping: when a course is added from
    // the catalog the card visibly settles into its new state.
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
        else MaterialTheme.colorScheme.outline.copy(alpha = 0.75f),
        animationSpec = tween(AppMotion.DURATION_BASE, easing = AppMotion.EaseOut),
        label = "offeringBorder"
    )
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainerHigh,
        animationSpec = tween(AppMotion.DURATION_BASE, easing = AppMotion.EaseOut),
        label = "offeringContainer"
    )

    Surface(
        shape = MaterialTheme.shapes.large,
        color = containerColor,
        tonalElevation = 0.dp,
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("offering_${ScheduleOfferingIdentity.key(course).hashCode()}")
            .then(if (onClick != null) Modifier.tactileClick(onClick = onClick) else Modifier)
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        course.courseName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        buildList {
                            if (course.courseCode.isNotBlank()) add("کد ${PersianTextNormalizer.toPersianDigits(course.courseCode)}")
                            if (course.groupCode.isNotBlank()) add("گروه ${PersianTextNormalizer.toPersianDigits(course.groupCode)}")
                            if (course.units > 0) add("${PersianTextNormalizer.toPersianDigits(course.units.toString())} واحد")
                        }.joinToString("  ·  "),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (onAction != null) {
                    if (isSelected) {
                        Surface(
                            shape = MaterialTheme.shapes.extraSmall,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = "✓ افزوده شد",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    } else {
                        Button(
                            onClick = onAction,
                            shape = MaterialTheme.shapes.small,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(actionLabel ?: "افزودن", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(course.dayOfWeek, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Text(
                        "${PersianTextNormalizer.toPersianDigits(course.startTime)} – ${PersianTextNormalizer.toPersianDigits(course.endTime)}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.MeetingRoom, null, modifier = Modifier.size(13.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(3.dp))
                        Text(course.classroom.ifBlank { "مکان مشخص نشده" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            if (course.teacher.isNotBlank() || course.parity.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (course.teacher.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Person, null, modifier = Modifier.size(13.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.width(3.dp))
                            Text(course.teacher, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                        }
                    }
                    val parityText = when {
                        course.parity.contains("زوج") -> "هفته زوج"
                        course.parity.contains("فرد") -> "هفته فرد"
                        else -> "تمام هفته‌ها"
                    }
                    val isEven = course.parity.contains("زوج")
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = when {
                            isEven -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            course.parity.contains("فرد") -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    ) {
                        Text(
                            text = parityText,
                            style = MaterialTheme.typography.labelSmall,
                            color = when {
                                isEven -> MaterialTheme.colorScheme.primary
                                course.parity.contains("فرد") -> MaterialTheme.colorScheme.secondary
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
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
