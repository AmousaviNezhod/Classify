package com.example

import com.example.domain.model.ScheduleClass
import com.example.domain.model.ScheduleOfferingIdentity
import com.example.domain.normalizer.PersianTextNormalizer
import com.example.domain.parser.PdfScheduleParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PdfScheduleParserGridTest {
    private fun row(page: Int, y: Int, cells: List<Pair<Int, String>>) =
        "@@ROW@@$page,$y@@" + cells.joinToString("") { "⟦${it.first}⟧${it.second}" }

    @Test
    fun `bare numeric cells cannot be returned as course names`() {
        val result = PdfScheduleParser.parsePositionedScheduleText(
            listOf(
                row(1, 10, listOf(100 to "برنامه کلاسی روز شنبه زوج (ساختمان ۱)")),
                row(1, 20, listOf(100 to "۸-۱۰", 200 to "۱۰-۱۲", 300 to "۱۲-۱۴", 400 to "۱۴-۱۶", 500 to "۱۶-۱۸", 600 to "۱۸-۲۰")),
                row(1, 30, listOf(100 to "۲۳۰", 700 to "۱"))
            ).joinToString("\n"),
            "numeric-cell-test"
        )
        assertTrue(result.classes.none { it.courseName.matches(Regex("[0-9۰-۹]+")) })
    }

    @Test
    fun `positioned header labels keep group room course day and time in their fields`() {
        val result = PdfScheduleParser.parsePositionedScheduleText(
            listOf(
                row(1, 10, listOf(100 to "برنامه کلاسی روز شنبه زوج ساختمان ۲")),
                row(1, 15, listOf(700 to "گروه", 800 to "اتاق")),
                row(1, 20, listOf(100 to "۸-۱۰", 200 to "۱۰-۱۲", 300 to "۱۲-۱۴", 400 to "۱۴-۱۶", 500 to "۱۶-۱۸", 600 to "۱۸-۲۰")),
                row(1, 30, listOf(100 to "فارسی", 700 to "۲", 800 to "۲/۲۳۰"))
            ).joinToString("\n"),
            "labeled-grid-test"
        )

        val offering = result.normalClasses.single()
        assertEquals("فارسی", offering.courseName)
        assertEquals("2", offering.groupCode)
        assertEquals("ساختمان 2 - کلاس/اتاق 230", offering.classroom)
        assertEquals("شنبه", offering.dayOfWeek)
        assertEquals(0, offering.dayIndex)
        assertEquals("08:00", offering.startTime)
        assertEquals("10:00", offering.endTime)
    }

    @Test
    fun `Persian Arabic letters and zero-width marks normalize before strict match`() {
        val normalized = PersianTextNormalizer.normalizeCourseName("  فا\u200Cرسي\n")
        assertEquals("فارسی", normalized)
        val imported = ScheduleClass(
            courseName = normalized,
            courseCode = "۴۱۵۰۰۷",
            dayOfWeek = "شنبه",
            dayIndex = 0,
            startTime = "08:00",
            endTime = "10:00",
            groupCode = "۰۱"
        )
        val catalog = imported.copy(courseName = "فارسی", courseCode = "415007", groupCode = "1")
        assertNotNull(ScheduleOfferingIdentity.findCatalogMatch(imported, listOf(catalog)))
        assertFalse(ScheduleOfferingIdentity.searchText(imported).isBlank())
    }

    @Test
    fun `unrelated class at same time never matches`() {
        val imported = ScheduleClass(
            courseName = "فارسی",
            dayOfWeek = "شنبه",
            dayIndex = 0,
            startTime = "08:00",
            endTime = "10:00"
        )
        val unrelated = imported.copy(courseName = "ریاضی", classroom = "Room 230")
        assertEquals(null, ScheduleOfferingIdentity.findCatalogMatch(imported, listOf(unrelated)))
    }
}
