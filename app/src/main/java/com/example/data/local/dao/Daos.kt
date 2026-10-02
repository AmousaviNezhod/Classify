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
