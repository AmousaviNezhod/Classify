package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.local.PdfStorageManager
import com.example.data.local.database.AppDatabase
import com.example.data.local.entities.AppSettingsEntity
import com.example.data.local.entities.DownloadedPdfEntity
import com.example.data.local.entities.ScheduleClassEntity
import com.example.data.local.entities.ScheduleDayAvailabilityEntity
import com.example.data.local.entities.ScheduleEntity
import com.example.data.local.entities.toEntity
import com.example.data.remote.UniversityRemoteDataSource
import com.example.domain.comparator.ScheduleComparator
import com.example.domain.model.NormalizedSchedule
import com.example.domain.model.PdfAvailabilityStatus
import com.example.domain.model.PdfScheduleParseResult
import com.example.domain.model.ScheduleClass
import com.example.domain.model.ScheduleDayAvailability
import com.example.domain.model.ScheduleOfferingIdentity
import com.example.domain.model.ScheduleComparisonSummary
import com.example.domain.model.CourseEvent
import com.example.domain.model.CourseTask
import com.example.domain.model.CourseNote
import com.example.notification.ReminderNotificationManager
import com.example.domain.parser.JsonScheduleParser
import com.example.domain.parser.PdfScheduleParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.UUID

sealed class UpdateStatus {
    object Idle : UpdateStatus()
    data class Progress(val stage: String, val progressFraction: Float = -1f) : UpdateStatus()
    data class Success(val message: String, val isUpToDate: Boolean = false) : UpdateStatus()
    data class Error(val message: String) : UpdateStatus()
}

