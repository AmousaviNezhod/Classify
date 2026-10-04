package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.database.AppDatabase
import com.example.data.local.entities.DownloadedPdfEntity
import com.example.data.repository.UniversityScheduleRepository
import com.example.domain.model.PdfScheduleParseResult
import com.example.domain.model.ScheduleClass
import com.example.domain.parser.PdfScheduleParser
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LocalPdfReprocessingTest {
    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var pdfFile: File
    private lateinit var repository: UniversityScheduleRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        PdfScheduleParser.init(context)
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        pdfFile = File(context.cacheDir, "local-reprocess-fixture.pdf")
        pdfFile.writeBytes(minimalPdf)
        repository = UniversityScheduleRepository(
            context = context,
            databaseOverride = database,
            localPdfParser = { _, scheduleId, _ ->
                PdfScheduleParseResult(
                    normalClasses = listOf(
                        ScheduleClass(
                            scheduleId = scheduleId,
                            courseName = "فارسی",
                            courseCode = "۴۱۵۰۰۷",
                            dayOfWeek = "شنبه",
                            dayIndex = 0,
                            startTime = "08:00",
                            endTime = "10:00"
                        )
                    )
                )
            }
        )
        runBlocking {
            database.downloadedPdfDao().insertOrUpdatePdf(
                DownloadedPdfEntity(
                    id = "cached-pdf",
                    url = "file://cached-pdf",
                    fileName = pdfFile.name,
                    localFilePath = pdfFile.absolutePath,
                    fileSize = pdfFile.length(),
                    parseStatus = "FAILED",
                    extractedClassCount = 17
                )
            )
        }
    }

    @After
    fun tearDown() {
        pdfFile.delete()
        database.close()
    }

    @Test
    fun `reprocesses the stored PDF without fetching deleting or replacing the file`() = runBlocking {
        val originalBytes = pdfFile.readBytes()
        val originalEntity = database.downloadedPdfDao().getAllPdfsDirect().single()
        val result = repository.reprocessLocalPdfs()

        assertTrue("Reprocessing failed: ${result.exceptionOrNull()?.message}", result.isSuccess)
        assertEquals(originalBytes.toList(), pdfFile.readBytes().toList())
        val savedEntity = database.downloadedPdfDao().getAllPdfsDirect().single()
        assertEquals(originalEntity.id, savedEntity.id)
        assertEquals(originalEntity.localFilePath, savedEntity.localFilePath)
        assertEquals(originalEntity.url, savedEntity.url)
        assertEquals("SUCCESS", savedEntity.parseStatus)
        assertEquals(1, savedEntity.extractedClassCount)
        val classes = database.scheduleDao().getClassesDirect(UniversityScheduleRepository.SCHEDULE_ID_UNIVERSITY)
        assertEquals(1, classes.size)
        assertEquals("فارسی", classes.single().courseName)
    }

    private val minimalPdf = """%PDF-1.4
1 0 obj
<< /Type /Catalog /Pages 2 0 R >>
endobj
2 0 obj
<< /Type /Pages /Kids [3 0 R] /Count 1 >>
endobj
3 0 obj
<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>
endobj
4 0 obj
<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>
endobj
5 0 obj
<< /Length 57 >>
stream
BT /F1 12 Tf 50 700 Td (Saturday 08-10 Farsi Group 1) Tj ET
endstream
endobj
trailer
<< /Root 1 0 R >>
%%EOF
""".toByteArray(Charsets.ISO_8859_1)
}
