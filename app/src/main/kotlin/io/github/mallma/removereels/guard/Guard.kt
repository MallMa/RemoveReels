package io.github.mallma.removereels.guard

import io.github.mallma.removereels.AutomaticTime
import io.github.mallma.removereels.budget.BlockPolicy
import io.github.mallma.removereels.budget.BlockReason
import io.github.mallma.removereels.budget.Decision
import io.github.mallma.removereels.budget.TargetApp
import io.github.mallma.removereels.budget.WatchTimer
import io.github.mallma.removereels.detection.CanaryResult
import io.github.mallma.removereels.detection.EntryPoint
import io.github.mallma.removereels.detection.ReelsPageRule
import io.github.mallma.removereels.detection.SurfaceRule
import io.github.mallma.removereels.storage.StateStore
import kotlinx.coroutines.flow.first
import java.time.Clock
import java.time.LocalDate
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * A target app in front with the screen on: the blocked surface it shows, if any, its entry points to that surface, the
 * page it shows that lists only Reels, if any, and its canary's result.
 */
internal data class Screen(
    val app: TargetApp,
    val surface: SurfaceRule?,
    val canary: CanaryResult = CanaryResult.NOT_APPLICABLE,
    val entryPoints: List<EntryPoint> = emptyList(),
    val reelsPage: ReelsPageRule? = null,
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

    /** Covers exactly [entryPoints]; none removes every cover. */
    fun cover(entryPoints: List<EntryPoint>)

    /** Turns the pager of [page], a page of [app] that lists only Reels, back to the page before. */
    fun leave(
        app: TargetApp,
        page: ReelsPageRule,
    )
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

    /** Watching time of [watching] on [unsavedOn] that is not saved yet; see [count]. */
    private var unsaved = Duration.ZERO
    private var unsavedOn = LocalDate.MIN

    /** An allowed Reels or Shorts screen is on, and its time counts. */
    val isWatching: Boolean get() = watching != null

    /** Entry points are covered, so the screen must be looked at again even without events. */
    var isCovering: Boolean = false
        private set

    /** The Reels page last left and still in view, which is not left again; see [leaveReelsPage]. */
    private var leftPage: ReelsPageRule? = null

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
            // Counted after the session has ended, so that a failed write cannot keep it running.
            count(watched, today, watchedTime, saveNow = !stillWatching)
        }
        val blocked = screen != null && surface == null && decision(screen.app, today) is Decision.Block
        coverEntryPoints(screen, blocked)
        leaveReelsPage(screen, blocked)
        if (surface == null) {
            actions.surfaceGone()
        } else {
            decide(screen.app, surface, today)
        }
        if (screen != null) reportCanary(screen)
    }

    /**
     * Covers the entry points while the app is [blocked] (ADR 0008). With allowance left they stay visible, and over a
     * blocked surface the block card shows instead.
     */
    private fun coverEntryPoints(
        screen: Screen?,
        blocked: Boolean,
    ) {
        val entryPoints = screen?.entryPoints?.takeIf { blocked }.orEmpty()
        isCovering = entryPoints.isNotEmpty()
        actions.cover(entryPoints)
    }

    /**
     * Leaves a page that lists only Reels while the app is [blocked] (ADR 0009), once each time it comes into view: if
     * the app does not move, the guard does not keep trying against the user.
     */
    private fun leaveReelsPage(
        screen: Screen?,
        blocked: Boolean,
    ) {
        val page = screen?.reelsPage?.takeIf { blocked }
        if (page != null && page != leftPage) actions.leave(screen.app, page)
        leftPage = page
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
        when (val decision = decision(app, today)) {
            is Decision.Allow -> {
                watching = app
                timer.start()
                actions.showTimeLeft(app, decision.remaining)
            }

            is Decision.Block -> {
                if (watching != null) {
                    timer.stop()
                    endSession()
                    save(app)
                }
                actions.block(app, surface, decision.reason)
            }
        }
    }

    /** What the rules decide for [app] now, counting the watching time not saved yet. */
    private suspend fun decision(
        app: TargetApp,
        today: LocalDate,
    ): Decision {
        val state = store.state.first()
        val usage =
            if (app == watching &&
                unsaved.isPositive()
            ) {
                state.usage.record(unsavedOn, app, unsaved)
            } else {
                state.usage
            }
        return BlockPolicy.decide(app, today, state.schedule, usage, automaticTime.isOn())
    }

    /**
     * Adds [watched] to the unsaved time, and saves it every [SAVE_INTERVAL], when the date changes and when [saveNow]
     * ends a session. Saving on every check would rewrite the state file up to four times a second.
     */
    private suspend fun count(
        app: TargetApp,
        today: LocalDate,
        watched: Duration,
        saveNow: Boolean,
    ) {
        if (unsaved.isPositive() && today != unsavedOn) save(app)
        unsaved += watched
        unsavedOn = today
        if (saveNow || unsaved >= SAVE_INTERVAL) save(app)
    }

    private suspend fun save(app: TargetApp) {
        val time = unsaved
        unsaved = Duration.ZERO
        if (time.isPositive()) store.recordUsage(app, unsavedOn, time)
    }

    private fun endSession() {
        watching = null
        actions.hideTimeLeft()
    }

    private companion object {
        const val STALE_CHECKS = 8

        /** At most this much watching time is lost if the service stops, well within the 5 s accuracy target. */
        val SAVE_INTERVAL = 5.seconds
    }
}
