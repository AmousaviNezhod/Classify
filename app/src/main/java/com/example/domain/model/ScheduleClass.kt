package com.example.domain.model

/**
 * Normalized representation of a single university class session.
 */
data class ScheduleClass(
    val id: String = "",
    val scheduleId: String = "",
    val courseName: String,
    val courseCode: String = "",
    val teacher: String = "",
    val dayOfWeek: String,       // Persian day: شنبه, یکشنبه, etc.
    val dayIndex: Int = 0,       // 0 for شنبه, 1 for یکشنبه, ..., 6 for جمعه
    val startTime: String,       // HH:mm, e.g. "08:00"
    val endTime: String,         // HH:mm, e.g. "10:00"
    val classroom: String = "",  // e.g. "101", "سایت ۱"
    val groupCode: String = "",  // e.g. "1", "01"
    val parity: String = "",     // e.g. "زوج", "فرد", "هفتگی"
    val units: Int = 0,          // Course units e.g. 2, 3
    val notes: String = ""       // e.g. "نظری/عملی"
) {
    /**
     * Unique key for matching classes semantically (course name + group or course code).
     */
    val semanticKey: String
        get() = "${courseName.trim().lowercase()}_${groupCode.trim().ifEmpty { "0" }}"
}
