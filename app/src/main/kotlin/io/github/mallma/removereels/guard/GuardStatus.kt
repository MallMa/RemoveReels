package io.github.mallma.removereels.guard

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** What the accessibility service tells the rest of the app, which runs in the same process. */
internal class GuardStatus {
    private val runningState = MutableStateFlow(false)
    private val blockCount = MutableStateFlow(0)
    private var turnOnRequested = false

    /**
     * The service is connected, so blocking is on. This is more reliable than Android's list of enabled services: after
     * a crash, a service stays on that list but is not started again until it is switched off and on.
     */
    val running: StateFlow<Boolean> = runningState.asStateFlow()

    /** How many screens were blocked since the app started. */
    val blocks: StateFlow<Int> = blockCount.asStateFlow()

    fun serviceConnected() {
        runningState.value = true
    }

    fun serviceDisconnected() {
        runningState.value = false
    }

    fun blocked() {
        blockCount.update { it + 1 }
    }

    /** A Turn on button sent the user to Android's settings; the service brings the app back when it connects. */
    fun requestTurnOn() {
        turnOnRequested = true
    }

    /** Whether a Turn on button is waiting for the service; true once per request. */
    fun takeTurnOnRequest(): Boolean = turnOnRequested.also { turnOnRequested = false }
}
