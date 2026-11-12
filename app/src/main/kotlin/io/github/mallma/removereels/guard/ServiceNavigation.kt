package io.github.mallma.removereels.guard

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityNodeInfo

/** [Navigation] through the service's global actions and the views of the active window. */
internal class ServiceNavigation(
    private val service: AccessibilityService,
    private val onSurface: () -> Boolean,
) : Navigation {
    override fun back() {
        service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
    }

    override fun tap(viewId: String) {
        service.rootInActiveWindow
            ?.findAccessibilityNodeInfosByViewId(viewId)
            ?.firstOrNull()
            ?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    override fun home() {
        service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
    }

    /** Turns the visible pager with the full id [viewId] back a page, as swiping to the tab before would. */
    fun turnBack(viewId: String) {
        service.rootInActiveWindow
            ?.findAccessibilityNodeInfosByViewId(viewId)
            ?.firstOrNull { it.isVisibleToUser }
            ?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)
    }

    override fun isOnSurface(): Boolean = onSurface()
}
