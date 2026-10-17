package com.example

import com.example.domain.comparator.ScheduleComparator
import com.example.domain.model.NormalizedSchedule
import com.example.domain.model.ScheduleClass
import com.example.domain.model.ScheduleOfferingIdentity
import com.example.ui.MainViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests verifying that theoretical and practical sessions of the same course
 * remain strictly isolated and retain their respective locations without cross-contamination.
 */
class TheoreticalPracticalIsolationTest {

    private fun createSession(
        name: String,
        dayIndex: Int,
        day: String,
        start: String,
        end: String,
        classroom: String,
        teacher: String = "استاد نمونه",
        notes: String = "",
        group: String = "1",
        code: String = "101"
    ) = ScheduleClass(
        courseName = name,
        courseCode = code,
        dayIndex = dayIndex,
        dayOfWeek = day,
        startTime = start,
        endTime = end,
        classroom = classroom,
        teacher = teacher,
        notes = notes,
        groupCode = group
    )

    @Test
    fun `duration heuristic correctly classifies theoretical and practical sessions`() {
        // 2-hour standard block -> theoretical
        val theoreticalClass = createSession(
            name = "مدارهای الکتریکی",
            dayIndex = 0, day = "شنبه",
            start = "08:00", end = "10:00",
            classroom = "کلاس ۳۰۱"
        )
        assertEquals(120, theoreticalClass.durationMinutes)
        assertTrue(theoreticalClass.isTheoretical)
        assertFalse(theoreticalClass.isPractical)

        // 3-hour block -> practical / lab
        val labClass3h = createSession(
            name = "آزمایشگاه مدارهای الکتریکی",
            dayIndex = 2, day = "دوشنبه",
            start = "13:00", end = "16:00",
            classroom = "ساختمان آزمایشگاه‌ها - آز مدار"
        )
        assertEquals(180, labClass3h.durationMinutes)
        assertTrue(labClass3h.isPractical)
        assertFalse(labClass3h.isTheoretical)

        // 4-hour block -> practical / workshop
        val workshopClass4h = createSession(
            name = "کارگاه کامپیوتر",
            dayIndex = 3, day = "سه‌شنبه",
            start = "08:00", end = "12:00",
            classroom = "سایت شماره ۲"
        )
        assertEquals(240, workshopClass4h.durationMinutes)
        assertTrue(workshopClass4h.isPractical)
        assertFalse(workshopClass4h.isTheoretical)
    }

    @Test
    fun `explicit session label overrides duration heuristic`() {
        // Even if scheduled in a 2-hour block, explicit notes or title mark it as practical
        val explicitPractical = createSession(
            name = "کارگاه ماشین ابزار",
            dayIndex = 1, day = "یکشنبه",
            start = "10:00", end = "12:00",
            classroom = "سوله کارگاهی",
            notes = "کارگاه"
        )
        assertTrue(explicitPractical.isPractical)
        assertFalse(explicitPractical.isTheoretical)

        val explicitTheory = createSession(
            name = "فیزیک ۲",
            dayIndex = 0, day = "شنبه",
            start = "14:00", end = "17:00",
            classroom = "آمفی‌تئاتر ۱",
            notes = "نظری"
        )
        assertTrue(explicitTheory.isTheoretical)
        assertFalse(explicitTheory.isPractical)
    }

