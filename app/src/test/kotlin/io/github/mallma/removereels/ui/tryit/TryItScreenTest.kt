package io.github.mallma.removereels.ui.tryit

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import io.github.mallma.removereels.budget.TargetApp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TryItScreenTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `asks to open the app and tap Reels`() {
        var opened: TargetApp? = null
        show(TryItState(blockingOn = true, app = TargetApp.INSTAGRAM, worked = false), onOpenApp = { opened = it })

        compose.onNodeWithText("Open Instagram and tap the Reels button.").assertIsDisplayed()
        compose.onNodeWithText("Open Instagram").performClick()

        assertThat(opened).isEqualTo(TargetApp.INSTAGRAM)
    }

    @Test
    fun `asks to turn blocking on first`() {
        show(TryItState(blockingOn = false, app = TargetApp.INSTAGRAM, worked = false))

        compose
            .onNodeWithText(
                "Blocking is off, so there is nothing to test yet. Turn it on first.",
            ).assertIsDisplayed()
    }

    @Test
    fun `confirms that it works`() {
        show(TryItState(blockingOn = true, app = TargetApp.INSTAGRAM, worked = true))

        compose.onNodeWithText("✓ It works. Your feed and messages are untouched.").assertIsDisplayed()
    }

    private fun show(
        state: TryItState,
        onOpenApp: (TargetApp) -> Unit = {},
    ) = compose.setContent { TryItScreen(state, onOpenApp = onOpenApp, onTurnOn = {}, onDone = {}, onBack = {}) }
}
