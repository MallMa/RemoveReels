package io.github.mallma.removereels.budget

import java.time.LocalDate
import kotlin.time.Duration

/** How long each app showed Reels or Shorts on each of the last [RETENTION_DAYS] days. */
public data class UsageLedger(
    public val days: Map<LocalDate, Map<TargetApp, Duration>> = emptyMap(),
) {
    public fun usedOn(
        date: LocalDate,
        app: TargetApp,
    ): Duration = days[date]?.get(app) ?: Duration.ZERO

    /** Adds [watched] to [app]'s time on [date] and forgets days more than [RETENTION_DAYS] before it. */
    public fun record(
        date: LocalDate,
        app: TargetApp,
        watched: Duration,
    ): UsageLedger {
        require(!watched.isNegative()) { "Watched time cannot be negative: $watched" }
        val day = days[date].orEmpty() + (app to usedOn(date, app) + watched)
        val oldestKept = date.minusDays(RETENTION_DAYS - 1)
        return UsageLedger((days + (date to day)).filterKeys { !it.isBefore(oldestKept) })
    }

    public companion object {
        public const val RETENTION_DAYS: Long = 7
    }
}
