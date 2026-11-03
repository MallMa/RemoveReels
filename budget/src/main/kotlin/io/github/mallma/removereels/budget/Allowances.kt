package io.github.mallma.removereels.budget

import io.github.mallma.removereels.budget.Allowance.Companion.DAILY_MAXIMUM_MINUTES

/** The allowance of each app. Together they never exceed [DAILY_MAXIMUM_MINUTES]. */
public data class Allowances(
    public val instagram: Allowance,
    public val youtube: Allowance,
) {
    init {
        require(instagram.minutes + youtube.minutes <= DAILY_MAXIMUM_MINUTES) {
            "Allowances add up to ${instagram.minutes + youtube.minutes} minutes, more than $DAILY_MAXIMUM_MINUTES"
        }
    }

    public operator fun get(app: TargetApp): Allowance =
        when (app) {
            TargetApp.INSTAGRAM -> instagram
            TargetApp.YOUTUBE -> youtube
        }

    /** A copy with [allowance] for [app]. Fails if both apps together would exceed the daily maximum. */
    public fun with(
        app: TargetApp,
        allowance: Allowance,
    ): Allowances =
        when (app) {
            TargetApp.INSTAGRAM -> copy(instagram = allowance)
            TargetApp.YOUTUBE -> copy(youtube = allowance)
        }

    /** The most minutes [app] can have while the other app keeps its allowance. */
    public fun maximumMinutesFor(app: TargetApp): Int =
        when (app) {
            TargetApp.INSTAGRAM -> DAILY_MAXIMUM_MINUTES - youtube.minutes
            TargetApp.YOUTUBE -> DAILY_MAXIMUM_MINUTES - instagram.minutes
        }

    public companion object {
        /** Both apps blocked: the state after installing. */
        public val BLOCKED: Allowances = Allowances(Allowance.BLOCKED, Allowance.BLOCKED)
    }
}
