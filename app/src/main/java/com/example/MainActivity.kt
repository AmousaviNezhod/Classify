package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.repository.UpdateStatus
import com.example.ui.MainViewModel
import com.example.ui.comparison.ComparisonScreen
import com.example.ui.components.AppToast
import com.example.ui.components.AppTopBar
import com.example.ui.components.ToastHost
import com.example.ui.components.ToastType
import com.example.ui.components.UpdateStatusBanner
import com.example.ui.home.HomeScreen
import com.example.ui.pdfmanager.PdfManagerScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.theme.MyApplicationTheme

enum class Screen { HOME, ALL_CLASSES, ADD_IMPORT, COMPARISON, PDFS, SETTINGS }

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            MyApplicationTheme(themeMode = themeMode) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    ScheduleApp(viewModel)
                }
            }
        }
    }
}

@Composable
fun ScheduleApp(viewModel: MainViewModel) {
    var currentScreen by remember { mutableStateOf(Screen.HOME) }
    val schedule by viewModel.schedule.collectAsStateWithLifecycle()
    val mySchedule by viewModel.mySchedule.collectAsStateWithLifecycle()
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
    val isCoursePage = currentScreen in setOf(Screen.HOME, Screen.ALL_CLASSES, Screen.ADD_IMPORT)
    var toasts by remember { mutableStateOf(emptyList<AppToast>()) }
    val toastMessage = when (val status = updateStatus) {
        is UpdateStatus.Success -> status.message
        is UpdateStatus.Error -> status.message
        else -> null
    }

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

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            if (isCoursePage) {
                val title = when (currentScreen) {
                    Screen.HOME -> "درس‌های من"
                    Screen.ALL_CLASSES -> "همهٔ کلاس‌ها"
                    else -> "افزودن / وارد کردن"
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
            if (isCoursePage || currentScreen == Screen.SETTINGS) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier.widthIn(max = 430.dp).fillMaxWidth(),
                        shape = RoundedCornerShape(26.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        tonalElevation = 5.dp,
                        shadowElevation = 12.dp,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.16f))
                    ) {
                        NavigationBar(
                            containerColor = Color.Transparent,
                            tonalElevation = 0.dp,
                            modifier = Modifier.height(70.dp).testTag("main_bottom_nav")
                        ) {
                            NavigationBarItem(
                                selected = currentScreen == Screen.HOME,
                                onClick = { currentScreen = Screen.HOME },
                                icon = { Icon(Icons.Default.EditCalendar, "درس‌های من") },
                                label = { Text("درس‌های من") },
                                modifier = Modifier.testTag("nav_item_home"),
                                colors = NavigationBarItemDefaults.colors(indicatorColor = MaterialTheme.colorScheme.primaryContainer)
                            )
                            NavigationBarItem(
                                selected = currentScreen == Screen.ALL_CLASSES,
                                onClick = { currentScreen = Screen.ALL_CLASSES },
                                icon = { Icon(Icons.Default.AccountBalance, "همهٔ کلاس‌ها") },
                                label = { Text("همهٔ کلاس‌ها") },
                                modifier = Modifier.testTag("nav_item_all_classes"),
                                colors = NavigationBarItemDefaults.colors(indicatorColor = MaterialTheme.colorScheme.primaryContainer)
                            )
                            NavigationBarItem(
                                selected = currentScreen == Screen.ADD_IMPORT,
                                onClick = { currentScreen = Screen.ADD_IMPORT },
                                icon = { Icon(Icons.Default.AddCircleOutline, "افزودن و وارد کردن") },
                                label = { Text("افزودن") },
                                modifier = Modifier.testTag("nav_item_import"),
                                colors = NavigationBarItemDefaults.colors(indicatorColor = MaterialTheme.colorScheme.primaryContainer)
                            )
                            NavigationBarItem(
                                selected = currentScreen == Screen.SETTINGS,
                                onClick = { currentScreen = Screen.SETTINGS },
                                icon = { Icon(Icons.Default.Settings, "تنظیمات") },
                                label = { Text("تنظیمات") },
                                modifier = Modifier.testTag("nav_item_settings"),
                                colors = NavigationBarItemDefaults.colors(indicatorColor = MaterialTheme.colorScheme.primaryContainer)
                            )
                        }
                    }
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
                        when (currentScreen) {
                        Screen.HOME, Screen.ALL_CLASSES, Screen.ADD_IMPORT -> HomeScreen(
                            mySchedule = mySchedule,
                            universitySchedule = schedule?.copy(dayAvailability = dayAvailability),
                            selectedTab = when (currentScreen) {
                                Screen.ALL_CLASSES -> 1
                                Screen.ADD_IMPORT -> 2
                                else -> 0
                            },
                            inputJson = inputJson,
                            searchQuery = searchQuery,
                            isUpdating = isUpdating,
                            onTabChange = { index ->
                                currentScreen = when (index) {
                                    1 -> Screen.ALL_CLASSES
                                    2 -> Screen.ADD_IMPORT
                                    else -> Screen.HOME
                                }
                            },
                            onUpdateInputJson = viewModel::updateInputJson,
                            onSearchQueryChange = viewModel::setSearchQuery,
                            onFetchSchedule = { viewModel.triggerUpdate(forceRedownload = false) },
                            dayAvailability = dayAvailability,
                            onRemoveOffering = viewModel::removeCourseOffering,
                            onAddOffering = viewModel::addCourseOffering
                        )
                        Screen.COMPARISON -> ComparisonScreen(
                            summary = comparisonSummary,
                            onRefreshComparison = viewModel::loadComparison,
                            onBack = { currentScreen = Screen.HOME }
                        )
                        Screen.PDFS -> PdfManagerScreen(
                            pdfs = downloadedPdfs,
                            onImportPdf = viewModel::importPdf,
                            onOpenPdf = viewModel::openPdf,
                            onDeletePdf = viewModel::deletePdf,
                            onBack = { currentScreen = Screen.SETTINGS }
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
                            onBack = { currentScreen = Screen.HOME }
                        )
                        }
                    }
                }
            }
            ToastHost(
                toasts = toasts,
                onDismiss = { toast -> toasts = toasts.filterNot { it.id == toast.id } },
                modifier = Modifier.align(Alignment.BottomCenter).widthIn(max = 520.dp).fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}
