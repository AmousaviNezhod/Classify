package com.example.domain.calendar

import com.example.domain.normalizer.PersianTextNormalizer
import java.util.Calendar

data class JalaliDate(
    val year: Int,
    val month: Int,
    val day: Int
) {
    val monthName: String
        get() = PersianCalendarHelper.MONTH_NAMES.getOrElse(month - 1) { "" }

    val formatted: String
        get() = "${PersianTextNormalizer.toPersianDigits(day.toString())} $monthName"

    val formattedFull: String
        get() = "${PersianTextNormalizer.toPersianDigits(day.toString())} $monthName ${PersianTextNormalizer.toPersianDigits(year.toString())}"

    val formattedNumeric: String
        get() = "${PersianTextNormalizer.toPersianDigits(year.toString())}/${PersianTextNormalizer.toPersianDigits("%02d".format(month))}/${PersianTextNormalizer.toPersianDigits("%02d".format(day))}"
}

data class EducationalWeekConfig(
    val referenceJdn: Long = PersianCalendarHelper.getTodayJdn(),
    val referenceWeekNumber: Int = 1
) {
    fun getWeekForDate(jdn: Long): Int =
        PersianCalendarHelper.calculateEducationalWeek(referenceJdn, referenceWeekNumber, jdn)

    fun getParityForDate(jdn: Long): String =
        PersianCalendarHelper.getWeekParityString(getWeekForDate(jdn))
}

object PersianCalendarHelper {

    val MONTH_NAMES = listOf(
        "فروردین", "اردیبهشت", "خرداد",
        "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر",
        "دی", "بهمن", "اسفند"
    )

    val WEEKDAY_NAMES = listOf(
        "شنبه", "یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنج‌شنبه", "جمعه"
    )

    private val BREAKS = intArrayOf(
        -61, 9, 38, 199, 426, 686, 756, 818, 1111, 1181, 1210,
        1635, 2060, 2097, 2192, 2262, 2324, 2394, 2456, 3178
    )

    private data class JalCalResult(val leap: Int, val gy: Int, val march: Int)

    private fun jalCal(jy: Int): JalCalResult {
        val bl = BREAKS.size
        val gy = jy + 621
        var leapJ = -14
        var jp = BREAKS[0]
        var jm = 0
        var jump = 0
        var n = 0

        if (jy < jp || jy >= BREAKS[bl - 1]) {
            // Fallback approximation for extreme years
            val leap = if ((((jy + 38) * 31) % 128) <= 31) 1 else 0
            return JalCalResult(leap, gy, 20)
        }

        for (i in 1 until bl) {
            jm = BREAKS[i]
            jump = jm - jp
            if (jy < jm) break
            leapJ += (jump / 33) * 8 + ((jump % 33) / 4)
            jp = jm
        }
        n = jy - jp
        leapJ += (n / 33) * 8 + (((n % 33) + 3) / 4)
        if ((jump % 33) == 4 && (jump - n) == 4) leapJ += 1

        val leapG = (gy / 4) - (((gy / 100) + 1) * 3 / 4) - 150
        val march = 20 + leapJ - leapG

        if ((jump - n) < 6) n = n - jump + ((jump + 4) / 33) * 33
        var leap = (((n + 1) % 33) - 1) % 4
        if (leap == -1) leap = 4

        return JalCalResult(if (leap == 0) 1 else 0, gy, march)
    }

    fun gregorianToJdn(gy: Int, gm: Int, gd: Int): Long {
        val a = (14 - gm) / 12
        val y = gy + 4800 - a
        val m = gm + 12 * a - 3
        return gd.toLong() + ((153 * m + 2) / 5) + 365L * y + (y / 4) - (y / 100) + (y / 400) - 32045L
    }

    fun jdnToGregorian(jdn: Long): Triple<Int, Int, Int> {
        val a = jdn + 32044
        val b = (4 * a + 3) / 146097
        val c = a - (146097 * b) / 4
        val d = (4 * c + 3) / 1461
        val e = c - (1461 * d) / 4
        val m = (5 * e + 2) / 153
        val gd = (e - (153 * m + 2) / 5 + 1).toInt()
        val gm = (m + 3 - 12 * (m / 10)).toInt()
        val gy = (100 * b + d - 4800 + (m / 10)).toInt()
        return Triple(gy, gm, gd)
    }

    fun jalaliToJdn(jy: Int, jm: Int, jd: Int): Long {
        val r = jalCal(jy)
        val jdn1f = gregorianToJdn(r.gy, 3, r.march)
        val monthOffset = if (jm <= 7) (jm - 1) * 31 else (6 * 31) + (jm - 7) * 30
        return jdn1f + monthOffset + (jd - 1)
    }

    fun jdnToJalali(jdn: Long): JalaliDate {
        val (gy, _, _) = jdnToGregorian(jdn)
        var jy = gy - 621
        var r = jalCal(jy)
        var jdn1f = gregorianToJdn(r.gy, 3, r.march)
        var k = (jdn - jdn1f).toInt()

        if (k >= 0) {
            if (k <= 185) {
                val jm = 1 + (k / 31)
                val jd = (k % 31) + 1
                return JalaliDate(jy, jm, jd)
            } else {
                k -= 186
            }
        } else {
            jy -= 1
            r = jalCal(jy)
            jdn1f = gregorianToJdn(r.gy, 3, r.march)
            k = (jdn - jdn1f).toInt()
            if (k <= 185) {
                val jm = 1 + (k / 31)
                val jd = (k % 31) + 1
                return JalaliDate(jy, jm, jd)
            } else {
                k -= 186
            }
        }
        val jm = 7 + (k / 30)
        val jd = (k % 30) + 1
        return JalaliDate(jy, jm, jd)
    }

