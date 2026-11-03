package io.github.mallma.removereels.detection

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * A recorded copy of a screen that keeps only view ids, classes, bounds and visibility: never text or descriptions,
 * so it can be committed as a test fixture without personal data.
 */
@Serializable
public data class ScreenSnapshot(
    public val packageName: String,
    public val appVersion: String,
    override val screen: Bounds,
    public val nodes: List<SnapshotNode>,
) : ScreenTree {
    override fun findByViewId(viewId: String): List<ScreenNode> =
        nodes.filter { it.viewId == viewId }.map { ScreenNode(it.bounds, it.visible, it.selected) }

    /** Compact JSON with one view per line, so fixture diffs between app versions stay readable. */
    public fun toJson(): String {
        // [nodes] is the last property, so the header ends with its empty array.
        val header = json.encodeToString(copy(nodes = emptyList())).removeSuffix("[]}")
        return nodes.joinToString(",\n", prefix = "$header[\n", postfix = "\n]}\n") { json.encodeToString(it) }
    }

    public companion object {
        private val json = Json { encodeDefaults = true }

        public fun fromJson(text: String): ScreenSnapshot = json.decodeFromString(text)
    }
}

/** One view of a [ScreenSnapshot] that has a view id. */
@Serializable
public data class SnapshotNode(
    public val viewId: String,
    public val className: String,
    public val bounds: Bounds,
    public val visible: Boolean,
    public val selected: Boolean = false,
)
