package io.github.mallma.removereels.budget

import java.time.LocalDate
import kotlin.time.Duration

/** Whether a Reels or Shorts screen may stay open. */
public sealed interface Decision {
    /** It may, for [remaining] more today. */
    public data class Allow(
        public val remaining: Duration,
    ) : Decision

    /** It may not, because of [reason]. */
    public data class Block(
        public val reason: BlockReason,
    ) : Decision
}

public sealed interface BlockReason {
    /** Today's allowance for the app is "blocked". */
    public data object NoAllowance : BlockReason

    /** Today's [allowance] has been used up. */
    public data class UsedUp(
        public val allowance: Allowance,
    ) : BlockReason

    /** Android's automatic date, time or time zone is off, which suspends every allowance. */
    public data object AutomaticTimeOff : BlockReason
}

/** The product rules that decide whether a Reels or Shorts screen is blocked. */
public object BlockPolicy {
    /**
     * Decides for [app] on [today]. Allowances apply only while [automaticTimeOn], so changing the date cannot unlock
     * another day's time.
     */
    public fun decide(
        app: TargetApp,
        today: LocalDate,
        schedule: AllowanceSchedule,
        ledger: UsageLedger,
        automaticTimeOn: Boolean,
    ): Decision {
        val allowance = schedule.allowancesOn(today)[app]
        val remaining = allowance.duration - ledger.usedOn(today, app)
        return when {
            allowance.isBlocked -> Decision.Block(BlockReason.NoAllowance)
            !automaticTimeOn -> Decision.Block(BlockReason.AutomaticTimeOff)
            remaining.isPositive() -> Decision.Allow(remaining)
            else -> Decision.Block(BlockReason.UsedUp(allowance))
        }
    }
}
