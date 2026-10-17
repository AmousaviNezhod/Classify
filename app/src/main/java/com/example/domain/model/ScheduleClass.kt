package com.example.domain.model

import androidx.compose.runtime.Immutable
import com.example.domain.calendar.EducationalWeekConfig
import com.example.domain.calendar.JalaliDate
import com.example.domain.calendar.PersianCalendarHelper
import java.util.Calendar

enum class WeekParity {
    ALL,
    EVEN,
    ODD;

    val titleFa: String
        get() = when (this) {
            ALL -> "تمام هفته‌ها"
            EVEN -> "هفته زوج"
            ODD -> "هفته فرد"
        }
}

/**
 * Normalized representation of a single university class session.
 */
@Immutable
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

    val durationMinutes: Int
        get() {
            val partsStart = com.example.domain.normalizer.PersianTextNormalizer.toAsciiDigits(startTime).split(":")
            val partsEnd = com.example.domain.normalizer.PersianTextNormalizer.toAsciiDigits(endTime).split(":")
            val s = (partsStart.getOrNull(0)?.toIntOrNull() ?: 0) * 60 + (partsStart.getOrNull(1)?.toIntOrNull() ?: 0)
            val e = (partsEnd.getOrNull(0)?.toIntOrNull() ?: 0) * 60 + (partsEnd.getOrNull(1)?.toIntOrNull() ?: 0)
            return if (e > s) e - s else 120
        }

    val isPractical: Boolean
        get() {
            if (notes.contains("عملی") || notes.contains("کارگاه") || notes.contains("آزمایشگاه") || notes.contains("سایت") || notes.contains("پروژه")) return true
            if (classroom.contains("کارگاه") || classroom.contains("سایت") || classroom.contains("آزمایشگاه")) return true
            if (courseName.contains("کارگاه") || courseName.contains("آزمایشگاه")) return true
            if (notes.contains("نظری") || notes.contains("تئوری")) return false
            return durationMinutes >= 180
        }

    val isTheoretical: Boolean
        get() {
            if (notes.contains("نظری") || notes.contains("تئوری")) return true
            if (isPractical) return false
            return durationMinutes <= 120
        }

    val isWorkshop: Boolean
        get() = isPractical

    val sessionTypeLabel: String
        get() = when {
            notes.contains("کارگاه") || courseName.contains("کارگاه") || classroom.contains("کارگاه") -> "کارگاه"
            notes.contains("آزمایشگاه") || courseName.contains("آزمایشگاه") || classroom.contains("آزمایشگاه") -> "آزمایشگاه"
            notes.contains("سایت") || classroom.contains("سایت") -> "سایت"
            isPractical -> "عملی"
            isTheoretical -> "نظری"
            else -> ""
        }

    val weekParity: WeekParity
        get() = when {
            parity.contains("زوج") -> WeekParity.EVEN
            parity.contains("فرد") -> WeekParity.ODD
            else -> WeekParity.ALL
        }

    val parityLabel: String
        get() = weekParity.titleFa

    fun isValidForWeek(weekNumber: Int): Boolean {
        val isEven = PersianCalendarHelper.isEvenWeek(weekNumber)
        return when (weekParity) {
            WeekParity.ALL -> true
            WeekParity.EVEN -> isEven
            WeekParity.ODD -> !isEven
        }
    }
}

data class NextClassOccurrence(
    val jdn: Long,
    val jalaliDate: JalaliDate,
    val dateString: String,      // "yyyy/MM/dd"
    val timeString: String,      // "HH:mm"
    val scheduleClass: ScheduleClass
)

fun findNextClassOccurrence(
    sessions: List<ScheduleClass>,
    eduWeekConfig: EducationalWeekConfig = EducationalWeekConfig(),
    fromTimeMillis: Long = System.currentTimeMillis()
): NextClassOccurrence? {
    if (sessions.isEmpty()) return null

    val cal = Calendar.getInstance().apply { timeInMillis = fromTimeMillis }
    val curHour = cal.get(Calendar.HOUR_OF_DAY)
    val curMinute = cal.get(Calendar.MINUTE)
    val todayJdn = PersianCalendarHelper.gregorianToJdn(
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH) + 1,
        cal.get(Calendar.DAY_OF_MONTH)
    )

    // Check up to 28 days ahead (covers weekly and alternating-week schedules)
    for (dayOffset in 0..28) {
        val checkJdn = todayJdn + dayOffset
        val dayIndex = PersianCalendarHelper.getDayOfWeekIndex(checkJdn)
        val weekNumber = eduWeekConfig.getWeekForDate(checkJdn)

        val daySessions = sessions
            .filter { it.dayIndex == dayIndex && it.isValidForWeek(weekNumber) }
            .sortedBy { it.startTime }

        for (session in daySessions) {
            val asciiTime = com.example.domain.normalizer.PersianTextNormalizer.toAsciiDigits(session.startTime)
            val parts = asciiTime.split(":")
            val sHour = parts.getOrNull(0)?.toIntOrNull() ?: 0
            val sMinute = parts.getOrNull(1)?.toIntOrNull() ?: 0

            val isFuture = if (dayOffset == 0) {
                // Must be strictly in the future (not past or already started)
                sHour > curHour || (sHour == curHour && sMinute > curMinute)
            } else {
                true
            }

            if (isFuture) {
                val jDate = PersianCalendarHelper.jdnToJalali(checkJdn)
                val dateStr = "%04d/%02d/%02d".format(jDate.year, jDate.month, jDate.day)
                val timeStr = "%02d:%02d".format(sHour, sMinute)
                return NextClassOccurrence(
                    jdn = checkJdn,
                    jalaliDate = jDate,
                    dateString = dateStr,
                    timeString = timeStr,
                    scheduleClass = session
                )
            }
        }
    }
    return null
}

