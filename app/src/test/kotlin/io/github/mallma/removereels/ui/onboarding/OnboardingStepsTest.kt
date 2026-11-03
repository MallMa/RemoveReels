package io.github.mallma.removereels.ui.onboarding

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import io.github.mallma.removereels.ui.PHONE_SCREEN
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = PHONE_SCREEN)
class OnboardingStepsTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `welcome states the no-off-switch rule up front`() {
        compose.setContent { WelcomeStep(onGetStarted = {}) }

        compose.onNodeWithText("Keep Instagram and YouTube. Lose Reels and Shorts.").assertIsDisplayed()
        compose.onNodeWithText("There is no off switch. Uninstalling is the only way to stop it.").assertIsDisplayed()
    }

    @Test
    fun `how it works promises what the service does not do`() {
        compose.setContent { HowItWorksStep(onContinue = {}) }

        compose.onNodeWithText("• It saves nothing you see or type.").assertIsDisplayed()
        compose.onNodeWithText("• It has no internet access.").assertIsDisplayed()
    }

    @Test
    fun `the notification step can be skipped`() {
        var continued = false
        compose.setContent { NotificationsStep(granted = false, onAllow = {}, onContinue = { continued = true }) }

        compose.onNodeWithText("Not now").performClick()

        assertThat(continued).isTrue()
    }

    @Test
    fun `turning on explains the restricted setting for apps installed from a file`() {
        compose.setContent {
            TurnOnStep(
                blockingOn = false,
                installedFromFile = true,
                onOpenSettings = {},
                onOpenAppInfo = {},
                onContinue = {},
            )
        }

        compose.onNodeWithText("Open app info").assertIsDisplayed()
        compose.onNodeWithText("Open settings").assertIsDisplayed()
    }

    @Test
    fun `turning on skips the restricted setting for other installs`() {
        compose.setContent {
            TurnOnStep(
                blockingOn = false,
                installedFromFile = false,
                onOpenSettings = {},
                onOpenAppInfo = {},
                onContinue = {},
            )
        }

        compose.onNodeWithText("Open app info").assertDoesNotExist()
    }

    @Test
    fun `confirms when blocking is on`() {
        var continued = false
        compose.setContent {
            TurnOnStep(
                blockingOn = true,
                installedFromFile = false,
                onOpenSettings = {},
                onOpenAppInfo = {},
                onContinue = { continued = true },
            )
        }

        compose.onNodeWithText("✓ Blocking is on").assertIsDisplayed()
        compose.onNodeWithText("Continue").performClick()
        assertThat(continued).isTrue()
    }
}
