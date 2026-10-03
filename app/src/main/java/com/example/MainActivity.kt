package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.data.repository.UpdateStatus
import com.example.domain.model.ScheduleClass
import com.example.ui.MainViewModel
import com.example.ui.calendar.CalendarScreen
import com.example.ui.comparison.ComparisonScreen
import com.example.ui.components.AppBottomBar
import com.example.ui.components.AppToast
import com.example.ui.components.AppTopBar
import com.example.ui.components.BottomTab
import com.example.ui.components.GraphiteBackdrop
import com.example.ui.components.ToastHost
import com.example.ui.components.ToastType
import com.example.ui.components.UpdateStatusBanner
import com.example.ui.course.CourseDetailScreen
import com.example.ui.home.HomeScreen
import com.example.ui.pdfmanager.PdfManagerScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.theme.AppMotion
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.today.TodayScreen
import com.example.ui.upcoming.UpcomingScreen
import kotlinx.coroutines.launch

enum class Screen {
    TODAY,
    COURSES,
    CALENDAR,
    UPCOMING,
    SETTINGS,
    COURSE_DETAIL,
    COMPARISON,
    PDFS
}

/** Destinations that live in the bottom bar, in layout order (RTL: right to left). */
private val MAIN_TAB_ORDER = listOf(
    Screen.TODAY,
    Screen.COURSES,
    Screen.CALENDAR,
    Screen.UPCOMING,
    Screen.SETTINGS
)

private val BOTTOM_TABS = listOf(
    BottomTab("امروز", Icons.Default.Today, "nav_today"),
    BottomTab("درس‌ها", Icons.Default.School, "nav_courses"),
    BottomTab("تقویم", Icons.Default.CalendarMonth, "nav_calendar"),
    BottomTab("نزدیک", Icons.Default.NotificationsActive, "nav_upcoming"),
    BottomTab("تنظیمات", Icons.Default.Settings, "nav_settings")
)

/**
 * Depth of a destination in the navigation stack. Tabs are siblings (0..4);
 * detail screens sit one level deeper so the transition can tell "going in"
 * apart from "coming back".
 */
