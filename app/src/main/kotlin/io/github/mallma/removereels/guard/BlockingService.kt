package io.github.mallma.removereels.guard

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import io.github.mallma.removereels.container

/**
 * Blocks Reels and Shorts screens. Android delivers events from Instagram and YouTube only, as configured in
 * `res/xml/blocking_service.xml`; the [GuardHost] decides and acts on them.
 */
class BlockingService : AccessibilityService() {
    private val recorder = SnapshotRecorder(this)
    private var host: GuardHost? = null

    override fun onServiceConnected() {
        host = GuardHost(this, container).also { it.start() }
        recorder.start()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        host?.onEvent(windowChanged = event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED)
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        recorder.stop()
        host?.stop()
        host = null
        super.onDestroy()
    }
}
