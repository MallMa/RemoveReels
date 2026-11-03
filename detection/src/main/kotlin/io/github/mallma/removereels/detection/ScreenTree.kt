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
    public fun shareOf(screen: Bounds): Double =
        if (screen.area ==
            0L
        ) {
            0.0
        } else {
            intersect(screen).area.toDouble() / screen.area
        }

    /** The part of this rectangle inside [other]; empty, with no area, if they do not overlap. */
    public fun intersect(other: Bounds): Bounds =
        Bounds(
            left = maxOf(left, other.left),
            top = maxOf(top, other.top),
            right = minOf(right, other.right),
            bottom = minOf(bottom, other.bottom),
        )

    public operator fun contains(other: Bounds): Boolean =
        other.left >= left && other.top >= top && other.right <= right && other.bottom <= bottom
}
