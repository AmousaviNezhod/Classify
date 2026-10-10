package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.data.local.entities.AppSettingsEntity
import com.example.data.local.entities.DownloadedPdfEntity
import com.example.data.local.entities.ScheduleClassEntity
import com.example.data.local.entities.ScheduleDayAvailabilityEntity
import com.example.data.local.entities.ScheduleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleDao {

    @Query("SELECT * FROM schedules WHERE id = :id LIMIT 1")
    fun getSchedule(id: String): Flow<ScheduleEntity?>

    @Query("SELECT * FROM schedules WHERE id = :id LIMIT 1")
    suspend fun getScheduleDirect(id: String): ScheduleEntity?

    @Query("SELECT * FROM schedule_classes WHERE scheduleId = :scheduleId ORDER BY dayIndex ASC, startTime ASC")
    fun getClassesForSchedule(scheduleId: String): Flow<List<ScheduleClassEntity>>

    @Query("SELECT * FROM schedule_classes WHERE scheduleId = :scheduleId ORDER BY dayIndex ASC, startTime ASC")
    suspend fun getClassesDirect(scheduleId: String): List<ScheduleClassEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: ScheduleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClasses(classes: List<ScheduleClassEntity>)

    @Query("DELETE FROM schedule_classes WHERE scheduleId = :scheduleId")
    suspend fun deleteClassesForSchedule(scheduleId: String)

    @Query("DELETE FROM schedules WHERE id = :scheduleId")
    suspend fun deleteSchedule(scheduleId: String)

    @Query("DELETE FROM schedule_classes")
    suspend fun clearAllClasses()

    @Query("DELETE FROM schedules")
    suspend fun clearAllSchedules()

    @Transaction
    suspend fun saveScheduleWithClasses(schedule: ScheduleEntity, classes: List<ScheduleClassEntity>) {
        deleteClassesForSchedule(schedule.id)
        insertSchedule(schedule)
        insertClasses(classes)
    }
}

@Dao
interface DownloadedPdfDao {

    @Query("SELECT * FROM downloaded_pdfs ORDER BY downloadTime DESC")
    fun getAllPdfs(): Flow<List<DownloadedPdfEntity>>

    @Query("SELECT * FROM downloaded_pdfs ORDER BY downloadTime DESC")
    suspend fun getAllPdfsDirect(): List<DownloadedPdfEntity>

    @Query("SELECT * FROM downloaded_pdfs WHERE url = :url LIMIT 1")
    suspend fun getPdfByUrl(url: String): DownloadedPdfEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdatePdf(pdf: DownloadedPdfEntity)

    @Query("DELETE FROM downloaded_pdfs WHERE id = :id")
    suspend fun deletePdfById(id: String)

    @Query("DELETE FROM downloaded_pdfs")
    suspend fun deleteAllPdfs()

    @Query("SELECT COUNT(*) FROM downloaded_pdfs")
    fun getPdfCount(): Flow<Int>

    @Query("UPDATE downloaded_pdfs SET weekNumber = :weekNumber, weekParity = :weekParity WHERE id = :id")
    suspend fun updatePdfWeek(id: String, weekNumber: Int?, weekParity: String)
}

@Dao
interface ScheduleDayAvailabilityDao {
    @Query("SELECT * FROM schedule_day_availability ORDER BY dayIndex ASC")
    fun observeAll(): Flow<List<ScheduleDayAvailabilityEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<ScheduleDayAvailabilityEntity>)

    @Query("DELETE FROM schedule_day_availability")
    suspend fun clearAll()
}

@Dao
interface SettingsDao {

    @Query("SELECT value FROM app_settings WHERE `key` = :key LIMIT 1")
    fun getSetting(key: String): Flow<String?>

