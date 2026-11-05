package io.github.mallma.removereels.guard

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import io.github.mallma.removereels.detection.Bounds
import kotlin.math.abs

/**
 * What the [CoverProbe] covers: Reels covers stretched past their container's edges, the target whose position is
 * followed frame by frame, and the container that clips the covers.
 */
internal data class ProbeTargets(
    val rects: List<Rect>,
    val anchor: AccessibilityNodeInfo?,
    val anchorBounds: Rect?,
    val container: Rect,
    val queries: Int,
) {
    fun visibleRects(): List<Rect> = rects.map { Rect(it).apply { if (!intersect(container)) setEmpty() } }

    fun movedBy(dy: Int): ProbeTargets = copy(rects = rects.map { Rect(it).apply { offset(0, dy) } })

    companion object {
        private const val INSTAGRAM = "com.instagram.android"
        private const val EXTENSION = 4000

        /** Reels tiles in a grid: the grid's children with a play count. The Reels tab: the second tab on profiles. */
        fun find(service: AccessibilityService): ProbeTargets {
            var queries = 1
            val root = service.rootInActiveWindow ?: return ProbeTargets(emptyList(), null, null, Rect(), queries)
            val targets = mutableListOf<AccessibilityNodeInfo>()
            var container = root.boundsInScreen().toRect()
            val grids = root.findAccessibilityNodeInfosByViewId("$INSTAGRAM:id/recycler_view")
            for (grid in grids.filter { it.isVisibleToUser }) {
                container = grid.boundsInScreen().toRect()
                for (index in 0 until grid.childCount) {
                    val tile = grid.getChild(index) ?: continue
                    queries++
                    if (tile.isVisibleToUser && tile.hasChildWithId("$INSTAGRAM:id/play_count_container")) {
                        targets += tile
                    }
                }
            }
            val rows = root.findAccessibilityNodeInfosByViewId("$INSTAGRAM:id/profile_tab_layout")
            rows.firstOrNull { it.isVisibleToUser }?.let { row -> reelsTab(row)?.let(targets::add) }
            queries += 2
            val bounds = targets.map { it.boundsInScreen().toRect() }
            val anchor = bounds.indices.minByOrNull { abs(bounds[it].centerY() - container.centerY()) }
            return ProbeTargets(
                rects = bounds.map { it.extendedOver(container) },
                anchor = anchor?.let(targets::get),
                anchorBounds = anchor?.let(bounds::get),
                container = container,
                queries = queries,
            )
        }

        private fun AccessibilityNodeInfo.hasChildWithId(viewId: String): Boolean =
            (0 until childCount).any { getChild(it)?.viewIdResourceName == viewId }

        /** The tab row holds one strip of tabs: Posts, Reels, then the others. */
        private fun reelsTab(row: AccessibilityNodeInfo): AccessibilityNodeInfo? {
            var node: AccessibilityNodeInfo = row
            while (node.childCount == 1) node = node.getChild(0) ?: return null
            return if (node.childCount >= 2) node.getChild(1) else null
        }

        /**
         * A view clipped at its container's edge is only partly reported; its cover reaches past that edge, so that
         * moving it in from there reveals nothing. Covers are clipped to the container again when shown.
         */
        private fun Rect.extendedOver(container: Rect): Rect =
            Rect(this).apply {
                if (top <= container.top) top = container.top - EXTENSION
                if (bottom >= container.bottom) bottom = container.bottom + EXTENSION
            }
    }
}

internal fun Bounds.toRect() = Rect(left, top, right, bottom)
