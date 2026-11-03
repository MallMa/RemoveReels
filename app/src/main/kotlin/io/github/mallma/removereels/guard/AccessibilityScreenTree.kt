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
        root.findAccessibilityNodeInfosByViewId(viewId).map { node ->
            ScreenNode(node.boundsInScreen(), node.isVisibleToUser, node.isSelected)
        }
}

internal fun AccessibilityNodeInfo.boundsInScreen(): Bounds {
    val rect = Rect()
    getBoundsInScreen(rect)
    return Bounds(rect.left, rect.top, rect.right, rect.bottom)
}
