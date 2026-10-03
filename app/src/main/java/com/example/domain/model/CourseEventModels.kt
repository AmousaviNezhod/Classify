package com.example.domain.model

import com.example.data.local.entities.CourseEventEntity
import com.example.data.local.entities.CourseNoteEntity
import com.example.data.local.entities.CourseTaskEntity
import com.squareup.moshi.JsonClass
import java.util.UUID

// Badge hues are desaturated to sit inside the graphite palette; each type still
// needs its own hue to stay scannable. Keep in sync with ui/theme/Color.kt.
enum class CourseEventType(val titleFa: String, val badgeColorHex: Long) {
    EXAM("امتحان", 0xFFC97A7E),
    MIDTERM("میان‌ترم", 0xFFC79A6A),
    PRESENTATION("ارائه", 0xFF9A8CC4),
    ASSIGNMENT("تمرین / تحویل", 0xFF7A9BC9),
    QUIZ("کوئیز", 0xFF6FB2AC),
    OTHER("سایر", 0xFF9BA4AF);

    companion object {
        fun fromString(value: String): CourseEventType {
            return entries.firstOrNull { it.name.equals(value.trim(), ignoreCase = true) } ?: OTHER
        }
    }
}

@JsonClass(generateAdapter = true)
data class CourseEvent(
    val id: String = UUID.randomUUID().toString(),
    val courseKey: String = "",
    val courseName: String = "",
    val courseCode: String = "",
    val title: String,
    val type: CourseEventType = CourseEventType.OTHER,
    val dateString: String = "",
    val timeString: String = "",
    val timestamp: Long = 0L,
    val description: String = "",
    val location: String = "",
    val reminderMinutesBefore: Int = -1,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toEntity(): CourseEventEntity = CourseEventEntity(
        id = id,
        courseKey = courseKey,
        courseName = courseName,
        courseCode = courseCode,
        title = title,
        type = type.name,
        dateString = dateString,
        timeString = timeString,
        timestamp = timestamp,
        description = description,
        location = location,
        reminderMinutesBefore = reminderMinutesBefore,
        isCompleted = isCompleted,
        createdAt = createdAt
    )

    companion object {
        fun fromEntity(entity: CourseEventEntity): CourseEvent = CourseEvent(
            id = entity.id,
            courseKey = entity.courseKey,
            courseName = entity.courseName,
            courseCode = entity.courseCode,
            title = entity.title,
            type = CourseEventType.fromString(entity.type),
            dateString = entity.dateString,
            timeString = entity.timeString,
            timestamp = entity.timestamp,
            description = entity.description,
            location = entity.location,
            reminderMinutesBefore = entity.reminderMinutesBefore,
            isCompleted = entity.isCompleted,
            createdAt = entity.createdAt
        )
    }
}

@JsonClass(generateAdapter = true)
data class CourseTask(
    val id: String = UUID.randomUUID().toString(),
    val courseKey: String = "",
    val courseName: String = "",
    val title: String,
    val isDone: Boolean = false,
    val deadlineString: String = "",
    val deadlineTimestamp: Long = 0L,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toEntity(): CourseTaskEntity = CourseTaskEntity(
        id = id,
        courseKey = courseKey,
        courseName = courseName,
        title = title,
        isDone = isDone,
        deadlineString = deadlineString,
        deadlineTimestamp = deadlineTimestamp,
        createdAt = createdAt
    )

    companion object {
        fun fromEntity(entity: CourseTaskEntity): CourseTask = CourseTask(
            id = entity.id,
            courseKey = entity.courseKey,
            courseName = entity.courseName,
            title = entity.title,
            isDone = entity.isDone,
            deadlineString = entity.deadlineString,
            deadlineTimestamp = entity.deadlineTimestamp,
            createdAt = entity.createdAt
        )
    }
}

@JsonClass(generateAdapter = true)
data class CourseNote(
    val id: String = UUID.randomUUID().toString(),
    val courseKey: String = "",
    val courseName: String = "",
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toEntity(): CourseNoteEntity = CourseNoteEntity(
        id = id,
        courseKey = courseKey,
        courseName = courseName,
        content = content,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    companion object {
        fun fromEntity(entity: CourseNoteEntity): CourseNote = CourseNote(
            id = entity.id,
            courseKey = entity.courseKey,
            courseName = entity.courseName,
            content = entity.content,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }
}
