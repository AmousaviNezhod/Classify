package com.example

import com.example.domain.model.NormalizedSchedule
import com.example.domain.model.ScheduleClass
import com.example.domain.model.ScheduleOfferingIdentity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CourseMatchingAndWorkshopTest {

    @Test
    fun `matching succeeds when digits are in Persian or ASCII`() {
        val imported = ScheduleClass(
            courseName = "فیزیک ۱",
            courseCode = "101",
            teacher = "",
            dayOfWeek = "شنبه",
            dayIndex = 0,
            startTime = "08:00",
            endTime = "10:00"
        )
        val catalogEntry = ScheduleClass(
            courseName = "فیزیک 1",
            courseCode = "101",
            teacher = "دکتر رضایی",
            dayOfWeek = "شنبه",
            dayIndex = 0,
            startTime = "08:00",
            endTime = "10:00",
            classroom = "کلاس ۱۰۲"
        )

        val enriched = ScheduleOfferingIdentity.enrichFromCatalog(imported, listOf(catalogEntry))
        assertEquals("کلاس ۱۰۲", enriched.classroom)
        assertEquals("دکتر رضایی", enriched.teacher)
    }

    @Test
    fun `placeholder classrooms like نامشخص or مشخص نشده are overwritten by catalog`() {
        val imported1 = ScheduleClass(
            courseName = "معماری کامپیوتر",
            courseCode = "202",
            classroom = "نامشخص",
            dayOfWeek = "یکشنبه",
            dayIndex = 1,
            startTime = "10:00",
            endTime = "12:00"
        )
        val imported2 = ScheduleClass(
            courseName = "سیستم عامل",
            courseCode = "203",
            classroom = "مشخص نشده",
            dayOfWeek = "یکشنبه",
            dayIndex = 1,
            startTime = "14:00",
            endTime = "16:00"
        )
        val catalog = listOf(
            ScheduleClass(
                courseName = "معماری کامپیوتر",
                courseCode = "202",
                classroom = "سایت ۱",
                teacher = "مهندس محمدی",
                dayOfWeek = "یکشنبه",
                dayIndex = 1,
                startTime = "10:00",
                endTime = "12:00"
            ),
            ScheduleClass(
                courseName = "سیستم عامل",
                courseCode = "203",
                classroom = "کلاس ۳۰۵",
                teacher = "دکتر حسینی",
                dayOfWeek = "یکشنبه",
                dayIndex = 1,
                startTime = "14:00",
                endTime = "16:00"
            )
        )

        val enriched1 = ScheduleOfferingIdentity.enrichFromCatalog(imported1, catalog)
        val enriched2 = ScheduleOfferingIdentity.enrichFromCatalog(imported2, catalog)

        assertEquals("سایت ۱", enriched1.classroom)
        assertEquals("کلاس ۳۰۵", enriched2.classroom)
    }

    @Test
    fun `biweekly offerings with same classroom do not return null on tie`() {
        val imported = ScheduleClass(
            courseName = "ریاضی عمومی ۱",
            courseCode = "100",
            dayOfWeek = "شنبه",
            dayIndex = 0,
            startTime = "08:00",
            endTime = "10:00"
        )
        val catalog = listOf(
            ScheduleClass(
                courseName = "ریاضی عمومی ۱",
                courseCode = "100",
                parity = "زوج",
                classroom = "کلاس ۲۰۱",
                teacher = "دکتر اکبری",
                dayOfWeek = "شنبه",
                dayIndex = 0,
                startTime = "08:00",
                endTime = "10:00"
            ),
            ScheduleClass(
                courseName = "ریاضی عمومی ۱",
                courseCode = "100",
                parity = "فرد",
                classroom = "کلاس ۲۰۱",
                teacher = "دکتر اکبری",
                dayOfWeek = "شنبه",
                dayIndex = 0,
                startTime = "08:00",
                endTime = "10:00"
            )
        )

        val match = ScheduleOfferingIdentity.findCatalogMatch(imported, catalog)
        assertNotNull("Tie with same classroom must match", match)
        assertEquals("کلاس ۲۰۱", match?.classroom)
    }

    @Test
    fun `isWorkshop correctly identifies workshops across notes, classroom and name`() {
        val w1 = ScheduleClass(courseName = "کارگاه شبکه", dayOfWeek = "پنج‌شنبه", startTime = "16:00", endTime = "18:00")
        val w2 = ScheduleClass(courseName = "فیزیک ۲", classroom = "آزمایشگاه فیزیک", dayOfWeek = "شنبه", startTime = "08:00", endTime = "10:00")
        val w3 = ScheduleClass(courseName = "مباحث ویژه", classroom = "سایت ۳", dayOfWeek = "دوشنبه", startTime = "14:00", endTime = "16:00")
        val w4 = ScheduleClass(courseName = "ریاضی", notes = "آزمایشگاه / کارگاه / سایت", dayOfWeek = "سه‌شنبه", startTime = "10:00", endTime = "12:00")
        val normal = ScheduleClass(courseName = "ادبیات فارسی", classroom = "کلاس ۱۰۱", dayOfWeek = "چهارشنبه", startTime = "10:00", endTime = "12:00")

        assertTrue(w1.isWorkshop)
        assertTrue(w2.isWorkshop)
        assertTrue(w3.isWorkshop)
        assertTrue(w4.isWorkshop)
        assertTrue(!normal.isWorkshop)
    }
}
