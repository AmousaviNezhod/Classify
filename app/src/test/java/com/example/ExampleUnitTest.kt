package com.example

import com.example.domain.comparator.ScheduleComparator
import com.example.domain.model.DiffType
import com.example.domain.model.NormalizedSchedule
import com.example.domain.model.ScheduleClass
import com.example.domain.normalizer.PersianTextNormalizer
import com.example.domain.parser.JsonScheduleParser
import com.example.domain.parser.PdfScheduleParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleUnitTest {

    @Test
    fun testPersianTextNormalization() {
        val rawArabic = "دانشكده مهندسي كامپيوتر"
        val normalized = PersianTextNormalizer.normalizeText(rawArabic)
        assertEquals("دانشکده مهندسی کامپیوتر", normalized)

        val persianDigits = "۰۸:۳۰"
        val asciiDigits = PersianTextNormalizer.toAsciiDigits(persianDigits)
        assertEquals("08:30", asciiDigits)

        val timeRange = PersianTextNormalizer.extractTimeRange("از ۰۸:۰۰ الی ۱۰:۰۰")
        assertNotNull(timeRange)
        assertEquals("08:00", timeRange?.first)
        assertEquals("10:00", timeRange?.second)

        val (day, index) = PersianTextNormalizer.normalizeDay("سه شنبه")
        assertEquals("سه‌شنبه", day)
        assertEquals(3, index)
    }

    @Test
    fun testJsonScheduleParser() {
        val result = JsonScheduleParser.parse(JsonScheduleParser.DEFAULT_INPUT_JSON)
        assertTrue(result.isSuccess)
        val schedule = result.getOrNull()
        assertNotNull(schedule)
        assertTrue((schedule?.classes?.size ?: 0) >= 4)

        val firstClass = schedule?.classes?.firstOrNull()
        assertNotNull(firstClass)
        assertEquals("فارسی", firstClass?.courseName)
        assertEquals("رویا یداللهی شاه راه", firstClass?.teacher)
    }

    @Test
    fun testScheduleComparatorIdentifiesChanges() {
        val baseClass1 = ScheduleClass(
            id = "c1",
            courseName = "برنامه نویسی پیشرفته",
            dayOfWeek = "شنبه",
            dayIndex = 0,
            startTime = "08:00",
            endTime = "10:00",
            classroom = "101",
            groupCode = "1"
        )
        val baseClass2 = ScheduleClass(
            id = "c2",
            courseName = "پایگاه داده",
            dayOfWeek = "دوشنبه",
            dayIndex = 2,
            startTime = "10:00",
            endTime = "12:00",
            classroom = "204",
            groupCode = "1"
        )
        val currentSchedule = NormalizedSchedule(classes = listOf(baseClass1, baseClass2))

        // University schedule: class 1 is unchanged, class 2 has time changed to 14:00 - 16:00
        val uniClass1 = baseClass1.copy(id = "u1")
        val uniClass2 = baseClass2.copy(id = "u2", startTime = "14:00", endTime = "16:00")
        val uniClass3 = ScheduleClass(
            id = "u3",
            courseName = "هوش مصنوعی",
            dayOfWeek = "چهارشنبه",
            dayIndex = 4,
            startTime = "08:00",
            endTime = "10:00",
            classroom = "سایت 1",
            groupCode = "1"
        )
        val uniSchedule = NormalizedSchedule(classes = listOf(uniClass1, uniClass2, uniClass3))

        val summary = ScheduleComparator.compare(currentSchedule, uniSchedule)
        assertEquals(1, summary.unchangedCount)
        assertEquals(1, summary.modifiedCount)
        assertEquals(1, summary.addedCount)
        assertEquals(0, summary.removedCount)

        val modifiedDiff = summary.diffs.firstOrNull { it.diffType == DiffType.MODIFIED }
        assertNotNull(modifiedDiff)
        assertEquals("پایگاه داده", modifiedDiff?.courseName)
        assertTrue(modifiedDiff?.changes?.any { it.fieldName == "ساعت برگزاری" } == true)
    }

    @Test
    fun testPdfTextParserEngine() {
        val sampleScheduleText = """
            دانشکده مهندسی کامپیوتر
            برنامه هفتگی ترم ۱۴۰۴
            
            روز شنبه:
            ساعت 08:00 - 10:00 درس برنامه نویسی پیشرفته استاد دکتر حسینی کلاس 101 گروه 1
            ساعت 10:00 - 12:00 درس پایگاه داده استاد دکتر رضایی کلاس 204 گروه 1
        """.trimIndent()

        val parsedClasses = PdfScheduleParser.parseScheduleText(sampleScheduleText, "test_schedule")
        assertEquals(2, parsedClasses.size)

        val c1 = parsedClasses[0]
        assertEquals("برنامه نویسی پیشرفته", c1.courseName)
        assertEquals("شنبه", c1.dayOfWeek)
        assertEquals("08:00", c1.startTime)
        assertEquals("10:00", c1.endTime)
        assertTrue(c1.teacher.contains("حسینی"))
        assertEquals("کلاس 101", c1.classroom)
    }

    @Test
    fun testRealSadjadPdf() {
        val dir = java.io.File("/tmp/sadjad_pdfs")
        if (!dir.exists()) return
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val files = dir.listFiles { f -> f.extension == "pdf" }?.sortedBy { it.name } ?: return
        var totalExtracted = 0
        for (f in files) {
            val parsed = PdfScheduleParser.parsePdfFile(f, context = context)
            totalExtracted += parsed.size
            println("PDF: ${f.name} | ParsedClasses: ${parsed.size}")
        }
        println("=== NEW TOTAL EXTRACTED CLASSES ACROSS ALL 7 REAL SADJAD PDFS: $totalExtracted ===")
        assertTrue(totalExtracted > 950)
    }
}
