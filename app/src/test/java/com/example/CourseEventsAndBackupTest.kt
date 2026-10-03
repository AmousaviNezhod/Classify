package com.example

import com.example.domain.manager.BackupAndImportManager
import com.example.domain.model.CourseEvent
import com.example.domain.model.CourseEventType
import com.example.domain.model.CourseNote
import com.example.domain.model.CourseTask
import com.example.domain.model.ScheduleClass
import com.example.ui.util.DateTimeUtils
import com.example.widget.ScheduleWidgetProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CourseEventsAndBackupTest {

    @Test
    fun testEventEntityMapping() {
        val event = CourseEvent(
            id = "test-123",
            courseKey = "os_1",
            courseName = "سیستم عامل",
            courseCode = "101",
            title = "میان‌ترم",
            type = CourseEventType.MIDTERM,
            dateString = "1403/08/20",
            timeString = "10:00",
            timestamp = 1732085400000L,
            description = "فصل‌های ۱ تا ۳",
            location = "کلاس ۲۰۴",
            reminderMinutesBefore = 2880,
            isCompleted = false
        )

        val entity = event.toEntity()
        assertEquals("test-123", entity.id)
        assertEquals("os_1", entity.courseKey)
        assertEquals("MIDTERM", entity.type)
        assertEquals(2880, entity.reminderMinutesBefore)

        val mappedBack = CourseEvent.fromEntity(entity)
        assertEquals(event.id, mappedBack.id)
        assertEquals(event.courseKey, mappedBack.courseKey)
        assertEquals(event.type, mappedBack.type)
        assertEquals(event.title, mappedBack.title)
        assertEquals(event.timestamp, mappedBack.timestamp)
    }

    @Test
    fun testTaskAndNoteEntityMapping() {
        val task = CourseTask(
            id = "task-1",
            courseKey = "db_1",
            courseName = "پایگاه داده",
            title = "حل تمرین SQL",
            isDone = false,
            deadlineString = "یکشنبه"
        )
        val taskEntity = task.toEntity()
        val mappedTask = CourseTask.fromEntity(taskEntity)
        assertEquals(task.title, mappedTask.title)
        assertFalse(mappedTask.isDone)

        val note = CourseNote(
            id = "note-1",
            courseKey = "db_1",
            courseName = "پایگاه داده",
            content = "استاد گفت امتحان نمره منفی ندارد."
        )
        val noteEntity = note.toEntity()
        val mappedNote = CourseNote.fromEntity(noteEntity)
        assertEquals(note.content, mappedNote.content)
    }

    @Test
    fun testEventsJsonExportAndImport() {
        val events = listOf(
            CourseEvent(
                id = "ev-1",
                courseKey = "math_1",
                courseName = "ریاضی عمومی",
                title = "کوئیز انتگرال",
                type = CourseEventType.QUIZ,
                dateString = "1403/09/10",
                timeString = "08:30",
                timestamp = 1733815800000L,
                reminderMinutesBefore = 1440
            ),
            CourseEvent(
                id = "ev-2",
                courseKey = "network_2",
                courseName = "شبکه‌های کامپیوتری",
                title = "ارائه سوکت",
                type = CourseEventType.PRESENTATION,
                dateString = "1403/09/15",
                timeString = "14:00",
                timestamp = 1734262200000L,
                reminderMinutesBefore = 4320
            )
        )

        val json = BackupAndImportManager.exportEventsJson(events)
        assertTrue(json.contains("classify-events"))
        assertTrue(json.contains("کوئیز انتگرال"))
        assertTrue(json.contains("ارائه سوکت"))

        val parsed = BackupAndImportManager.parseEventsJson(json)
        assertTrue(parsed.isSuccess)
        val list = parsed.getOrThrow()
        assertEquals(2, list.size)
        assertEquals(CourseEventType.QUIZ, list[0].type)
        assertEquals(CourseEventType.PRESENTATION, list[1].type)
    }

    @Test
    fun testFullBackupExportAndRestore() {
        val events = listOf(
            CourseEvent(title = "امتحان پایانی", type = CourseEventType.EXAM, courseKey = "c1")
        )
        val tasks = listOf(
            CourseTask(title = "پروژه نهایی", courseKey = "c1")
        )
        val notes = listOf(
            CourseNote(content = "نکته مهم", courseKey = "c1")
        )

        val backupJson = BackupAndImportManager.createFullBackup(
            sourceUrl = "https://uni.example.ac.ir",
            themeMode = "DARK",
            inputScheduleJson = "{\"courses\": []}",
            events = events,
            tasks = tasks,
            notes = notes
        )

        val parsedResult = BackupAndImportManager.parseFullBackup(backupJson)
        assertTrue(parsedResult.isSuccess)
        val data = parsedResult.getOrThrow()
        assertEquals("classify-full-backup", data.format)
        assertEquals("DARK", data.themeMode)
        assertEquals("https://uni.example.ac.ir", data.sourceUrl)
        assertEquals(1, data.events.size)
        assertEquals(1, data.tasks.size)
        assertEquals(1, data.notes.size)
        assertEquals("امتحان پایانی", data.events[0].title)
    }

    @Test
    fun testDateTimeUtilsCountdown() {
        val now = System.currentTimeMillis()
        val inOneHour = now + (60 * 60 * 1000L)
        val inThreeDays = now + (3 * 24 * 60 * 60 * 1000L)

        val countdownHour = DateTimeUtils.formatCountdown(inOneHour)
        val countdownThreeDays = DateTimeUtils.formatCountdown(inThreeDays)

        assertNotNull(countdownHour)
        assertTrue(countdownHour.isNotBlank())
        assertTrue(countdownThreeDays.contains("روز"))
    }

    @Test
    fun testWidgetPersianDayIndexConversion() {
        assertEquals(0, ScheduleWidgetProvider.getPersianDayIndex(Calendar.SATURDAY))
        assertEquals(1, ScheduleWidgetProvider.getPersianDayIndex(Calendar.SUNDAY))
        assertEquals(2, ScheduleWidgetProvider.getPersianDayIndex(Calendar.MONDAY))
        assertEquals(3, ScheduleWidgetProvider.getPersianDayIndex(Calendar.TUESDAY))
        assertEquals(4, ScheduleWidgetProvider.getPersianDayIndex(Calendar.WEDNESDAY))
        assertEquals(5, ScheduleWidgetProvider.getPersianDayIndex(Calendar.THURSDAY))
        assertEquals(6, ScheduleWidgetProvider.getPersianDayIndex(Calendar.FRIDAY))

        assertEquals("شنبه", ScheduleWidgetProvider.getDayNameFa(0))
        assertEquals("جمعه", ScheduleWidgetProvider.getDayNameFa(6))
    }

    @Test
    fun testAddCourseOfferingToEmptySchedule() {
        val course = ScheduleClass(
            id = "c1",
            courseName = "هوش مصنوعی",
            courseCode = "401",
            groupCode = "1",
            units = 3,
            dayIndex = 0,
            dayOfWeek = "شنبه",
            startTime = "10:00",
            endTime = "12:00",
            classroom = "۳۰۱",
            teacher = "دکتر رضایی"
        )
        val schedule = com.example.domain.model.NormalizedSchedule(
            id = "input_schedule",
            classes = listOf(course)
        )
        val json = com.example.domain.parser.JsonScheduleParser.toUnitSelectionJson(schedule)
        assertTrue(json.isNotBlank())
        val parsed = com.example.domain.parser.JsonScheduleParser.parse(json, "input_schedule")
        assertTrue(parsed.isSuccess)
        val parsedSchedule = parsed.getOrThrow()
        assertEquals(1, parsedSchedule.classes.size)
        assertEquals("هوش مصنوعی", parsedSchedule.classes[0].courseName)
        assertEquals("401", parsedSchedule.classes[0].courseCode)
    }
}
