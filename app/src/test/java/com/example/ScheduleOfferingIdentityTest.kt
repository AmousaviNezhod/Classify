package com.example

import com.example.domain.model.ScheduleClass
import com.example.domain.model.ScheduleOfferingIdentity
import com.example.domain.normalizer.PersianTextNormalizer
import com.example.domain.parser.JsonScheduleParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ScheduleOfferingIdentityTest {
    @Test
    fun `same code offerings remain distinct while exact duplicates collapse`() {
        val json = """
            {"courses":[
              {"code":"ABC123","name":"Programming","group":"1","professor":"A","classroom":"Lab 2","schedule":[{"day":"شنبه","start":"08:00","end":"10:00"}]},
              {"code":"ABC123","name":"Programming","group":"2","professor":"A","classroom":"Lab 2","schedule":[{"day":"شنبه","start":"10:00","end":"12:00"}]},
              {"code":"ABC123","name":"Programming","group":"3","professor":"B","classroom":"Room 4","schedule":[{"day":"یکشنبه","start":"08:00","end":"10:00"}]},
              {"code":"ABC123","name":"Programming","group":"1","professor":"A","classroom":"Lab 2","schedule":[{"day":"شنبه","start":"08:00","end":"10:00"}]},
              {"code":"ABC123","name":"Programming","group":"1","professor":"A","classroom":"Lab 3","schedule":[{"day":"شنبه","start":"08:00","end":"10:00"}]}
            ]}
        """.trimIndent()

        val offerings = JsonScheduleParser.parse(json).getOrThrow().classes
        assertEquals(4, offerings.size)
        assertEquals(4, offerings.map(ScheduleOfferingIdentity::key).distinct().size)
        assertEquals(setOf("1", "2", "3"), offerings.map { it.groupCode }.toSet())
        assertEquals(setOf("Lab 2", "Lab 3", "Room 4"), offerings.map { it.classroom }.toSet())
    }

    @Test
    fun `json without room matches one pdf room by normalized course and meeting`() {
        val imported = ScheduleClass(
            courseName = "آزمایشگاه برنامه نویسی",
            courseCode = "۴۱۵۰۰۷",
            teacher = "",
            dayOfWeek = "سه شنبه",
            dayIndex = 3,
            startTime = "9",
            endTime = "11",
            groupCode = "۰۱"
        )
        val pdfOffering = ScheduleClass(
            courseName = "آزمایشگاه برنامه‌نویسی",
            courseCode = "415007",
            teacher = "استاد PDF",
            dayOfWeek = "سه‌شنبه",
            dayIndex = 3,
            startTime = "09:00",
            endTime = "11:00",
            classroom = "آزمایشگاه ۲",
            groupCode = "1"
        )

        val enriched = ScheduleOfferingIdentity.enrichFromCatalog(imported, listOf(pdfOffering))
        assertEquals("آزمایشگاه ۲", enriched.classroom)
        assertEquals("استاد PDF", enriched.teacher)
    }

    @Test
    fun `ambiguous time match does not assign a possibly incorrect classroom`() {
        val imported = ScheduleClass(
            courseName = "Programming",
            courseCode = "123",
            teacher = "",
            dayOfWeek = "شنبه",
            dayIndex = 0,
            startTime = "08:00",
            endTime = "10:00"
        )
        val candidates = listOf(
            imported.copy(classroom = "Room 1", groupCode = "1"),
            imported.copy(classroom = "Room 2", groupCode = "2")
        )

        assertEquals(null, ScheduleOfferingIdentity.findCatalogMatch(imported, candidates))
    }

    @Test
    fun `search includes course code day time location and teacher`() {
        val offering = ScheduleClass(
            courseName = "Programming Lab",
            courseCode = "12345",
            teacher = "Sara",
            dayOfWeek = "شنبه",
            dayIndex = 0,
            startTime = "08:00",
            endTime = "12:30",
            classroom = "آزمایشگاه",
            groupCode = "2"
        )
        val searchable = ScheduleOfferingIdentity.searchText(offering)
        listOf("programming", "12345", "شنبه", "08:00", "12:30", "آزمایشگاه", "sara", "2").forEach { query ->
            val normalizedQuery = PersianTextNormalizer.toAsciiDigits(
                PersianTextNormalizer.normalizeText(query)
            ).lowercase()
            assertTrue("Search text did not contain $query", searchable.contains(normalizedQuery))
        }
    }
}
