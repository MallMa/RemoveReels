package io.github.mallma.removereels.detection

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * How to recognise the short-video screens of one app. View ids are short names such as `clips_tab`; the detector
 * adds the app's package to them.
 */
@Serializable
public data class AppRules(
    public val packageName: String,
    /** The app version whose recorded screens the rules are tested against. */
    public val testedVersion: String,
    /** The screens to block. */
    public val surfaces: List<SurfaceRule>,
    /** A check that the rules still fit the app's current layout, if the app has a suitable view. */
    public val canary: Canary? = null,
    /** The tab that leads back to the app's main screen, tapped if pressing back does not leave a blocked screen. */
    public val homeTabViewId: String? = null,
    /** The ways into the surfaces, covered while the app is blocked. */
    public val entryPoints: List<EntryPointRule> = emptyList(),
    /** Pages that list only Reels, left while the app is blocked. */
    public val reelsPages: List<ReelsPageRule> = emptyList(),
) {
    init {
        require(packageName.isNotBlank()) { "Rules need a package name" }
        require(testedVersion.isNotBlank()) { "Rules for $packageName need the app version they were tested with" }
        require(surfaces.isNotEmpty()) { "Rules for $packageName name no surface" }
        require(surfaces.map { it.name }.toSet().size == surfaces.size) { "Rules for $packageName repeat a surface" }
        require(entryPoints.map { it.name }.toSet().size == entryPoints.size) {
            "Rules for $packageName repeat an entry point"
        }
        require(reelsPages.map { it.name }.toSet().size == reelsPages.size) {
            "Rules for $packageName repeat a Reels page"
        }
    }

    /** The full resource name of the app's view [viewId]. */
    public fun fullViewId(viewId: String): String = "$packageName:id/$viewId"

    public companion object {
        private val json = Json { ignoreUnknownKeys = false }

        /** Parses one rules file. Fails with [IllegalArgumentException] if it is malformed or incomplete. */
        public fun parse(text: String): AppRules = json.decodeFromString(text)
    }
}

/**
 * One screen to block, recognised when any signal of [anyOf] and every signal of [allOf] match. Surfaces are tried in
 * order, so a narrower one, such as Reels opened from messages, comes before the general one.
 */
@Serializable
public data class SurfaceRule(
    public val name: String,
    public val anyOf: List<Signal>,
    public val allOf: List<Signal> = emptyList(),
    /** The screen was opened from a chat, which the block notice mentions. */
    public val openedFromMessages: Boolean = false,
) {
    init {
        require(name.isNotBlank()) { "A surface needs a name" }
        require(anyOf.isNotEmpty()) { "Surface $name has no signal" }
    }
}

/**
 * A view that marks a screen: present, visible, covering at least [minScreenShare] of the screen and, if [selected]
 * is set, in that selection state. The share keeps small previews, such as a Reels thumbnail in the feed, from
 * matching.
 */
@Serializable
public data class Signal(
    public val viewId: String,
    public val minScreenShare: Double = 0.0,
    public val selected: Boolean? = null,
) {
    init {
        require(viewId.isNotBlank()) { "A signal needs a view id" }
        require(minScreenShare in 0.0..1.0) { "Screen share of $viewId must be between 0 and 1: $minScreenShare" }
    }
}

/**
 * [expectedViewId] should be visible whenever [anchorViewId] is, for example the Reels tab whenever the tab bar is.
 * When the anchor shows without it, the app's layout has probably changed and the rules may be out of date.
 */
@Serializable
public data class Canary(
    public val anchorViewId: String,
    public val expectedViewId: String,
)
