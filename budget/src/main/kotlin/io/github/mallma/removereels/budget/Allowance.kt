package io.github.mallma.removereels.budget

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/** How long one app may show Reels or Shorts per day: [BLOCKED], or 5 to 45 minutes in steps of 5. */
@JvmInline
public value class Allowance private constructor(
    public val minutes: Int,
) {
    public val isBlocked: Boolean get() = minutes == 0

    public val duration: Duration get() = minutes.minutes

    public companion object {
        public const val STEP_MINUTES: Int = 5

        /** The most one app may get per day, and also the most both apps may get together. */
        public const val DAILY_MAXIMUM_MINUTES: Int = 45

        public val BLOCKED: Allowance = Allowance(0)

        /** The allowance of [minutes]; 0 means [BLOCKED]. */
        public fun ofMinutes(minutes: Int): Allowance {
            require(minutes in 0..DAILY_MAXIMUM_MINUTES && minutes % STEP_MINUTES == 0) {
                "An allowance is 0 to $DAILY_MAXIMUM_MINUTES minutes in steps of $STEP_MINUTES, not $minutes"
            }
            return Allowance(minutes)
        }
    }
}
