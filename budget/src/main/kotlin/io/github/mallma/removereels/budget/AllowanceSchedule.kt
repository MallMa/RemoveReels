package io.github.mallma.removereels.budget

import java.time.LocalDate

/**
 * Today's allowances and an optional change that starts on a later day.
 *
 * A change never applies on the day it is made, whether it raises or lowers an allowance, so there is no way to get
 * more time in a weak moment (ADR 0003).
 */
public data class AllowanceSchedule(
    public val current: Allowances,
    public val change: ScheduledChange? = null,
) {
    /** [allowances] replace the current ones from the start of [startsOn]. */
    public data class ScheduledChange(
        public val allowances: Allowances,
        public val startsOn: LocalDate,
    )

    /** The allowances in effect on [date]. */
    public fun allowancesOn(date: LocalDate): Allowances = settledOn(date).current

    /** The change that has not started by [date], if there is one. */
    public fun changeAfter(date: LocalDate): ScheduledChange? = settledOn(date).change

    /**
     * Schedules [allowances] to start the day after [today], replacing any change that has not started yet. Choosing
     * today's allowances again cancels the change.
     */
    public fun scheduleChange(
        allowances: Allowances,
        today: LocalDate,
    ): AllowanceSchedule {
        val settled = settledOn(today)
        val change = if (allowances == settled.current) null else ScheduledChange(allowances, today.plusDays(1))
        return settled.copy(change = change)
    }

    /** This schedule with a change that has started by [date] made current. */
    public fun settledOn(date: LocalDate): AllowanceSchedule =
        if (change != null && !date.isBefore(change.startsOn)) AllowanceSchedule(change.allowances) else this

    public companion object {
        /** Both apps blocked and nothing scheduled: the state after installing. */
        public val INITIAL: AllowanceSchedule = AllowanceSchedule(Allowances.BLOCKED)
    }
}
