package io.github.mallma.removereels.ui.allowance

import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import io.github.mallma.removereels.budget.Allowance
import io.github.mallma.removereels.budget.Allowances
import io.github.mallma.removereels.budget.TargetApp
import io.github.mallma.removereels.ui.PHONE_SCREEN
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "en-rGB-$PHONE_SCREEN")
class AllowanceScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val tomorrow = LocalDate.of(2026, 11, 3)

    private fun state(
        today: Allowances,
        draft: Allowances = today,
        automaticTimeOn: Boolean = true,
    ) = AllowanceEditorState(
        today,
        saved = today,
        draft = draft,
        startsOn = tomorrow,
        automaticTimeOn = automaticTimeOn,
    )

    private fun show(
        state: AllowanceEditorState,
        onAllow: (TargetApp, Int) -> Unit = { _, _ -> },
        onSave: () -> Unit = {},
    ) = compose.setContent {
        AllowanceScreen(
            state = state,
            onBlock = {},
            onAllow = onAllow,
            onSave = onSave,
            onOpenDateSettings = {},
            onBack = {},
            snackbarHostState = SnackbarHostState(),
        )
    }

    @Test
    fun `shows today's allowances as text and when changes start`() {
        show(state(Allowances(Allowance.BLOCKED, Allowance.ofMinutes(15))))

        compose.onNodeWithText("blocked").assertIsDisplayed()
        compose.onAllNodesWithText("15 min").assertCountEquals(2)
        compose
            .onNodeWithText("Changes start tomorrow (Tue 3 Nov) and stay until you change them again.")
            .assertIsDisplayed()
    }

    @Test
    fun `blocking completely is its own choice`() {
        show(state(Allowances.BLOCKED))

        compose.onAllNodesWithText("Block completely")[0].assertIsSelected()
    }

    @Test
    fun `allowing starts at the smallest step`() {
        var allowed: Pair<TargetApp, Int>? = null
        show(state(Allowances.BLOCKED), onAllow = { app, minutes -> allowed = app to minutes })

        compose.onAllNodesWithText("Allow")[1].performClick()

        assertThat(allowed).isEqualTo(TargetApp.YOUTUBE to 5)
    }

    @Test
    fun `asks to lower the other app first when it has the whole maximum`() {
        show(state(Allowances(Allowance.BLOCKED, Allowance.ofMinutes(45))))

        compose.onNodeWithText("Lower YouTube's allowance first.").assertIsDisplayed()
        compose.onAllNodesWithText("Allow")[0].assertIsNotEnabled()
    }

    @Test
    fun `describes the slider and the shared maximum for TalkBack`() {
        show(state(Allowances(Allowance.ofMinutes(20), Allowance.ofMinutes(15))))

        compose.onNodeWithContentDescription("Instagram Reels from tomorrow").assertIsDisplayed()
        compose.onNodeWithContentDescription("Instagram 20 min, YouTube 15 min, 10 min free").assertIsDisplayed()
    }

    @Test
    fun `saves only when something changed`() {
        var saved = false
        val today = Allowances.BLOCKED
        show(state(today, draft = Allowances(Allowance.ofMinutes(10), Allowance.BLOCKED)), onSave = { saved = true })

        compose.onNodeWithText("Save").performClick()

        assertThat(saved).isTrue()
    }

    @Test
    fun `save is disabled while automatic time is off`() {
        val draft = Allowances(Allowance.ofMinutes(10), Allowance.BLOCKED)
        show(state(Allowances.BLOCKED, draft = draft, automaticTimeOn = false))

        compose.onNodeWithText("Save").assertIsNotEnabled()
        compose
            .onNodeWithText("Changes can be saved only while automatic date, time and time zone are on.")
            .assertIsDisplayed()
    }

    @Test
    fun `save is disabled without changes`() {
        show(state(Allowances.BLOCKED))

        compose.onNodeWithText("Save").assertIsNotEnabled()
    }
}
