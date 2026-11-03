package io.github.mallma.removereels.guard

import android.accessibilityservice.AccessibilityService
import android.os.PowerManager
import androidx.compose.runtime.mutableStateOf
import io.github.mallma.removereels.AppContainer
import io.github.mallma.removereels.budget.BlockReason
import io.github.mallma.removereels.budget.TargetApp
import io.github.mallma.removereels.budget.WatchTimer
import io.github.mallma.removereels.detection.SurfaceRule
import io.github.mallma.removereels.guard.overlay.BlockCard
import io.github.mallma.removereels.guard.overlay.BlockNotice
import io.github.mallma.removereels.guard.overlay.OverlayWindow
import io.github.mallma.removereels.guard.overlay.TimerPill
import io.github.mallma.removereels.packageName
import io.github.mallma.removereels.targetAppOf
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
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
    private val power = service.getSystemService(PowerManager::class.java)
    private val audioFocus = AudioFocus(service)
    private val blockCard = OverlayWindow(service, OverlayWindow.fullScreen())
    private val timerPill = OverlayWindow(service, OverlayWindow.topCentre())
    private val timeLeft = mutableStateOf(Duration.ZERO)
    private var timerHiddenByUser = false
    private val backOut = BackOut(ServiceNavigation(service) { readScreen()?.surface != null }, BACK_OUT_SETTLE)
    private val guard =
        Guard(container.clock, container.automaticTime, container.stateStore, WatchTimer(container.timeSource), this)
    private val checks =
        CheckLoop(
            timeSource = container.timeSource,
            spacing = CHECK_SPACING,
            tick = WATCH_TICK,
            ticking = { guard.isWatching },
            check = { guard.check(readScreen()) },
        )
    private val scope = MainScope()

    fun start() {
        scope.launch { checks.run() }
    }

    /** Something changed in Instagram or YouTube; [windowChanged] means a new screen opened. */
    fun onEvent(windowChanged: Boolean) = checks.request(urgent = windowChanged)

    fun stop() {
        scope.cancel()
        blockCard.destroy()
        timerPill.destroy()
        audioFocus.release()
    }

    override fun showTimeLeft(
        app: TargetApp,
        remaining: Duration,
    ) {
        timeLeft.value = remaining
        if (!timerHiddenByUser && !timerPill.isShowing) {
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
        blockCard.show { BlockCard(BlockNotice(app, reason, surface.openedFromMessages), onContinue = blockCard::hide) }
        val rules = detector.rulesFor(app.packageName)
        backOut.leave(rules?.homeTabViewId?.let(rules::fullViewId))
    }

    override fun surfaceGone() = audioFocus.release()

    /** Null while the screen is off or neither app is in front. */
    private fun readScreen(): Screen? {
        val root = service.rootInActiveWindow?.takeIf { power.isInteractive }
        val app = targetAppOf(root?.packageName)
        return if (root == null || app == null) {
            null
        } else {
            val tree = AccessibilityScreenTree(root)
            Screen(app, detector.findSurface(app.packageName, tree), detector.checkCanary(app.packageName, tree))
        }
    }

    private companion object {
        /** At most four checks per second while the content of a screen changes. */
        val CHECK_SPACING = 250.milliseconds

        /** How often watching time is counted when nothing else happens. */
        val WATCH_TICK = 1.seconds

        /** How long a back press or tap gets before the screen is checked again. */
        val BACK_OUT_SETTLE = 400.milliseconds
    }
}
