package io.github.mallma.removereels.ui.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import io.github.mallma.removereels.budget.Allowance
import io.github.mallma.removereels.budget.TargetApp
import io.github.mallma.removereels.ui.PHONE_SCREEN
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = PHONE_SCREEN)
class HomeScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val instagramBlocked = card(TargetApp.INSTAGRAM, Allowance.BLOCKED)
    private val youtubeWithTime = card(TargetApp.YOUTUBE, Allowance.ofMinutes(15), used = 7.minutes + 50.seconds)

    private fun card(
        app: TargetApp,
        today: Allowance,
        used: Duration = Duration.ZERO,
    ) = AppCard(app, installed = true, rulesStale = false, today = today, used = used, fromTomorrow = null)

    private fun show(
        state: HomeState,
        onTurnOn: () -> Unit = {},
        onOpenAllowance: () -> Unit = {},
    ) = compose.setContent {
        HomeScreen(state, onTurnOn = onTurnOn, onOpenDateSettings = {}, onOpenAllowance = onOpenAllowance)
    }

    @Test
    fun `shows that blocking is on and each app's state`() {
        show(HomeState(blockingOn = true, automaticTimeOn = true, cards = listOf(instagramBlocked, youtubeWithTime)))

        compose.onNodeWithText("Blocking is on").assertIsDisplayed()
        compose.onNodeWithText("Reels: blocked").assertIsDisplayed()
        compose.onNodeWithText("Shorts: 8 of 15 min left").assertIsDisplayed()
        compose.onNodeWithText("Today: 7 of 15 min · resets at midnight").assertIsDisplayed()
        compose.onNodeWithText("Turn on").assertDoesNotExist()
    }

    @Test
    fun `opens the daily allowance`() {
        var opened = false
        show(HomeState(blockingOn = true, automaticTimeOn = true, cards = listOf(instagramBlocked)), onOpenAllowance = {
            opened = true
        })

        compose.onNodeWithText("Daily allowance").performClick()

        assertThat(opened).isTrue()
    }

    @Test
    fun `offers to turn blocking back on`() {
        var turnedOn = false
        show(
            HomeState(blockingOn = false, automaticTimeOn = true, cards = listOf(instagramBlocked)),
            onTurnOn = { turnedOn = true },
        )

        compose.onNodeWithText("Blocking is off").assertIsDisplayed()
        compose.onNodeWithText("Turn on").performClick()

        assertThat(turnedOn).isTrue()
    }

    @Test
    fun `shows a used-up allowance and tomorrow's change`() {
        val youtube =
            card(
                TargetApp.YOUTUBE,
                Allowance.ofMinutes(15),
                used = 15.minutes,
            ).copy(fromTomorrow = Allowance.ofMinutes(20))
        val instagram = instagramBlocked.copy(fromTomorrow = Allowance.BLOCKED)
        show(HomeState(blockingOn = true, automaticTimeOn = true, cards = listOf(instagram, youtube)))

        compose.onNodeWithText("Shorts: used up today").assertIsDisplayed()
        compose.onNodeWithText("From tomorrow: 20 min").assertIsDisplayed()
        compose.onNodeWithText("From tomorrow: blocked").assertIsDisplayed()
    }

    @Test
    fun `explains missing apps, stale rules and automatic time`() {
        val youtube = card(TargetApp.YOUTUBE, Allowance.BLOCKED).copy(installed = false)
        val instagram = instagramBlocked.copy(rulesStale = true)
        show(HomeState(blockingOn = true, automaticTimeOn = false, cards = listOf(instagram, youtube)))

        compose.onNodeWithText("Not installed").assertIsDisplayed()
        compose
            .onNodeWithText(
                "This app may have changed. Some of its screens may not be blocked until RemoveReels is updated.",
            ).assertIsDisplayed()
        compose
            .onNodeWithText("Allowances need automatic date and time. Until it's on, Reels and Shorts stay blocked.")
            .assertIsDisplayed()
    }
}
