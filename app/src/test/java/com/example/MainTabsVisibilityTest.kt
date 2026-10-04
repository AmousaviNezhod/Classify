package com.example

import androidx.activity.ComponentActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MainTabsVisibilityTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @OptIn(ExperimentalAnimationApi::class)
    @Test
    fun `main tab layer is not occluded when detail screen is null`() {
        val currentScreen = Screen.TODAY
        val isMainTab = true

        composeRule.setContent {
            Box(Modifier.fillMaxSize()) {
                // Simulates main tab layer
                Text(text = "محتوای تب امروز")

                // Detail layer
                AnimatedContent(
                    targetState = if (isMainTab) null else currentScreen,
                    label = "detailTransition"
                ) { detailScreen ->
                    if (detailScreen != null) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background)
                        ) {
                            Text("جزئیات درس")
                        }
                    }
                }
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithText("محتوای تب امروز").assertIsDisplayed()
    }
}