private fun screenRank(screen: Screen): Int =
    MAIN_TAB_ORDER.indexOf(screen).let { if (it >= 0) it else MAIN_TAB_ORDER.size }

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIncomingFileIntent(intent)
        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            MyApplicationTheme(themeMode = themeMode) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    ScheduleApp(viewModel)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingFileIntent(intent)
    }

    private fun handleIncomingFileIntent(intent: Intent?) {
        val uri: Uri = intent?.data ?: return
        lifecycleScope.launch {
            try {
                val text = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: return@launch
                when {
                    text.contains("classify-events") || (text.contains("\"events\"") && text.contains("\"title\"")) -> {
                        viewModel.importEventsFromJson(text, createMissingCourses = true) { _, _ -> }
                    }
                    text.contains("classify-full-backup") -> {
                        viewModel.restoreFullBackup(text) { _ -> }
                    }
                    text.contains("unit-selection-schedule") || text.contains("\"courses\"") -> {
                        viewModel.updateInputJson(text)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ScheduleApp(viewModel: MainViewModel) {
    val context = LocalContext.current
    var currentScreen by remember { mutableStateOf(Screen.TODAY) }
    var previousScreen by remember { mutableStateOf(Screen.TODAY) }
    var selectedCourseForDetail by remember { mutableStateOf<ScheduleClass?>(null) }
    var coursesTab by remember { androidx.compose.runtime.mutableIntStateOf(0) }

    val schedule by viewModel.schedule.collectAsStateWithLifecycle()
    val mySchedule by viewModel.mySchedule.collectAsStateWithLifecycle()
    val allEvents by viewModel.allEvents.collectAsStateWithLifecycle()
    val allTasks by viewModel.allTasks.collectAsStateWithLifecycle()

    val updateStatus by viewModel.updateStatus.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val comparisonSummary by viewModel.comparisonSummary.collectAsStateWithLifecycle()
    val downloadedPdfs by viewModel.downloadedPdfs.collectAsStateWithLifecycle()
    val dayAvailability by viewModel.dayAvailability.collectAsStateWithLifecycle()
    val sourceUrl by viewModel.sourceUrl.collectAsStateWithLifecycle()
    val inputJson by viewModel.inputJson.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val storageSize by viewModel.storageSize.collectAsStateWithLifecycle()
    val pdfCount by viewModel.pdfCount.collectAsStateWithLifecycle()
    val isUpdating = updateStatus is UpdateStatus.Progress

    val isMainTab = currentScreen in MAIN_TAB_ORDER

    var toasts by remember { mutableStateOf(emptyList<AppToast>()) }
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

    androidx.compose.runtime.LaunchedEffect(updateStatus) {
        val message = when (val status = updateStatus) {
            is UpdateStatus.Success -> status.message
            is UpdateStatus.Error -> status.message
            else -> return@LaunchedEffect
        }
        val toast = AppToast(
            id = System.nanoTime(),
            message = message,
            type = if (updateStatus is UpdateStatus.Error) ToastType.ERROR else ToastType.SUCCESS
        )
        toasts = (toasts + toast).takeLast(3)
        viewModel.dismissUpdateStatus()
    }

    val openCourseDetail: (ScheduleClass) -> Unit = { course ->
        selectedCourseForDetail = course
        previousScreen = currentScreen
        currentScreen = Screen.COURSE_DETAIL
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GraphiteBackdrop()

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                if (isMainTab && currentScreen != Screen.SETTINGS) {
                    val title = when (currentScreen) {
                        Screen.TODAY -> "امروز"
                        Screen.COURSES -> "درس‌ها و کلاس‌ها"
                        Screen.CALENDAR -> "تقویم هفتگی"
                        Screen.UPCOMING -> "رویدادها و امتحانات"
                        else -> "Classify"
                    }
                    AppTopBar(
                        title = title,
                        lastUpdatedTimestamp = schedule?.updatedAt,
                        isUpdating = isUpdating,
                        themeMode = themeMode,
                        onToggleTheme = viewModel::toggleThemeMode,
                        onRefresh = { viewModel.triggerUpdate(forceRedownload = false) },
                        onOpenSettings = { currentScreen = Screen.SETTINGS }
                    )
                }
            },
            bottomBar = {
                if (isMainTab) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .imePadding()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AppBottomBar(
                            tabs = BOTTOM_TABS,
                            selectedIndex = MAIN_TAB_ORDER.indexOf(currentScreen).coerceAtLeast(0),
                            onSelect = { index ->
                                val destination = MAIN_TAB_ORDER.getOrNull(index) ?: return@AppBottomBar
                                if (destination != currentScreen) currentScreen = destination
                            },
                            modifier = Modifier.widthIn(max = 520.dp)
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(Modifier.fillMaxSize().padding(innerPadding)) {
                Column(Modifier.fillMaxSize()) {
                    if (isUpdating) {
                        UpdateStatusBanner(
                            status = updateStatus,
                            onDismiss = viewModel::dismissUpdateStatus
                        )
                    }
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                        Box(Modifier.widthIn(max = 1040.dp).fillMaxSize()) {
                            // Depth-aware transition: deeper screens enter from the start
                            // (left, in RTL) and the outgoing screen retreats to the end,
                            // so the motion reads as forward/back rather than a generic fade.
                            AnimatedContent(
                                targetState = currentScreen,
                                modifier = Modifier.fillMaxSize().clipToBounds(),
                                transitionSpec = {
                                    val goingDeeper = screenRank(targetState) > screenRank(initialState)
                                    val sign = if (goingDeeper) -1 else 1
                                    val travel = { full: Int -> sign * (full / 5) }
                                    (
                                        slideInHorizontally(
                                            animationSpec = AppMotion.springGlideOffset,
                                            initialOffsetX = travel
                                        ) + fadeIn(tween(AppMotion.DURATION_BASE, easing = AppMotion.EaseOut))
                                    ).togetherWith(
                                        slideOutHorizontally(
                                            animationSpec = tween(AppMotion.DURATION_BASE, easing = AppMotion.EaseIn),
                                            targetOffsetX = { full -> -sign * (full / 8) }
                                        ) + fadeOut(tween(AppMotion.DURATION_QUICK, easing = AppMotion.EaseIn))
                                    )
                                },
                                label = "screenTransition"
                            ) { screen ->
                                when (screen) {
                                    Screen.TODAY -> TodayScreen(
                                        myClasses = mySchedule?.classes.orEmpty(),
                                        universitySchedule = schedule?.copy(dayAvailability = dayAvailability),
                                        events = allEvents,
                                        tasks = allTasks,
                                        isUpdating = isUpdating,
                                        onFetchSchedule = { viewModel.triggerUpdate(forceRedownload = false) },
                                        onNavigateToCourses = { targetTab ->
                                            coursesTab = targetTab
                                            currentScreen = Screen.COURSES
                                        },
                                        onOpenCourseDetail = openCourseDetail,
                                        onToggleTaskDone = viewModel::toggleTaskDone
                                    )

                                    Screen.COURSES -> HomeScreen(
                                        mySchedule = mySchedule,
                                        universitySchedule = schedule?.copy(dayAvailability = dayAvailability),
                                        selectedTab = coursesTab,
                                        inputJson = inputJson,
                                        searchQuery = searchQuery,
                                        isUpdating = isUpdating,
                                        onTabChange = { coursesTab = it },
                                        onUpdateInputJson = viewModel::updateInputJson,
                                        onSearchQueryChange = viewModel::setSearchQuery,
                                        onFetchSchedule = { viewModel.triggerUpdate(forceRedownload = false) },
                                        dayAvailability = dayAvailability,
                                        onRemoveOffering = viewModel::removeCourseOffering,
                                        onAddOffering = viewModel::addCourseOffering,
                                        onOpenCourseDetail = openCourseDetail
                                    )

                                    Screen.CALENDAR -> CalendarScreen(
                                        myClasses = mySchedule?.classes.orEmpty(),
                                        events = allEvents,
                                        onOpenCourseDetail = openCourseDetail,
                                        onToggleEventCompleted = viewModel::toggleEventCompleted,
                                        onDeleteEvent = viewModel::deleteEvent
                                    )

                                    Screen.UPCOMING -> UpcomingScreen(
                                        events = allEvents,
                                        myClasses = mySchedule?.classes.orEmpty(),
                                        onAddOrUpdateEvent = viewModel::upsertEvent,
                                        onDeleteEvent = viewModel::deleteEvent,
                                        onToggleEventCompleted = viewModel::toggleEventCompleted,
                                        onOpenCourseDetail = openCourseDetail
                                    )

                                    Screen.SETTINGS -> SettingsScreen(
                                        sourceUrl = sourceUrl,
                                        themeMode = themeMode,
                                        storageSize = storageSize,
                                        pdfCount = pdfCount,
                                        inputJson = inputJson,
                                        onUpdateSourceUrl = viewModel::updateSourceUrl,
                                        onResetSourceUrl = viewModel::resetSourceUrl,
                                        onSetThemeMode = viewModel::setTheme,
                                        onUpdateInputJson = viewModel::updateInputJson,
                                        onClearCache = viewModel::clearCache,
                                        onClearPdfs = viewModel::clearDownloadedPdfs,
                                        onForceRefetchAll = { viewModel.triggerUpdate(forceRedownload = true) },
                                        onResetAllData = viewModel::resetAllData,
                                        onOpenPdfs = { currentScreen = Screen.PDFS },
                                        onOpenComparison = { currentScreen = Screen.COMPARISON; viewModel.loadComparison() },
                                        onExportFullBackup = {
                                            coroutineScope.launch {
                                                val backupJson = viewModel.createFullBackup()
                                                viewModel.shareExportFile(context, backupJson, "classify_backup_${System.currentTimeMillis()}.json", "پشتیبان داده‌های Classify")
                                            }
                                        },
                                        onRestoreFullBackup = { json ->
                                            viewModel.restoreFullBackup(json) { _ -> }
                                        },
                                        onExportEvents = {
                                            val eventsJson = viewModel.exportEventsJson()
                                            viewModel.shareExportFile(context, eventsJson, "classify_events_${System.currentTimeMillis()}.json", "ارسال رویدادها")
                                        },
                                        onImportEvents = { json ->
                                            viewModel.importEventsFromJson(json, createMissingCourses = true) { _, _ -> }
                                        },
                                        onExportSchedule = {
                                            val scheduleJson = viewModel.exportScheduleJson()
                                            viewModel.shareExportFile(context, scheduleJson, "unit_selection_schedule.json", "اشتراک برنامه کلاسی")
                                        },
                                        onBack = { currentScreen = Screen.TODAY }
                                    )

                                    Screen.COURSE_DETAIL -> {
                                        val currentCourse = selectedCourseForDetail
                                        if (currentCourse != null) {
                                            val courseKey = currentCourse.semanticKey
                                            val courseSessions = mySchedule?.classes.orEmpty().filter {
                                                it.semanticKey == courseKey || it.courseName == currentCourse.courseName
                                            }
                                            val courseEvents = allEvents.filter { it.courseKey == courseKey }
                                            val courseTasks = allTasks.filter { it.courseKey == courseKey }
                                            val courseNotes by viewModel.notesForCourse(courseKey).collectAsStateWithLifecycle(emptyList())

                                            CourseDetailScreen(
                                                course = currentCourse,
                                                allSessions = courseSessions,
                                                events = courseEvents,
                                                tasks = courseTasks,
                                                notes = courseNotes,
                                                onBack = { currentScreen = previousScreen },
                                                onDeleteCourse = {
                                                    viewModel.deleteCourseCascade(courseKey)
                                                    currentScreen = Screen.COURSES
                                                },
                                                onAddOrUpdateEvent = viewModel::upsertEvent,
                                                onDeleteEvent = viewModel::deleteEvent,
                                                onToggleEventCompleted = viewModel::toggleEventCompleted,
                                                onAddOrUpdateTask = viewModel::upsertTask,
                                                onDeleteTask = viewModel::deleteTask,
                                                onToggleTaskDone = viewModel::toggleTaskDone,
                                                onAddOrUpdateNote = viewModel::upsertNote,
                                                onDeleteNote = viewModel::deleteNote
                                            )
                                        } else {
                                            currentScreen = Screen.COURSES
                                        }
                                    }

                                    Screen.COMPARISON -> ComparisonScreen(
                                        summary = comparisonSummary,
                                        onRefreshComparison = viewModel::loadComparison,
                                        onBack = { currentScreen = Screen.SETTINGS }
                                    )

                                    Screen.PDFS -> PdfManagerScreen(
                                        pdfs = downloadedPdfs,
                                        onImportPdf = viewModel::importPdf,
                                        onOpenPdf = viewModel::openPdf,
                                        onDeletePdf = viewModel::deletePdf,
                                        onBack = { currentScreen = Screen.SETTINGS }
                                    )
                                }
                            }
                        }
                    }
                }

                ToastHost(
                    toasts = toasts,
                    onDismiss = { toast -> toasts = toasts.filterNot { it.id == toast.id } },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .widthIn(max = 520.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = if (isMainTab) 84.dp else 12.dp)
                )
            }
        }
    }
}
