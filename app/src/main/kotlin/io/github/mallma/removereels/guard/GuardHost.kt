package io.github.mallma.removereels.guard

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import io.github.mallma.removereels.AppContainer
import io.github.mallma.removereels.MainActivity
import io.github.mallma.removereels.budget.BlockReason
import io.github.mallma.removereels.budget.TargetApp
import io.github.mallma.removereels.budget.WatchTimer
import io.github.mallma.removereels.detection.EntryPoint
import io.github.mallma.removereels.detection.SurfaceRule
import io.github.mallma.removereels.guard.overlay.BlockCard
import io.github.mallma.removereels.guard.overlay.BlockNotice
import io.github.mallma.removereels.guard.overlay.EntryPointCovers
import io.github.mallma.removereels.guard.overlay.OverlayWindow
import io.github.mallma.removereels.guard.overlay.TimerPill
import io.github.mallma.removereels.packageName
import io.github.mallma.removereels.storage.AppState
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException
import java.time.LocalDate
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Runs the [Guard] inside the accessibility service and carries out its decisions: reads the screen, pauses the
 * video, shows the block card and the time left, and backs out of blocked screens.
 */
internal class GuardHost(
    private val service: AccessibilityService,
    container: AppContainer,
) : GuardActions {
    private val detector = container.detector
    private val clock = container.clock
    private val store = container.stateStore
    private val status = container.guardStatus
    private val screenReader = ScreenReader(service, detector)
    private val audioFocus = AudioFocus(service)
    private val blockCard = OverlayWindow(service, OverlayWindow.fullScreen())
    private val timerPill = OverlayWindow(service, OverlayWindow.topCentre())
    private val covers = EntryPointCovers(service)
    private val timeLeft = mutableStateOf(Duration.ZERO)
    private var timerHiddenByUser = false
    private val backOut = BackOut(ServiceNavigation(service, screenReader::isOnSurface), BACK_OUT_SETTLE)
    private val guard =
        Guard(container.clock, container.automaticTime, container.stateStore, WatchTimer(container.timeSource), this)
    private val checks =
        CheckLoop(
            timeSource = container.timeSource,
            spacing = CHECK_SPACING,
            tick = WATCH_TICK,
            ticking = { guard.isWatching || guard.isCovering },
            check = ::checkScreen,
        )
    private val scope = MainScope()
    private val state = store.state.stateIn(scope, SharingStarted.Eagerly, AppState.INITIAL)

    fun start() {
        scope.launch { checks.run() }
        if (status.takeTurnOnRequest()) returnToApp()
    }

    /** Something changed in Instagram or YouTube; [windowChanged] means a new screen opened. */
    fun onEvent(windowChanged: Boolean) = checks.request(urgent = windowChanged)

    fun stop() {
        scope.cancel()
        blockCard.destroy()
        timerPill.destroy()
        covers.hide()
        audioFocus.release()
    }

    override fun showTimeLeft(
        app: TargetApp,
        remaining: Duration,
    ) {
        timeLeft.value = remaining
        if (state.value.showTimeLeft && !timerHiddenByUser && !timerPill.isShowing) {
            timerPill.show {
                TimerPill(
                    remaining = timeLeft.value,
                    onHide = {
                        timerHiddenByUser = true
                        timerPill.hide()
                    },
                )
            }
        }
    }

    override fun hideTimeLeft() {
        timerPill.hide()
        timerHiddenByUser = false
    }

    override suspend fun block(
        app: TargetApp,
        surface: SurfaceRule,
        reason: BlockReason,
    ) {
        audioFocus.hold()
        status.blocked()
        val tomorrow = state.value.schedule.allowancesOn(LocalDate.now(clock).plusDays(1))[app]
        val notice = BlockNotice(app, reason, surface.openedFromMessages, allowedTomorrow = !tomorrow.isBlocked)
        blockCard.show { BlockCard(notice, onContinue = blockCard::hide) }
        val rules = detector.rulesFor(app.packageName)
        backOut.leave(rules?.homeTabViewId?.let(rules::fullViewId))
        // The back-out may end on the launcher, which sends no events; look again so that audio focus is released.
        checks.request(urgent = true)
    }

    override fun surfaceGone() = audioFocus.release()

    override fun cover(entryPoints: List<EntryPoint>) = covers.show(entryPoints)

    private suspend fun checkScreen() {
        val screen = screenReader.read()
        try {
            guard.check(screen)
        } catch (e: IOException) {
            // Without the stored state the rules cannot be applied. Blocking is the safe answer, as for a corrupt file,
            // and an exception here would crash the service, which Android then does not restart.
            Log.e(TAG, "Stored state unavailable, blocking", e)
            screen?.surface?.let { block(screen.app, it, BlockReason.NoAllowance) }
        }
    }

    /**
     * A Turn on button sent the user to Android's settings; this brings them back as soon as the switch is on. An
     * accessibility service may start an activity from the background.
     */
    private fun returnToApp() {
        service.startActivity(
            Intent(service, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT),
        )
    }

    private companion object {
        /** At most four checks per second while the content of a screen changes. */
        val CHECK_SPACING = 250.milliseconds

        /** How often watching time is counted when nothing else happens. */
        val WATCH_TICK = 1.seconds

        const val TAG = "GuardHost"

        /** How long a back press or tap gets before the screen is checked again. */
        val BACK_OUT_SETTLE = 400.milliseconds
    }
}
