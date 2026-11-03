package io.github.mallma.removereels.ui

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.mallma.removereels.ui.theme.RemoveReelsTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = PHONE_SCREEN)
class RemoveReelsAppTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Before
    fun setUp() {
        // Robolectric does not record where its test app was installed from, which the Turn on step reads.
        val context = RuntimeEnvironment.getApplication()
        shadowOf(context.packageManager).setInstallSourceInfo(context.packageName, null, null)
    }

    private fun waitFor(text: String) =
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty()
        }

    @Test
    fun `the first start leads through onboarding to turning blocking on`() {
        composeRule.setContent {
            RemoveReelsTheme {
                RemoveReelsApp()
            }
        }

        waitFor("Keep Instagram and YouTube. Lose Reels and Shorts.")
        composeRule.onNodeWithText("Get started").performClick()
        composeRule.onNodeWithText("Continue").performClick()
        composeRule.onNodeWithText("Not now").performClick()

        waitFor("Turn on RemoveReels")
    }
}
