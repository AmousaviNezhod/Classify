package com.example

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.ui.settings.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SettingsReprocessActionTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun localPdfActionCallsReprocessInsteadOfRefresh() {
        var reprocessCalls = 0
        composeRule.setContent {
            MyApplicationTheme {
                SettingsScreen(
                    sourceUrl = "https://example.com/schedule",
                    themeMode = "DARK",
                    storageSize = "۱ مگابایت",
                    pdfCount = 2,
                    inputJson = "",
                    onUpdateSourceUrl = {},
                    onResetSourceUrl = {},
                    onSetThemeMode = {},
                    onUpdateInputJson = {},
                    onClearCache = {},
                    onClearPdfs = {},
                    onForceRefetchAll = {},
                    onResetAllData = {},
                    onReprocessLocalPdfs = { reprocessCalls++ },
                    onBack = {}
                )
            }
        }

        composeRule.onNodeWithTag("reprocess_local_pdfs_button")
            .performScrollTo()
            .assertIsDisplayed()
            .assertIsEnabled()
            .performClick()

        assertEquals(1, reprocessCalls)
    }
}
