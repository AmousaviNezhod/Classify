package com.example

import com.example.domain.normalizer.PersianTextNormalizer
import com.example.domain.parser.PdfScheduleParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class UnlabeledTableRegressionTest {

    private fun row(page: Int, y: Int, cells: List<Pair<Int, String>>) =
        "@@ROW@@$page,$y@@" + cells.joinToString("") { "⟦${it.first}⟧${it.second}" }

    @Test
    fun `table without explicit group or room headers infers room column and extracts classes`() {
        val input = listOf(
            row(1, 40, listOf(110 to "برنامه کلاسی روز شنبه-زوج(ساختمان1)")),
            row(1, 80, listOf(38 to "18-20", 123 to "16-18", 208 to "14-16", 294 to "12-14", 379 to "10-12", 467 to "8-10")),
            row(1, 108, listOf(145 to "شبکه های عصبی ویادگیری عمیق", 282 to "مهندسی فاکتورهای انسانی", 375 to "مبانی مهندسی برق", 449 to "مقدمات فیزیولوژی ورزشی")),
            row(1, 117, listOf(546 to "121")),
            row(1, 121, listOf(161 to "گ 1 – نوری", 291 to "گ1-یوسفی*زوج", 381 to "گ2- یوسفی", 465 to "گ1- فراحتی"))
        ).joinToString("\n")

        val result = PdfScheduleParser.parsePositionedScheduleText(input, "unlabeled-grid-test")
        assertTrue("Should extract classes from unlabeled grid", result.normalClasses.isNotEmpty())
        val first = result.normalClasses.first()
        assertEquals("شنبه", first.dayOfWeek)
        assertEquals("زوج", first.parity)
        assertTrue(result.normalClasses.any { PersianTextNormalizer.toAsciiDigits(it.classroom).contains("121") })
        assertTrue(result.normalClasses.any { it.courseName.contains("عصبی") || it.courseName.contains("مهندسی") || it.courseName.contains("فیزیولوژی") })
    }
}
