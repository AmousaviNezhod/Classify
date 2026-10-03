package com.example.ui.course

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
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.CourseEvent
import com.example.domain.model.CourseEventType
import com.example.domain.model.CourseNote
import com.example.domain.model.CourseTask
import com.example.domain.model.ScheduleClass
import com.example.domain.normalizer.PersianTextNormalizer
import com.example.ui.components.SegmentedTabs
import com.example.ui.theme.tactileClick
import com.example.ui.util.DateTimeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseDetailScreen(
    course: ScheduleClass,
    allSessions: List<ScheduleClass>,
    events: List<CourseEvent>,
    tasks: List<CourseTask>,
    notes: List<CourseNote>,
    onBack: () -> Unit,
    onDeleteCourse: () -> Unit,
    onAddOrUpdateEvent: (CourseEvent) -> Unit,
    onDeleteEvent: (String) -> Unit,
    onToggleEventCompleted: (CourseEvent) -> Unit,
    onAddOrUpdateTask: (CourseTask) -> Unit,
    onDeleteTask: (String) -> Unit,
    onToggleTaskDone: (CourseTask) -> Unit,
    onAddOrUpdateNote: (CourseNote) -> Unit,
    onDeleteNote: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var eventToEdit by remember { mutableStateOf<CourseEvent?>(null) }
    var isAddingEvent by remember { mutableStateOf(false) }
    var isAddingNote by remember { mutableStateOf(false) }
    var noteToEdit by remember { mutableStateOf<CourseNote?>(null) }
    var isAddingTask by remember { mutableStateOf(false) }

    // Course delete confirmation dialog (with cascade deletion warning)
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("حذف درس «${course.courseName}»") },
            text = {
                Text(
                    "آیا از حذف این درس مطمئن هستید؟ با حذف درس، تمام رویدادها، امتحانات، وظایف و یادداشت‌های وابسته به آن نیز به‌طور کامل حذف خواهند شد.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDeleteCourse()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("حذف درس و داده‌های وابسته")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) { Text("انصراف") }
            }
        )
    }

    // Add / Edit Event Dialog
    if (isAddingEvent || eventToEdit != null) {
        EventEditDialog(
            initialEvent = eventToEdit ?: CourseEvent(
                courseKey = course.semanticKey,
                courseName = course.courseName,
                courseCode = course.courseCode,
                title = ""
            ),
            onDismiss = {
                isAddingEvent = false
                eventToEdit = null
            },
            onSave = { savedEvent ->
                onAddOrUpdateEvent(savedEvent)
                isAddingEvent = false
                eventToEdit = null
            }
        )
    }

    // Add / Edit Note Dialog
    if (isAddingNote || noteToEdit != null) {
        NoteEditDialog(
            initialNote = noteToEdit ?: CourseNote(
                courseKey = course.semanticKey,
                courseName = course.courseName,
                content = ""
            ),
            onDismiss = {
                isAddingNote = false
                noteToEdit = null
            },
            onSave = { savedNote ->
                onAddOrUpdateNote(savedNote)
                isAddingNote = false
                noteToEdit = null
            }
        )
    }

    // Add Task Dialog
    if (isAddingTask) {
        TaskEditDialog(
            courseKey = course.semanticKey,
            courseName = course.courseName,
            onDismiss = { isAddingTask = false },
            onSave = { newTask ->
                onAddOrUpdateTask(newTask)
                isAddingTask = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        course.courseName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteConfirmDialog = true }) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = "حذف درس",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.76f)
                )
            )
        },
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Header card with course info & sessions
            CourseHeaderCard(
                course = course,
                sessions = allSessions.ifEmpty { listOf(course) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Tabs: Events, Tasks, Notes. Same segmented control as the Courses
            // screen so in-screen navigation has one consistent behaviour.
            SegmentedTabs(
                labels = listOf(
                    "رویدادها ${PersianTextNormalizer.toPersianDigits(events.size.toString())}",
                    "وظایف ${PersianTextNormalizer.toPersianDigits(tasks.size.toString())}",
                    "یادداشت‌ها ${PersianTextNormalizer.toPersianDigits(notes.size.toString())}"
                ),
                selectedIndex = selectedTab,
                onSelect = { selectedTab = it },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                testTagPrefix = "course_detail_tab"
            )

            Spacer(Modifier.height(8.dp))

            when (selectedTab) {
                0 -> EventsTabContent(
                    events = events,
                    onAdd = { isAddingEvent = true },
                    onEdit = { eventToEdit = it },
                    onDelete = onDeleteEvent,
                    onToggleComplete = onToggleEventCompleted
                )
                1 -> TasksTabContent(
                    tasks = tasks,
                    onAdd = { isAddingTask = true },
                    onToggleDone = onToggleTaskDone,
                    onDelete = onDeleteTask
                )
                else -> NotesTabContent(
                    notes = notes,
                    onAdd = { isAddingNote = true },
                    onEdit = { noteToEdit = it },
                    onDelete = onDeleteNote
                )
            }
        }
    }
}

