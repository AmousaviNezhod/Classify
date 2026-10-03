package com.example.ui.calendar

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.domain.model.CourseEvent
import com.example.domain.model.CourseEventType
import com.example.domain.model.ScheduleClass
import com.example.domain.normalizer.PersianTextNormalizer
import com.example.ui.theme.tactileClick
import com.example.ui.course.EventCard
import com.example.ui.util.DateTimeUtils

@Composable
fun CalendarScreen(
    myClasses: List<ScheduleClass>,
    events: List<CourseEvent>,
    onOpenCourseDetail: (ScheduleClass) -> Unit,
    onToggleEventCompleted: (CourseEvent) -> Unit,
    onDeleteEvent: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val todayIndex = remember { DateTimeUtils.getTodayDayIndex() }
    var selectedDayIndex by remember { mutableIntStateOf(todayIndex) }

    val dayNames = listOf(
        0 to "شنبه",
        1 to "یکشنبه",
        2 to "دوشنبه",
        3 to "سه‌شنبه",
        4 to "چهارشنبه",
        5 to "پنج‌شنبه",
        6 to "جمعه"
    )

    val classesByDay = remember(myClasses) {
        myClasses.groupBy { it.dayIndex }.mapValues { (_, classes) -> classes.sortedBy { it.startTime } }
    }
    val classesForSelectedDay = classesByDay[selectedDayIndex].orEmpty()
    val courseKeysByDay = remember(classesByDay) {
        classesByDay.mapValues { (_, classes) -> classes.mapTo(HashSet()) { it.semanticKey } }
    }
    val classCountsByDay = remember(classesByDay) { classesByDay.mapValues { it.value.size } }

    // Events matching this day (or weekly classes/events)
    val eventsForSelectedDay = remember(events, selectedDayIndex, classesForSelectedDay) {
        val dayName = dayNames.firstOrNull { it.first == selectedDayIndex }?.second.orEmpty()
        val courseKeys = courseKeysByDay[selectedDayIndex].orEmpty()
        events.filter { event ->
            event.dateString.contains(dayName) || event.courseKey in courseKeys
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 18.dp,
            end = 18.dp,
            top = 14.dp,
            bottom = 100.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Header & Today shortcut
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "تقویم برنامهٔ ترم",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "مشاهدهٔ کلاس‌ها و رویدادها بر اساس روزهای هفته",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (selectedDayIndex != todayIndex) {
                    OutlinedButton(
                        onClick = { selectedDayIndex = todayIndex },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.CalendarToday, null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("امروز")
                    }
                }
            }
        }

        // 2. Day selector strip
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(dayNames) { (dayIdx, dayName) ->
                    val isSelected = dayIdx == selectedDayIndex
                    val classesCount = classCountsByDay[dayIdx] ?: 0
                    val dayCourseKeys = courseKeysByDay[dayIdx].orEmpty()
                    val hasEvents = remember(events, dayName, dayCourseKeys) {
                        events.any { event ->
                            event.dateString.contains(dayName) || event.courseKey in dayCourseKeys
                        }
                    }

                    Card(
                        onClick = { selectedDayIndex = dayIdx },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
                            else MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.75f)
                        ),
                        modifier = Modifier.width(88.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp, horizontal = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = dayName,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(Modifier.height(6.dp))

                            Text(
                                text = if (classesCount > 0) "${PersianTextNormalizer.toPersianDigits(classesCount.toString())} کلاس" else "بدون کلاس",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(Modifier.height(6.dp))

                            // Indicator dots
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (classesCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .size(5.dp)
                                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                                    )
                                }
                                if (hasEvents) {
                                    Box(
                                        modifier = Modifier
                                            .size(5.dp)
                                            .background(MaterialTheme.colorScheme.error, CircleShape)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Section Title for Classes
        item {
            val selectedDayName = dayNames.firstOrNull { it.first == selectedDayIndex }?.second ?: ""
            Text(
                "کلاس‌های $selectedDayName (${PersianTextNormalizer.toPersianDigits(classesForSelectedDay.size.toString())})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 6.dp)
            )
        }

        if (classesForSelectedDay.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.75f)),
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
                            null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "در این روز هیچ کلاسی برای شما ثبت نشده است.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(classesForSelectedDay, key = { it.id.ifBlank { it.semanticKey + it.startTime } }) { session ->
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.75f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateItem()
                        .tactileClick { onOpenCourseDetail(session) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = session.courseName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
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
                                        text = session.classroom.ifBlank { "مکان مشخص نشده" },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (session.teacher.isNotBlank()) {
                                    Text(
                                        text = session.teacher,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        ) {
                            Text(
                                text = "${PersianTextNormalizer.toPersianDigits(session.startTime)} – ${PersianTextNormalizer.toPersianDigits(session.endTime)}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // 4. Events for selected day / courses
        if (eventsForSelectedDay.isNotEmpty()) {
            item {
                Text(
                    "رویدادها و تکالیف مرتبط (${PersianTextNormalizer.toPersianDigits(eventsForSelectedDay.size.toString())})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }
            items(eventsForSelectedDay, key = { it.id }) { event ->
                EventCard(
                    event = event,
                    onEdit = {},
                    onDelete = { onDeleteEvent(event.id) },
                    onToggleComplete = { onToggleEventCompleted(event) }
                )
            }
        }
    }
}
