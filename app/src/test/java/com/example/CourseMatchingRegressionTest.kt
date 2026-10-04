package com.example

import com.example.domain.comparator.ScheduleComparator
import com.example.ui.MainViewModel
import com.example.domain.model.DiffType
import com.example.domain.model.NormalizedSchedule
import com.example.domain.model.ScheduleClass
import com.example.domain.model.ScheduleOfferingIdentity
import com.example.domain.normalizer.PersianTextNormalizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression coverage for "کلاس‌های یکشنبه پیدا نمی‌شود":
 * the PDFs carry the classes, but matching lost them because of name spacing,
 * section suffixes like "(کاردانی)", parity markers inside teacher fields, and
 * greedy assignment that let one session steal another session's offering.
 */
class CourseMatchingRegressionTest {

    private fun offering(
        name: String,
        dayIndex: Int,
        day: String,
        start: String,
        end: String,
        teacher: String = "",
        group: String = "",
        code: String = "",
        parity: String = "",
        classroom: String = "",
        id: String = ""
    ) = ScheduleClass(
        id = id,
        courseName = name,
        courseCode = code,
        teacher = teacher,
        dayOfWeek = day,
        dayIndex = dayIndex,
        startTime = start,
        endTime = end,
        classroom = classroom,
        groupCode = group,
        parity = parity
    )

    private val webCatalogEntry = offering(
        // PDF spelling: section suffix + different word spacing than the user's input.
        name = "برنامه نویسی مبتنی بر وب(کاردانی)",
        dayIndex = 1, day = "یکشنبه", start = "16:00", end = "18:00",
        teacher = "مهندس سمیه مقدم زاده کاشانی",
        classroom = "نظام مهندسی سایت5 (2/1)",
        id = "cat-web-1"
    )

    private val userWebSunday = offering(
        name = "برنامه نویسی مبتنی بروب",
        dayIndex = 1, day = "یکشنبه", start = "16:00", end = "19:00",
        teacher = "سمیه مقدم زاده کاشانی", group = "3", code = "315012"
    )

    private val userWebThursday = offering(
        name = "برنامه نویسی مبتنی بروب",
        dayIndex = 5, day = "پنج‌شنبه", start = "14:00", end = "16:00",
        teacher = "سمیه مقدم زاده کاشانی", group = "3", code = "315012", parity = "زوج"
    )

    @Test
    fun `teacher field noise does not break name matching`() {
        assertEquals("رویا یداللهی شاه راه", PersianTextNormalizer.cleanTeacherName("رویا یداللهی شاه راه"))
        // Parity markers are stripped, leaving the surname form the PDF uses;
        // matching works through containment of the cleaned names.
        assertEquals("یداللهی شاه راه", PersianTextNormalizer.cleanTeacherName("یداللهی شاه راه *زوج"))
        assertEquals("یزدانجو", PersianTextNormalizer.cleanTeacherName("یزدانجو *زوج"))
        assertTrue(
            PersianTextNormalizer.cleanTeacherName("رویا یداللهی شاه راه")
                .contains(PersianTextNormalizer.cleanTeacherName("یداللهی شاه راه *زوج"))
        )
        assertEquals("کهرم کلانی", PersianTextNormalizer.cleanTeacherName("کهرم*زوج گ1 -کلانی"))
        assertEquals("شریف زاده", PersianTextNormalizer.cleanTeacherName("شریف زاده(تا71)"))
        // Surnames that merely contain a parity word survive.
        assertEquals("یزدی فرد", PersianTextNormalizer.cleanTeacherName("یزدی فرد"))
    }

    @Test
    fun `course names starting with engineering are not erased`() {
        assertEquals("مهندسی پی", PersianTextNormalizer.cleanCourseName("مهندسی پی"))
        assertEquals("مهندسی نرم افزار", PersianTextNormalizer.cleanCourseName("مهندسی نرم افزار استاد: کلانی"))
    }

    @Test
    fun `sunday session is found despite spacing and section suffix`() {
        val summary = ScheduleComparator.compare(
            NormalizedSchedule(id = "input", classes = listOf(userWebSunday)),
            NormalizedSchedule(id = "uni", classes = listOf(webCatalogEntry))
        )
        val diff = summary.diffs.single()
        assertTrue("Session must be found, not reported missing", diff.diffType != DiffType.REMOVED)
        assertNotNull(diff.universityClass)
        // 16:00-19:00 vs the PDF's 16:00-18:00: only the time differs.
        assertEquals(listOf("ساعت برگزاری"), diff.changes.map { it.fieldName })
    }

    @Test
    fun `same-day session claims the offering before a cross-day session`() {
        val summary = ScheduleComparator.compare(
            NormalizedSchedule(id = "input", classes = listOf(userWebThursday, userWebSunday)),
            NormalizedSchedule(id = "uni", classes = listOf(webCatalogEntry))
        )
        val sunday = summary.diffs.single { it.currentClass?.dayIndex == 1 }
        val thursday = summary.diffs.single { it.currentClass?.dayIndex == 5 }
        assertNotNull("یکشنبه session must win the یکشنبه offering", sunday.universityClass)
        assertEquals("Only the time may differ", listOf("ساعت برگزاری"), sunday.changes.map { it.fieldName })
        assertNull("Cross-day session must not steal it", thursday.universityClass)
        assertEquals(DiffType.REMOVED, thursday.diffType)
    }