@Composable
private fun CourseHeaderCard(
    course: ScheduleClass,
    sessions: List<ScheduleClass>,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.75f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            // Badges row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (course.courseCode.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = "کد ${PersianTextNormalizer.toPersianDigits(course.courseCode)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                if (course.groupCode.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "گروه ${PersianTextNormalizer.toPersianDigits(course.groupCode)}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                if (course.units > 0) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = "${PersianTextNormalizer.toPersianDigits(course.units.toString())} واحد",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                if (course.parity.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = course.parity,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            if (course.teacher.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "استاد: ${course.teacher}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(
                "جلسات کلاس در هفته:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))

            sessions.forEach { session ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            session.dayOfWeek,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "${PersianTextNormalizer.toPersianDigits(session.startTime)} – ${PersianTextNormalizer.toPersianDigits(session.endTime)}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.MeetingRoom,
                                null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                session.classroom.ifBlank { "مکان مشخص نشده" },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EventsTabContent(
    events: List<CourseEvent>,
    onAdd: () -> Unit,
    onEdit: (CourseEvent) -> Unit,
    onDelete: (String) -> Unit,
    onToggleComplete: (CourseEvent) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Button(
                onClick = onAdd,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(6.dp))
                Text("افزودن رویداد (امتحان، ارائه، تمرین...)")
            }
        }

        if (events.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Event,
                        null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "هنوز رویدادی برای این درس ثبت نشده است.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(events, key = { it.id }, contentType = { "event" }) { event ->
                EventCard(
                    modifier = Modifier.animateItem(),
                    event = event,
                    onEdit = { onEdit(event) },
                    onDelete = { onDelete(event.id) },
                    onToggleComplete = { onToggleComplete(event) }
                )
            }
        }
    }
}