    @Test
    fun `theoretical session never borrows lab or workshop classroom from catalog`() {
        // Course: فیزیک ۲ with theoretical (کلاس ۱۰۲) and practical lab (آزمایشگاه فیزیک)
        val catalogTheory = createSession(
            name = "فیزیک ۲",
            dayIndex = 0, day = "شنبه",
            start = "08:00", end = "10:00",
            classroom = "ساختمان علوم - کلاس ۱۰۲",
            notes = "نظری"
        )

        val catalogLab = createSession(
            name = "فیزیک ۲",
            dayIndex = 2, day = "دوشنبه",
            start = "13:00", end = "16:00",
            classroom = "مجموعه آزمایشگاه‌ها - آز فیزیک ۲",
            notes = "عملی"
        )

        val catalog = listOf(catalogTheory, catalogLab)

        // User imports their theoretical session without classroom info
        val userTheory = createSession(
            name = "فیزیک ۲",
            dayIndex = 0, day = "شنبه",
            start = "08:00", end = "10:00",
            classroom = "",
            notes = "نظری"
        )

        // User imports their practical session without classroom info
        val userLab = createSession(
            name = "فیزیک ۲",
            dayIndex = 2, day = "دوشنبه",
            start = "13:00", end = "16:00",
            classroom = "",
            notes = "عملی"
        )

        val enrichedTheory = ScheduleOfferingIdentity.enrichFromCatalog(userTheory, catalog)
        val enrichedLab = ScheduleOfferingIdentity.enrichFromCatalog(userLab, catalog)

        assertEquals("ساختمان علوم - کلاس ۱۰۲", enrichedTheory.classroom)
        assertEquals("مجموعه آزمایشگاه‌ها - آز فیزیک ۲", enrichedLab.classroom)
    }

    @Test
    fun `theoretical session does not match practical catalog entry even if on same day`() {
        // Edge case: theoretical in morning, lab in afternoon on same day
        val catalogTheory = createSession(
            name = "برنامه نویسی پیشرفته",
            dayIndex = 1, day = "یکشنبه",
            start = "08:00", end = "10:00",
            classroom = "کلاس ۴۰۵",
            notes = "نظری"
        )

        val catalogLab = createSession(
            name = "برنامه نویسی پیشرفته",
            dayIndex = 1, day = "یکشنبه",
            start = "13:00", end = "16:00",
            classroom = "سایت کامپیوتر ۱",
            notes = "کارگاه"
        )

        val catalog = listOf(catalogTheory, catalogLab)

        val userTheory = createSession(
            name = "برنامه نویسی پیشرفته",
            dayIndex = 1, day = "یکشنبه",
            start = "08:00", end = "10:00",
            classroom = "",
            notes = "نظری"
        )

        val matched = ScheduleOfferingIdentity.findCatalogMatch(userTheory, catalog)
        assertNotNull(matched)
        assertEquals("کلاس ۴۰۵", matched?.classroom)
        assertFalse(matched?.classroom?.contains("سایت") == true)
    }

    @Test
    fun `MainViewModel enrichMySchedule correctly isolates sessions for full schedule`() {
        val catalogTheory = createSession(
            name = "مبانی کامپیوتر",
            dayIndex = 0, day = "شنبه",
            start = "10:00", end = "12:00",
            classroom = "کلاس ۲۰۳",
            notes = "نظری"
        )

        val catalogLab = createSession(
            name = "مبانی کامپیوتر",
            dayIndex = 4, day = "چهارشنبه",
            start = "14:00", end = "17:00",
            classroom = "سایت مرکزی ۳",
            notes = "عملی"
        )

        val myTheory = createSession(
            name = "مبانی کامپیوتر",
            dayIndex = 0, day = "شنبه",
            start = "10:00", end = "12:00",
            classroom = "",
            notes = "نظری"
        )

        val myLab = createSession(
            name = "مبانی کامپیوتر",
            dayIndex = 4, day = "چهارشنبه",
            start = "14:00", end = "17:00",
            classroom = "",
            notes = "عملی"
        )

        val userSchedule = NormalizedSchedule(id = "my", classes = listOf(myTheory, myLab))
        val catalogSchedule = NormalizedSchedule(id = "cat", classes = listOf(catalogTheory, catalogLab))

        val enriched = MainViewModel.enrichMySchedule(userSchedule, catalogSchedule)

        val enrichedTheory = enriched.classes.first { it.dayIndex == 0 }
        val enrichedLab = enriched.classes.first { it.dayIndex == 4 }

        assertEquals("کلاس ۲۰۳", enrichedTheory.classroom)
        assertEquals("سایت مرکزی ۳", enrichedLab.classroom)
    }
}
