package com.example.domain.model

data class ScheduleValidationInfo(
    val capacity: Boolean = true,
    val duplicate: Boolean = true,
    val conflict: Boolean = true,
    val units: Boolean = true
)

/**
 * Normalized representation of a full weekly university schedule.
 */
data class NormalizedSchedule(
    val id: String = "latest_schedule",
    val title: String = "برنامه کلاسی دانشگاه",
    val term: String = "۱۴۰۵-۱۴۰۶-۱",
    val sourceUrl: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
    val classes: List<ScheduleClass> = emptyList(),
    val rawJson: String = "",
    val validationInfo: ScheduleValidationInfo? = null,
    val dayAvailability: List<ScheduleDayAvailability> = emptyList()
) {
    val totalCourses: Int
        get() = classes.map { it.courseName }.distinct().size

    val totalWeeklySessions: Int
        get() = classes.size

    val totalUnits: Int
        get() = classes.distinctBy { "${it.courseCode.ifBlank { it.courseName }}|${it.courseName}|${it.groupCode}" }.sumOf { it.units }

    fun classesForDay(dayIndex: Int): List<ScheduleClass> {
        return classes
            .filter { it.dayIndex == dayIndex }
            .sortedBy { it.startTime }
    }
}
