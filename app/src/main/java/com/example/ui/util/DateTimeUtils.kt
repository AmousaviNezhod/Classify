package com.example.ui.util

import com.example.domain.normalizer.PersianTextNormalizer
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateTimeUtils {

    fun formatCountdown(targetTimestamp: Long): String {
        if (targetTimestamp <= 0L) return ""
        val now = System.currentTimeMillis()
        val diffMillis = targetTimestamp - now

        if (diffMillis < 0) {
            val daysAgo = (-diffMillis) / (24 * 60 * 60 * 1000L)
            return if (daysAgo == 0L) "امروز (پایان‌یافته)" else "${PersianTextNormalizer.toPersianDigits(daysAgo.toString())} روز قبل"
        }

        val diffMinutes = diffMillis / (60 * 1000L)
        val diffHours = diffMillis / (60 * 60 * 1000L)
        val diffDays = diffMillis / (24 * 60 * 60 * 1000L)

        return when {
            diffMinutes < 60 -> "${PersianTextNormalizer.toPersianDigits(diffMinutes.coerceAtLeast(1).toString())} دقیقه دیگر"
            diffHours < 24 -> "${PersianTextNormalizer.toPersianDigits(diffHours.toString())} ساعت دیگر"
            diffDays == 0L -> "امروز"
            diffDays == 1L -> "فردا"
            diffDays < 7L -> "${PersianTextNormalizer.toPersianDigits(diffDays.toString())} روز مانده"
            diffDays < 30L -> {
                val weeks = diffDays / 7
                val remDays = diffDays % 7
                if (remDays == 0L) "${PersianTextNormalizer.toPersianDigits(weeks.toString())} هفته مانده"
                else "${PersianTextNormalizer.toPersianDigits(diffDays.toString())} روز مانده"
            }
            else -> {
                val months = diffDays / 30
                "${PersianTextNormalizer.toPersianDigits(months.toString())} ماه مانده"
            }
        }
    }

    fun isOverdue(targetTimestamp: Long): Boolean {
        return targetTimestamp > 0L && targetTimestamp < System.currentTimeMillis()
    }

    fun parseDateToTimestamp(dateStr: String, timeStr: String = ""): Long {
        if (dateStr.isBlank()) return 0L
        val cleanDate = PersianTextNormalizer.toAsciiDigits(dateStr.trim())
        val cleanTime = PersianTextNormalizer.toAsciiDigits(timeStr.trim()).ifBlank { "09:00" }

        val timeParts = cleanTime.split(":")
        val hour = timeParts.getOrNull(0)?.filter { it.isDigit() }?.toIntOrNull() ?: 9
        val minute = timeParts.getOrNull(1)?.filter { it.isDigit() }?.toIntOrNull() ?: 0

        // Split date parts by slash, dash, dot, or space
        val dateParts = cleanDate.split('/', '-', '.', ' ').filter { it.isNotBlank() }
        if (dateParts.size >= 3) {
            val p0 = dateParts[0].filter { it.isDigit() }.toIntOrNull() ?: 0
            val p1 = dateParts[1].filter { it.isDigit() }.toIntOrNull() ?: 0
            val p2 = dateParts[2].filter { it.isDigit() }.toIntOrNull() ?: 0

            val (year, month, day) = when {
                p0 in 1300..1499 || p0 in 1900..2100 -> Triple(p0, p1, p2)
                p2 in 1300..1499 || p2 in 1900..2100 -> Triple(p2, p1, p0)
                else -> Triple(p0, p1, p2)
            }

            if (year in 1300..1499 && month in 1..12 && day in 1..31) {
                val jdn = com.example.domain.calendar.PersianCalendarHelper.jalaliToJdn(year, month, day)
                val (gy, gm, gd) = com.example.domain.calendar.PersianCalendarHelper.jdnToGregorian(jdn)
                val cal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, gy)
                    set(Calendar.MONTH, gm - 1)
                    set(Calendar.DAY_OF_MONTH, gd)
                    set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
                    set(Calendar.MINUTE, minute.coerceIn(0, 59))
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                return cal.timeInMillis
            } else if (year in 1900..2100 && month in 1..12 && day in 1..31) {
                val cal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month - 1)
                    set(Calendar.DAY_OF_MONTH, day)
                    set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
                    set(Calendar.MINUTE, minute.coerceIn(0, 59))
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                return cal.timeInMillis
            }
        }

        // Fallback for standard formats
        val patterns = listOf(
            "yyyy-MM-dd HH:mm",
            "yyyy/MM/dd HH:mm",
            "yyyy-MM-dd",
            "yyyy/MM/dd"
        )
        for (pattern in patterns) {
            try {
                val text = if (pattern.contains("HH:mm")) "$cleanDate $cleanTime" else cleanDate
                val sdf = SimpleDateFormat(pattern, Locale.US).apply { isLenient = true }
                val parsed = sdf.parse(text)
                if (parsed != null && parsed.time > 0L) return parsed.time
            } catch (_: Exception) {}
        }
        return 0L
    }

    fun getTodayPersianWeekday(): String {
        val cal = Calendar.getInstance()
        return when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SATURDAY -> "شنبه"
            Calendar.SUNDAY -> "یکشنبه"
            Calendar.MONDAY -> "دوشنبه"
            Calendar.TUESDAY -> "سه‌شنبه"
            Calendar.WEDNESDAY -> "چهارشنبه"
            Calendar.THURSDAY -> "پنج‌شنبه"
            Calendar.FRIDAY -> "جمعه"
            else -> "شنبه"
        }
    }

    fun getTodayDayIndex(): Int {
        val cal = Calendar.getInstance()
        return when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SATURDAY -> 0
            Calendar.SUNDAY -> 1
            Calendar.MONDAY -> 2
            Calendar.TUESDAY -> 3
            Calendar.WEDNESDAY -> 4
            Calendar.THURSDAY -> 5
            Calendar.FRIDAY -> 6
            else -> 0
        }
    }
}
