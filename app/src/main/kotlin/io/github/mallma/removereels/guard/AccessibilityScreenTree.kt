package io.github.mallma.removereels.guard

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import io.github.mallma.removereels.detection.Bounds
import io.github.mallma.removereels.detection.ScreenNode
import io.github.mallma.removereels.detection.ScreenTree

/** The window an accessibility service sees, as the detector's [ScreenTree]. */
internal class AccessibilityScreenTree(
    private val root: AccessibilityNodeInfo,
) : ScreenTree {
    override val screen: Bounds = root.boundsInScreen()

    override fun findByViewId(viewId: String): List<ScreenNode> =
        root.findAccessibilityNodeInfosByViewId(viewId).map(::AccessibilityScreenNode)
}

/** A view of another app; each property asks that app, so the detector reads only what it needs. */
private class AccessibilityScreenNode(
    private val node: AccessibilityNodeInfo,
) : ScreenNode {
    override val bounds: Bounds get() = node.boundsInScreen()
    override val isVisibleToUser: Boolean get() = node.isVisibleToUser
    override val isSelected: Boolean get() = node.isSelected
    override val parent: ScreenNode? get() = node.parent?.let(::AccessibilityScreenNode)
}

internal fun AccessibilityNodeInfo.boundsInScreen(): Bounds {
    val rect = Rect()
    getBoundsInScreen(rect)
    return Bounds(rect.left, rect.top, rect.right, rect.bottom)
}
