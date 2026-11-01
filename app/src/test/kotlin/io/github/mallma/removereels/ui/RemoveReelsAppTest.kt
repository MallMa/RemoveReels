package io.github.mallma.removereels.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.mallma.removereels.ui.theme.RemoveReelsTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RemoveReelsAppTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsAppName() {
        composeRule.setContent {
            RemoveReelsTheme {
                RemoveReelsApp()
            }
        }

        composeRule.onNodeWithText("RemoveReels").assertIsDisplayed()
    }
}
