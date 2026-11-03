package io.github.mallma.removereels.guard

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import io.github.mallma.removereels.detection.BundledRules
import io.github.mallma.removereels.detection.SurfaceDetector

/**
 * Watches Instagram and YouTube for Reels and Shorts screens. Android delivers events from those two apps only, as
 * configured in `res/xml/blocking_service.xml`.
 */
class BlockingService : AccessibilityService() {
    private val detector = SurfaceDetector(BundledRules.load())
    private val recorder = SnapshotRecorder(this)
    private var lastSurface: String? = null

    override fun onServiceConnected() {
        recorder.start()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val root = rootInActiveWindow ?: return
        val surface = detector.findSurface(root.packageName.toString(), AccessibilityScreenTree(root))?.name
        if (surface != lastSurface) {
            Log.d(TAG, "Now showing ${surface ?: "no surface"} in ${root.packageName}")
            lastSurface = surface
        }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        recorder.stop()
        super.onDestroy()
    }

    private companion object {
        const val TAG = "BlockingService"
    }
}
