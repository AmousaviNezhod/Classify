package com.example

import com.example.domain.calendar.PersianCalendarHelper
import com.example.ui.util.DateTimeUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class DateTimeUtilsTest {

    @Test
    fun `parseDateToTimestamp correctly converts Jalali date strings`() {
        // Date: 1403/10/25 10:30
        // JDN for 1403/10/25:
        val jdn = PersianCalendarHelper.jalaliToJdn(1403, 10, 25)
        val (gy, gm, gd) = PersianCalendarHelper.jdnToGregorian(jdn)

        val expectedCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, gy)
            set(Calendar.MONTH, gm - 1)
            set(Calendar.DAY_OF_MONTH, gd)
            set(Calendar.HOUR_OF_DAY, 10)
            set(Calendar.MINUTE, 30)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val parsedTimestamp = DateTimeUtils.parseDateToTimestamp("1403/10/25", "10:30")
        assertEquals(expectedCal.timeInMillis, parsedTimestamp)
    }

    @Test
    fun `parseDateToTimestamp supports Persian and Arabic digits`() {
        val parsedAscii = DateTimeUtils.parseDateToTimestamp("1403/10/25", "10:30")
        val parsedPersian = DateTimeUtils.parseDateToTimestamp("۱۴۰۳/۱۰/۲۵", "۱۰:۳۰")
        val parsedArabic = DateTimeUtils.parseDateToTimestamp("١٤٠٣/١٠/٢٥", "١٠:٣٠")

        assertEquals(parsedAscii, parsedPersian)
        assertEquals(parsedAscii, parsedArabic)
    }

    @Test
    fun `isOverdue detects past and future events accurately`() {
        val now = System.currentTimeMillis()
        val oneHourAgo = now - 3600_000L
        val oneDayAgo = now - 86400_000L
        val oneHourLater = now + 3600_000L
        val oneDayLater = now + 86400_000L

        assertTrue(DateTimeUtils.isOverdue(oneHourAgo))
        assertTrue(DateTimeUtils.isOverdue(oneDayAgo))
        assertFalse(DateTimeUtils.isOverdue(oneHourLater))
        assertFalse(DateTimeUtils.isOverdue(oneDayLater))
        assertFalse(DateTimeUtils.isOverdue(0L))
        assertFalse(DateTimeUtils.isOverdue(-100L))
    }

    @Test
    fun `formatCountdown generates appropriate Persian time representations`() {
        val now = System.currentTimeMillis()
        val inTwoHours = now + (2 * 3600_000L) + 60_000L
        val countdownTwoHours = DateTimeUtils.formatCountdown(inTwoHours)
        assertTrue(countdownTwoHours.contains("ساعت دیگر") || countdownTwoHours.contains("امروز"))

        val inThreeDays = now + (3 * 86400_000L) + 3600_000L
        val countdownDays = DateTimeUtils.formatCountdown(inThreeDays)
        assertTrue(countdownDays.contains("روز مانده") || countdownDays.contains("روز دیگر"))

        val twoDaysAgo = now - (2 * 86400_000L) - 3600_000L
        val countdownAgo = DateTimeUtils.formatCountdown(twoDaysAgo)
        assertTrue(countdownAgo.contains("روز قبل"))
    }
}
