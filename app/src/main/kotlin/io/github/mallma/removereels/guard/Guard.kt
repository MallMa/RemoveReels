package io.github.mallma.removereels.guard

import io.github.mallma.removereels.AutomaticTime
import io.github.mallma.removereels.budget.BlockPolicy
import io.github.mallma.removereels.budget.BlockReason
import io.github.mallma.removereels.budget.Decision
import io.github.mallma.removereels.budget.TargetApp
import io.github.mallma.removereels.budget.WatchTimer
import io.github.mallma.removereels.detection.CanaryResult
import io.github.mallma.removereels.detection.SurfaceRule
import io.github.mallma.removereels.storage.StateStore
import kotlinx.coroutines.flow.first
import java.time.Clock
import java.time.LocalDate
import kotlin.time.Duration

/** A target app in front with the screen on, the blocked surface it shows, if any, and its canary's result. */
internal data class Screen(
    val app: TargetApp,
    val surface: SurfaceRule?,
    val canary: CanaryResult = CanaryResult.NOT_APPLICABLE,
)

/** What the guard asks the accessibility service to do. */
internal interface GuardActions {
    fun showTimeLeft(
        app: TargetApp,
        remaining: Duration,
    )

    fun hideTimeLeft()

    /** Stops the video, explains the block and leaves [surface]; returns once the steps are done. */
    suspend fun block(
        app: TargetApp,
        surface: SurfaceRule,
        reason: BlockReason,
    )

    /** No blocked surface is on screen. */
    fun surfaceGone()
}

/**
 * Applies the product rules to what is on screen: counts the time an allowed Reels or Shorts screen is visible, and
 * blocks it when no allowance is left. The service calls [check] whenever the screen may have changed, and every
 * second while [isWatching].
 */
internal class Guard(
    private val clock: Clock,
    private val automaticTime: AutomaticTime,
    private val store: StateStore,
    private val timer: WatchTimer,
    private val actions: GuardActions,
) {
    private var watching: TargetApp? = null
    private val staleChecks = mutableMapOf<TargetApp, Int>()

    /** An allowed Reels or Shorts screen is on, and its time counts. */
    val isWatching: Boolean get() = watching != null

    /**
     * [screen] is null when no target app is in front or the screen is off; neither counts as watching. Throws an
     * [java.io.IOException] if the stored state cannot be read or saved.
     */
    suspend fun check(screen: Screen?) {
        val today = LocalDate.now(clock)
        val surface = screen?.surface
        val watched = watching
        if (watched != null) {
            val stillWatching = surface != null && screen.app == watched
            val watchedTime = if (stillWatching) timer.lap() else timer.stop()
            if (!stillWatching) endSession()
            // Saved after the session has ended, so that a failed write cannot keep it running.
            store.recordUsage(watched, today, watchedTime)
        }
        if (surface == null) {
            actions.surfaceGone()
        } else {
            decide(screen.app, surface, today)
        }
        if (screen != null) reportCanary(screen)
    }

    /**
     * A layout can be briefly incomplete while it loads, so the rules count as stale only after [STALE_CHECKS] stale
     * checks in a row, and as fine again after one healthy check.
     */
    private suspend fun reportCanary(screen: Screen) {
        if (screen.canary == CanaryResult.NOT_APPLICABLE) return
        val staleInARow = if (screen.canary == CanaryResult.STALE) staleChecks.getOrDefault(screen.app, 0) + 1 else 0
        staleChecks[screen.app] = staleInARow
        when {
            staleInARow == 0 -> store.setRulesStale(screen.app, stale = false)
            staleInARow >= STALE_CHECKS -> store.setRulesStale(screen.app, stale = true)
        }
    }

    private suspend fun decide(
        app: TargetApp,
        surface: SurfaceRule,
        today: LocalDate,
    ) {
        val state = store.state.first()
        when (val decision = BlockPolicy.decide(app, today, state.schedule, state.usage, automaticTime.isOn())) {
            is Decision.Allow -> {
                watching = app
                timer.start()
                actions.showTimeLeft(app, decision.remaining)
            }

            is Decision.Block -> {
                if (watching != null) {
                    timer.stop()
                    endSession()
                }
                actions.block(app, surface, decision.reason)
            }
        }
    }

    private fun endSession() {
        watching = null
        actions.hideTimeLeft()
    }

    private companion object {
        const val STALE_CHECKS = 8
    }
}
