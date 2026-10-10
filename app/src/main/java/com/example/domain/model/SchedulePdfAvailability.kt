package com.example.domain.model

import androidx.compose.runtime.Immutable

enum class PdfAvailabilityStatus {
    UNKNOWN,
    AVAILABLE,
    NOT_FOUND,
    DOWNLOAD_FAILED,
    PARSE_FAILED,
    CHECK_FAILED
}

@Immutable
data class ScheduleDayAvailability(
    val dayIndex: Int,
    val dayName: String,
    val status: PdfAvailabilityStatus = PdfAvailabilityStatus.UNKNOWN,
    val discoveredPdfCount: Int = 0,
    val normalClassCount: Int = 0,
    val workshopClassCount: Int = 0,
    val sourceFileName: String = "",
    val sourceUrl: String = "",
    val checkedAt: Long = 0L,
    val weekNumber: Int? = null,
    val weekParity: String = ""
)

@Immutable
data class PdfScheduleParseResult(
    val normalClasses: List<ScheduleClass> = emptyList(),
    val workshopClasses: List<ScheduleClass> = emptyList(),
    val detectedTableCount: Int = 0,
    val detectedGroups: Set<String> = emptySet(),
    val diagnostics: List<String> = emptyList(),
    val detectedWeekNumber: Int? = null,
    val detectedWeekParity: String = ""
) {
    val classes: List<ScheduleClass> get() = normalClasses + workshopClasses
    val succeeded: Boolean get() = classes.isNotEmpty()
    val normalExtractionSucceeded: Boolean get() = normalClasses.isNotEmpty()
}
