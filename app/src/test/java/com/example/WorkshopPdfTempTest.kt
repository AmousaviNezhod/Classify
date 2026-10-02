package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.domain.parser.PdfScheduleParser
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class WorkshopPdfTempTest {
    @Test
    fun parsesWorkshopPdfWithIndependentTextAndPositionMethods() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val file = File("C:/Users/AM!R/Downloads/Telegram Desktop/mehr405-az-site_2.pdf")
        assumeTrue("Local workshop PDF is available for the optional regression", file.exists())
        val parsed = PdfScheduleParser.parsePdfFile(file, "workshop-real-test", context)
        assertTrue("Sample PDF should yield lab/workshop offerings", parsed.isNotEmpty())
        assertTrue("Every weekday should be represented", parsed.map { it.dayIndex }.toSet().containsAll((0..5).toList()))
        assertTrue("Position-based rooms should be retained", parsed.any { it.classroom.contains("114") })
        assertTrue("Workshop classes should be retained", parsed.any { it.courseName.contains("کارگاه") })
        assertTrue("Every offering must have a real schedule name", parsed.none { it.courseName == "اتاق" || it.courseName == "ساعت" })
    }
}
