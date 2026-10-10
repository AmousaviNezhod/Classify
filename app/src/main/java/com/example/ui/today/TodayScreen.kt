package com.example.ui.today

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.calendar.EducationalWeekConfig
import com.example.domain.calendar.PersianCalendarHelper
import com.example.domain.model.CourseEvent
import com.example.domain.model.CourseTask
import com.example.domain.model.ScheduleClass
import com.example.domain.normalizer.PersianTextNormalizer
import com.example.ui.components.EducationalWeekEditDialog
import com.example.ui.components.PersianDatePickerDialog
import com.example.ui.theme.AppMotion
import com.example.ui.theme.revealOnEnter
import com.example.ui.theme.tactileClick
import com.example.ui.util.DateTimeUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TodayScreen(
    myClasses: List<ScheduleClass>,
    universitySchedule: com.example.domain.model.NormalizedSchedule? = null,
    events: List<CourseEvent>,
    tasks: List<CourseTask>,
    isUpdating: Boolean,
    educationalWeekConfig: EducationalWeekConfig = EducationalWeekConfig(),
    onSetEducationalWeek: (Int) -> Unit = {},
    onFetchSchedule: () -> Unit,
    onNavigateToCourses: (Int) -> Unit = {},
    onOpenCourseDetail: (ScheduleClass) -> Unit,
    onToggleTaskDone: (CourseTask) -> Unit,
    modifier: Modifier = Modifier
) {
    val todayJdn = remember { PersianCalendarHelper.getTodayJdn() }
    var selectedJdn by remember { mutableLongStateOf(todayJdn) }
    val isToday = selectedJdn == todayJdn

    val selectedDayIndex = remember(selectedJdn) { PersianCalendarHelper.getDayOfWeekIndex(selectedJdn) }
    val selectedJalali = remember(selectedJdn) { PersianCalendarHelper.jdnToJalali(selectedJdn) }
    val selectedDayName = remember(selectedDayIndex) { PersianCalendarHelper.getDayOfWeekName(selectedDayIndex) }
    val currentWeekNumber = remember(educationalWeekConfig, selectedJdn) {
        educationalWeekConfig.getWeekForDate(selectedJdn)
    }
    val currentParity = remember(currentWeekNumber) {
        PersianCalendarHelper.getWeekParityString(currentWeekNumber)
    }

    var showWeekEditorDialog by remember { mutableStateOf(false) }
    var showDatePickerDialog by remember { mutableStateOf(false) }

    val nowTimeStr = remember { SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()) }

    // Filter classes for the selected day AND respecting educational week parity
    val dayClasses = remember(myClasses, selectedDayIndex, currentWeekNumber) {
        myClasses
            .filter { it.dayIndex == selectedDayIndex && it.isValidForWeek(currentWeekNumber) }
            .sortedBy { it.startTime }
    }

    // Next class calculation (active when viewing Today)
    val nextClass = remember(dayClasses, nowTimeStr, isToday) {
        if (!isToday) null
        else dayClasses.firstOrNull { it.endTime >= nowTimeStr } ?: dayClasses.firstOrNull()
    }

    // Upcoming important events
    val upcomingEvents = remember(events) {
        events.filter { !it.isCompleted }
            .sortedWith(compareBy({ if (it.timestamp > 0) it.timestamp else Long.MAX_VALUE }, { it.dateString }))
            .take(3)
    }

    // Incomplete tasks
    val urgentTasks = remember(tasks) {
        tasks.filter { !it.isDone }.take(4)
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 18.dp,
            end = 18.dp,
            top = 14.dp,
            bottom = 100.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Compact Educational Calendar Strip with day navigation
        item {
            EducationalCalendarStrip(
                dayName = selectedDayName,
                dateFormatted = selectedJalali.formatted,
                weekNumber = currentWeekNumber,
                parity = currentParity,
                isToday = isToday,
                onPreviousDay = { selectedJdn -= 1 },
                onNextDay = { selectedJdn += 1 },
                onTodayClick = { selectedJdn = todayJdn },
                onOpenDatePicker = { showDatePickerDialog = true },
                onOpenWeekEditor = { showWeekEditorDialog = true },
                modifier = Modifier.revealOnEnter(index = 0)
            )
        }

        // 2. Today / Selected Day Dashboard Hero
        item {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 0.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.8f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .revealOnEnter(index = 1)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = if (isToday) "برنامهٔ امروز ($selectedDayName)" else "برنامهٔ $selectedDayName",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "${selectedJalali.formattedFull} · هفته $currentWeekNumber ($currentParity)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (isToday) {
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.AccessTime,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = PersianTextNormalizer.toPersianDigits(nowTimeStr),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TodaySummaryPill(
                            count = dayClasses.size,
                            label = if (isToday) "کلاس امروز" else "کلاس این روز",
                            modifier = Modifier.weight(1f)
                        )
                        TodaySummaryPill(
                            count = upcomingEvents.size,
                            label = "رویداد پیش‌رو",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 3. Class Hero Card / Empty State Card
        item {
            val catalogClassesCount = universitySchedule?.classes.orEmpty().size
            if (myClasses.isEmpty()) {
                EmptyCoursesHeroCard(
                    hasCatalog = catalogClassesCount > 0,
                    catalogCount = catalogClassesCount,
                    isUpdating = isUpdating,
                    onFetch = onFetchSchedule,
                    onPickFromCatalog = { onNavigateToCourses(1) },
                    onImportSchedule = { onNavigateToCourses(2) }
                )
            } else if (dayClasses.isEmpty()) {
                NoClassesForDayCard(
                    dayName = selectedDayName,
                    dateFormatted = selectedJalali.formatted,
                    weekNumber = currentWeekNumber,
                    parity = currentParity
                )
            } else if (isToday) {
                if (nextClass != null) {
                    val isHappeningNow = nextClass.startTime <= nowTimeStr && nextClass.endTime >= nowTimeStr
                    NextClassHeroCard(
                        scheduleClass = nextClass,
                        isHappeningNow = isHappeningNow,
                        onClick = { onOpenCourseDetail(nextClass) }
                    )
                } else {
                    NoMoreClassesHeroCard()
                }
            } else {
                DayOverviewHeroCard(
                    dayName = selectedDayName,
                    dateFormatted = selectedJalali.formatted,
                    classCount = dayClasses.size,
                    weekNumber = currentWeekNumber,
                    parity = currentParity
                )
            }
        }

        // 4. Classes Timeline
        if (dayClasses.isNotEmpty()) {
            item {
                SectionHeader(title = "کلاس‌های $selectedDayName ($currentParity)")
            }
            items(dayClasses, key = { it.id.ifBlank { it.semanticKey + it.startTime } }) { itemClass ->
                TodayClassRowCard(
                    scheduleClass = itemClass,
                    isCurrentOrNext = isToday && itemClass == nextClass,
                    onClick = { onOpenCourseDetail(itemClass) },
                    modifier = Modifier.animateItem()
                )
            }
        }

        // 5. Nearest Important Events
        if (upcomingEvents.isNotEmpty()) {
            item {
                SectionHeader(title = "نزدیک‌ترین رویدادها")
            }
            items(upcomingEvents, key = { it.id }) { event ->
                val badgeColor = Color(event.type.badgeColorHex)
                val countdown = DateTimeUtils.formatCountdown(event.timestamp)

                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.75f)),
                    modifier = Modifier.fillMaxWidth().animateItem()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = badgeColor.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.4f)),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Event,
                                    contentDescription = null,
                                    tint = badgeColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(Modifier.width(12.dp))

                        Column(Modifier.weight(1f)) {
                            Text(
                                text = event.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (event.courseName.isNotBlank()) {
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = event.courseName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (countdown.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = countdown,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 6. Incomplete Tasks / Deadlines
        if (urgentTasks.isNotEmpty()) {
            item {
                SectionHeader(title = "وظایف در انتظار انجام")
            }
            items(urgentTasks, key = { it.id }) { task ->
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
                    modifier = Modifier.fillMaxWidth().animateItem()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { onToggleTaskDone(task) }, modifier = Modifier.size(28.dp)) {
                            Icon(
                                Icons.Default.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = task.title,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            if (task.courseName.isNotBlank() || task.deadlineString.isNotBlank()) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (task.courseName.isNotBlank()) {
                                        Text(
                                            task.courseName,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (task.deadlineString.isNotBlank()) {
                                        Text(
                                            "مهلت: ${PersianTextNormalizer.toPersianDigits(task.deadlineString)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showWeekEditorDialog) {
        EducationalWeekEditDialog(
            currentWeekNumber = currentWeekNumber,
            onDismiss = { showWeekEditorDialog = false },
            onSave = { newNum ->
                onSetEducationalWeek(newNum)
                showWeekEditorDialog = false
            }
        )
    }

    if (showDatePickerDialog) {
        PersianDatePickerDialog(
            initialJdn = selectedJdn,
            onDismiss = { showDatePickerDialog = false },
            onDateSelected = { newJdn ->
                selectedJdn = newJdn
                showDatePickerDialog = false
            }
        )
    }
}

@Composable
private fun EducationalCalendarStrip(
    dayName: String,
    dateFormatted: String,
    weekNumber: Int,
    parity: String,
    isToday: Boolean,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onTodayClick: () -> Unit,
    onOpenDatePicker: () -> Unit,
    onOpenWeekEditor: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Next day button (in Persian RTL: ArrowBack points to the left / forward in reading order)
            IconButton(
                onClick = onNextDay,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "روز بعد",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Center Info Surface (Tapping opens week editor)
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                modifier = Modifier
                    .weight(1f, fill = false)
                    .clickable(onClick = onOpenWeekEditor)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "$dayName، $dateFormatted",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = " | ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = "هفته آموزشی ${PersianTextNormalizer.toPersianDigits(weekNumber.toString())}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = " | ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = parity,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (PersianCalendarHelper.isEvenWeek(weekNumber))
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.secondary
                    )
                }
            }

            // Previous day button (in Persian RTL: ArrowForward points to the right / backward)
            IconButton(
                onClick = onPreviousDay,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "روز قبل",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Calendar picker icon
            IconButton(
                onClick = onOpenDatePicker,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Default.CalendarMonth,
                    contentDescription = "انتخاب تاریخ مستقیم",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            // Quick Today jump button
            if (!isToday) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable(onClick = onTodayClick)
                ) {
                    Text(
                        text = "امروز",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun NoClassesForDayCard(
    dayName: String,
    dateFormatted: String,
    weekNumber: Int,
    parity: String
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.EventBusy,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                modifier = Modifier.size(40.dp)
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "در این روز کلاسی ندارید",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "برای $dayName، $dateFormatted (هفته آموزشی ${PersianTextNormalizer.toPersianDigits(weekNumber.toString())} · $parity) کلاسی در برنامهٔ شما ثبت نشده است.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun DayOverviewHeroCard(
    dayName: String,
    dateFormatted: String,
    classCount: Int,
    weekNumber: Int,
    parity: String
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "برنامهٔ $dayName",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "$dateFormatted · هفته $weekNumber ($parity)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
            ) {
                Text(
                    text = "${PersianTextNormalizer.toPersianDigits(classCount.toString())} کلاس",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun TodaySummaryPill(count: Int, label: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Text(
                text = PersianTextNormalizer.toPersianDigits(count.toString()),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(width = 3.dp, height = 15.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.85f), RoundedCornerShape(2.dp))
        )
        Spacer(Modifier.width(9.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun NextClassHeroCard(
    scheduleClass: ScheduleClass,
    isHappeningNow: Boolean,
    onClick: () -> Unit
) {
    val liveBorder by animateColorAsState(
        targetValue = if (isHappeningNow) MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
        else MaterialTheme.colorScheme.outline.copy(alpha = 0.8f),
        animationSpec = tween(AppMotion.DURATION_BASE, easing = AppMotion.EaseOut),
        label = "liveBorder"
    )

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        border = BorderStroke(1.dp, liveBorder),
        modifier = Modifier
            .fillMaxWidth()
            .tactileClick(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = MaterialTheme.shapes.extraSmall,
                    color = if (isHappeningNow) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    border = BorderStroke(
                        1.dp,
                        if (isHappeningNow) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                    )
                ) {
                    Text(
                        text = if (isHappeningNow) "هم‌اکنون در حال برگزاری" else "کلاس بعدی امروز",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.AccessTime,
                        null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "${PersianTextNormalizer.toPersianDigits(scheduleClass.startTime)} – ${PersianTextNormalizer.toPersianDigits(scheduleClass.endTime)}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            Text(
                text = scheduleClass.courseName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.MeetingRoom,
                        null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = scheduleClass.classroom.ifBlank { "مکان ثبت نشده" },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (scheduleClass.teacher.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Person,
                            null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = scheduleClass.teacher,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NoMoreClassesHeroCard() {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.78f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.CheckCircle,
                null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "برای امروز کلاس دیگری ندارید",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "همهٔ کلاس‌های امروز تمام شده‌اند؛ استراحت کنید یا به رویدادهای آینده برسید.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun EmptyCoursesHeroCard(
    hasCatalog: Boolean,
    catalogCount: Int,
    isUpdating: Boolean,
    onFetch: () -> Unit,
    onPickFromCatalog: () -> Unit,
    onImportSchedule: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.School,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(38.dp)
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "هنوز درسی به برنامهٔ شما اضافه نشده",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (hasCatalog) {
                    "فهرست ${PersianTextNormalizer.toPersianDigits(catalogCount.toString())} ارائه کلاسی آماده است. درس‌های خود را انتخاب کنید."
                } else {
                    "برای مشاهده برنامه، ابتدا فایل برنامه دانشگاه را دریافت کنید یا فایل خود را وارد نمایید."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(16.dp))

            if (hasCatalog) {
                Button(
                    onClick = onPickFromCatalog,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("انتخاب از درس‌های دانشگاه")
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onFetch,
                        modifier = Modifier.weight(1f),
                        enabled = !isUpdating,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isUpdating) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        else Icon(Icons.Default.CloudDownload, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("دریافت داده‌ها")
                    }
                    OutlinedButton(
                        onClick = onImportSchedule,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("افزودن برنامه")
                    }
                }
            }
        }
    }
}

@Composable
private fun TodayClassRowCard(
    scheduleClass: ScheduleClass,
    isCurrentOrNext: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rowBorder by animateColorAsState(
        targetValue = if (isCurrentOrNext) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        else MaterialTheme.colorScheme.outline.copy(alpha = 0.75f),
        animationSpec = tween(AppMotion.DURATION_BASE, easing = AppMotion.EaseOut),
        label = "rowBorder"
    )

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 0.dp,
        border = BorderStroke(1.dp, rowBorder),
        modifier = modifier
            .fillMaxWidth()
            .tactileClick(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = scheduleClass.courseName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    // Parity badge
                    val parityText = when {
                        scheduleClass.parity.contains("زوج") -> "هفته زوج"
                        scheduleClass.parity.contains("فرد") -> "هفته فرد"
                        else -> "تمام هفته‌ها"
                    }
                    val isEven = scheduleClass.parity.contains("زوج")
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = when {
                            isEven -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            scheduleClass.parity.contains("فرد") -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    ) {
                        Text(
                            text = parityText,
                            style = MaterialTheme.typography.labelSmall,
                            color = when {
                                isEven -> MaterialTheme.colorScheme.primary
                                scheduleClass.parity.contains("فرد") -> MaterialTheme.colorScheme.secondary
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.MeetingRoom,
                            null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = scheduleClass.classroom.ifBlank { "مکان مشخص نشده" },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (scheduleClass.teacher.isNotBlank()) {
                        Text(
                            text = scheduleClass.teacher,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
            ) {
                Text(
                    text = "${PersianTextNormalizer.toPersianDigits(scheduleClass.startTime)} – ${PersianTextNormalizer.toPersianDigits(scheduleClass.endTime)}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}
