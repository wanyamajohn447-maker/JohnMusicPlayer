package com.wanyamajohn.musicplayer

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wanyamajohn.musicplayer.ui.screens.AboutScreen
import com.wanyamajohn.musicplayer.ui.theme.JohnTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AboutScreenTest {
    @get:Rule val rule = createComposeRule()

    @Test fun showsRequiredBranding() {
        rule.setContent { JohnTheme { AboutScreen() } }
        rule.onNodeWithText("Product: JOHN MUSIC PLAYER").assertExists()
        rule.onNodeWithText("Developer: Wanyama John").assertExists()
        rule.onNodeWithText("Organization: Wanyama John Technologies").assertExists()
    }
}
