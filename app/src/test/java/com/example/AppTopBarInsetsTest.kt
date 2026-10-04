package com.example

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.core.graphics.Insets
import androidx.core.view.WindowInsetsCompat
import com.example.ui.components.AppTopBar
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The status bar (clock, battery/charging icons) must never overlap the
 * refresh/theme/settings row: Scaffold places its topBar at (0, 0) and expects
 * the bar to handle the top window insets itself.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AppTopBarInsetsTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `top bar row sits below the status bar inset`() {
        composeRule.setContent {
            Box(Modifier.fillMaxSize()) {
                AppTopBar(
                    title = "امروز",
                    lastUpdatedTimestamp = null,
                    isUpdating = false,
                    onRefresh = {},
                    onOpenSettings = {}
                )
            }
        }

        val statusBarPx = 100f
        val insets = WindowInsetsCompat.Builder()
            .setInsets(
                WindowInsetsCompat.Type.statusBars(),
                Insets.of(0, statusBarPx.toInt(), 0, 0)
            )
            .build()
        composeRule.activity.window.decorView.dispatchApplyWindowInsets(insets.toWindowInsets()!!)
        composeRule.waitForIdle()

        val buttonTopPx = composeRule.onNodeWithTag("settings_top_button")
            .getUnclippedBoundsInRoot().top.value * composeRule.density.density

        assertTrue(
            "Top bar buttons must start below the $statusBarPx px status bar, but started at $buttonTopPx px",
            buttonTopPx >= statusBarPx
        )
    }
}