    @Test
    fun `different section with another teacher is not matched`() {
        val otherSection = offering(
            name = "اصول و فناوری مذاکره",
            dayIndex = 1, day = "یکشنبه", start = "14:00", end = "16:00",
            teacher = "نادری", group = "1", classroom = "ساختمان ۲ - کلاس ۲۳۴", id = "cat-moz"
        )
        val mine = offering(
            name = "اصول و فناوری مذاکره",
            dayIndex = 3, day = "سه‌شنبه", start = "10:00", end = "12:00",
            teacher = "ندا نمائی قاسمی", group = "1", code = "315020"
        )
        val summary = ScheduleComparator.compare(
            NormalizedSchedule(id = "input", classes = listOf(mine)),
            NormalizedSchedule(id = "uni", classes = listOf(otherSection))
        )
        val mineDiff = summary.diffs.single { it.currentClass != null }
        assertEquals("Must be reported missing, not as a moved class", DiffType.REMOVED, mineDiff.diffType)
        assertNull(mineDiff.universityClass)
    }

    @Test
    fun `enrichment does not borrow a room from a different teacher's section`() {
        val mine = offering(
            name = "مباحث ویژه دربرنامه نویسی",
            dayIndex = 3, day = "سه‌شنبه", start = "16:00", end = "19:00",
            teacher = "جواد یزدانجو", group = "2", code = "315022"
        )
        val otherTeacher = offering(
            name = "مباحث ویژه در برنامه نویسی",
            dayIndex = 3, day = "سه‌شنبه", start = "16:00", end = "18:00",
            teacher = "مهندس سعیده وارسته پور", classroom = "نظام مهندسی سایت2 (2/1)", id = "cat-mab"
        )
        val match = ScheduleOfferingIdentity.findCatalogMatch(mine, listOf(otherTeacher))
        assertNull("Different teacher's section must not lend its room", match)

        val sameTeacher = otherTeacher.copy(id = "cat-mab2", teacher = "مهندس جواد یزدانجو")
        val enriched = ScheduleOfferingIdentity.enrichFromCatalog(mine, listOf(sameTeacher))
        assertEquals("نظام مهندسی سایت2 (2/1)", enriched.classroom)
    }

    @Test
    fun `cross-day match needs teacher or code corroboration`() {
        val anonymousSection = offering(
            name = "مباحث ویژه در برنامه نویسی",
            dayIndex = 1, day = "یکشنبه", start = "18:00", end = "20:00",
            classroom = "نظام مهندسی سایت6 (2/1)", id = "cat-anon"
        )
        val mine = offering(
            name = "مباحث ویژه دربرنامه نویسی",
            dayIndex = 3, day = "سه‌شنبه", start = "16:00", end = "19:00",
            teacher = "جواد یزدانجو", group = "2", code = "315022"
        )
        val summary = ScheduleComparator.compare(
            NormalizedSchedule(id = "input", classes = listOf(mine)),
            NormalizedSchedule(id = "uni", classes = listOf(anonymousSection))
        )
        val mineDiff = summary.diffs.single { it.currentClass != null }
        assertEquals(
            "Same name on another day without teacher/code is not a move",
            DiffType.REMOVED, mineDiff.diffType
        )

        val sameTeacherElsewhere = anonymousSection.copy(id = "cat-anon2", teacher = "مهندس جواد یزدانجو")
        val corroborated = ScheduleComparator.compare(
            NormalizedSchedule(id = "input", classes = listOf(mine)),
            NormalizedSchedule(id = "uni", classes = listOf(sameTeacherElsewhere))
        ).diffs.single { it.currentClass != null }
        assertNotNull("With the same teacher a day change reads as a move", corroborated.universityClass)
    }

    @Test
    fun `my schedule is enriched once so every screen sees the same location`() {
        val mine = offering(
            name = "پایگاه داده ها",
            dayIndex = 1, day = "یکشنبه", start = "14:00", end = "16:00",
            teacher = "جواد یزدانجو", group = "2", code = "315008"
        )
        val catalogEntry = offering(
            name = "پایگاه داده ها",
            dayIndex = 1, day = "یکشنبه", start = "14:00", end = "16:00",
            teacher = "یزدانجو", group = "2", classroom = "ساختمان ۳ - کلاس ۳۴۱", id = "cat-db"
        )
        val enriched = MainViewModel.enrichMySchedule(
            NormalizedSchedule(id = "input", classes = listOf(mine)),
            NormalizedSchedule(id = "uni", classes = listOf(catalogEntry))
        )
        assertEquals("ساختمان ۳ - کلاس ۳۴۱", enriched.classes.single().classroom)
        // User-provided values win over catalog values.
        assertEquals("جواد یزدانجو", enriched.classes.single().teacher)
        // Without catalog data the schedule passes through untouched.
        val untouched = MainViewModel.enrichMySchedule(
            NormalizedSchedule(id = "input", classes = listOf(mine)), null
        )
        assertEquals("", untouched.classes.single().classroom)
    }

    @Test
    fun `parity marker in teacher field does not count as a teacher change`() {
        val mine = offering(
            name = "فارسی",
            dayIndex = 0, day = "شنبه", start = "16:00", end = "18:00",
            teacher = "رویا یداللهی شاه راه", group = "1", code = "415007", parity = "زوج"
        )
        val pdf = offering(
            name = "فارسی",
            dayIndex = 0, day = "شنبه", start = "16:00", end = "18:00",
            teacher = "یداللهی شاه راه *زوج", group = "1", classroom = "ساختمان ۲ - کلاس ۲۲۳", id = "cat-fa"
        )
        val summary = ScheduleComparator.compare(
            NormalizedSchedule(id = "input", classes = listOf(mine)),
            NormalizedSchedule(id = "uni", classes = listOf(pdf))
        )
        val diff = summary.diffs.single()
        assertTrue("Expected no teacher diff, got ${diff.changes}", diff.changes.isEmpty())
    }
}
