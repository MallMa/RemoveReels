package io.github.mallma.removereels.guard

import android.accessibilityservice.AccessibilityService

/** Release builds record nothing; see the debug source set. */
internal class SnapshotRecorder(
    // Unused here, but the debug recorder with the same constructor needs it.
    @Suppress("UnusedPrivateProperty") service: AccessibilityService,
) {
    fun start() = Unit

    fun stop() = Unit
}