@Composable
fun EventCard(
    event: CourseEvent,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val badgeColor = Color(event.type.badgeColorHex)
    val countdownText = DateTimeUtils.formatCountdown(event.timestamp)

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
        modifier = modifier.fillMaxWidth()
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            // Header: Type badge, title, countdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = badgeColor.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.35f))
                    ) {
                        Text(
                            text = event.type.titleFa,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }

                    if (countdownText.isNotBlank()) {
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = countdownText,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Row {
                    IconButton(onClick = onToggleComplete, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = if (event.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = "انجام شد",
                            tint = if (event.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = "حذف",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Title
            Text(
                text = event.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                textDecoration = if (event.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                color = if (event.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
            )

            // Date & Time
            if (event.dateString.isNotBlank() || event.timeString.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Schedule,
                        null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "${PersianTextNormalizer.toPersianDigits(event.dateString)}   ${PersianTextNormalizer.toPersianDigits(event.timeString)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Location
            if (event.location.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
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

            // Description
            if (event.description.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    event.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Reminder status
            if (event.reminderMinutesBefore >= 0) {
                Spacer(Modifier.height(8.dp))
                val reminderLabel = when (event.reminderMinutesBefore) {
                    0 -> "یادآور: همزمان با شروع"
                    1440 -> "یادآور: ۱ روز قبل"
                    2880 -> "یادآور: ۲ روز قبل"
                    4320 -> "یادآور: ۳ روز قبل"
                    10080 -> "یادآور: ۱ هفته قبل"
                    else -> "یادآور: ${PersianTextNormalizer.toPersianDigits((event.reminderMinutesBefore / 60).toString())} ساعت قبل"
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Alarm,
                        null,
                        modifier = Modifier.size(13.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        reminderLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun TasksTabContent(
    tasks: List<CourseTask>,
    onAdd: () -> Unit,
    onToggleDone: (CourseTask) -> Unit,
    onDelete: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Button(
                onClick = onAdd,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(6.dp))
                Text("افزودن وظیفه / تکلیف جدید")
            }
        }

        if (tasks.isEmpty()) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.outline)
                    Spacer(Modifier.height(8.dp))
                    Text("هیچ وظیفه‌ای ثبت نشده است.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            items(tasks, key = { it.id }, contentType = { "task" }) { task ->
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
                    modifier = Modifier.fillMaxWidth().animateItem()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { onToggleDone(task) }, modifier = Modifier.size(28.dp)) {
                            Icon(
                                if (task.isDone) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = if (task.isDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                task.title,
                                style = MaterialTheme.typography.bodyMedium,
                                textDecoration = if (task.isDone) TextDecoration.LineThrough else TextDecoration.None,
                                color = if (task.isDone) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                            )
                            if (task.deadlineString.isNotBlank()) {
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    "مهلت: ${PersianTextNormalizer.toPersianDigits(task.deadlineString)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                        IconButton(onClick = { onDelete(task.id) }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.DeleteOutline, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotesTabContent(
    notes: List<CourseNote>,
    onAdd: () -> Unit,
    onEdit: (CourseNote) -> Unit,
    onDelete: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Button(
                onClick = onAdd,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(6.dp))
                Text("افزودن یادداشت جدید")
            }
        }

        if (notes.isEmpty()) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.AutoMirrored.Filled.Notes, null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.outline)
                    Spacer(Modifier.height(8.dp))
                    Text("هیچ یادداشتی برای این درس نوشته نشده است.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            items(notes, key = { it.id }, contentType = { "note" }) { note ->
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateItem()
                        .tactileClick { onEdit(note) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            note.content,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { onDelete(note.id) }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.DeleteOutline, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventEditDialog(
    initialEvent: CourseEvent,
    onDismiss: () -> Unit,
    onSave: (CourseEvent) -> Unit
) {
    var title by remember { mutableStateOf(initialEvent.title) }
    var selectedType by remember { mutableStateOf(initialEvent.type) }
    var dateString by remember { mutableStateOf(initialEvent.dateString) }
    var timeString by remember { mutableStateOf(initialEvent.timeString) }
    var location by remember { mutableStateOf(initialEvent.location) }
    var description by remember { mutableStateOf(initialEvent.description) }
    var reminderMinutes by remember { mutableIntStateOf(initialEvent.reminderMinutesBefore) }
    var typeDropdownExpanded by remember { mutableStateOf(false) }
    var reminderDropdownExpanded by remember { mutableStateOf(false) }

    val reminderOptions = listOf(
        -1 to "بدون یادآور",
        0 to "همزمان با شروع",
        1440 to "۱ روز قبل",
        2880 to "۲ روز قبل",
        4320 to "۳ روز قبل",
        10080 to "۱ هفته قبل (۷ روز)"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialEvent.title.isBlank()) "رویداد جدید" else "ویرایش رویداد") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("عنوان رویداد *") },
                    placeholder = { Text("مثلاً میان‌ترم، تمرین فصل ۴") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Event Type Dropdown
                ExposedDropdownMenuBox(
                    expanded = typeDropdownExpanded,
                    onExpandedChange = { typeDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedType.titleFa,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("نوع رویداد") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeDropdownExpanded) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = typeDropdownExpanded,
                        onDismissRequest = { typeDropdownExpanded = false }
                    ) {
                        CourseEventType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.titleFa) },
                                onClick = {
                                    selectedType = type
                                    typeDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = dateString,
                        onValueChange = { dateString = it },
                        label = { Text("تاریخ") },
                        placeholder = { Text("1403/10/25") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = timeString,
                        onValueChange = { timeString = it },
                        label = { Text("ساعت") },
                        placeholder = { Text("10:30") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("مکان برگزاری") },
                    placeholder = { Text("کلاس ۱۰۲ یا آنلاین") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Reminder Dropdown
                ExposedDropdownMenuBox(
                    expanded = reminderDropdownExpanded,
                    onExpandedChange = { reminderDropdownExpanded = it }
                ) {
                    val currentReminderText = reminderOptions.firstOrNull { it.first == reminderMinutes }?.second ?: "بدون یادآور"
                    OutlinedTextField(
                        value = currentReminderText,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("ارسال یادآور (Notification)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = reminderDropdownExpanded) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = reminderDropdownExpanded,
                        onDismissRequest = { reminderDropdownExpanded = false }
                    ) {
                        reminderOptions.forEach { (mins, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    reminderMinutes = mins
                                    reminderDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("توضیحات (اختیاری)") },
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val computedTimestamp = DateTimeUtils.parseDateToTimestamp(dateString, timeString)
                        onSave(
                            initialEvent.copy(
                                title = title.trim(),
                                type = selectedType,
                                dateString = dateString.trim(),
                                timeString = timeString.trim(),
                                timestamp = computedTimestamp,
                                location = location.trim(),
                                reminderMinutesBefore = reminderMinutes,
                                description = description.trim()
                            )
                        )
                    }
                },
                enabled = title.isNotBlank()
            ) {
                Text("ذخیره")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        }
    )
}

@Composable
fun TaskEditDialog(
    courseKey: String,
    courseName: String,
    onDismiss: () -> Unit,
    onSave: (CourseTask) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var deadline by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("وظیفه جدید") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("عنوان وظیفه *") },
                    placeholder = { Text("تمرین فصل ۴، مطالعه خلاصه") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = deadline,
                    onValueChange = { deadline = it },
                    label = { Text("مهلت (اختیاری)") },
                    placeholder = { Text("مثلاً تا یکشنبه ساعت ۱۲") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onSave(
                            CourseTask(
                                courseKey = courseKey,
                                courseName = courseName,
                                title = title.trim(),
                                deadlineString = deadline.trim()
                            )
                        )
                    }
                },
                enabled = title.isNotBlank()
            ) {
                Text("افزودن")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        }
    )
}

@Composable
fun NoteEditDialog(
    initialNote: CourseNote,
    onDismiss: () -> Unit,
    onSave: (CourseNote) -> Unit
) {
    var content by remember { mutableStateOf(initialNote.content) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialNote.content.isBlank()) "یادداشت جدید" else "ویرایش یادداشت") },
        text = {
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                placeholder = { Text("نکات مهم کلاس، مباحث امتحان، گفته‌های استاد...") },
                minLines = 4,
                maxLines = 8,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (content.isNotBlank()) {
                        onSave(initialNote.copy(content = content.trim(), updatedAt = System.currentTimeMillis()))
                    }
                },
                enabled = content.isNotBlank()
            ) {
                Text("ذخیره")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        }
    )
}
