package com.example.ui.upcoming

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.domain.model.CourseEvent
import com.example.domain.model.CourseEventType
import com.example.domain.model.ScheduleClass
import com.example.domain.normalizer.PersianTextNormalizer
import com.example.ui.course.EventEditDialog
import com.example.ui.util.DateTimeUtils

@Composable
fun UpcomingScreen(
    events: List<CourseEvent>,
    myClasses: List<ScheduleClass>,
    onAddOrUpdateEvent: (CourseEvent) -> Unit,
    onDeleteEvent: (String) -> Unit,
    onToggleEventCompleted: (CourseEvent) -> Unit,
    onOpenCourseDetail: (ScheduleClass) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTypeFilter by remember { mutableStateOf<CourseEventType?>(null) } // null = All
    var showCompleted by remember { mutableStateOf(false) }
    var isAddingExamOrEvent by remember { mutableStateOf(false) }
    var defaultTypeForAdd by remember { mutableStateOf(CourseEventType.EXAM) }

    val filteredEvents = remember(events, selectedTypeFilter, showCompleted) {
        events
            .filter { (selectedTypeFilter == null || it.type == selectedTypeFilter) }
            .filter { showCompleted || !it.isCompleted }
            .sortedWith(
                compareBy(
                    { it.isCompleted },
                    { if (it.timestamp > 0) it.timestamp else Long.MAX_VALUE },
                    { it.dateString }
                )
            )
    }

    if (isAddingExamOrEvent) {
        val firstCourse = myClasses.firstOrNull()
        EventEditDialog(
            initialEvent = CourseEvent(
                courseKey = firstCourse?.semanticKey ?: "",
                courseName = firstCourse?.courseName ?: "",
                courseCode = firstCourse?.courseCode ?: "",
                type = defaultTypeForAdd,
                title = ""
            ),
            onDismiss = { isAddingExamOrEvent = false },
            onSave = { savedEvent ->
                onAddOrUpdateEvent(savedEvent)
                isAddingExamOrEvent = false
            }
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 18.dp,
            end = 18.dp,
            top = 14.dp,
            bottom = 100.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Top Title & Quick Add
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "رویدادها و امتحانات پیش‌رو",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "${PersianTextNormalizer.toPersianDigits(filteredEvents.size.toString())} مورد در این بخش",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = {
                        defaultTypeForAdd = if (selectedTypeFilter == CourseEventType.EXAM) CourseEventType.EXAM else CourseEventType.MIDTERM
                        isAddingExamOrEvent = true
                    },
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("ثبت رویداد / امتحان")
                }
            }
        }

        // 2. Filter chips
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = selectedTypeFilter == null,
                        onClick = { selectedTypeFilter = null },
                        label = { Text("همه") },
                        shape = RoundedCornerShape(10.dp)
                    )
                }
                item {
                    FilterChip(
                        selected = selectedTypeFilter == CourseEventType.EXAM,
                        onClick = { selectedTypeFilter = if (selectedTypeFilter == CourseEventType.EXAM) null else CourseEventType.EXAM },
                        label = { Text("امتحانات پایان‌ترم") },
                        shape = RoundedCornerShape(10.dp)
                    )
                }
                item {
                    FilterChip(
                        selected = selectedTypeFilter == CourseEventType.MIDTERM,
                        onClick = { selectedTypeFilter = if (selectedTypeFilter == CourseEventType.MIDTERM) null else CourseEventType.MIDTERM },
                        label = { Text("میان‌ترم‌ها") },
                        shape = RoundedCornerShape(10.dp)
                    )
                }
                item {
                    FilterChip(
                        selected = selectedTypeFilter == CourseEventType.PRESENTATION,
                        onClick = { selectedTypeFilter = if (selectedTypeFilter == CourseEventType.PRESENTATION) null else CourseEventType.PRESENTATION },
                        label = { Text("ارائه‌ها") },
                        shape = RoundedCornerShape(10.dp)
                    )
                }
                item {
                    FilterChip(
                        selected = selectedTypeFilter == CourseEventType.ASSIGNMENT,
                        onClick = { selectedTypeFilter = if (selectedTypeFilter == CourseEventType.ASSIGNMENT) null else CourseEventType.ASSIGNMENT },
                        label = { Text("تمرین‌ها / پروژه‌ها") },
                        shape = RoundedCornerShape(10.dp)
                    )
                }
                item {
                    FilterChip(
                        selected = selectedTypeFilter == CourseEventType.QUIZ,
                        onClick = { selectedTypeFilter = if (selectedTypeFilter == CourseEventType.QUIZ) null else CourseEventType.QUIZ },
                        label = { Text("کوئیزها") },
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }
        }

        // 3. Events List
        if (filteredEvents.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Event,
                        null,
                        modifier = Modifier.size(44.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "هیچ رویداد یا امتحانی در این دسته یافت نشد.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { isAddingExamOrEvent = true },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("ثبت رویداد جدید")
                    }
                }
            }
        } else {
            items(filteredEvents, key = { it.id }) { event ->
                val badgeColor = Color(event.type.badgeColorHex)
                val countdown = DateTimeUtils.formatCountdown(event.timestamp)

                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = if (event.isCompleted) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        else MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (event.isCompleted) MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.75f)
                    ),
                    modifier = Modifier.fillMaxWidth().animateItem()
                ) {
                    Column(Modifier.fillMaxWidth().padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = MaterialTheme.shapes.extraSmall,
                                    color = badgeColor.copy(alpha = 0.16f),
                                    border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.35f))
                                ) {
                                    Text(
                                        text = event.type.titleFa,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = badgeColor,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }

                                if (countdown.isNotBlank()) {
                                    Spacer(Modifier.width(8.dp))
                                    Surface(
                                        shape = MaterialTheme.shapes.extraSmall,
                                        color = MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Text(
                                            text = countdown,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }

                            Row {
                                IconButton(
                                    onClick = { onToggleEventCompleted(event) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        if (event.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                        null,
                                        tint = if (event.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        // Title & Course
                        Text(
                            text = event.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textDecoration = if (event.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                            color = if (event.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                        )

                        if (event.courseName.isNotBlank()) {
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "درس: ${event.courseName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // Date, time, location
                        if (event.dateString.isNotBlank() || event.timeString.isNotBlank() || event.location.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (event.dateString.isNotBlank() || event.timeString.isNotBlank()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Schedule,
                                            null,
                                            modifier = Modifier.size(14.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            "${PersianTextNormalizer.toPersianDigits(event.dateString)}  ${PersianTextNormalizer.toPersianDigits(event.timeString)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                if (event.location.isNotBlank()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.LocationOn,
                                            null,
                                            modifier = Modifier.size(14.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            event.location,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        if (event.reminderMinutesBefore >= 0) {
                            Spacer(Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Alarm,
                                    null,
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "یادآور فعال",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
