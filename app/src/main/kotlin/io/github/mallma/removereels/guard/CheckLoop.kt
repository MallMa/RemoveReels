package io.github.mallma.removereels.guard

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/**
 * Decides when to look at the screen: at once when a window opens, at most once per [spacing] while content changes,
 * and every [tick] while [ticking] even without events, so watching time keeps counting.
 */
internal class CheckLoop(
    private val timeSource: TimeSource,
    private val spacing: Duration,
    private val tick: Duration,
    private val ticking: () -> Boolean,
    private val check: suspend () -> Unit,
) {
    private val requests = Channel<Unit>(Channel.CONFLATED)
    private var urgent = false

    /** Asks for a check; [urgent] skips the spacing, for a newly opened window. */
    fun request(urgent: Boolean) {
        this.urgent = this.urgent || urgent
        requests.trySend(Unit)
    }

    /** Runs until cancelled. */
    suspend fun run() {
        var lastCheck: TimeMark? = null
        while (true) {
            val requested =
                if (ticking()) {
                    withTimeoutOrNull(tick) { requests.receive() } != null
                } else {
                    requests.receive()
                    true
                }
            if (requested && !urgent) lastCheck?.let { delay(spacing - it.elapsedNow()) }
            urgent = false
            lastCheck = timeSource.markNow()
            check()
        }
    }
}