    fun getTodayJdn(): Long {
        val cal = Calendar.getInstance()
        return gregorianToJdn(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    /**
     * Persian day of week index:
     * Saturday (شنبه) = 0, Sunday (یکشنبه) = 1, ..., Friday (جمعه) = 6
     */
    fun getDayOfWeekIndex(jdn: Long): Int {
        val idx = ((jdn + 2) % 7).toInt()
        return if (idx < 0) idx + 7 else idx
    }

    fun getDayOfWeekName(dayIndex: Int): String {
        return WEEKDAY_NAMES.getOrElse(dayIndex.coerceIn(0, 6)) { "شنبه" }
    }

    /**
     * Calculates educational week number:
     * Week runs from Saturday (day 0) to Friday (day 6).
     */
    fun calculateEducationalWeek(
        refJdn: Long,
        refWeekNumber: Int,
        targetJdn: Long
    ): Int {
        val refSaturday = refJdn - getDayOfWeekIndex(refJdn)
        val targetSaturday = targetJdn - getDayOfWeekIndex(targetJdn)
        val weekDiff = (targetSaturday - refSaturday) / 7
        return (refWeekNumber + weekDiff).toInt().coerceAtLeast(1)
    }

    fun isEvenWeek(weekNumber: Int): Boolean {
        val norm = if (weekNumber < 0) -weekNumber else weekNumber
        return norm % 2 == 0
    }

    fun getWeekParityString(weekNumber: Int): String {
        return if (isEvenWeek(weekNumber)) "زوج" else "فرد"
    }

    fun formatShortDate(timestamp: Long): String {
        if (timestamp <= 0L) return ""
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        val jdn = gregorianToJdn(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
        val jDate = jdnToJalali(jdn)
        return jDate.formatted
    }

    fun formatFullDate(timestamp: Long): String {
        if (timestamp <= 0L) return ""
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        val jdn = gregorianToJdn(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
        val dayIndex = getDayOfWeekIndex(jdn)
        val jDate = jdnToJalali(jdn)
        return "${getDayOfWeekName(dayIndex)}، ${jDate.formattedFull}"
    }

    /**
     * Extracts week number from text (e.g. titles, file names, notes).
     */
    fun extractWeekNumber(text: String): Int? {
        if (text.isBlank()) return null
        val clean = PersianTextNormalizer.normalizeText(text)

        val digitMatch = Regex("(?:هفته(?:[\\s‌]+آموزشی)?|week|w)[\\s_:-]*([0-9۰-۹]{1,2})", RegexOption.IGNORE_CASE)
            .find(clean)
        if (digitMatch != null) {
            val num = PersianTextNormalizer.toAsciiDigits(digitMatch.groupValues[1]).toIntOrNull()
            if (num in 1..25) return num
        }

        return when {
            clean.contains("هفته اول") || clean.contains("هفتهٔ اول") || clean.contains("هفته نخست") -> 1
            clean.contains("هفته دوم") || clean.contains("هفتهٔ دوم") -> 2
            clean.contains("هفته سوم") || clean.contains("هفتهٔ سوم") -> 3
            clean.contains("هفته چهارم") || clean.contains("هفتهٔ چهارم") -> 4
            clean.contains("هفته پنجم") || clean.contains("هفتهٔ پنجم") -> 5
            clean.contains("هفته ششم") || clean.contains("هفتهٔ ششم") -> 6
            clean.contains("هفته هفتم") || clean.contains("هفتهٔ هفتم") -> 7
            clean.contains("هفته هشتم") || clean.contains("هفتهٔ هشتم") -> 8
            clean.contains("هفته نهم") || clean.contains("هفتهٔ نهم") -> 9
            clean.contains("هفته دهم") || clean.contains("هفتهٔ دهم") -> 10
            clean.contains("هفته یازدهم") || clean.contains("هفتهٔ یازدهم") -> 11
            clean.contains("هفته دوازدهم") || clean.contains("هفتهٔ دوازدهم") -> 12
            clean.contains("هفته سیزدهم") || clean.contains("هفتهٔ سیزدهم") -> 13
            clean.contains("هفته چهاردهم") || clean.contains("هفتهٔ چهاردهم") -> 14
            clean.contains("هفته پانزدهم") || clean.contains("هفتهٔ پانزدهم") -> 15
            clean.contains("هفته شانزدهم") || clean.contains("هفتهٔ شانزدهم") -> 16
            else -> null
        }
    }

    /**
     * Extracts parity (زوج / فرد) from text.
     */
    fun extractParity(text: String): String {
        if (text.isBlank()) return ""
        val clean = PersianTextNormalizer.normalizeText(text)
        return when {
            clean.contains("هفته زوج") || clean.contains("(زوج)") || clean.contains("زوج") -> "زوج"
            clean.contains("هفته فرد") || clean.contains("(فرد)") || clean.contains("فرد") -> "فرد"
            else -> ""
        }
    }
}
