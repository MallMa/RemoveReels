package io.github.mallma.removereels.detection

/** Whether the rules of an app still fit its layout. */
public enum class CanaryResult {
    /** The anchor and the expected view are both visible. */
    HEALTHY,

    /** The anchor is visible without the expected view: the rules may be out of date. */
    STALE,

    /** The anchor is not visible, so this screen says nothing about the rules. */
    NOT_APPLICABLE,
}

/** Recognises the screens of [rules] in what an app shows. */
public class SurfaceDetector(
    rules: List<AppRules>,
) {
    private val rulesByPackage = rules.associateBy { it.packageName }

    init {
        require(rulesByPackage.size == rules.size) { "More than one rule list for the same app" }
    }

    /** The apps the detector has rules for. */
    public val packageNames: Set<String> get() = rulesByPackage.keys

    public fun rulesFor(packageName: String): AppRules? = rulesByPackage[packageName]

    /** The surface [tree], a screen of [packageName], shows; null if it shows none or there are no rules. */
    public fun findSurface(
        packageName: String,
        tree: ScreenTree,
    ): SurfaceRule? {
        val rules = rulesByPackage[packageName] ?: return null
        return rules.surfaces.firstOrNull { surface ->
            surface.anyOf.any { rules.matches(it, tree) } && surface.allOf.all { rules.matches(it, tree) }
        }
    }

    /** The entry points [tree], a screen of [packageName], shows, clipped to its window. */
    public fun findEntryPoints(
        packageName: String,
        tree: ScreenTree,
    ): List<EntryPoint> {
        val rules = rulesByPackage[packageName] ?: return emptyList()
        return rules.entryPoints.flatMap { rules.find(it, tree) }
    }

    public fun checkCanary(
        packageName: String,
        tree: ScreenTree,
    ): CanaryResult {
        val rules = rulesByPackage[packageName] ?: return CanaryResult.NOT_APPLICABLE
        return rules.canary?.let { rules.check(it, tree) } ?: CanaryResult.NOT_APPLICABLE
    }

    private fun AppRules.check(
        canary: Canary,
        tree: ScreenTree,
    ): CanaryResult =
        when {
            !isVisible(canary.anchorViewId, tree) -> CanaryResult.NOT_APPLICABLE
            isVisible(canary.expectedViewId, tree) -> CanaryResult.HEALTHY
            else -> CanaryResult.STALE
        }

    private fun AppRules.matches(
        signal: Signal,
        tree: ScreenTree,
    ): Boolean =
        tree.findByViewId(fullViewId(signal.viewId)).any { node ->
            node.isVisibleToUser &&
                (signal.selected == null || node.isSelected == signal.selected) &&
                node.bounds.shareOf(tree.screen) >= signal.minScreenShare
        }

    private fun AppRules.find(
        rule: EntryPointRule,
        tree: ScreenTree,
    ): List<EntryPoint> {
        val containers = rule.within?.let { within -> visibleBounds(within, tree) }
        val contents = rule.containing?.let { containing -> visibleBounds(containing, tree) }
        val floor =
            rule.below?.let { below ->
                visibleBounds(below, tree).maxOfOrNull { it.bottom }
                    ?: return emptyList()
            }
        val marks =
            tree
                .findByViewId(fullViewId(rule.viewId))
                .filter { it.isVisibleToUser }
                .filter { mark -> containers == null || containers.any { mark.bounds in it } }
                .filter { mark -> contents == null || contents.any { it in mark.bounds } }
                .sortedWith(compareBy({ it.bounds.left }, { it.bounds.top }))
        val chosen =
            when {
                rule.count != null && marks.size != rule.count -> emptyList()
                rule.index != null -> listOf(marks[rule.index])
                else -> marks
            }
        return chosen
            .mapNotNull { it.ancestor(rule.coverParent)?.bounds?.intersect(tree.screen) }
            .map { if (floor == null) it else it.copy(top = maxOf(it.top, floor)) }
            .filter { it.area > 0 }
            .map { EntryPoint(rule.name, it, rule.coverColor, rule.coverInsetTopDp) }
    }

    private fun AppRules.visibleBounds(
        viewId: String,
        tree: ScreenTree,
    ): List<Bounds> = tree.findByViewId(fullViewId(viewId)).filter { it.isVisibleToUser }.map { it.bounds }

    private fun ScreenNode.ancestor(levels: Int): ScreenNode? =
        (1..levels).fold<Int, ScreenNode?>(this) { node, _ -> node?.parent }

    private fun AppRules.isVisible(
        viewId: String,
        tree: ScreenTree,
    ): Boolean = tree.findByViewId(fullViewId(viewId)).any { it.isVisibleToUser }
}
