package io.github.mallma.removereels.guard

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

/**
 * Watches Instagram and YouTube for Reels and Shorts screens. Android delivers events from those two apps only, as
 * configured in `res/xml/blocking_service.xml`.
 */
class BlockingService : AccessibilityService() {
    private val recorder = SnapshotRecorder(this)

    override fun onServiceConnected() {
        recorder.start()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) = Unit

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        recorder.stop()
        super.onDestroy()
    }
}
