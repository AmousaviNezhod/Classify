package com.example.domain.model

enum class DiffType {
    UNCHANGED,   // بدون تغییر
    MODIFIED,    // تغییر یافته (ساعت، روز، استاد یا کلاس)
    ADDED,       // کلاس جدید در دانشگاه
    REMOVED      // حذف شده از برنامه
}

data class FieldDiff(
    val fieldName: String,     // e.g. "زمان", "روز", "استاد", "کلاس"
    val oldValue: String,
    val newValue: String
)

data class ScheduleClassDiff(
    val courseName: String,
    val groupCode: String,
    val diffType: DiffType,
    val currentClass: ScheduleClass?,
    val universityClass: ScheduleClass?,
    val changes: List<FieldDiff> = emptyList()
)

data class ScheduleComparisonSummary(
    val totalChecked: Int,
    val unchangedCount: Int,
    val modifiedCount: Int,
    val addedCount: Int,
    val removedCount: Int,
    val diffs: List<ScheduleClassDiff>
)
