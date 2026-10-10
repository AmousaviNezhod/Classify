package com.example.ui

import android.app.Application
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entities.DownloadedPdfEntity
import com.example.data.repository.UniversityScheduleRepository
import com.example.data.repository.UpdateStatus
import com.example.domain.model.NormalizedSchedule
import com.example.domain.model.ScheduleClass
import com.example.domain.model.ScheduleOfferingIdentity
import com.example.domain.model.ScheduleComparisonSummary
import com.example.domain.model.CourseEvent
import com.example.domain.model.CourseTask
import com.example.domain.model.CourseNote
import com.example.domain.calendar.EducationalWeekConfig
import com.example.domain.calendar.PersianCalendarHelper
import com.example.domain.manager.BackupAndImportManager
import com.example.domain.manager.FullBackupData
import com.example.widget.ScheduleWidgetProvider
import com.example.domain.parser.JsonScheduleParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        /**
         * Fills classroom/teacher/parity blanks of the imported schedule from the
         * university catalog in one place, so Today, Calendar and Courses all show
         * the same locations instead of "مکان مشخص نشده".
         */
        fun enrichMySchedule(schedule: NormalizedSchedule, catalog: NormalizedSchedule?): NormalizedSchedule {
            val catalogClasses = catalog?.classes.orEmpty()
            if (catalogClasses.isEmpty()) return schedule
            val catalogByDay = catalogClasses.groupBy { it.dayIndex }
            return schedule.copy(
                classes = schedule.classes.map {
                    ScheduleOfferingIdentity.enrichFromCatalog(it, catalogByDay[it.dayIndex].orEmpty())
                }
            )
        }
    }

    private val repository = UniversityScheduleRepository(application.applicationContext)

    val educationalWeekConfig: StateFlow<EducationalWeekConfig> = repository.educationalWeekConfigFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = repository.getFastCachedEduWeekConfig()
        )

    val schedule: StateFlow<NormalizedSchedule?> = repository.universityScheduleFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = repository.getFastCachedUniversitySchedule()
        )

    val mySchedule: StateFlow<NormalizedSchedule?> = combine(
        repository.inputJsonFlow,
        repository.universityScheduleFlow
    ) { json, catalog ->
        val parsed = JsonScheduleParser.parse(json, UniversityScheduleRepository.SCHEDULE_ID_INPUT).getOrNull()
        if (parsed == null) null else enrichMySchedule(parsed, catalog)
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = repository.getFastCachedMySchedule()
        )

    val inputJson: StateFlow<String> = repository.inputJsonFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = repository.getFastCachedInputJson()
        )

    fun setEducationalWeek(weekNumber: Int, forJdn: Long = PersianCalendarHelper.getTodayJdn()) {
        viewModelScope.launch {
            repository.setEducationalWeekConfig(forJdn, weekNumber)
        }
    }

    fun updatePdfWeekNumber(pdfId: String, weekNumber: Int?) {
        viewModelScope.launch {
            repository.updatePdfWeekNumber(pdfId, weekNumber)
        }
    }

    private val _selectedScheduleTab = MutableStateFlow(0) // 0 = My Schedule, 1 = University Master
    val selectedScheduleTab: StateFlow<Int> = _selectedScheduleTab.asStateFlow()

    fun setSelectedScheduleTab(tab: Int) {
        _selectedScheduleTab.value = tab
    }

    val updateStatus: StateFlow<UpdateStatus> = repository.updateStatus

    val dayAvailability: StateFlow<List<com.example.domain.model.ScheduleDayAvailability>> = repository.dayAvailabilityFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val downloadedPdfs: StateFlow<List<DownloadedPdfEntity>> = repository.downloadedPdfsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val sourceUrl: StateFlow<String> = repository.sourceUrlFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ""
        )

    val themeMode: StateFlow<String> = repository.themeModeFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "SYSTEM"
        )

    val allEvents: StateFlow<List<CourseEvent>> = repository.allEventsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allTasks: StateFlow<List<CourseTask>> = repository.allTasksFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _selectedDayIndex = MutableStateFlow<Int?>(null) // null = all days
    val selectedDayIndex: StateFlow<Int?> = _selectedDayIndex.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedParity = MutableStateFlow("") // "" = all, "زوج", "فرد"
    val selectedParity: StateFlow<String> = _selectedParity.asStateFlow()

    private val _comparisonSummary = MutableStateFlow<ScheduleComparisonSummary?>(null)
    val comparisonSummary: StateFlow<ScheduleComparisonSummary?> = _comparisonSummary.asStateFlow()

    private val _storageSize = MutableStateFlow("۰ بایت")
    val storageSize: StateFlow<String> = _storageSize.asStateFlow()

    private val _pdfCount = MutableStateFlow(0)
    val pdfCount: StateFlow<Int> = _pdfCount.asStateFlow()

    init {
        refreshStorageStats()
    }

    fun triggerUpdate(forceRedownload: Boolean = false) {
        viewModelScope.launch {
            repository.checkAndUpdateSchedule(forceRedownload)
            refreshStorageStats()
            loadComparison()
        }
    }

    fun reprocessLocalPdfs() {
        viewModelScope.launch {
            repository.reprocessLocalPdfs()
            refreshStorageStats()
            loadComparison()
            ScheduleWidgetProvider.updateAllWidgets(getApplication())
        }
    }

    fun setSelectedDay(dayIndex: Int?) {
        _selectedDayIndex.value = dayIndex
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedParity(parity: String) {
        _selectedParity.value = parity
    }

    fun importPdf(uri: Uri) {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>().applicationContext
                val contentResolver = context.contentResolver

                // Resolve file name
                var fileName = "imported_${System.currentTimeMillis()}.pdf"
                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1 && cursor.moveToFirst()) {
                        cursor.getString(nameIndex)?.let { fileName = it }
                    }
                }

                val inputStream = contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    repository.importManualPdf(fileName, inputStream)
                    refreshStorageStats()
                    loadComparison()
                }
            } catch (e: Exception) {
                // Handled in repository updateStatus
            }
        }
    }

    fun loadComparison() {
        viewModelScope.launch {
            _comparisonSummary.value = repository.getScheduleComparison()
        }
    }

    fun updateSourceUrl(url: String) {
        viewModelScope.launch {
            repository.setSourceUrl(url)
        }
    }

    fun resetSourceUrl() {
        viewModelScope.launch {
            repository.resetSourceUrl()
        }
    }

    fun updateInputJson(json: String) {
        val parsedResult = JsonScheduleParser.parse(json, UniversityScheduleRepository.SCHEDULE_ID_INPUT)
        val imported = parsedResult.getOrNull()
        if (parsedResult.isFailure || imported?.classes.isNullOrEmpty()) {
            repository.showErrorMessage("JSON معتبر با حداقل یک کلاس وارد کنید.")
            return
        }

        val catalog = schedule.value?.classes.orEmpty()
        val enrichedJson = if (catalog.isEmpty()) {
            json
        } else {
            val enrichedClasses = imported!!.classes.map { offering ->
                ScheduleOfferingIdentity.enrichFromCatalog(offering, catalog)
            }
            JsonScheduleParser.toUnitSelectionJson(imported.copy(classes = enrichedClasses))
        }

        viewModelScope.launch {
            repository.setInputJson(enrichedJson)
            repository.showSuccessMessage(
                if (catalog.isEmpty()) "برنامه ذخیره شد؛ برای تطبیق محل کلاس‌ها ابتدا داده‌های PDF دانشگاه را دریافت کنید."
                else "برنامه ذخیره و با اطلاعات استاد و محل کلاس‌های دانشگاه تطبیق داده شد."
            )
            loadComparison()
        }
    }

    fun addCourseOffering(course: ScheduleClass) {
        val existing = JsonScheduleParser.parse(inputJson.value, UniversityScheduleRepository.SCHEDULE_ID_INPUT)
            .getOrNull()?.classes.orEmpty()
        val canonicalCourse = schedule.value?.classes?.firstOrNull {
            ScheduleOfferingIdentity.key(it) == ScheduleOfferingIdentity.key(course)
        } ?: course
        val offeringKey = ScheduleOfferingIdentity.key(canonicalCourse)
        if (existing.any { ScheduleOfferingIdentity.key(it) == offeringKey }) return
        saveCourseOfferings(existing + canonicalCourse)
    }

    fun removeCourseOffering(course: ScheduleClass) {
        val offeringKey = ScheduleOfferingIdentity.key(course)
        val existing = JsonScheduleParser.parse(inputJson.value, UniversityScheduleRepository.SCHEDULE_ID_INPUT)
            .getOrNull()?.classes.orEmpty()
        saveCourseOfferings(existing.filterNot { ScheduleOfferingIdentity.key(it) == offeringKey })
    }

    private fun saveCourseOfferings(classes: List<ScheduleClass>) {
        viewModelScope.launch {
            if (classes.isEmpty()) {
                repository.setInputJson("")
                repository.showSuccessMessage("همهٔ درس‌ها حذف شدند.")
            } else {
                val catalog = schedule.value?.classes.orEmpty()
                val enrichedClasses = classes.map { offering ->
                    ScheduleOfferingIdentity.enrichFromCatalog(offering, catalog)
                }
                val scheduleToSave = NormalizedSchedule(
                    id = UniversityScheduleRepository.SCHEDULE_ID_INPUT,
                    classes = enrichedClasses.distinctBy(ScheduleOfferingIdentity::key)
                )
                repository.setInputJson(JsonScheduleParser.toUnitSelectionJson(scheduleToSave))
                repository.showSuccessMessage("درس‌های من به‌روز شد.")
            }
            ScheduleWidgetProvider.updateAllWidgets(getApplication())
            loadComparison()
        }
    }

    // ----------------------------------------------------
    // EVENTS, TASKS & NOTES OPERATIONS
    // ----------------------------------------------------

    fun eventsForCourse(courseKey: String): Flow<List<CourseEvent>> = repository.eventsForCourseFlow(courseKey)

    fun tasksForCourse(courseKey: String): Flow<List<CourseTask>> = repository.tasksForCourseFlow(courseKey)

    fun notesForCourse(courseKey: String): Flow<List<CourseNote>> = repository.notesForCourseFlow(courseKey)

    fun upsertEvent(event: CourseEvent) {
        viewModelScope.launch {
            repository.upsertEvent(event)
            ScheduleWidgetProvider.updateAllWidgets(getApplication())
        }
    }

    fun deleteEvent(eventId: String) {
        viewModelScope.launch {
            repository.deleteEvent(eventId)
            ScheduleWidgetProvider.updateAllWidgets(getApplication())
        }
    }

    fun toggleEventCompleted(event: CourseEvent) {
        viewModelScope.launch {
            repository.toggleEventCompleted(event)
            ScheduleWidgetProvider.updateAllWidgets(getApplication())
        }
    }

    fun upsertTask(task: CourseTask) {
        viewModelScope.launch {
            repository.upsertTask(task)
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            repository.deleteTask(taskId)
        }
    }

    fun toggleTaskDone(task: CourseTask) {
        viewModelScope.launch {
            repository.toggleTaskDone(task)
        }
    }

    fun upsertNote(note: CourseNote) {
        viewModelScope.launch {
            repository.upsertNote(note)
        }
    }

    fun deleteNote(noteId: String) {
        viewModelScope.launch {
            repository.deleteNote(noteId)
        }
    }

    fun deleteCourseCascade(courseKey: String) {
        viewModelScope.launch {
            repository.deleteCourseCascade(courseKey)
            ScheduleWidgetProvider.updateAllWidgets(getApplication())
            loadComparison()
        }
    }

    // ----------------------------------------------------
    // EXPORT / IMPORT / BACKUP
    // ----------------------------------------------------

    fun exportEventsJson(): String {
        return BackupAndImportManager.exportEventsJson(allEvents.value)
    }

    fun exportScheduleJson(): String {
        return inputJson.value.ifBlank {
            mySchedule.value?.let { JsonScheduleParser.toUnitSelectionJson(it) } ?: ""
        }
    }

    fun importEventsFromJson(json: String, createMissingCourses: Boolean = false, onFinished: (imported: Int, unmatched: Int) -> Unit) {
        viewModelScope.launch {
            val parseResult = BackupAndImportManager.parseEventsJson(json)
            val parsedEvents = parseResult.getOrNull()
            if (parseResult.isFailure || parsedEvents.isNullOrEmpty()) {
                repository.showErrorMessage("فایل رویدادها نامعتبر است یا رویدادی یافت نشد.")
                onFinished(0, 0)
                return@launch
            }

            val currentCourses = mySchedule.value?.classes.orEmpty()
            val courseMap = currentCourses.associateBy { it.semanticKey }

            var importedCount = 0
            var unmatchedCount = 0
            val eventsToInsert = mutableListOf<CourseEvent>()
            val coursesToCreate = mutableListOf<ScheduleClass>()

            for (event in parsedEvents) {
                // Try matching by courseKey, or courseName
                val matchedCourse = courseMap[event.courseKey]
                    ?: currentCourses.firstOrNull { it.courseName.trim().equals(event.courseName.trim(), ignoreCase = true) }

                if (matchedCourse != null) {
                    eventsToInsert.add(event.copy(
                        courseKey = matchedCourse.semanticKey,
                        courseName = matchedCourse.courseName,
                        courseCode = matchedCourse.courseCode.ifBlank { event.courseCode }
                    ))
                    importedCount++
                } else if (createMissingCourses && event.courseName.isNotBlank()) {
                    val newClass = ScheduleClass(
                        courseName = event.courseName,
                        courseCode = event.courseCode,
                        dayOfWeek = "شنبه",
                        startTime = "08:00",
                        endTime = "10:00"
                    )
                    coursesToCreate.add(newClass)
                    eventsToInsert.add(event.copy(courseKey = newClass.semanticKey))
                    importedCount++
                } else {
                    unmatchedCount++
                }
            }

            if (coursesToCreate.isNotEmpty()) {
                val updatedCourses = currentCourses + coursesToCreate
                saveCourseOfferings(updatedCourses)
            }

            if (eventsToInsert.isNotEmpty()) {
                repository.insertImportedEvents(eventsToInsert)
                repository.showSuccessMessage("$importedCount رویداد با موفقیت وارد شد.")
            } else if (unmatchedCount > 0) {
                repository.showErrorMessage("$unmatchedCount رویداد مربوط به درس‌هایی بود که در برنامهٔ شما وجود ندارند.")
            }

            onFinished(importedCount, unmatchedCount)
        }
    }

    suspend fun createFullBackup(): String {
        val notes = repository.getAllNotesDirect()
        return BackupAndImportManager.createFullBackup(
            sourceUrl = sourceUrl.value,
            themeMode = themeMode.value,
            inputScheduleJson = inputJson.value,
            events = allEvents.value,
            tasks = allTasks.value,
            notes = notes
        )
    }

    fun restoreFullBackup(json: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = BackupAndImportManager.parseFullBackup(json)
            val data = result.getOrNull()
            if (data == null) {
                repository.showErrorMessage("فایل پشتیبان نامعتبر است.")
                onComplete(false)
                return@launch
            }

            try {
                if (data.sourceUrl.isNotBlank()) repository.setSourceUrl(data.sourceUrl)
                if (data.themeMode.isNotBlank()) repository.setThemeMode(data.themeMode)
                if (data.inputScheduleJson.isNotBlank()) {
                    repository.setInputJson(data.inputScheduleJson)
                }

                if (data.events.isNotEmpty()) {
                    repository.insertImportedEvents(data.events)
                }
                if (data.tasks.isNotEmpty()) {
                    repository.insertImportedTasks(data.tasks)
                }
                if (data.notes.isNotEmpty()) {
                    repository.insertImportedNotes(data.notes)
                }

                ScheduleWidgetProvider.updateAllWidgets(getApplication())
                repository.showSuccessMessage("اطلاعات پشتیبان با موفقیت بازیابی شدند.")
                refreshStorageStats()
                loadComparison()
                onComplete(true)
            } catch (e: Exception) {
                repository.showErrorMessage("خطا در بازیابی نسخه پشتیبان: ${e.message}")
                onComplete(false)
            }
        }
    }

    fun shareExportFile(context: Context, content: String, fileName: String, title: String) {
        BackupAndImportManager.shareJsonFile(context, content, fileName, title)
    }

    fun setTheme(mode: String) {
        viewModelScope.launch {
            repository.setThemeMode(mode)
        }
    }

    fun toggleThemeMode() {
        val next = if (themeMode.value == "BLACK") "DARK" else "BLACK"
        setTheme(next)
    }

    fun clearCache() {
        viewModelScope.launch {
            repository.clearCachedSchedule()
            _comparisonSummary.value = null
            refreshStorageStats()
        }
    }

    fun clearDownloadedPdfs() {
        viewModelScope.launch {
            repository.clearDownloadedPdfs()
            refreshStorageStats()
        }
    }

    fun deletePdf(pdf: DownloadedPdfEntity) {
        viewModelScope.launch {
            repository.deletePdf(pdf)
            refreshStorageStats()
            loadComparison()
        }
    }

    fun openPdf(pdf: DownloadedPdfEntity) {
        val context: Context = getApplication<Application>().applicationContext
        val file = File(pdf.localFilePath)
        if (!file.exists()) {
            repository.showErrorMessage("فایل PDF در حافظه گوشی یافت نشد.")
            return
        }
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(viewIntent, "باز کردن فایل PDF")
            if (chooser.resolveActivity(context.packageManager) != null) {
                context.startActivity(chooser)
            } else {
                repository.showErrorMessage("برای باز کردن فایل PDF برنامه‌ای روی گوشی نصب نیست.")
            }
        } catch (e: ActivityNotFoundException) {
            repository.showErrorMessage("برای باز کردن فایل PDF برنامه‌ای روی گوشی نصب نیست.")
        } catch (e: Exception) {
            repository.showErrorMessage("باز کردن فایل PDF ممکن نشد.")
        }
    }

    fun dismissUpdateStatus() {
        repository.dismissUpdateStatus()
    }

    fun resetAllData() {
        viewModelScope.launch {
            repository.resetAllData()
            _comparisonSummary.value = null
            refreshStorageStats()
        }
    }

    fun refreshStorageStats() {
        viewModelScope.launch {
            val stats = withContext(Dispatchers.IO) {
                repository.getStorageSizeFormatted() to repository.getDownloadedPdfsCount()
            }
            _storageSize.value = stats.first
            _pdfCount.value = stats.second
        }
    }
}
