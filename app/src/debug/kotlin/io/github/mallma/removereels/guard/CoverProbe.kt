package io.github.mallma.removereels.guard

import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Rect
import android.os.SystemClock
import android.util.Log
import android.view.Choreographer
import android.view.accessibility.AccessibilityEvent
import java.io.File

/**
 * Debug builds only, for the H0 spike in `docs/hiding-plan.md`: covers the Reels tiles of Instagram's grids and the
 * Reels tab on profiles, follows them while they scroll, and measures how far a cover is from what it covers. Only
 * timings and pixel positions are saved, never what the screen shows.
 *
 * ```
 * adb shell am broadcast -a io.github.mallma.removereels.PROBE --es track frames --es draw canvas --es name run1
 * adb shell am broadcast -a io.github.mallma.removereels.PROBE --es track off
 * adb exec-out run-as io.github.mallma.removereels cat files/probe/run1.csv
 * ```
 *
 * - `track`: `events` moves covers on accessibility events only; `frames` also asks for one target's position on every
 *   display frame from an event until the screen is still again; `poll` asks on every frame while covers show; `off`
 *   stops and saves.
 * - `draw`: `windows` is one touchable window per cover, as in H1; `pass` the same windows letting touches through;
 *   `canvas` one window over the whole screen that draws every cover and lets touches through.
 * - `measure`: also reads the position on every frame, only to measure how far the covers are from it.
 * - `cover`: `tiles` covers each Reels tile; `grid` covers a whole grid that shows any.
 */
