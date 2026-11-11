package io.github.mallma.removereels.detection

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class EntryPointsTest {
    private val screen = Bounds(0, 0, 1000, 2000)

    /** A bottom bar of five tabs without ids; all but the third hold an icon with the id `icon`. */
    private val bar = Bounds(0, 1900, 1000, 2000)

    private fun tab(index: Int) = Bounds(index * 200, 1900, index * 200 + 200, 2000)

    private fun icon(index: Int) = Bounds(index * 200 + 70, 1920, index * 200 + 130, 1980)

    /** The bar's views; parents count the window's root, which [screenWith] puts first. */
    private fun tabBar(
        iconTabs: List<Int> = listOf(0, 1, 3, 4),
        visible: Boolean = true,
    ): List<SnapshotNode> {
        val nodes = mutableListOf(SnapshotNode("$APP:id/bar", "android.widget.LinearLayout", bar, visible, parent = 0))
        iconTabs.forEach { index ->
            nodes +=
                SnapshotNode(className = "android.widget.Button", bounds = tab(index), visible = visible, parent = 1)
            val tabPosition = nodes.size
            nodes +=
                SnapshotNode("$APP:id/icon", "android.widget.ImageView", icon(index), visible, parent = tabPosition)
        }
        return nodes
    }

    private fun screenWith(nodes: List<SnapshotNode>) =
        ScreenSnapshot(
            APP,
            "1.0",
            screen,
            listOf(SnapshotNode(className = "android.widget.FrameLayout", bounds = screen, visible = true)) + nodes,
        )

    private fun find(
        rule: EntryPointRule,
        nodes: List<SnapshotNode>,
    ): List<EntryPoint> {
        val rules =
            AppRules(APP, "1.0", listOf(SurfaceRule("viewer", listOf(Signal("player")))), entryPoints = listOf(rule))
        return SurfaceDetector(listOf(rules)).findEntryPoints(APP, screenWith(nodes))
    }

    private val secondIconTab =
        EntryPointRule("tab", viewId = "icon", within = "bar", count = 4, index = 1, coverParent = 1)

    @Test
    fun `covers every visible view with the id`() {
        val nodes = listOf(SnapshotNode("$APP:id/reels", "android.view.View", tab(1), true, parent = 0))

        assertThat(find(EntryPointRule("reels", viewId = "reels"), nodes)).containsExactly(EntryPoint("reels", tab(1)))
    }

    @Test
    fun `picks one of several look-alike marks and covers the view around it`() {
        assertThat(find(secondIconTab, tabBar())).containsExactly(EntryPoint("tab", tab(1)))
    }

    @Test
    fun `covers nothing when the number of marks changed`() {
        assertThat(find(secondIconTab, tabBar(iconTabs = listOf(0, 1, 2, 3, 4)))).isEmpty()
        assertThat(find(secondIconTab, tabBar(iconTabs = listOf(0, 1, 3)))).isEmpty()
    }

    @Test
    fun `covers nothing the user cannot see`() {
        assertThat(find(secondIconTab, tabBar(visible = false))).isEmpty()
    }

    @Test
    fun `counts only marks inside the container`() {
        val elsewhere =
            SnapshotNode("$APP:id/icon", "android.widget.ImageView", Bounds(10, 10, 60, 60), true, parent = 0)

        assertThat(find(secondIconTab, tabBar() + elsewhere)).containsExactly(EntryPoint("tab", tab(1)))
    }

    @Test
    fun `covers nothing when the view around the mark is not known`() {
        val noParent = SnapshotNode("$APP:id/reels", "android.view.View", tab(1), true)

        assertThat(find(EntryPointRule("reels", viewId = "reels", coverParent = 1), listOf(noParent))).isEmpty()
    }

    @Test
    fun `clips covers to the window`() {
        val partly = SnapshotNode("$APP:id/reels", "android.view.View", Bounds(0, 1950, 200, 2100), true, parent = 0)
        val outside = SnapshotNode("$APP:id/reels", "android.view.View", Bounds(0, 2100, 200, 2200), true, parent = 0)

        assertThat(find(EntryPointRule("reels", viewId = "reels"), listOf(partly, outside)))
            .containsExactly(EntryPoint("reels", Bounds(0, 1950, 200, 2000)))
    }

    /** A grid of tiles whose top a search bar slides over; one tile shows a play count unless [playCount] is false. */
    private fun grid(
        top: Int = 100,
        searchBar: Boolean = true,
        playCount: Boolean = true,
    ) = listOfNotNull(
        SnapshotNode("$APP:id/grid", "android.widget.GridView", Bounds(0, top, 1000, 1900), true, parent = 0),
        SnapshotNode("$APP:id/search", "android.widget.FrameLayout", Bounds(0, 100, 1000, 250), searchBar, parent = 0),
        SnapshotNode("$APP:id/plays", "android.widget.TextView", Bounds(20, 800, 120, 840), true, parent = 1)
            .takeIf { playCount },
    )

    private val wholeGrid = EntryPointRule("grid", viewId = "grid", containing = "plays", below = "search")

    @Test
    fun `covers a whole region below the view that slides over its top`() {
        assertThat(find(wholeGrid, grid())).containsExactly(EntryPoint("grid", Bounds(0, 250, 1000, 1900)))
    }

    @Test
    fun `starts a region's cover no higher than the region`() {
        assertThat(find(wholeGrid, grid(top = 300))).containsExactly(EntryPoint("grid", Bounds(0, 300, 1000, 1900)))
    }

    @Test
    fun `covers a region only while it shows the view it must contain`() {
        assertThat(find(wholeGrid, grid(playCount = false))).isEmpty()
    }

    @Test
    fun `covers nothing without the view to start below`() {
        assertThat(find(wholeGrid, grid(searchBar = false))).isEmpty()
    }

    @Test
    fun `asks the app for the views a region depends on only while the region shows`() {
        val asked = mutableListOf<String>()
        val recorded = screenWith(emptyList())
        val tree =
            object : ScreenTree {
                override val screen = recorded.screen

                override fun findByViewId(viewId: String): List<ScreenNode> =
                    recorded.findByViewId(viewId).also { asked += viewId }
            }
        val rules =
            AppRules(
                APP,
                "1.0",
                listOf(SurfaceRule("viewer", listOf(Signal("player")))),
                entryPoints = listOf(wholeGrid),
            )

        SurfaceDetector(listOf(rules)).findEntryPoints(APP, tree)

        assertThat(asked).containsExactly("$APP:id/grid")
    }

    @Test
    fun `rejects blank ids to contain or to start below`() {
        assertThrows<IllegalArgumentException> { EntryPointRule("grid", "grid", containing = " ") }
        assertThrows<IllegalArgumentException> { EntryPointRule("grid", "grid", below = "") }
    }

    @Test
    fun `knows no entry points of apps without rules`() {
        val rules = AppRules(APP, "1.0", listOf(SurfaceRule("viewer", listOf(Signal("player")))))

        assertThat(SurfaceDetector(listOf(rules)).findEntryPoints("com.other.app", screenWith(tabBar()))).isEmpty()
        assertThat(SurfaceDetector(listOf(rules)).findEntryPoints(APP, screenWith(tabBar()))).isEmpty()
    }

    @ParameterizedTest
    @CsvSource(
        "'', icon, , , 0",
        "tab, '', , , 0",
        "tab, icon, 0, , 0",
        "tab, icon, , 1, 0",
        "tab, icon, 4, 4, 0",
        "tab, icon, 4, -1, 0",
        "tab, icon, , , -1",
    )
    fun `rejects incomplete or impossible rules`(
        name: String,
        viewId: String,
        count: Int?,
        index: Int?,
        coverParent: Int,
    ) {
        assertThrows<IllegalArgumentException> {
            EntryPointRule(name, viewId, count = count, index = index, coverParent = coverParent)
        }
    }

    @Test
    fun `entry points carry the colour of what they hide and their inset`() {
        val dark = CoverColor(light = "#FFFFFF", dark = "#0C1014")
        val nodes = listOf(SnapshotNode("$APP:id/reels", "android.view.View", tab(1), true, parent = 0))

        val rule = EntryPointRule("reels", viewId = "reels", coverColor = dark, coverInsetTopDp = 1)

        assertThat(find(rule, nodes)).containsExactly(EntryPoint("reels", tab(1), dark, insetTopDp = 1))
    }

    @Test
    fun `cover colours become opaque ARGB values`() {
        val color = CoverColor(light = "#FFFFFF", dark = "#0C1014")

        assertThat(color.argb(darkTheme = false)).isEqualTo(0xFFFFFFFF.toInt())
        assertThat(color.argb(darkTheme = true)).isEqualTo(0xFF0C1014.toInt())
    }

    @Test
    fun `rejects colours that are not #RRGGBB`() {
        assertThrows<IllegalArgumentException> { CoverColor(light = "white", dark = "#000000") }
        assertThrows<IllegalArgumentException> { CoverColor(light = "#FFFFFF", dark = "#00000000") }
    }

    @Test
    fun `rejects a negative inset`() {
        assertThrows<IllegalArgumentException> { EntryPointRule("tab", "icon", coverInsetTopDp = -1) }
    }

    @Test
    fun `rules may not repeat an entry point`() {
        assertThrows<IllegalArgumentException> {
            AppRules(
                APP,
                "1.0",
                listOf(SurfaceRule("viewer", listOf(Signal("player")))),
                entryPoints = listOf(EntryPointRule("tab", "icon"), EntryPointRule("tab", "other")),
            )
        }
    }

    private companion object {
        const val APP = "com.example.app"
    }
}
