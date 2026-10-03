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

    fun parseDateToTimestamp(dateStr: String, timeStr: String = ""): Long {
        if (dateStr.isBlank()) return 0L
        val cleanDate = PersianTextNormalizer.toAsciiDigits(dateStr.trim())
        val cleanTime = PersianTextNormalizer.toAsciiDigits(timeStr.trim()).ifBlank { "09:00" }

        // Try standard format yyyy-MM-dd HH:mm or yyyy/MM/dd HH:mm
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
                if (parsed != null) return parsed.time
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
