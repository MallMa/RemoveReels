package io.github.mallma.removereels.guard

import kotlinx.coroutines.delay
import kotlin.time.Duration

/** The ways the service can move another app away from a screen. */
internal interface Navigation {
    fun back()

    /** Taps the view with the full id [viewId]. */
    fun tap(viewId: String)

    fun home()

    fun isOnSurface(): Boolean
}

/**
 * Leaves a blocked screen with the mildest step that works: back, up to [MAX_BACKS] times, then the app's home tab,
 * then the launcher. Each step gets [settle] to take effect before the screen is checked again.
 */
internal class BackOut(
    private val navigation: Navigation,
    private val settle: Duration,
) {
    /** [homeTabViewId] is the full id of the app's home tab, if it has one. */
    suspend fun leave(homeTabViewId: String?) {
        val steps = List(MAX_BACKS) { navigation::back } + listOfNotNull(homeTabViewId?.let { { navigation.tap(it) } })
        val left =
            steps.any { step ->
                step()
                delay(settle)
                !navigation.isOnSurface()
            }
        if (!left) navigation.home()
    }

    private companion object {
        const val MAX_BACKS = 3
    }
}
