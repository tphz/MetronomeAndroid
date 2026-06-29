package com.tangpenghui.metronome.integration

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.tangpenghui.metronome.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SmokeTest {
    @get:Rule val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test fun home_screen_displays_super_jogging_title() {
        composeTestRule.onNodeWithText("SUPER JOGGING").assertExists()
    }

    @Test fun clicking_start_button_works() {
        composeTestRule.onNodeWithText("开始").performClick()
    }
}
