package io.github.mallma.removereels.ui.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import io.github.mallma.removereels.budget.TargetApp
import io.github.mallma.removereels.ui.PHONE_SCREEN
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = PHONE_SCREEN)
class SettingsScreensTest {
    @get:Rule
    val compose = createComposeRule()

    private fun showSettings(
        showTimeLeft: Boolean,
        onShowTimeLeftChange: (Boolean) -> Unit = {},
        onTestBlocking: () -> Unit = {},
    ) = compose.setContent {
        SettingsScreen(
            showTimeLeft = showTimeLeft,
            onShowTimeLeftChange = onShowTimeLeftChange,
            onOpenAllowance = {},
            onTestBlocking = onTestBlocking,
            onKeepRunning = {},
            onAbout = {},
            onBack = {},
        )
    }

    @Test
    fun `switches the time left off and on`() {
        var changedTo: Boolean? = null
        showSettings(showTimeLeft = true, onShowTimeLeftChange = { changedTo = it })

        compose.onNode(isToggleable()).assertIsOn().performClick()

        assertThat(changedTo).isFalse()
    }

    @Test
    fun `shows the time left switch as off`() {
        showSettings(showTimeLeft = false)

        compose.onNode(isToggleable()).assertIsOff()
    }

    @Test
    fun `leads to test blocking`() {
        var opened = false
        showSettings(showTimeLeft = true, onTestBlocking = { opened = true })

        compose.onNodeWithText("Test blocking").performClick()

        assertThat(opened).isTrue()
    }

    @Test
    fun `about compares the tested app versions with the installed ones`() {
        compose.setContent {
            AboutScreen(
                appVersion = "0.1.0",
                testedVersions =
                    listOf(
                        TestedVersion(TargetApp.INSTAGRAM, tested = "453.0", installed = "453.0"),
                        TestedVersion(TargetApp.YOUTUBE, tested = "21.38", installed = "21.40"),
                    ),
                onOpenSource = {},
                onOpenLicense = {},
                onBack = {},
            )
        }

        compose.onNodeWithText("RemoveReels 0.1.0").assertIsDisplayed()
        compose.onNodeWithText("Installed: 453.0 ✓").assertIsDisplayed()
        compose.onNodeWithText("Installed: 21.40, not tested yet").assertIsDisplayed()
    }
}
