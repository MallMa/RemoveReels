package io.github.mallma.removereels.detection

import kotlinx.serialization.Serializable

/**
 * A way into Reels or Shorts, such as the Reels tab, covered while the app is blocked and marked by the views with
 * [viewId].
 *
 * When several views look alike, as the items of a tab bar without ids do, [within], [count] and [index] pick one:
 * the marks inside [within] must number exactly [count], or the layout has changed and nothing is covered, and [index]
 * counts them from the left.
 *
 * A whole region, such as a grid that shows little but Reels, is a mark [containing] a view that only Reels show, and
 * its cover may start [below] a view that slides over its top.
 */
@Serializable
public data class EntryPointRule(
    public val name: String,
    public val viewId: String,
    public val within: String? = null,
    public val count: Int? = null,
    public val index: Int? = null,
    /** Only marks with a visible view of this id inside them count, such as a grid while it shows a play count. */
    public val containing: String? = null,
    /** The cover starts below the visible view with this id, such as a search bar; without it, nothing is covered. */
    public val below: String? = null,
    /** Cover the view this many levels above the mark, such as the whole tab around its icon. */
    public val coverParent: Int = 0,
    /** The colour of what the cover hides, such as the app's tab bar, so that the entry point seems to be gone. */
    public val coverColor: CoverColor = CoverColor.DEFAULT,
    /** Leaves this many dp at the top uncovered, such as a divider line the app draws along its tab bar. */
    public val coverInsetTopDp: Int = 0,
) {
    init {
        require(name.isNotBlank()) { "An entry point needs a name" }
        require(viewId.isNotBlank()) { "Entry point $name needs a view id" }
        require(count == null || count > 0) { "Entry point $name needs a positive count: $count" }
        require(index == null || (count != null && index in 0 until count)) {
            "Entry point $name needs a count that includes its index"
        }
        require(containing?.isNotBlank() ?: true) { "Entry point $name needs a view id for what it contains" }
        require(below?.isNotBlank() ?: true) { "Entry point $name needs a view id to start below" }
        require(coverParent >= 0) { "Entry point $name cannot cover a view below its mark" }
        require(coverInsetTopDp >= 0) { "Entry point $name needs an inset of zero or more" }
    }
}

/** A colour in the app's light theme and in its dark theme, each written as `#RRGGBB`. */
@Serializable
public data class CoverColor(
    public val light: String,
    public val dark: String,
) {
    init {
        require(light.matches(HEX) && dark.matches(HEX)) { "Cover colours are #RRGGBB: $light, $dark" }
    }

    /** The colour as an opaque ARGB value, as Android draws it. */
    public fun argb(darkTheme: Boolean): Int =
        (OPAQUE or (if (darkTheme) dark else light).drop(1).toLong(radix = 16)).toInt()

    public companion object {
        private val HEX = Regex("#[0-9A-Fa-f]{6}")
        private const val OPAQUE = 0xFF000000

        public val DEFAULT: CoverColor = CoverColor(light = "#FFFFFF", dark = "#000000")
    }
}

/**
 * An entry point on screen: the area to cover, already clipped to the app's window, the cover's colour, and the top
 * inset that the app converts from dp to pixels.
 */
public data class EntryPoint(
    public val name: String,
    public val bounds: Bounds,
    public val color: CoverColor = CoverColor.DEFAULT,
    public val insetTopDp: Int = 0,
)
