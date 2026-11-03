package io.github.mallma.removereels.guard.overlay

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@RunWith(AndroidJUnit4::class)
class TimerPillTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `shows the time left`() {
        compose.setContent { TimerPill(remaining = 4.minutes + 32.seconds, onHide = {}) }

        compose.onNodeWithText("4:32 left").assertIsDisplayed()
    }

    @Test
    fun `hides when tapped`() {
        var hidden = false
        compose.setContent { TimerPill(remaining = 5.minutes, onHide = { hidden = true }) }

        compose.onNodeWithText("5:00 left").performClick()

        assertThat(hidden).isTrue()
    }

    @Test
    fun `formats minutes and seconds, rounding up`() {
        assertThat(formatTimeLeft(45.minutes)).isEqualTo("45:00")
        assertThat(formatTimeLeft(9.minutes + 5.seconds)).isEqualTo("9:05")
        assertThat(formatTimeLeft(59.seconds + 1.milliseconds)).isEqualTo("1:00")
        assertThat(formatTimeLeft(400.milliseconds)).isEqualTo("0:01")
        assertThat(formatTimeLeft(Duration.ZERO)).isEqualTo("0:00")
    }
}
