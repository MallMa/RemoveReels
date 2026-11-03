package io.github.mallma.removereels.detection

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * A recorded copy of a screen that keeps only view ids, classes, bounds, visibility and the tree: never text or
 * descriptions, so it can be committed as a test fixture without personal data.
 *
 * Snapshots recorded for the MVP keep only views with an id and no parents; later ones keep every view.
 */
@Serializable
public data class ScreenSnapshot(
    public val packageName: String,
    public val appVersion: String,
    override val screen: Bounds,
    public val nodes: List<SnapshotNode>,
) : ScreenTree {
    override fun findByViewId(viewId: String): List<ScreenNode> =
        nodes.indices.filter { nodes[it].viewId == viewId }.map(::RecordedNode)

    /** Compact JSON with one view per line, so fixture diffs between app versions stay readable. */
    public fun toJson(): String {
        // [nodes] is the last property, so the header ends with its empty array.
        val header = json.encodeToString(copy(nodes = emptyList())).removeSuffix("[]}")
        return nodes.joinToString(",\n", prefix = "$header[\n", postfix = "\n]}\n") { json.encodeToString(it) }
    }

    /** The view at [index] of [nodes], as the detector sees it. */
    private inner class RecordedNode(
        private val index: Int,
    ) : ScreenNode {
        private val recorded get() = nodes[index]
        override val bounds: Bounds get() = recorded.bounds
        override val isVisibleToUser: Boolean get() = recorded.visible
        override val isSelected: Boolean get() = recorded.selected
        override val parent: ScreenNode? get() = recorded.parent?.let(::RecordedNode)
    }

    public companion object {
        private val json =
            Json {
                encodeDefaults = true
                explicitNulls = false
            }

        public fun fromJson(text: String): ScreenSnapshot = json.decodeFromString(text)
    }
}

/** One view of a [ScreenSnapshot]. */
@Serializable
public data class SnapshotNode(
    public val viewId: String? = null,
    public val className: String,
    public val bounds: Bounds,
    public val visible: Boolean,
    public val selected: Boolean = false,
    /** The position of the containing view in [ScreenSnapshot.nodes]; null for the root. */
    public val parent: Int? = null,
)