class UniversityScheduleRepository(
    private val context: Context,
    private val databaseOverride: AppDatabase? = null,
    private val remoteDataSource: UniversityRemoteDataSource = UniversityRemoteDataSource(),
    private val storageManager: PdfStorageManager = PdfStorageManager(context),
    private val localPdfParser: (File, String, Context) -> PdfScheduleParseResult = { file, scheduleId, appContext ->
        PdfScheduleParser.parsePdfFileDetailed(file, scheduleId, appContext)
    }
) {

    companion object {
        private const val TAG = "UniversityScheduleRepo"
        const val KEY_SOURCE_URL = "source_url"
        const val KEY_INPUT_JSON = "input_schedule_json"
        const val KEY_THEME_MODE = "theme_mode" // SYSTEM, LIGHT, DARK
        const val KEY_AUTO_CHECK = "auto_check_enabled"
        const val SCHEDULE_ID_UNIVERSITY = "university_schedule"
        const val SCHEDULE_ID_INPUT = "input_schedule"
    }

    // Lazily created so the heavy Room initialization (schema creation, integrity
    // checks) never runs on the main thread during app start-up.
    private val database: AppDatabase by lazy { databaseOverride ?: AppDatabase.getDatabase(context) }
    private val scheduleDao by lazy { database.scheduleDao() }
    private val pdfDao by lazy { database.downloadedPdfDao() }
    private val availabilityDao by lazy { database.scheduleDayAvailabilityDao() }
    private val settingsDao by lazy { database.settingsDao() }
    private val courseEventDao by lazy { database.courseEventDao() }
    private val courseTaskDao by lazy { database.courseTaskDao() }
    private val courseNoteDao by lazy { database.courseNoteDao() }

    private val _updateStatus = MutableStateFlow<UpdateStatus>(UpdateStatus.Idle)
    val updateStatus: StateFlow<UpdateStatus> = _updateStatus.asStateFlow()

    // ----------------------------------------------------
    // SCHEDULE DATA FLOWS
    // ----------------------------------------------------

    val universityScheduleFlow: Flow<NormalizedSchedule?> by lazy {
        combine(
            scheduleDao.getSchedule(SCHEDULE_ID_UNIVERSITY),
            scheduleDao.getClassesForSchedule(SCHEDULE_ID_UNIVERSITY),
            dayAvailabilityFlow
        ) { scheduleEntity, classEntities, availability ->
            if (scheduleEntity == null) null
            else {
                val classes = classEntities.map { it.toDomain() }
                val currentAvailability = availability.ifEmpty { defaultDayAvailability() }
                scheduleEntity.toDomain(classes).copy(dayAvailability = currentAvailability)
            }
        }.flowOn(Dispatchers.IO)
    }

    val downloadedPdfsFlow: Flow<List<DownloadedPdfEntity>> by lazy { pdfDao.getAllPdfs() }

    val dayAvailabilityFlow: Flow<List<ScheduleDayAvailability>> by lazy {
        availabilityDao.observeAll().map { rows ->
            if (rows.isNotEmpty()) rows.map { it.toDomain() }
            else defaultDayAvailability()
        }
    }

    private fun defaultDayAvailability() = dayNames.mapIndexed { index, name -> ScheduleDayAvailability(index, name) }

    private fun ScheduleDayAvailabilityEntity.toDomain() = ScheduleDayAvailability(
        dayIndex = dayIndex,
        dayName = dayName,
        status = runCatching { PdfAvailabilityStatus.valueOf(status) }.getOrDefault(PdfAvailabilityStatus.UNKNOWN),
        discoveredPdfCount = discoveredPdfCount,
        normalClassCount = normalClassCount,
        workshopClassCount = workshopClassCount,
        sourceFileName = sourceFileName,
        sourceUrl = sourceUrl,
        checkedAt = checkedAt
    )

    private suspend fun saveAvailability(items: List<ScheduleDayAvailability>) {
        availabilityDao.upsertAll(items.map { item ->
            ScheduleDayAvailabilityEntity(
                dayIndex = item.dayIndex,
                dayName = item.dayName,
                status = item.status.name,
                discoveredPdfCount = item.discoveredPdfCount,
                normalClassCount = item.normalClassCount,
                workshopClassCount = item.workshopClassCount,
                sourceFileName = item.sourceFileName,
                sourceUrl = item.sourceUrl,
                checkedAt = item.checkedAt
            )
        })
    }

    private val dayNames = listOf("شنبه", "یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنج‌شنبه")


    val sourceUrlFlow: Flow<String> by lazy {
        settingsDao.getSetting(KEY_SOURCE_URL)
            .map { it?.ifBlank { null } ?: UniversityRemoteDataSource.DEFAULT_SOURCE_URL }
    }

    // No default schedule: an empty/blank value simply means "not set yet" and
    // the UI shows the empty state instead of a fabricated sample schedule.
    val inputJsonFlow: Flow<String> by lazy {
        settingsDao.getSetting(KEY_INPUT_JSON).map { it ?: "" }
    }

    val themeModeFlow: Flow<String> by lazy {
        settingsDao.getSetting(KEY_THEME_MODE)
            .map { it?.ifBlank { null } ?: "SYSTEM" }
    }

    val allEventsFlow: Flow<List<CourseEvent>> by lazy {
        courseEventDao.observeAllEvents()
            .map { list -> list.map { CourseEvent.fromEntity(it) } }
            .flowOn(Dispatchers.IO)
    }

    fun eventsForCourseFlow(courseKey: String): Flow<List<CourseEvent>> {
        return courseEventDao.observeEventsForCourse(courseKey)
            .map { list -> list.map { CourseEvent.fromEntity(it) } }
            .flowOn(Dispatchers.IO)
    }

    val allTasksFlow: Flow<List<CourseTask>> by lazy {
        courseTaskDao.observeAllTasks()
            .map { list -> list.map { CourseTask.fromEntity(it) } }
            .flowOn(Dispatchers.IO)
    }

    fun tasksForCourseFlow(courseKey: String): Flow<List<CourseTask>> {
        return courseTaskDao.observeTasksForCourse(courseKey)
            .map { list -> list.map { CourseTask.fromEntity(it) } }
            .flowOn(Dispatchers.IO)
    }

    fun notesForCourseFlow(courseKey: String): Flow<List<CourseNote>> {
        return courseNoteDao.observeNotesForCourse(courseKey)
            .map { list -> list.map { CourseNote.fromEntity(it) } }
            .flowOn(Dispatchers.IO)
    }

    // ----------------------------------------------------
    // ACTIONS & SETTINGS
    // ----------------------------------------------------

    suspend fun setSourceUrl(url: String) = withContext(Dispatchers.IO) {
        settingsDao.setSetting(AppSettingsEntity(KEY_SOURCE_URL, url.trim()))
    }

    suspend fun resetSourceUrl() = withContext(Dispatchers.IO) {
        settingsDao.setSetting(AppSettingsEntity(KEY_SOURCE_URL, UniversityRemoteDataSource.DEFAULT_SOURCE_URL))
    }

    suspend fun setInputJson(json: String) = withContext(Dispatchers.IO) {
        settingsDao.setSetting(AppSettingsEntity(KEY_INPUT_JSON, json.trim()))
    }

    suspend fun setThemeMode(mode: String) = withContext(Dispatchers.IO) {
        settingsDao.setSetting(AppSettingsEntity(KEY_THEME_MODE, mode))
    }

    fun dismissUpdateStatus() {
        _updateStatus.value = UpdateStatus.Idle
    }

    fun showErrorMessage(message: String) {
        _updateStatus.value = UpdateStatus.Error(message)
    }

    fun showSuccessMessage(message: String) {
        _updateStatus.value = UpdateStatus.Success(message)
    }

    suspend fun clearCachedSchedule() = withContext(Dispatchers.IO) {
        scheduleDao.clearAllSchedules()
        scheduleDao.clearAllClasses()
    }

    suspend fun clearDownloadedPdfs() = withContext(Dispatchers.IO) {
        pdfDao.deleteAllPdfs()
        availabilityDao.clearAll()
        storageManager.deleteAllPdfs()
    }

    /**
     * Deletes a single PDF: removes the file from disk, its database record, and
     * rebuilds the university schedule from the remaining PDFs so the classes
     * extracted from the deleted file disappear at the same time.
     */
    suspend fun deletePdf(pdf: DownloadedPdfEntity): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            _updateStatus.value = UpdateStatus.Progress("در حال حذف فایل و داده‌های آن...")

            try {
                File(pdf.localFilePath).delete()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to delete pdf file: ${pdf.localFilePath}", e)
            }
            pdfDao.deletePdfById(pdf.id)

            // Rebuild university schedule from the PDFs that are still present.
            val remaining = pdfDao.getAllPdfsDirect().filter { File(it.localFilePath).exists() }
            if (remaining.isEmpty()) {
                scheduleDao.clearAllClasses()
                scheduleDao.clearAllSchedules()
            } else {
                val parsedClasses = mutableListOf<ScheduleClass>()
                for (remainingPdf in remaining) {
                    parsedClasses.addAll(
                        PdfScheduleParser.parsePdfFile(
                            File(remainingPdf.localFilePath),
                            SCHEDULE_ID_UNIVERSITY,
                            context
                        )
                    )
                }
                val distinctClasses = parsedClasses.distinctBy(ScheduleOfferingIdentity::key)

                if (distinctClasses.isEmpty()) {
                    scheduleDao.clearAllClasses()
                    scheduleDao.clearAllSchedules()
                } else {
                    val existing = scheduleDao.getScheduleDirect(SCHEDULE_ID_UNIVERSITY)
                    val scheduleEntity = existing?.copy(
                        updatedAt = System.currentTimeMillis(),
                        totalCourses = distinctClasses.map { it.courseName }.distinct().size
                    ) ?: ScheduleEntity(
                        id = SCHEDULE_ID_UNIVERSITY,
                        title = "برنامه کلاسی دانشگاه",
                        term = "۱۴۰۴-۱۴۰۵-۱",
                        sourceUrl = settingsDao.getSettingDirect(KEY_SOURCE_URL)
                            ?: UniversityRemoteDataSource.DEFAULT_SOURCE_URL,
                        updatedAt = System.currentTimeMillis(),
                        totalCourses = distinctClasses.map { it.courseName }.distinct().size,
                        rawJson = ""
                    )
                    scheduleDao.saveScheduleWithClasses(
                        scheduleEntity,
                        distinctClasses.map { it.toEntity(SCHEDULE_ID_UNIVERSITY) }
                    )
                }
            }

            _updateStatus.value = UpdateStatus.Success("فایل «${pdf.fileName}» و داده‌های آن حذف شد.")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting pdf", e)
            _updateStatus.value = UpdateStatus.Error("حذف فایل انجام نشد.")
            Result.failure(e)
        }
    }

    // ----------------------------------------------------
    // COURSE EVENTS, TASKS & NOTES
    // ----------------------------------------------------

    suspend fun upsertEvent(event: CourseEvent) = withContext(Dispatchers.IO) {
        courseEventDao.insertEvent(event.toEntity())
        if (event.reminderMinutesBefore >= 0 && !event.isCompleted && event.timestamp > System.currentTimeMillis()) {
            ReminderNotificationManager.scheduleReminder(context, event)
        } else {
            ReminderNotificationManager.cancelReminder(context, event.id)
        }
    }

    suspend fun deleteEvent(eventId: String) = withContext(Dispatchers.IO) {
        ReminderNotificationManager.cancelReminder(context, eventId)
        courseEventDao.deleteEventById(eventId)
    }

    suspend fun toggleEventCompleted(event: CourseEvent) = withContext(Dispatchers.IO) {
        val updated = event.copy(isCompleted = !event.isCompleted)
        upsertEvent(updated)
    }

    suspend fun upsertTask(task: CourseTask) = withContext(Dispatchers.IO) {
        courseTaskDao.insertTask(task.toEntity())
    }

    suspend fun deleteTask(taskId: String) = withContext(Dispatchers.IO) {
        courseTaskDao.deleteTaskById(taskId)
    }

    suspend fun toggleTaskDone(task: CourseTask) = withContext(Dispatchers.IO) {
        val updated = task.copy(isDone = !task.isDone)
        courseTaskDao.insertTask(updated.toEntity())
    }

    suspend fun upsertNote(note: CourseNote) = withContext(Dispatchers.IO) {
        courseNoteDao.insertNote(note.toEntity())
    }

    suspend fun deleteNote(noteId: String) = withContext(Dispatchers.IO) {
        courseNoteDao.deleteNoteById(noteId)
    }

    suspend fun deleteCourseCascade(courseKey: String) = withContext(Dispatchers.IO) {
        // 1. Remove course from input schedule JSON
        val currentJson = inputJsonFlow.firstOrNull() ?: ""
        if (currentJson.isNotBlank()) {
            val parsed = JsonScheduleParser.parse(currentJson, SCHEDULE_ID_INPUT).getOrNull()
            if (parsed != null) {
                val remainingClasses = parsed.classes.filterNot {
                    it.semanticKey == courseKey ||
                    ScheduleOfferingIdentity.key(it) == courseKey ||
                    "${it.courseName.trim().lowercase()}_${it.groupCode.trim().ifEmpty { "0" }}" == courseKey
                }
                if (remainingClasses.isEmpty()) {
                    setInputJson("")
                } else {
                    val updatedSchedule = parsed.copy(classes = remainingClasses)
                    setInputJson(JsonScheduleParser.toUnitSelectionJson(updatedSchedule))
                }
            }
        }

        // 2. Cancel reminders for all events of this course
        val events = courseEventDao.getEventsForCourseDirect(courseKey)
        events.forEach {
            ReminderNotificationManager.cancelReminder(context, it.id)
        }

        // 3. Delete all dependent events, tasks, notes
        courseEventDao.deleteEventsForCourse(courseKey)
        courseTaskDao.deleteTasksForCourse(courseKey)
        courseNoteDao.deleteNotesForCourse(courseKey)

        _updateStatus.value = UpdateStatus.Success("درس و تمام رویدادها، وظایف و یادداشت‌های وابسته حذف شدند.")
    }

    suspend fun getAllEventsDirect(): List<CourseEvent> = withContext(Dispatchers.IO) {
        courseEventDao.getAllEventsDirect().map { CourseEvent.fromEntity(it) }
    }

    suspend fun getAllTasksDirect(): List<CourseTask> = withContext(Dispatchers.IO) {
        courseTaskDao.getAllTasksDirect().map { CourseTask.fromEntity(it) }
    }

    suspend fun getAllNotesDirect(): List<CourseNote> = withContext(Dispatchers.IO) {
        courseNoteDao.getAllNotesDirect().map { CourseNote.fromEntity(it) }
    }

    suspend fun insertImportedEvents(events: List<CourseEvent>) = withContext(Dispatchers.IO) {
        courseEventDao.insertEvents(events.map { it.toEntity() })
        events.forEach { event ->
            if (event.reminderMinutesBefore >= 0 && !event.isCompleted && event.timestamp > System.currentTimeMillis()) {
                ReminderNotificationManager.scheduleReminder(context, event)
            }
        }
    }

    suspend fun insertImportedTasks(tasks: List<CourseTask>) = withContext(Dispatchers.IO) {
        courseTaskDao.insertTasks(tasks.map { it.toEntity() })
    }

    suspend fun insertImportedNotes(notes: List<CourseNote>) = withContext(Dispatchers.IO) {
        courseNoteDao.insertNotes(notes.map { it.toEntity() })
    }

    suspend fun resetAllData() = withContext(Dispatchers.IO) {
        clearCachedSchedule()
        clearDownloadedPdfs()
        settingsDao.clearAllSettings()
        val allEvents = courseEventDao.getAllEventsDirect()
        allEvents.forEach { ReminderNotificationManager.cancelReminder(context, it.id) }
        courseEventDao.deleteAllEvents()
        courseTaskDao.deleteAllTasks()
        courseNoteDao.deleteAllNotes()
    }

    fun getStorageSizeFormatted(): String {
        return storageManager.formatFileSize(storageManager.getTotalStorageSizeBytes())
    }

    fun getDownloadedPdfsCount(): Int {
        return storageManager.getDownloadedFilesCount()
    }

    // ----------------------------------------------------
    // SCHEDULE COMPARISON
    // ----------------------------------------------------

    suspend fun getScheduleComparison(): ScheduleComparisonSummary? = withContext(Dispatchers.IO) {
        val uniSchedule = universityScheduleFlow.firstOrNull() ?: return@withContext null
        val inputJson = inputJsonFlow.firstOrNull() ?: ""
        if (inputJson.isBlank()) return@withContext null
        val parsedInputSchedule = JsonScheduleParser.parse(inputJson, SCHEDULE_ID_INPUT).getOrNull()
            ?: return@withContext null

        ScheduleComparator.compare(
            currentSchedule = parsedInputSchedule,
            universitySchedule = uniSchedule
        )
    }

    // ----------------------------------------------------
    // SYNC & FETCH WORKFLOW (Incremental)
    // ----------------------------------------------------

    suspend fun checkAndUpdateSchedule(forceRedownload: Boolean = false): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            _updateStatus.value = UpdateStatus.Progress("در حال دریافت فهرست فایل‌ها...")

            val currentSourceUrl = settingsDao.getSettingDirect(KEY_SOURCE_URL)
                ?.ifBlank { null } ?: UniversityRemoteDataSource.DEFAULT_SOURCE_URL

            if (forceRedownload) {
                _updateStatus.value = UpdateStatus.Progress("در حال پاکسازی کش قبلی...")
                clearDownloadedPdfs()
            }

            _updateStatus.value = UpdateStatus.Progress("در حال جستجوی فایل‌های برنامه در وبسایت...")

            // Try fetching from the remote university website
            val discoveryResult = remoteDataSource.fetchAndDiscoverPdfLinks(currentSourceUrl)

            if (discoveryResult.isFailure) {
                val error = discoveryResult.exceptionOrNull() ?: Exception("دریافت فهرست برنامه‌ها ناموفق بود.")
                val checkedAt = System.currentTimeMillis()
                val previous = dayAvailabilityFlow.firstOrNull().orEmpty().associateBy(ScheduleDayAvailability::dayIndex)
                saveAvailability(defaultDayAvailability().map { day ->
                    val old = previous[day.dayIndex]
                    old?.takeIf { it.status == PdfAvailabilityStatus.AVAILABLE }
                        ?.copy(checkedAt = checkedAt)
                        ?: day.copy(status = PdfAvailabilityStatus.CHECK_FAILED, checkedAt = checkedAt)
                })
                Log.w(TAG, "Remote discovery failed: ${error.message}")
                _updateStatus.value = UpdateStatus.Error(error.message ?: "خبر برنامه کلاس‌ها در سایت پیدا نشد.")
                return@withContext Result.failure(error)
            }
            val discoveredPdfs = discoveryResult.getOrThrow()
            val checkedAt = System.currentTimeMillis()
            val previous = dayAvailabilityFlow.firstOrNull().orEmpty().associateBy(ScheduleDayAvailability::dayIndex)
            val dayStates = defaultDayAvailability().map { day ->
                previous[day.dayIndex]
                    ?.takeIf { it.status == PdfAvailabilityStatus.AVAILABLE }
                    ?.copy(checkedAt = checkedAt)
                    ?: day.copy(status = PdfAvailabilityStatus.NOT_FOUND, checkedAt = checkedAt)
            }.toMutableList()
            val discoveredByDay = discoveredPdfs.mapNotNull { pdf ->
                val day = dayIndexFromText(listOf(pdf.linkText, pdf.surroundingContext, pdf.suggestedFileName, pdf.url).joinToString(" "))
                if (day in dayNames.indices) day to pdf else null
            }.groupBy({ it.first }, { it.second })
            discoveredByDay.forEach { (day, pdfs) ->
                val existing = dayStates[day]
                dayStates[day] = existing.copy(
                    status = if (existing.status == PdfAvailabilityStatus.AVAILABLE) existing.status else PdfAvailabilityStatus.UNKNOWN,
                    discoveredPdfCount = pdfs.size,
                    sourceFileName = pdfs.first().suggestedFileName,
                    sourceUrl = pdfs.first().url,
                    checkedAt = checkedAt
                )
            }
            saveAvailability(dayStates)

            if (discoveredPdfs.isEmpty()) {
                val errorMsg = "صفحهٔ برنامه بررسی شد؛ هنوز PDF روزهای هفته بارگذاری نشده است."
                _updateStatus.value = UpdateStatus.Error(errorMsg)
                return@withContext Result.failure(Exception(errorMsg))
            }

            var downloadedCount = 0
            var reusedCount = 0
            val processedClasses = mutableListOf<ScheduleClass>()
            val dayResults = mutableMapOf<Int, MutableList<com.example.domain.model.PdfScheduleParseResult>>()

            for ((index, discovered) in discoveredPdfs.withIndex()) {
                val progressFraction = (index + 1).toFloat() / discoveredPdfs.size
                _updateStatus.value = UpdateStatus.Progress(
                    "در حال دریافت PDFها (${index + 1} از ${discoveredPdfs.size}): ${discovered.suggestedFileName}",
                    progressFraction
                )

                val cachedPdfEntity = pdfDao.getPdfByUrl(discovered.url)
                val targetTerm = discovered.semesterOrTerm

                var localFile: File? = null
                var finalEtag = cachedPdfEntity?.etag
                var finalLastModified = cachedPdfEntity?.lastModified
                var finalHash = cachedPdfEntity?.sha256Hash

                // Check if already downloaded and not forcing
                if (!forceRedownload && cachedPdfEntity != null && File(cachedPdfEntity.localFilePath).exists()) {
                    // Test if changed using HTTP HEAD / conditional GET
                    val downloadResult = remoteDataSource.downloadPdf(
                        discovered.url,
                        cachedEtag = cachedPdfEntity.etag,
                        cachedLastModified = cachedPdfEntity.lastModified
                    )

                    if (downloadResult.isSuccess && downloadResult.getOrNull() == null) {
                        // 304 Not Modified: Reuse existing file!
                        localFile = File(cachedPdfEntity.localFilePath)
                        reusedCount++
                        Log.d(TAG, "Reusing cached PDF: ${cachedPdfEntity.fileName}")
                    } else if (downloadResult.isSuccess && downloadResult.getOrNull() != null) {
                        val downloaded = downloadResult.getOrNull()!!
                        localFile = storageManager.savePdf(downloaded.inputStream, targetTerm, downloaded.fileName)
                        finalEtag = downloaded.etag
                        finalLastModified = downloaded.lastModified
                        finalHash = storageManager.computeSha256(localFile)
                        downloadedCount++
                    } else if (File(cachedPdfEntity.localFilePath).exists()) {
                        // Network error during re-check: reuse the last known local copy.
                        localFile = File(cachedPdfEntity.localFilePath)
                        reusedCount++
                    } else {
                        val day = dayIndexFromText(listOf(discovered.linkText, discovered.surroundingContext, discovered.suggestedFileName, discovered.url).joinToString(" "))
                        if (day in dayNames.indices) {
                            if (dayStates[day].status != PdfAvailabilityStatus.AVAILABLE) {
                                dayStates[day] = dayStates[day].copy(status = PdfAvailabilityStatus.DOWNLOAD_FAILED, checkedAt = System.currentTimeMillis())
                                saveAvailability(dayStates)
                            }
                        }
                        continue
                    }
                } else {
                    // Needs fresh download
                    val downloadResult = remoteDataSource.downloadPdf(discovered.url)
                    if (downloadResult.isSuccess && downloadResult.getOrNull() != null) {
                        val downloaded = downloadResult.getOrNull()!!
                        localFile = storageManager.savePdf(downloaded.inputStream, targetTerm, downloaded.fileName)
                        finalEtag = downloaded.etag
                        finalLastModified = downloaded.lastModified
                        finalHash = storageManager.computeSha256(localFile)
                        downloadedCount++
                    } else {
                        val day = dayIndexFromText(listOf(discovered.linkText, discovered.surroundingContext, discovered.suggestedFileName, discovered.url).joinToString(" "))
                        if (day in dayNames.indices) {
                            if (dayStates[day].status != PdfAvailabilityStatus.AVAILABLE) {
                                dayStates[day] = dayStates[day].copy(status = PdfAvailabilityStatus.DOWNLOAD_FAILED, checkedAt = System.currentTimeMillis())
                                saveAvailability(dayStates)
                            }
                        }
                        Log.w(TAG, "Failed to download ${discovered.suggestedFileName}: ${downloadResult.exceptionOrNull()?.message}")
                        continue
                    }
                }

                // Parse the PDF
                if (localFile != null && localFile.exists()) {
                    _updateStatus.value = UpdateStatus.Progress("در حال خواندن PDF و استخراج کلاس‌ها: ${localFile.name}")
                    val parseResult = PdfScheduleParser.parsePdfFileDetailed(localFile, SCHEDULE_ID_UNIVERSITY, context)
                    val extracted = parseResult.classes
                    Log.d(TAG, "PDF parse ${localFile.name}: normal=${parseResult.normalClasses.size}, workshops=${parseResult.workshopClasses.size}, tables=${parseResult.detectedTableCount}, groups=${parseResult.detectedGroups}")
                    parseResult.diagnostics.take(3).forEach { Log.d(TAG, "PDF row: $it") }

                    val discoveredDay = dayIndexFromText(listOf(discovered.linkText, discovered.surroundingContext, discovered.suggestedFileName, discovered.url, localFile.name).joinToString(" "))
                    val fileDay = discoveredDay.takeIf { it in dayNames.indices }
                        ?: extracted.firstOrNull()?.dayIndex?.takeIf { it in dayNames.indices }
                        ?: -1
                    val stateDay = fileDay.takeIf { it in dayNames.indices }
                        ?: dayIndexFromText(listOf(discovered.linkText, discovered.surroundingContext, discovered.suggestedFileName).joinToString(" "))
                    if (stateDay in dayNames.indices) {
                        val resultsForDay = dayResults.getOrPut(stateDay) { mutableListOf() }
                        resultsForDay.add(parseResult)
                        dayStates[stateDay] = dayStates[stateDay].copy(
                            status = if (resultsForDay.any { it.succeeded }) PdfAvailabilityStatus.AVAILABLE else PdfAvailabilityStatus.PARSE_FAILED,
                            discoveredPdfCount = maxOf(dayStates[stateDay].discoveredPdfCount, 1),
                            normalClassCount = resultsForDay.sumOf { it.normalClasses.size },
                            workshopClassCount = resultsForDay.sumOf { it.workshopClasses.size },
                            sourceFileName = localFile.name,
                            sourceUrl = discovered.url,
                            checkedAt = System.currentTimeMillis()
                        )
                        saveAvailability(dayStates)
                    }
                    val pdfEntity = DownloadedPdfEntity(
                        id = cachedPdfEntity?.id ?: UUID.randomUUID().toString(),
                        url = discovered.url,
                        fileName = localFile.name,
                        localFilePath = localFile.absolutePath,
                        fileSize = localFile.length(),
                        etag = finalEtag,
                        lastModified = finalLastModified,
                        sha256Hash = finalHash,
                        downloadTime = System.currentTimeMillis(),
                        dayIndex = fileDay,
                        parseStatus = if (extracted.isNotEmpty()) "SUCCESS" else "FAILED",
                        parseError = parseResult.diagnostics.firstOrNull().takeIf { extracted.isEmpty() },
                        extractedClassCount = parseResult.normalClasses.size,
                        extractedWorkshopCount = parseResult.workshopClasses.size
                    )
                    pdfDao.insertOrUpdatePdf(pdfEntity)
                    processedClasses.addAll(extracted)
                }
            }

            if (processedClasses.isEmpty()) {
                saveAvailability(dayStates)
                val errorMsg = "PDF دریافت شد، اما جدول‌های برنامه از آن استخراج نشدند. وضعیت فایل‌ها را بررسی کنید."
                _updateStatus.value = UpdateStatus.Error(errorMsg)
                return@withContext Result.failure(Exception(errorMsg))
            }

            saveAvailability(dayStates)
            _updateStatus.value = UpdateStatus.Progress("در حال یکسان‌سازی مشخصات کلاس‌ها...")
            val normalizedClasses = processedClasses.map { course ->
                course.copy(
                    courseCode = com.example.domain.normalizer.PersianTextNormalizer.toAsciiDigits(course.courseCode),
                    groupCode = com.example.domain.normalizer.PersianTextNormalizer.toAsciiDigits(course.groupCode),
                    startTime = com.example.domain.normalizer.PersianTextNormalizer.normalizeTime(course.startTime),
                    endTime = com.example.domain.normalizer.PersianTextNormalizer.normalizeTime(course.endTime)
                )
            }
            _updateStatus.value = UpdateStatus.Progress("در حال حذف موارد کاملاً تکراری...")
            val distinctClasses = normalizedClasses.distinctBy(ScheduleOfferingIdentity::key)
            _updateStatus.value = UpdateStatus.Progress("در حال ساخت فهرست همهٔ ارائه‌های کلاسی...")

            val scheduleEntity = ScheduleEntity(
                id = SCHEDULE_ID_UNIVERSITY,
                title = "برنامه کلاسی دانشگاه",
                term = discoveredPdfs.firstOrNull()?.semesterOrTerm ?: "۱۴۰۴-۱۴۰۵-۱",
                sourceUrl = currentSourceUrl,
                updatedAt = System.currentTimeMillis(),
                totalCourses = distinctClasses.map { it.courseName }.distinct().size,
                rawJson = ""
            )

            val classEntities = distinctClasses.map { it.toEntity(SCHEDULE_ID_UNIVERSITY) }
            _updateStatus.value = UpdateStatus.Progress("در حال ذخیره‌سازی فهرست آماده‌شده...")
            scheduleDao.saveScheduleWithClasses(scheduleEntity, classEntities)

            // Recalculate true per-day counts so multi-day workshop PDFs don't lump all workshops into Saturday.
            val recalculatedAvailability = dayStates.map { day ->
                val classesForDay = distinctClasses.filter { it.dayIndex == day.dayIndex }
                val normalCount = classesForDay.count { !it.isWorkshop }
                val workshopCount = classesForDay.count { it.isWorkshop }
                if (classesForDay.isNotEmpty()) {
                    day.copy(
                        status = PdfAvailabilityStatus.AVAILABLE,
                        normalClassCount = normalCount,
                        workshopClassCount = workshopCount,
                        checkedAt = System.currentTimeMillis()
                    )
                } else {
                    day.copy(
                        normalClassCount = 0,
                        workshopClassCount = 0,
                        checkedAt = System.currentTimeMillis()
                    )
                }
            }
            saveAvailability(recalculatedAvailability)

            val isUpToDate = downloadedCount == 0 && reusedCount > 0

            val successMessage = if (isUpToDate) {
                "برنامه شما در حال حاضر به‌روز است."
            } else {
                "برنامه با موفقیت به‌روز شد ($downloadedCount فایل جدید، $reusedCount فایل بدون تغییر)."
            }

            _updateStatus.value = UpdateStatus.Progress("آماده است؛ ${distinctClasses.size} ارائهٔ کلاسی در فهرست قرار گرفت.")
            _updateStatus.value = UpdateStatus.Success(successMessage, isUpToDate)
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating schedule", e)
            val errorMsg = "دریافت برنامه انجام نشد.\nساختار صفحه دانشگاه تغییر کرده یا در حال حاضر دسترسی به سایت امکان‌پذیر نیست."
            _updateStatus.value = UpdateStatus.Error(errorMsg)
            Result.failure(e)
        }
    }

    /**
     * Manually imports a PDF file from an input stream (e.g. from user file picker).
     */
    suspend fun importManualPdf(
        fileName: String,
        inputStream: java.io.InputStream
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            _updateStatus.value = UpdateStatus.Progress("در حال وارد کردن و ذخیره فایل PDF...")
            val term = "۱۴۰۵-۱۴۰۶-۱"
            val targetFile = storageManager.getTermDirectory(term).resolve(fileName)
            targetFile.outputStream().use { out ->
                inputStream.copyTo(out)
            }

            _updateStatus.value = UpdateStatus.Progress("در حال استخراج کلاس‌ها از فایل پی‌دی‌اف...")
            val parsedResult = PdfScheduleParser.parsePdfFileDetailed(targetFile, SCHEDULE_ID_UNIVERSITY, context)
            val extractedClasses = parsedResult.classes
            if (extractedClasses.isEmpty()) {
                val manualDay = dayIndexFromText(fileName)
                if (manualDay in dayNames.indices) {
                    val availability = dayAvailabilityFlow.firstOrNull().orEmpty().ifEmpty { defaultDayAvailability() }
                    saveAvailability(availability.filterNot { it.dayIndex == manualDay } +
                        availability[manualDay].copy(
                            status = PdfAvailabilityStatus.PARSE_FAILED,
                            discoveredPdfCount = 1,
                            sourceFileName = fileName,
                            sourceUrl = "file://$fileName",
                            checkedAt = System.currentTimeMillis()
                        ))
                }
                val errorMsg = "فایل دریافت شد اما کلاسی از آن استخراج نشد."
                _updateStatus.value = UpdateStatus.Error(errorMsg)
                return@withContext Result.failure(Exception(errorMsg))
            }

            // Save PDF entity
            val pdfEntity = DownloadedPdfEntity(
                id = UUID.randomUUID().toString(),
                url = "file://$fileName",
                fileName = fileName,
                localFilePath = targetFile.absolutePath,
                fileSize = targetFile.length(),
                downloadTime = System.currentTimeMillis(),
                dayIndex = extractedClasses.firstOrNull()?.dayIndex ?: -1,
                parseStatus = if (parsedResult.succeeded) "SUCCESS" else "FAILED",
                parseError = parsedResult.diagnostics.firstOrNull().takeIf { !parsedResult.succeeded },
                extractedClassCount = parsedResult.normalClasses.size,
                extractedWorkshopCount = parsedResult.workshopClasses.size
            )
            pdfDao.insertOrUpdatePdf(pdfEntity)

            // Save classes into schedule
            val existingClassEntities = scheduleDao.getClassesDirect(SCHEDULE_ID_UNIVERSITY)
            val existingClasses = existingClassEntities.map { it.toDomain() }

            val combined = (existingClasses + extractedClasses)
                .map { course -> course.copy(groupCode = com.example.domain.normalizer.PersianTextNormalizer.toAsciiDigits(course.groupCode)) }
                .distinctBy(ScheduleOfferingIdentity::key)

            val currentSourceUrl = settingsDao.getSettingDirect(KEY_SOURCE_URL) ?: UniversityRemoteDataSource.DEFAULT_SOURCE_URL
            val scheduleEntity = ScheduleEntity(
                id = SCHEDULE_ID_UNIVERSITY,
                title = "برنامه کلاسی دانشگاه",
                term = term,
                sourceUrl = currentSourceUrl,
                updatedAt = System.currentTimeMillis(),
                totalCourses = combined.map { it.courseName }.distinct().size,
                rawJson = ""
            )

            scheduleDao.saveScheduleWithClasses(scheduleEntity, combined.map { it.toEntity(SCHEDULE_ID_UNIVERSITY) })
            val currentAvailability = dayAvailabilityFlow.firstOrNull().orEmpty().ifEmpty { defaultDayAvailability() }.toMutableList()
            defaultDayAvailability().forEach { day ->
                val classesForDay = combined.filter { it.dayIndex == day.dayIndex }
                if (classesForDay.isNotEmpty()) {
                    val normalCount = classesForDay.count { !it.isWorkshop }
                    val workshopCount = classesForDay.count { it.isWorkshop }
                    val idx = currentAvailability.indexOfFirst { it.dayIndex == day.dayIndex }
                    val old = if (idx >= 0) currentAvailability[idx] else day
                    val updated = old.copy(
                        status = PdfAvailabilityStatus.AVAILABLE,
                        discoveredPdfCount = maxOf(old.discoveredPdfCount, 1),
                        normalClassCount = normalCount,
                        workshopClassCount = workshopCount,
                        sourceFileName = if (extractedClasses.any { it.dayIndex == day.dayIndex }) fileName else old.sourceFileName,
                        sourceUrl = if (extractedClasses.any { it.dayIndex == day.dayIndex }) "file://$fileName" else old.sourceUrl,
                        checkedAt = System.currentTimeMillis()
                    )
                    if (idx >= 0) currentAvailability[idx] = updated else currentAvailability.add(updated)
                }
            }
            saveAvailability(currentAvailability)

            _updateStatus.value = UpdateStatus.Success("فایل با موفقیت وارد شد و ${extractedClasses.size} کلاس اضافه گردید.", false)
            Result.success(extractedClasses.size)
        } catch (e: Exception) {
            Log.e(TAG, "Error importing PDF manually", e)
            val errorMsg = "خطا در وارد کردن فایل PDF: ${e.message}"
            _updateStatus.value = UpdateStatus.Error(errorMsg)
            Result.failure(e)
        }
    }

    /**
     * Re-extracts classes from the PDFs already stored on this device — no
     * network, no downloads, no deletions — and rebuilds the matched schedule.
     */
    suspend fun reprocessLocalPdfs(): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            _updateStatus.value = UpdateStatus.Progress("در حال بازخوانی PDFهای ذخیره‌شده...")
            val cachedFiles = pdfDao.getAllPdfsDirect().mapNotNull { entity ->
                File(entity.localFilePath).takeIf(File::exists)?.let { entity to it }
            }
            if (cachedFiles.isEmpty()) {
                val msg = "PDF ذخیره‌شده‌ای روی دستگاه نیست؛ ابتدا یک‌بار برنامه را دریافت کنید."
                _updateStatus.value = UpdateStatus.Error(msg)
                return@withContext Result.failure(Exception(msg))
            }

            val processedClasses = mutableListOf<ScheduleClass>()
            for ((index, entry) in cachedFiles.withIndex()) {
                val (pdfEntity, file) = entry
                _updateStatus.value = UpdateStatus.Progress(
                    "در حال استخراج دوباره (${index + 1} از ${cachedFiles.size}): ${file.name}",
                    (index + 1).toFloat() / cachedFiles.size
                )
                val parseResult = localPdfParser(file, SCHEDULE_ID_UNIVERSITY, context)
                val extracted = parseResult.classes
                processedClasses.addAll(extracted)
                pdfDao.insertOrUpdatePdf(
                    pdfEntity.copy(
                        parseStatus = if (extracted.isNotEmpty()) "SUCCESS" else "FAILED",
                        parseError = parseResult.diagnostics.firstOrNull().takeIf { extracted.isEmpty() },
                        extractedClassCount = parseResult.normalClasses.size,
                        extractedWorkshopCount = parseResult.workshopClasses.size
                    )
                )
            }

            if (processedClasses.isEmpty()) {
                val msg = "PDFها بازخوانی شدند اما کلاسی از آن‌ها استخراج نشد."
                _updateStatus.value = UpdateStatus.Error(msg)
                return@withContext Result.failure(Exception(msg))
            }

            _updateStatus.value = UpdateStatus.Progress("در حال یکسان‌سازی مشخصات کلاس‌ها...")
            val normalizedClasses = processedClasses.map { course ->
                course.copy(
                    courseCode = com.example.domain.normalizer.PersianTextNormalizer.toAsciiDigits(course.courseCode),
                    groupCode = com.example.domain.normalizer.PersianTextNormalizer.toAsciiDigits(course.groupCode),
                    startTime = com.example.domain.normalizer.PersianTextNormalizer.normalizeTime(course.startTime),
                    endTime = com.example.domain.normalizer.PersianTextNormalizer.normalizeTime(course.endTime)
                )
            }
            val distinctClasses = normalizedClasses.distinctBy(ScheduleOfferingIdentity::key)

            val existingSchedule = scheduleDao.getScheduleDirect(SCHEDULE_ID_UNIVERSITY)
            val scheduleEntity = ScheduleEntity(
                id = SCHEDULE_ID_UNIVERSITY,
                title = "برنامه کلاسی دانشگاه",
                term = existingSchedule?.term ?: "۱۴۰۴-۱۴۰۵-۱",
                sourceUrl = existingSchedule?.sourceUrl
                    ?: settingsDao.getSettingDirect(KEY_SOURCE_URL)?.ifBlank { null }
                    ?: UniversityRemoteDataSource.DEFAULT_SOURCE_URL,
                updatedAt = System.currentTimeMillis(),
                totalCourses = distinctClasses.map { it.courseName }.distinct().size,
                rawJson = ""
            )
            _updateStatus.value = UpdateStatus.Progress("در حال ذخیره‌سازی فهرست آماده‌شده...")
            scheduleDao.saveScheduleWithClasses(scheduleEntity, distinctClasses.map { it.toEntity(SCHEDULE_ID_UNIVERSITY) })

            val availability = dayAvailabilityFlow.firstOrNull().orEmpty().ifEmpty { defaultDayAvailability() }
            val existingAvailability = availability.associateBy(ScheduleDayAvailability::dayIndex)
            val parsedAvailability = defaultDayAvailability().map { day ->
                val classesForDay = distinctClasses.filter { it.dayIndex == day.dayIndex }
                val previous = existingAvailability[day.dayIndex] ?: day
                if (classesForDay.isEmpty()) previous
                else previous.copy(
                    status = PdfAvailabilityStatus.AVAILABLE,
                    normalClassCount = classesForDay.count { !it.isWorkshop },
                    workshopClassCount = classesForDay.count { it.isWorkshop },
                    checkedAt = System.currentTimeMillis()
                )
            }
            saveAvailability(parsedAvailability)

            _updateStatus.value = UpdateStatus.Success("داده‌ها از PDFهای ذخیره‌شده دوباره استخراج شد (${distinctClasses.size} کلاس).", false)
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error reprocessing local PDFs", e)
            _updateStatus.value = UpdateStatus.Error("بازاستخراج داده‌ها انجام نشد: ${e.message}")
            Result.failure(e)
        }
    }

    // ----------------------------------------------------
    // MAPPER EXTENSIONS
    // ----------------------------------------------------

    private fun ScheduleEntity.toDomain(classes: List<ScheduleClass>): NormalizedSchedule = NormalizedSchedule(
        id = id,
        title = title,
        term = term,
        sourceUrl = sourceUrl,
        updatedAt = updatedAt,
        classes = classes,
        rawJson = rawJson
    )

    private fun dayIndexFromText(value: String): Int {
        val decoded = runCatching { URLDecoder.decode(value, StandardCharsets.UTF_8.name()) }.getOrDefault(value)
        val normalized = com.example.domain.normalizer.PersianTextNormalizer.normalizeText(decoded).replace(" ", "")
        return when {
            normalized.contains("پنجشنبه") -> 5
            normalized.contains("چهارشنبه") -> 4
            normalized.contains("سهشنبه") -> 3
            normalized.contains("دوشنبه") -> 2
            normalized.contains("یکشنبه") -> 1
            normalized.contains("شنبه") -> 0
            else -> -1
        }
    }
}
