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
        private val TILE_IDS = listOf("grid_card_layout_container", "layout_container")

        /**
         * Reels tiles in a grid: tiles with a play count inside them. The Reels tab: the second tab on profiles. Each
         * lookup by view id is one request to Instagram, and so is each child asked for one by one, so tiles and play
         * counts are looked up by id and matched by their bounds here.
         */
        fun find(
            service: AccessibilityService,
            wholeGrid: Boolean,
        ): ProbeTargets {
            val root = service.rootInActiveWindow ?: return ProbeTargets(emptyList(), null, null, Rect(), queries = 1)
            val grid = root.byId("recycler_view").firstOrNull { it.isVisibleToUser }
            val container = (grid ?: root).boundsInScreen().toRect()
            val playCounts = root.byId("play_count_container").map { it.boundsInScreen().toRect() }
            val tiles = TILE_IDS.flatMap { root.byId(it) }.filter { it.isVisibleToUser }
            val reels = tiles.filter { tile -> tile.boundsInScreen().toRect().let { b -> playCounts.any(b::contains) } }
            val tab = root.byId("profile_tab_layout").firstOrNull { it.isVisibleToUser }?.let(::reelsTab)
            val targets = reels + listOfNotNull(tab)
            val bounds = targets.map { it.boundsInScreen().toRect() }
            val anchor = bounds.indices.minByOrNull { abs(bounds[it].centerY() - container.centerY()) }
            // Explore's grid shows little but Reels; covering all of it needs no following at all.
            val covered = if (wholeGrid && grid != null && reels.isNotEmpty()) listOf(container) else bounds
            return ProbeTargets(
                rects = covered.map { it.extendedOver(container) },
                anchor = anchor?.let(targets::get),
                anchorBounds = anchor?.let(bounds::get),
                container = container,
                queries = 2 + TILE_IDS.size + 1 + (if (tab != null) 2 else 0),
            )
        }

        private fun AccessibilityNodeInfo.byId(id: String): List<AccessibilityNodeInfo> =
            findAccessibilityNodeInfosByViewId("$INSTAGRAM:id/$id")

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
