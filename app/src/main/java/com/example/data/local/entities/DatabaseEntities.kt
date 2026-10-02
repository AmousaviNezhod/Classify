package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "schedules")
data class ScheduleEntity(
    @PrimaryKey
    val id: String = "university_schedule",
    val title: String,
    val term: String,
    val sourceUrl: String,
    val updatedAt: Long,
    val totalCourses: Int,
    val rawJson: String = ""
)

@Entity(tableName = "schedule_classes")
data class ScheduleClassEntity(
    @PrimaryKey
    val id: String,
    val scheduleId: String,
    val courseName: String,
    val courseCode: String = "",
    val teacher: String = "",
    val dayOfWeek: String,
    val dayIndex: Int,
    val startTime: String,
    val endTime: String,
    val classroom: String = "",
    val groupCode: String = "",
    val parity: String = "",
    val units: Int = 0,
    val notes: String = ""
)

@Entity(tableName = "downloaded_pdfs")
data class DownloadedPdfEntity(
    @PrimaryKey
    val id: String,
    val url: String,
    val fileName: String,
    val localFilePath: String,
    val fileSize: Long,
    val etag: String? = null,
    val lastModified: String? = null,
    val sha256Hash: String? = null,
    val downloadTime: Long = System.currentTimeMillis(),
    val dayIndex: Int = -1,
    val parseStatus: String = "SUCCESS", // SUCCESS, FAILED, PENDING, EMPTY
    val parseError: String? = null,
    val extractedClassCount: Int = 0,
    val extractedWorkshopCount: Int = 0
)

@Entity(tableName = "schedule_day_availability")
data class ScheduleDayAvailabilityEntity(
    @PrimaryKey val dayIndex: Int,
    val dayName: String,
    val status: String,
    val discoveredPdfCount: Int = 0,
    val normalClassCount: Int = 0,
    val workshopClassCount: Int = 0,
    val sourceFileName: String = "",
    val sourceUrl: String = "",
    val checkedAt: Long = 0L
)

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey
    val key: String,
    val value: String
)
