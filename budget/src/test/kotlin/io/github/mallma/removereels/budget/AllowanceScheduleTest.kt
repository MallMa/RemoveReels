package io.github.mallma.removereels.budget

import com.google.common.truth.Truth.assertThat
import io.github.mallma.removereels.budget.AllowanceSchedule.ScheduledChange
import org.junit.jupiter.api.Test
import java.time.LocalDate

class AllowanceScheduleTest {
    private val today = LocalDate.of(2026, 11, 2)
    private val tomorrow = today.plusDays(1)

    @Test
    fun `starts with both apps blocked and nothing scheduled`() {
        val schedule = AllowanceSchedule.INITIAL

        assertThat(schedule.allowancesOn(today)).isEqualTo(Allowances.BLOCKED)
        assertThat(schedule.changeAfter(today)).isNull()
    }

    @Test
    fun `a raised allowance starts tomorrow, not today`() {
        val schedule = AllowanceSchedule.INITIAL.scheduleChange(allowances(instagram = 20, youtube = 0), today)

        assertThat(schedule.allowancesOn(today)).isEqualTo(Allowances.BLOCKED)
        assertThat(schedule.allowancesOn(tomorrow)).isEqualTo(allowances(instagram = 20, youtube = 0))
    }

    @Test
    fun `a lowered allowance also starts tomorrow`() {
        val schedule =
            AllowanceSchedule(allowances(instagram = 30, youtube = 15))
                .scheduleChange(Allowances.BLOCKED, today)

        assertThat(schedule.allowancesOn(today)).isEqualTo(allowances(instagram = 30, youtube = 15))
        assertThat(schedule.allowancesOn(tomorrow)).isEqualTo(Allowances.BLOCKED)
    }

    @Test
    fun `a change stays in effect on every later day`() {
        val schedule = AllowanceSchedule.INITIAL.scheduleChange(allowances(instagram = 0, youtube = 15), today)

        assertThat(schedule.allowancesOn(today.plusDays(30))).isEqualTo(allowances(instagram = 0, youtube = 15))
    }

    @Test
    fun `scheduling again the same day replaces the change, still from tomorrow`() {
        val schedule =
            AllowanceSchedule.INITIAL
                .scheduleChange(allowances(instagram = 20, youtube = 0), today)
                .scheduleChange(allowances(instagram = 5, youtube = 10), today)

        assertThat(schedule.allowancesOn(today)).isEqualTo(Allowances.BLOCKED)
        assertThat(schedule.changeAfter(today))
            .isEqualTo(ScheduledChange(allowances(instagram = 5, youtube = 10), tomorrow))
    }

    @Test
    fun `choosing today's allowances again cancels the change`() {
        val schedule =
            AllowanceSchedule.INITIAL
                .scheduleChange(allowances(instagram = 20, youtube = 0), today)
                .scheduleChange(Allowances.BLOCKED, today)

        assertThat(schedule.changeAfter(today)).isNull()
        assertThat(schedule.allowancesOn(tomorrow)).isEqualTo(Allowances.BLOCKED)
    }

    @Test
    fun `scheduling on the day a change starts builds on the started change`() {
        val schedule =
            AllowanceSchedule.INITIAL
                .scheduleChange(allowances(instagram = 20, youtube = 0), today)
                .scheduleChange(allowances(instagram = 10, youtube = 0), tomorrow)

        assertThat(schedule.allowancesOn(tomorrow)).isEqualTo(allowances(instagram = 20, youtube = 0))
        assertThat(schedule.allowancesOn(tomorrow.plusDays(1))).isEqualTo(allowances(instagram = 10, youtube = 0))
    }

    @Test
    fun `settling folds a started change into the current allowances`() {
        val schedule = AllowanceSchedule.INITIAL.scheduleChange(allowances(instagram = 20, youtube = 0), today)

        assertThat(schedule.settledOn(today)).isEqualTo(schedule)
        assertThat(schedule.settledOn(tomorrow)).isEqualTo(AllowanceSchedule(allowances(instagram = 20, youtube = 0)))
    }
}
