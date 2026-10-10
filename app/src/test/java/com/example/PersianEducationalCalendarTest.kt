package com.example

import com.example.domain.calendar.EducationalWeekConfig
import com.example.domain.calendar.PersianCalendarHelper
import com.example.domain.model.ScheduleClass
import com.example.domain.model.WeekParity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PersianEducationalCalendarTest {

    @Test
    fun testPersianDateConversion() {
        // 2024-03-20 is 1403-01-01 (Farvardin 1, 1403)
        val jdn = PersianCalendarHelper.gregorianToJdn(2024, 3, 20)
        val (jy, jm, jd) = PersianCalendarHelper.jdnToJalali(jdn)
        assertEquals(1403, jy)
        assertEquals(1, jm)
        assertEquals(1, jd)

        // Reverse back
        val (gy, gm, gd) = PersianCalendarHelper.jdnToGregorian(jdn)
        assertEquals(2024, gy)
        assertEquals(3, gm)
        assertEquals(20, gd)
    }

    @Test
    fun testDayOfWeekStartingSaturday() {
        // 2025-10-18 is Saturday, Mehr 26, 1404
        val jdnSaturday = PersianCalendarHelper.gregorianToJdn(2025, 10, 18)
        val satIdx = PersianCalendarHelper.getDayOfWeekIndex(jdnSaturday)
        assertEquals(0, satIdx)
        assertEquals("شنبه", PersianCalendarHelper.getDayOfWeekName(satIdx))

        // Next day Sunday
        val sunIdx = PersianCalendarHelper.getDayOfWeekIndex(jdnSaturday + 1)
        assertEquals(1, sunIdx)
        assertEquals("یکشنبه", PersianCalendarHelper.getDayOfWeekName(sunIdx))

        // Friday
        val friIdx = PersianCalendarHelper.getDayOfWeekIndex(jdnSaturday + 6)
        assertEquals(6, friIdx)
        assertEquals("جمعه", PersianCalendarHelper.getDayOfWeekName(friIdx))
    }

    @Test
    fun testEducationalWeekCalculation() {
        // Reference: Saturday 2025-10-18 as Week 1
        val refJdn = PersianCalendarHelper.gregorianToJdn(2025, 10, 18)
        val config = EducationalWeekConfig(referenceJdn = refJdn, referenceWeekNumber = 1)

        // Saturday (day 0) is week 1
        assertEquals(1, config.getWeekForDate(refJdn))
        assertEquals("فرد", PersianCalendarHelper.getWeekParityString(1))

        // Sunday through Friday of the same week remain in week 1
        for (offset in 1..6) {
            assertEquals("Day offset $offset should still be week 1", 1, config.getWeekForDate(refJdn + offset))
        }

        // Next Saturday (+7) steps to week 2
        val nextSaturday = refJdn + 7
        assertEquals(2, config.getWeekForDate(nextSaturday))
        assertEquals("زوج", PersianCalendarHelper.getWeekParityString(2))

        // Friday of week 2 (+13) remains in week 2
        assertEquals(2, config.getWeekForDate(refJdn + 13))

        // Next Saturday (+14) steps to week 3
        assertEquals(3, config.getWeekForDate(refJdn + 14))
        assertEquals("فرد", PersianCalendarHelper.getWeekParityString(3))
    }

    @Test
    fun testTextWeekExtraction() {
        assertEquals(4, PersianCalendarHelper.extractWeekNumber("برنامه هفته ۴ مهندسی کامپیوتر"))
        assertEquals(2, PersianCalendarHelper.extractWeekNumber("کلاس‌های هفته دوم"))
        assertEquals(3, PersianCalendarHelper.extractWeekNumber("week 3 schedule"))
        assertEquals(null, PersianCalendarHelper.extractWeekNumber("برنامه عمومی بدون هفته"))

        assertEquals("زوج", PersianCalendarHelper.extractParity("آزمایشگاه فیزیک (هفته های زوج)"))
        assertEquals("فرد", PersianCalendarHelper.extractParity("کارگاه شبکه فقط هفته فرد"))
        assertEquals("", PersianCalendarHelper.extractParity("ریاضی عمومی تمام جلسات"))
    }

    @Test
    fun testScheduleClassWeekParityFilter() {
        val evenClass = ScheduleClass(
            courseName = "سیستم عامل",
            dayOfWeek = "شنبه",
            startTime = "08:00",
            endTime = "10:00",
            parity = "هفته زوج"
        )
        assertEquals(WeekParity.EVEN, evenClass.weekParity)
        assertTrue(evenClass.isValidForWeek(2))
        assertTrue(evenClass.isValidForWeek(4))
        assertFalse(evenClass.isValidForWeek(1))
        assertFalse(evenClass.isValidForWeek(3))

        val oddClass = ScheduleClass(
            courseName = "هوش مصنوعی",
            dayOfWeek = "یکشنبه",
            startTime = "10:00",
            endTime = "12:00",
            parity = "هفته فرد"
        )
        assertEquals(WeekParity.ODD, oddClass.weekParity)
        assertTrue(oddClass.isValidForWeek(1))
        assertTrue(oddClass.isValidForWeek(3))
        assertFalse(oddClass.isValidForWeek(2))
        assertFalse(oddClass.isValidForWeek(4))

        val allWeeksClass = ScheduleClass(
            courseName = "فیزیک ۱",
            dayOfWeek = "دوشنبه",
            startTime = "14:00",
            endTime = "16:00",
            parity = ""
        )
        assertEquals(WeekParity.ALL, allWeeksClass.weekParity)
        assertTrue(allWeeksClass.isValidForWeek(1))
        assertTrue(allWeeksClass.isValidForWeek(2))
        assertTrue(allWeeksClass.isValidForWeek(3))
        assertTrue(allWeeksClass.isValidForWeek(4))
    }
}
