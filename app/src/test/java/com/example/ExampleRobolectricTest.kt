package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
class ExampleRobolectricTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun `read app name from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("KeyCraft", appName)
    }

    @Test
    fun `studio renders docked keyboard and navigates tabs`() {
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("keycraft_keyboard_root").assertIsDisplayed()
        composeRule.onNodeWithTag("sandbox_text_display").assertIsDisplayed()

        // Switch to symbols and back via toolbar
        composeRule.onNodeWithTag("kb_btn_emoji").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("kb_btn_clipboard").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("nav_snippets").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("fab_add_snippet").assertIsDisplayed()

        composeRule.onNodeWithTag("nav_lexicon").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("btn_add_custom_word").assertIsDisplayed()

        composeRule.onNodeWithTag("nav_tuning").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("theme_alabaster_light").assertIsDisplayed()
    }
}
