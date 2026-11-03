package io.github.mallma.removereels.budget

import com.google.common.truth.Truth.assertThat
import io.github.mallma.removereels.budget.BlockReason.AutomaticTimeOff
import io.github.mallma.removereels.budget.BlockReason.NoAllowance
import io.github.mallma.removereels.budget.BlockReason.UsedUp
import io.github.mallma.removereels.budget.Decision.Allow
import io.github.mallma.removereels.budget.Decision.Block
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class BlockPolicyTest {
    private val today = LocalDate.of(2026, 11, 2)
    private val youtube15 = AllowanceSchedule(allowances(instagram = 0, youtube = 15))

    private fun decide(
        app: TargetApp,
        schedule: AllowanceSchedule = youtube15,
        ledger: UsageLedger = UsageLedger(),
        automaticTimeOn: Boolean = true,
    ) = BlockPolicy.decide(app, today, schedule, ledger, automaticTimeOn)

    @Test
    fun `blocks both apps after installing`() {
        assertThat(decide(TargetApp.INSTAGRAM, AllowanceSchedule.INITIAL)).isEqualTo(Block(NoAllowance))
        assertThat(decide(TargetApp.YOUTUBE, AllowanceSchedule.INITIAL)).isEqualTo(Block(NoAllowance))
    }

    @Test
    fun `blocks an app without allowance while the other has time`() {
        assertThat(decide(TargetApp.INSTAGRAM)).isEqualTo(Block(NoAllowance))
    }

    @Test
    fun `allows the time that is left today`() {
        val ledger = UsageLedger().record(today, TargetApp.YOUTUBE, 8.minutes)

        assertThat(decide(TargetApp.YOUTUBE, ledger = ledger)).isEqualTo(Allow(7.minutes))
    }

    @Test
    fun `blocks once today's allowance is used up`() {
        val ledger = UsageLedger().record(today, TargetApp.YOUTUBE, 15.minutes + 2.seconds)

        assertThat(decide(TargetApp.YOUTUBE, ledger = ledger)).isEqualTo(Block(UsedUp(Allowance.ofMinutes(15))))
    }

    @Test
    fun `yesterday's use does not count today`() {
        val ledger = UsageLedger().record(today.minusDays(1), TargetApp.YOUTUBE, 15.minutes)

        assertThat(decide(TargetApp.YOUTUBE, ledger = ledger)).isEqualTo(Allow(15.minutes))
    }

    @Test
    fun `blocks every app while automatic date and time is off`() {
        assertThat(decide(TargetApp.YOUTUBE, automaticTimeOn = false)).isEqualTo(Block(AutomaticTimeOff))
    }

    @Test
    fun `a blocked app gives its own reason even while automatic time is off`() {
        assertThat(decide(TargetApp.INSTAGRAM, automaticTimeOn = false)).isEqualTo(Block(NoAllowance))
    }

    @Test
    fun `an allowance saved today does not unlock anything today`() {
        val schedule = AllowanceSchedule.INITIAL.scheduleChange(allowances(instagram = 45, youtube = 0), today)

        assertThat(decide(TargetApp.INSTAGRAM, schedule)).isEqualTo(Block(NoAllowance))
        assertThat(BlockPolicy.decide(TargetApp.INSTAGRAM, today.plusDays(1), schedule, UsageLedger(), true))
            .isEqualTo(Allow(45.minutes))
    }
}