    @Query("SELECT value FROM app_settings WHERE `key` = :key LIMIT 1")
    suspend fun getSettingDirect(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setSetting(setting: AppSettingsEntity)

    @Query("DELETE FROM app_settings WHERE `key` = :key")
    suspend fun deleteSetting(key: String)

    @Query("DELETE FROM app_settings")
    suspend fun clearAllSettings()
}

@Dao
interface CourseEventDao {
    @Query("SELECT * FROM course_events ORDER BY timestamp ASC, dateString ASC, timeString ASC")
    fun observeAllEvents(): Flow<List<com.example.data.local.entities.CourseEventEntity>>

    @Query("SELECT * FROM course_events WHERE courseKey = :courseKey ORDER BY timestamp ASC, dateString ASC")
    fun observeEventsForCourse(courseKey: String): Flow<List<com.example.data.local.entities.CourseEventEntity>>

    @Query("SELECT * FROM course_events ORDER BY timestamp ASC")
    suspend fun getAllEventsDirect(): List<com.example.data.local.entities.CourseEventEntity>

    @Query("SELECT * FROM course_events WHERE courseKey = :courseKey ORDER BY timestamp ASC")
    suspend fun getEventsForCourseDirect(courseKey: String): List<com.example.data.local.entities.CourseEventEntity>

    @Query("SELECT * FROM course_events WHERE id = :id LIMIT 1")
    suspend fun getEventById(id: String): com.example.data.local.entities.CourseEventEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: com.example.data.local.entities.CourseEventEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<com.example.data.local.entities.CourseEventEntity>)

    @Query("DELETE FROM course_events WHERE id = :id")
    suspend fun deleteEventById(id: String)

    @Query("DELETE FROM course_events WHERE courseKey = :courseKey")
    suspend fun deleteEventsForCourse(courseKey: String)

    @Query("DELETE FROM course_events")
    suspend fun deleteAllEvents()
}

@Dao
interface CourseTaskDao {
    @Query("SELECT * FROM course_tasks ORDER BY isDone ASC, deadlineTimestamp ASC, createdAt DESC")
    fun observeAllTasks(): Flow<List<com.example.data.local.entities.CourseTaskEntity>>

    @Query("SELECT * FROM course_tasks WHERE courseKey = :courseKey ORDER BY isDone ASC, deadlineTimestamp ASC, createdAt DESC")
    fun observeTasksForCourse(courseKey: String): Flow<List<com.example.data.local.entities.CourseTaskEntity>>

    @Query("SELECT * FROM course_tasks ORDER BY createdAt DESC")
    suspend fun getAllTasksDirect(): List<com.example.data.local.entities.CourseTaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: com.example.data.local.entities.CourseTaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<com.example.data.local.entities.CourseTaskEntity>)

    @Query("DELETE FROM course_tasks WHERE id = :id")
    suspend fun deleteTaskById(id: String)

    @Query("DELETE FROM course_tasks WHERE courseKey = :courseKey")
    suspend fun deleteTasksForCourse(courseKey: String)

    @Query("DELETE FROM course_tasks")
    suspend fun deleteAllTasks()
}

@Dao
interface CourseNoteDao {
    @Query("SELECT * FROM course_notes WHERE courseKey = :courseKey ORDER BY updatedAt DESC")
    fun observeNotesForCourse(courseKey: String): Flow<List<com.example.data.local.entities.CourseNoteEntity>>

    @Query("SELECT * FROM course_notes ORDER BY updatedAt DESC")
    suspend fun getAllNotesDirect(): List<com.example.data.local.entities.CourseNoteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: com.example.data.local.entities.CourseNoteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotes(notes: List<com.example.data.local.entities.CourseNoteEntity>)

    @Query("DELETE FROM course_notes WHERE id = :id")
    suspend fun deleteNoteById(id: String)

    @Query("DELETE FROM course_notes WHERE courseKey = :courseKey")
    suspend fun deleteNotesForCourse(courseKey: String)

    @Query("DELETE FROM course_notes")
    suspend fun deleteAllNotes()
}