internal class CoverProbe(
    private val service: AccessibilityService,
) {
    private val choreographer by lazy { Choreographer.getInstance() }
    private var run: Run? = null

    private val receiver =
        object : BroadcastReceiver() {
            override fun onReceive(
                context: Context,
                intent: Intent,
            ) {
                run?.let(::finish)
                val track = intent.getStringExtra("track").orEmpty()
                if (track.isNotEmpty() && track != "off") {
                    val draw = intent.getStringExtra("draw").orEmpty().ifEmpty { "canvas" }
                    start(
                        Run(
                            name = intent.getStringExtra("name").orEmpty().ifEmpty { "probe" },
                            track = Track.valueOf(track.uppercase()),
                            measure = intent.getBooleanExtra("measure", false),
                            wholeGrid = intent.getStringExtra("cover") == "grid",
                            covers = coversFor(Draw.valueOf(draw.uppercase())),
                        ),
                    )
                }
            }
        }

    private val frame =
        object : Choreographer.FrameCallback {
            override fun doFrame(frameTimeNanos: Long) {
                val run = run ?: return
                val tracking = run.isTracking()
                when {
                    run.changed -> find(run)
                    tracking || run.measure -> onFrame(run, tracking)
                }
                if (run.needsFrames()) {
                    choreographer.postFrameCallback(this)
                } else {
                    run.looping = false
                }
            }
        }

    fun start() {
        service.registerReceiver(
            receiver,
            IntentFilter(ACTION),
            Manifest.permission.DUMP,
            null,
            Context.RECEIVER_EXPORTED,
        )
    }

    fun stop() {
        service.unregisterReceiver(receiver)
        run?.let(::finish)
    }

    fun onEvent(event: AccessibilityEvent) {
        val run = run
        val kind = EVENT_KINDS[event.eventType]
        if (run != null && kind != null && event.packageName == INSTAGRAM) {
            run.sample(kind, trueTop = null, micros = 0, event.scrollDeltaY)
            run.changed = true
            run.trackUntil = SystemClock.uptimeMillis() + IDLE_MS
            loop(run)
        }
    }

    /** Finds the targets again on the next frame after events, however many arrived, and moves the covers onto them. */
    private fun find(run: Run) {
        run.changed = false
        val started = SystemClock.elapsedRealtimeNanos()
        val targets = ProbeTargets.find(service, run.wholeGrid)
        val micros = (SystemClock.elapsedRealtimeNanos() - started) / NANOS_PER_MICRO
        run.queries += targets.queries
        run.queryMicros += micros
        run.sample("find", targets.anchorBounds?.top, micros, scrollDy = 0)
        run.targets = targets
        run.shownTop = targets.anchorBounds?.top
        run.lastAnchor = targets.anchorBounds
        run.covers.show(targets.visibleRects())
    }

    private fun loop(run: Run) {
        if (!run.looping && run.needsFrames()) {
            run.looping = true
            choreographer.postFrameCallback(frame)
        }
    }

    /** Asks for the anchor's position; while [tracking], moves every cover as far as the anchor moved. */
    private fun onFrame(
        run: Run,
        tracking: Boolean,
    ) {
        val targets = run.targets
        val anchor = targets?.anchor ?: return
        val started = SystemClock.elapsedRealtimeNanos()
        val bounds = if (anchor.refresh() && anchor.isVisibleToUser) anchor.boundsInScreen().toRect() else null
        val micros = (SystemClock.elapsedRealtimeNanos() - started) / NANOS_PER_MICRO
        run.queries++
        run.queryMicros += micros
        run.sample("frame", bounds?.top, micros, scrollDy = 0)
        val dy = run.lastAnchor?.let { last -> bounds?.let { shift(last, it) } } ?: 0
        run.lastAnchor = bounds
        if (tracking && dy != 0) {
            val moved = targets.movedBy(dy)
            run.targets = moved
            run.shownTop = bounds?.top
            run.covers.show(moved.visibleRects())
            run.trackUntil = SystemClock.uptimeMillis() + IDLE_MS
        }
    }

    /** How far the anchor moved; a view clipped by its container keeps one edge still, so the other edge counts. */
    private fun shift(
        last: Rect,
        now: Rect,
    ): Int =
        when {
            now.height() == last.height() -> now.top - last.top
            now.top == last.top -> now.bottom - last.bottom
            else -> now.top - last.top
        }

    private fun start(run: Run) {
        this.run = run
        service.serviceInfo =
            service.serviceInfo.apply { eventTypes = eventTypes or AccessibilityEvent.TYPE_VIEW_SCROLLED }
        Log.i(TAG, "Started ${run.name}: ${run.track} measure=${run.measure}")
        loop(run)
    }

    private fun finish(run: Run) {
        this.run = null
        run.covers.hide()
        service.serviceInfo =
            service.serviceInfo.apply { eventTypes = eventTypes and AccessibilityEvent.TYPE_VIEW_SCROLLED.inv() }
        val file = File(service.filesDir, "probe/${run.name}.csv")
        file.parentFile?.mkdirs()
        file.writeText(run.samples.toString())
        Log.i(TAG, "Saved ${run.name}: ${run.queries} queries, ${run.queryMicros} us, to $file")
    }

    private fun coversFor(draw: Draw): ProbeCovers {
        val color = if (service.resources.configuration.isNightModeActive) DARK else LIGHT
        return when (draw) {
            Draw.WINDOWS -> WindowCovers(service, touchable = true, color)
            Draw.PASS -> WindowCovers(service, touchable = false, color)
            Draw.CANVAS -> CanvasCovers(service, color)
        }
    }

    private enum class Track { EVENTS, FRAMES, POLL }

    private enum class Draw { WINDOWS, PASS, CANVAS }

    private class Run(
        val name: String,
        val track: Track,
        val measure: Boolean,
        val wholeGrid: Boolean,
        val covers: ProbeCovers,
    ) {
        val samples = StringBuilder("kind,uptime_ms,true_top,shown_top,query_us,scroll_dy\n")
        var targets: ProbeTargets? = null

        /** Where the anchor's cover is, and where the anchor was on the last frame. */
        var shownTop: Int? = null
        var lastAnchor: Rect? = null

        /** Until when `frames` follows the screen frame by frame: shortly after the last event or movement. */
        var trackUntil = 0L

        /** Events arrived since the targets were last found. */
        var changed = false
        var looping = false
        var queries = 0
        var queryMicros = 0L

        fun needsFrames(): Boolean = changed || isTracking() || measure

        fun isTracking(): Boolean =
            when (track) {
                Track.EVENTS -> false
                Track.FRAMES -> SystemClock.uptimeMillis() <= trackUntil
                Track.POLL -> true
            }

        fun sample(
            kind: String,
            trueTop: Int?,
            micros: Long,
            scrollDy: Int,
        ) {
            samples.append("$kind,${SystemClock.uptimeMillis()},${trueTop ?: ""},${shownTop ?: ""},$micros,$scrollDy\n")
        }
    }

    private companion object {
        const val TAG = "CoverProbe"
        const val ACTION = "io.github.mallma.removereels.PROBE"
        const val INSTAGRAM = "com.instagram.android"
        const val IDLE_MS = 300L
        const val NANOS_PER_MICRO = 1000
        const val DARK = 0xFF0C1014.toInt()
        const val LIGHT = 0xFFFFFFFF.toInt()
        val EVENT_KINDS =
            mapOf(
                AccessibilityEvent.TYPE_VIEW_SCROLLED to "scrolled",
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED to "content",
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED to "window",
            )
    }
}
