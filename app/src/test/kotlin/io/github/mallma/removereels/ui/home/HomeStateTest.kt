package io.github.mallma.removereels.ui.home

import com.google.common.truth.Truth.assertThat
import io.github.mallma.removereels.InstalledApps
import io.github.mallma.removereels.budget.Allowance
import io.github.mallma.removereels.budget.AllowanceSchedule
import io.github.mallma.removereels.budget.Allowances
import io.github.mallma.removereels.budget.TargetApp
import io.github.mallma.removereels.budget.UsageLedger
import io.github.mallma.removereels.storage.AppState
import org.junit.Test
import java.time.LocalDate
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

class HomeStateTest {
    private val today = LocalDate.of(2026, 11, 2)
    private val bothInstalled = InstalledApps { "1.0" }

    private fun home(
        state: AppState,
        installedApps: InstalledApps = bothInstalled,
    ) = homeState(state, today, blockingOn = true, automaticTimeOn = true, installedApps = installedApps)

    @Test
    fun `after installing both apps are blocked and nothing is scheduled`() {
        val home = home(AppState.INITIAL)

        assertThat(home.cards.map { it.app }).containsExactly(TargetApp.INSTAGRAM, TargetApp.YOUTUBE).inOrder()
        assertThat(home.cards.all { it.today.isBlocked && it.fromTomorrow == null }).isTrue()
        assertThat(home.allowedToday).isEqualTo(Duration.ZERO)
    }

    @Test
    fun `shows today's allowance, use and time left`() {
        val state =
            AppState(
                schedule = AllowanceSchedule(Allowances(Allowance.BLOCKED, Allowance.ofMinutes(15))),
                usage = UsageLedger().record(today, TargetApp.YOUTUBE, 8.minutes),
            )

        val youtube = home(state).cards.single { it.app == TargetApp.YOUTUBE }

        assertThat(youtube.today).isEqualTo(Allowance.ofMinutes(15))
        assertThat(youtube.left).isEqualTo(7.minutes)
        assertThat(home(state).usedToday).isEqualTo(8.minutes)
        assertThat(home(state).allowedToday).isEqualTo(15.minutes)
    }

    @Test
    fun `use above the allowance counts as the allowance`() {
        val state =
            AppState(
                schedule = AllowanceSchedule(Allowances(Allowance.BLOCKED, Allowance.ofMinutes(5))),
                usage = UsageLedger().record(today, TargetApp.YOUTUBE, 6.minutes),
            )

        val youtube = home(state).cards.single { it.app == TargetApp.YOUTUBE }

        assertThat(youtube.used).isEqualTo(5.minutes)
        assertThat(youtube.left).isEqualTo(Duration.ZERO)
    }

    @Test
    fun `shows a scheduled change only for the app it changes`() {
        val state =
            AppState(
                schedule =
                    AllowanceSchedule(Allowances(Allowance.BLOCKED, Allowance.ofMinutes(15)))
                        .scheduleChange(Allowances(Allowance.ofMinutes(20), Allowance.ofMinutes(15)), today),
                usage = UsageLedger(),
            )

        val cards = home(state).cards

        assertThat(cards.single { it.app == TargetApp.INSTAGRAM }.fromTomorrow).isEqualTo(Allowance.ofMinutes(20))
        assertThat(cards.single { it.app == TargetApp.YOUTUBE }.fromTomorrow).isNull()
    }

    @Test
    fun `marks apps that are not installed or whose rules look stale`() {
        val state = AppState.INITIAL.copy(staleRules = setOf(TargetApp.INSTAGRAM))
        val onlyInstagram = InstalledApps { app -> if (app == TargetApp.INSTAGRAM) "1.0" else null }

        val cards = home(state, onlyInstagram).cards

        assertThat(cards.single { it.app == TargetApp.INSTAGRAM }.rulesStale).isTrue()
        assertThat(cards.single { it.app == TargetApp.YOUTUBE }.installed).isFalse()
    }
}
