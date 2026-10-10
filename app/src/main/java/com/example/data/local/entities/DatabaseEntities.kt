package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "schedules")
data class ScheduleEntity(
    @PrimaryKey
    val id: String = "university_schedule",
    val title: String,
    val term: String,
    val sourceUrl: String,
    val updatedAt: Long,
    val totalCourses: Int,
    val rawJson: String = ""
)

@Entity(tableName = "schedule_classes")
data class ScheduleClassEntity(
    @PrimaryKey
    val id: String,
    val scheduleId: String,
    val courseName: String,
    val courseCode: String = "",
    val teacher: String = "",
    val dayOfWeek: String,
    val dayIndex: Int,
    val startTime: String,
    val endTime: String,
    val classroom: String = "",
    val groupCode: String = "",
    val parity: String = "",
    val units: Int = 0,
    val notes: String = ""
) {
    fun toDomain(): com.example.domain.model.ScheduleClass = com.example.domain.model.ScheduleClass(
        id = id,
        scheduleId = scheduleId,
        courseName = courseName,
        courseCode = courseCode,
        teacher = teacher,
        dayOfWeek = dayOfWeek,
        dayIndex = dayIndex,
        startTime = startTime,
        endTime = endTime,
        classroom = classroom,
        groupCode = groupCode,
        parity = parity,
        units = units,
        notes = notes
    )
}

fun com.example.domain.model.ScheduleClass.toEntity(scheduleId: String = this.scheduleId): ScheduleClassEntity = ScheduleClassEntity(
    id = id.ifBlank { "${scheduleId}_${java.util.UUID.randomUUID().toString().take(8)}" },
    scheduleId = scheduleId,
    courseName = courseName,
    courseCode = courseCode,
    teacher = teacher,
    dayOfWeek = dayOfWeek,
    dayIndex = dayIndex,
    startTime = startTime,
    endTime = endTime,
    classroom = classroom,
    groupCode = groupCode,
    parity = parity,
    units = units,
    notes = notes
)

@Entity(tableName = "downloaded_pdfs")
data class DownloadedPdfEntity(
    @PrimaryKey
    val id: String,
    val url: String,
    val fileName: String,
    val localFilePath: String,
    val fileSize: Long,
    val etag: String? = null,
    val lastModified: String? = null,
    val sha256Hash: String? = null,
    val downloadTime: Long = System.currentTimeMillis(),
    val dayIndex: Int = -1,
    val parseStatus: String = "SUCCESS", // SUCCESS, FAILED, PENDING, EMPTY
    val parseError: String? = null,
    val extractedClassCount: Int = 0,
    val extractedWorkshopCount: Int = 0,
    val weekNumber: Int? = null,
    val weekParity: String = "",
    val lastExtractedTime: Long = 0L
)

@Entity(tableName = "schedule_day_availability")
data class ScheduleDayAvailabilityEntity(
    @PrimaryKey val dayIndex: Int,
    val dayName: String,
    val status: String,
    val discoveredPdfCount: Int = 0,
    val normalClassCount: Int = 0,
    val workshopClassCount: Int = 0,
    val sourceFileName: String = "",
    val sourceUrl: String = "",
    val checkedAt: Long = 0L,
    val weekNumber: Int? = null,
    val weekParity: String = ""
)

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey
    val key: String,
    val value: String
)

@Entity(
    tableName = "course_events",
    indices = [androidx.room.Index(value = ["courseKey"])]
)
data class CourseEventEntity(
    @PrimaryKey
    val id: String,
    val courseKey: String,
    val courseName: String,
    val courseCode: String = "",
    val title: String,
    val type: String, // EXAM, MIDTERM, PRESENTATION, ASSIGNMENT, QUIZ, OTHER
    val dateString: String, // e.g. "1403/10/25" or "2025-05-15"
    val timeString: String = "", // e.g. "10:30"
    val timestamp: Long = 0L, // epoch millis for sorting, alarms, countdown
    val description: String = "",
    val location: String = "",
    val reminderMinutesBefore: Int = -1, // -1: none, 0: at event time, 1440: 1 day, 2880: 2 days, 4320: 3 days, 10080: 7 days
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "course_tasks",
    indices = [androidx.room.Index(value = ["courseKey"])]
)
data class CourseTaskEntity(
    @PrimaryKey
    val id: String,
    val courseKey: String,
    val courseName: String,
    val title: String,
    val isDone: Boolean = false,
    val deadlineString: String = "",
    val deadlineTimestamp: Long = 0L,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "course_notes",
    indices = [androidx.room.Index(value = ["courseKey"])]
)
data class CourseNoteEntity(
    @PrimaryKey
    val id: String,
    val courseKey: String,
    val courseName: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
