package io.github.mallma.removereels.guard

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

/** Release builds probe nothing; see the debug source set. */
internal class CoverProbe(
    // Unused here, but the debug probe with the same constructor needs it.
    @Suppress("UnusedPrivateProperty") service: AccessibilityService,
) {
    fun start() = Unit

    fun stop() = Unit

    fun onEvent(
        // Unused here, but the debug probe reads it.
        @Suppress("UNUSED_PARAMETER") event: AccessibilityEvent,
    ) = Unit
}
