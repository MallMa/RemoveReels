package io.github.mallma.removereels.guard.overlay

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import io.github.mallma.removereels.budget.Allowance
import io.github.mallma.removereels.budget.BlockReason
import io.github.mallma.removereels.budget.TargetApp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BlockCardTest {
    @get:Rule
    val compose = createComposeRule()

    private fun show(
        notice: BlockNotice,
        onContinue: () -> Unit = {},
    ) = compose.setContent { BlockCard(notice, onContinue) }

    @Test
    fun `says what is blocked and what still works`() {
        show(BlockNotice(TargetApp.INSTAGRAM, BlockReason.NoAllowance, openedFromMessages = false))

        compose.onNodeWithText("Reels are blocked").assertIsDisplayed()
        compose.onNodeWithText("Your feed, messages and stories still work.").assertIsDisplayed()
        compose.onNodeWithText("Reels in messages are blocked too.").assertDoesNotExist()
    }

    @Test
    fun `mentions messages when the Reel was opened from a chat`() {
        show(BlockNotice(TargetApp.INSTAGRAM, BlockReason.NoAllowance, openedFromMessages = true))

        compose.onNodeWithText("Reels in messages are blocked too.").assertIsDisplayed()
    }

    @Test
    fun `says when today's allowance is used up`() {
        show(BlockNotice(TargetApp.YOUTUBE, BlockReason.UsedUp(Allowance.ofMinutes(20)), openedFromMessages = false))

        compose.onNodeWithText("Shorts are blocked").assertIsDisplayed()
        compose.onNodeWithText("You've used today's 20 min of Shorts. They're back tomorrow.").assertIsDisplayed()
        compose.onNodeWithText("Your videos, subscriptions and search still work.").assertIsDisplayed()
    }

    @Test
    fun `explains that allowances need automatic time`() {
        show(BlockNotice(TargetApp.YOUTUBE, BlockReason.AutomaticTimeOff, openedFromMessages = false))

        compose
            .onNodeWithText(
                "Allowances need automatic date, time and time zone. Until they're on, Reels and Shorts stay blocked.",
            ).assertIsDisplayed()
    }

    @Test
    fun `continue returns to the app`() {
        var continued = false
        show(BlockNotice(TargetApp.INSTAGRAM, BlockReason.NoAllowance, openedFromMessages = false)) { continued = true }

        compose.onNodeWithText("Continue in Instagram").performClick()

        assertThat(continued).isTrue()
    }
}
