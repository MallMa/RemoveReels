package io.github.mallma.removereels.budget

import kotlin.time.Duration
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/**
 * Measures watching time on a monotonic [timeSource], so changing the wall clock neither adds nor removes time.
 *
 * Callers take a [lap] every few seconds and record it under the current date, which splits a session that crosses
 * midnight between the two days.
 */
public class WatchTimer(
    private val timeSource: TimeSource,
) {
    private var mark: TimeMark? = null

    public val isRunning: Boolean get() = mark != null

    /** Starts measuring; does nothing if already running. */
    public fun start() {
        if (mark == null) mark = timeSource.markNow()
    }

    /** Time since [start] or the previous lap; zero when not running. */
    public fun lap(): Duration {
        val previous = mark ?: return Duration.ZERO
        val elapsed = previous.elapsedNow()
        mark = previous + elapsed
        return elapsed
    }

    /** The final lap, after which the timer is stopped. */
    public fun stop(): Duration = lap().also { mark = null }
}
