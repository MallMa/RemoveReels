package io.github.mallma.removereels.detection

/** The entry points of these rules that [tree] shows, clipped to its window. */
internal fun AppRules.findEntryPoints(tree: ScreenTree): List<EntryPoint> = entryPoints.flatMap { find(it, tree) }

/** The first page of these rules that lists only Reels and that [tree] shows. */
internal fun AppRules.findReelsPage(tree: ScreenTree): ReelsPageRule? =
    reelsPages.firstOrNull { page ->
        val lists = visibleBounds(page.viewId, tree)
        lists.isNotEmpty() && visibleBounds(page.pager, tree).any { pager -> lists.any { it in pager } }
    }

private fun AppRules.find(
    rule: EntryPointRule,
    tree: ScreenTree,
): List<EntryPoint> {
    val marks = marks(rule, tree)
    val below = rule.below?.takeIf { marks.isNotEmpty() }
    val floor = below?.let { visibleBounds(it, tree).maxOfOrNull(Bounds::bottom) }
    if (rule.below != null && floor == null) return emptyList()
    return marks
        .mapNotNull { it.ancestor(rule.coverParent)?.bounds?.intersect(tree.screen) }
        .map { if (floor == null) it else it.copy(top = maxOf(it.top, floor)) }
        .filter { it.area > 0 }
        .map { EntryPoint(rule.name, it, rule.coverColor, rule.coverInsetTopDp) }
}

/**
 * The marks of [rule] that the user can see, chosen as its fields say; none if their number is not its count.
 * Every lookup asks the app, so the views a mark depends on are looked up only once a mark shows.
 */
private fun AppRules.marks(
    rule: EntryPointRule,
    tree: ScreenTree,
): List<ScreenNode> {
    val candidates = tree.findByViewId(fullViewId(rule.viewId)).filter { it.isVisibleToUser }
    if (candidates.isEmpty()) return emptyList()
    val containers = rule.within?.let { visibleBounds(it, tree) }
    val contents = rule.containing?.let { visibleBounds(it, tree) }
    val marks =
        candidates
            .filter { mark -> containers == null || containers.any { mark.bounds in it } }
            .filter { mark -> contents == null || contents.any { it in mark.bounds } }
            .sortedWith(compareBy({ it.bounds.left }, { it.bounds.top }))
    return when {
        rule.count != null && marks.size != rule.count -> emptyList()
        rule.index != null -> listOf(marks[rule.index])
        else -> marks
    }
}

private fun AppRules.visibleBounds(
    viewId: String,
    tree: ScreenTree,
): List<Bounds> = tree.findByViewId(fullViewId(viewId)).filter { it.isVisibleToUser }.map { it.bounds }

private fun ScreenNode.ancestor(levels: Int): ScreenNode? =
    (1..levels).fold<Int, ScreenNode?>(this) { node, _ -> node?.parent }
