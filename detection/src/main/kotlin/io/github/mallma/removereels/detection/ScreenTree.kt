package io.github.mallma.removereels.detection

import kotlinx.serialization.Serializable

/** What the detector needs to know about the screen another app shows. */
public interface ScreenTree {
    /** The part of the screen the app's window covers. */
    public val screen: Bounds

    /** The views with [viewId], a full resource name such as `com.instagram.android:id/clips_tab`. */
    public fun findByViewId(viewId: String): List<ScreenNode>
}

/** One view: where it is, whether the user can see it, and the view that contains it. */
public interface ScreenNode {
    public val bounds: Bounds
    public val isVisibleToUser: Boolean
    public val isSelected: Boolean

    /** The view that contains this one; null for the window's root, and where the tree is not known. */
    public val parent: ScreenNode?
}

/** A rectangle in screen pixels. [right] and [bottom] are exclusive, as in Android's `Rect`. */
@Serializable
public data class Bounds(
    public val left: Int,
    public val top: Int,
    public val right: Int,
    public val bottom: Int,
) {
    public val area: Long get() = (right - left).coerceAtLeast(0).toLong() * (bottom - top).coerceAtLeast(0)

    /** The fraction of [screen] this rectangle covers, from 0 to 1. */
    public fun shareOf(screen: Bounds): Double {
        if (screen.area == 0L) return 0.0
        val visible =
            Bounds(
                left = maxOf(left, screen.left),
                top = maxOf(top, screen.top),
                right = minOf(right, screen.right),
                bottom = minOf(bottom, screen.bottom),
            )
        return visible.area.toDouble() / screen.area
    }
}
