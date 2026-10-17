package com.example

import com.example.domain.calendar.EducationalWeekConfig
import com.example.domain.calendar.PersianCalendarHelper
import com.example.domain.model.CourseEvent
import com.example.domain.model.CourseEventType
import com.example.domain.model.ScheduleClass
import com.example.domain.model.findNextClassOccurrence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class NextClassShortcutTest {

    @Test
    fun testOptionalTitleDefaultLabel() {
        val examEvent = CourseEvent(
            courseKey = "math_1",
            courseName = "ریاضی عمومی ۱",
            type = CourseEventType.EXAM,
            title = ""
        )
        assertEquals("امتحان", examEvent.displayTitle)

        val assignmentEvent = CourseEvent(
            courseKey = "cs_101",
            courseName = "برنامه‌سازی پیشرفته",
            type = CourseEventType.ASSIGNMENT,
            title = ""
        )
        assertEquals("تمرین / تحویل", assignmentEvent.displayTitle)

        val presentationEvent = CourseEvent(
            courseKey = "db_1",
            courseName = "پایگاه داده",
            type = CourseEventType.PRESENTATION,
            title = ""
        )
        assertEquals("ارائه", presentationEvent.displayTitle)

        val otherEvent = CourseEvent(
            courseKey = "general_1",
            courseName = "عمومی",
            type = CourseEventType.OTHER,
            title = ""
        )
        assertEquals("سایر (عنوان سفارشی)", otherEvent.displayTitle)
    }

    @Test
    fun testCustomTitleOverridesDefaultLabel() {
        val customExam = CourseEvent(
            courseKey = "physics_1",
            courseName = "فیزیک ۱",
            type = CourseEventType.MIDTERM,
            title = "میان‌ترم اول مبحث سینماتیک"
        )
        assertEquals("میان‌ترم اول مبحث سینماتیک", customExam.displayTitle)

        val customOther = CourseEvent(
            courseKey = "lab_1",
            courseName = "آزمایشگاه فیزیک",
            type = CourseEventType.OTHER,
            title = "تحویل گزارش کار شماره ۳"
        )
        assertEquals("تحویل گزارش کار شماره ۳", customOther.displayTitle)

        // Whitespace-only title falls back to type title
        val blankTitleEvent = CourseEvent(
            courseKey = "chem_1",
            courseName = "شیمی عمومی",
            type = CourseEventType.QUIZ,
            title = "   "
        )
        assertEquals("کوئیز", blankTitleEvent.displayTitle)
    }

    @Test
    fun testNextClassOccurrenceSameDayBeforeClass() {
        // Prepare a reference time: 2026-10-17 08:00:00 (which is a Saturday, Persian dayIndex 0)
        val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Tehran")).apply {
            set(2026, Calendar.OCTOBER, 17, 8, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val fromMillis = cal.timeInMillis

        // Saturday is dayIndex 0
        val saturdayJdn = PersianCalendarHelper.gregorianToJdn(2026, 10, 17)
        val dayIndexSat = PersianCalendarHelper.getDayOfWeekIndex(saturdayJdn)
        assertEquals(0, dayIndexSat) // Confirms Saturday == dayIndex 0

        val satSession = ScheduleClass(
            id = "sat_sess",
            courseName = "مدارهای منطقی",
            dayOfWeek = "شنبه",
            dayIndex = 0,
            startTime = "10:00",
            endTime = "12:00",
            classroom = "کلاس ۳۰۲"
        )

        val result = findNextClassOccurrence(
            sessions = listOf(satSession),
            fromTimeMillis = fromMillis
        )

        assertNotNull(result)
        assertEquals(saturdayJdn, result?.jdn)
        assertEquals("10:00", result?.timeString)
        assertEquals("شنبه", result?.scheduleClass?.dayOfWeek)
    }

    @Test
    fun testNextClassOccurrenceSameDayAfterClassAdvancesToNextMeeting() {
        // Reference time: Saturday at 11:30 (class was at 10:00 - already started)
        val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Tehran")).apply {
            set(2026, Calendar.OCTOBER, 17, 11, 30, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val fromMillis = cal.timeInMillis

        val satSession = ScheduleClass(
            id = "sat_sess",
            courseName = "مدارهای منطقی",
            dayOfWeek = "شنبه",
            dayIndex = 0,
            startTime = "10:00",
            endTime = "12:00"
        )
        val monSession = ScheduleClass(
            id = "mon_sess",
            courseName = "مدارهای منطقی",
            dayOfWeek = "دوشنبه",
            dayIndex = 2,
            startTime = "14:00",
            endTime = "16:00"
        )

        val result = findNextClassOccurrence(
            sessions = listOf(satSession, monSession),
            fromTimeMillis = fromMillis
        )

        assertNotNull(result)
        // Should advance to Monday (2 days after Saturday)
        assertEquals("دوشنبه", result?.scheduleClass?.dayOfWeek)
        assertEquals("14:00", result?.timeString)
        val satJdn = PersianCalendarHelper.gregorianToJdn(2026, 10, 17)
        assertEquals(satJdn + 2, result?.jdn)
    }

    @Test
    fun testNextClassOccurrenceAlternatingParity() {
        // Reference time: Saturday (dayIndex 0)
        val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Tehran")).apply {
            set(2026, Calendar.OCTOBER, 17, 8, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val fromMillis = cal.timeInMillis
        val baseJdn = PersianCalendarHelper.gregorianToJdn(2026, 10, 17)

        // Configure week 1 (odd) for baseJdn
        val eduConfig = EducationalWeekConfig(
            referenceJdn = baseJdn,
            referenceWeekNumber = 1 // Odd week
        )

        // Sunday (dayIndex 1) class that ONLY meets on even weeks (هفته زوج)
        val biWeeklyEvenSession = ScheduleClass(
            id = "biweekly_sess",
            courseName = "آزمایشگاه سیستم‌های دیجیتال",
            dayOfWeek = "یکشنبه",
            dayIndex = 1,
            startTime = "13:30",
            endTime = "16:30",
            parity = "زوج"
        )

        val result = findNextClassOccurrence(
            sessions = listOf(biWeeklyEvenSession),
            eduWeekConfig = eduConfig,
            fromTimeMillis = fromMillis
        )

        assertNotNull(result)
        // Sunday in week 1 (baseJdn + 1) is odd week -> skipped.
        // Sunday in week 2 (baseJdn + 8) is even week -> selected!
        assertEquals(baseJdn + 8, result?.jdn)
        assertEquals("یکشنبه", result?.scheduleClass?.dayOfWeek)
        assertEquals("13:30", result?.timeString)
    }

    @Test
    fun testNextClassOccurrenceEmptyReturnsNull() {
        val result = findNextClassOccurrence(sessions = emptyList())
        assertNull(result)
    }

    @Test
    fun testNextClassOccurrenceMultipleSessionsWrapsAround() {
        // Reference: Wednesday at 18:00 (all classes of this week done)
        val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Tehran")).apply {
            set(2026, Calendar.OCTOBER, 21, 18, 0, 0) // Wednesday
            set(Calendar.MILLISECOND, 0)
        }
        val fromMillis = cal.timeInMillis
        val wedJdn = PersianCalendarHelper.gregorianToJdn(2026, 10, 21)

        val satSession = ScheduleClass(
            id = "s1",
            courseName = "هوش مصنوعی",
            dayOfWeek = "شنبه",
            dayIndex = 0,
            startTime = "08:00",
            endTime = "10:00"
        )
        val wedSession = ScheduleClass(
            id = "s2",
            courseName = "هوش مصنوعی",
            dayOfWeek = "چهارشنبه",
            dayIndex = 4,
            startTime = "08:00",
            endTime = "10:00"
        )

        val result = findNextClassOccurrence(
            sessions = listOf(satSession, wedSession),
            fromTimeMillis = fromMillis
        )

        assertNotNull(result)
        // Next occurrence is upcoming Saturday (3 days after Wednesday)
        assertEquals("شنبه", result?.scheduleClass?.dayOfWeek)
        assertEquals("08:00", result?.timeString)
        assertEquals(wedJdn + 3, result?.jdn)
    }
}
