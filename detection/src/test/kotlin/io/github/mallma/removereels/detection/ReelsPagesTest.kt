package io.github.mallma.removereels.detection

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ReelsPagesTest {
    private val screen = Bounds(0, 0, 1000, 2000)
    private val pagerBounds = Bounds(0, 1000, 1000, 2000)
    private val reelsPage = ReelsPageRule("profile_reels", viewId = "reels_grid", pager = "pager")

    private fun node(
        id: String,
        bounds: Bounds,
        visible: Boolean = true,
    ) = SnapshotNode("$APP:id/$id", "android.view.View", bounds, visible)

    /** A pager whose Reels grid is the page in view, or the next page beside it, outside the screen. */
    private fun profile(reelsInView: Boolean = true) =
        listOf(
            node("pager", pagerBounds),
            if (reelsInView) {
                node("reels_grid", pagerBounds)
            } else {
                node("reels_grid", Bounds(1000, 1000, 1000, 2000), visible = false)
            },
        )

    private fun find(
        nodes: List<SnapshotNode>,
        packageName: String = APP,
    ): ReelsPageRule? {
        val rules =
            AppRules(
                APP,
                "1.0",
                listOf(SurfaceRule("viewer", listOf(Signal("player")))),
                reelsPages = listOf(reelsPage),
            )
        return SurfaceDetector(listOf(rules)).findReelsPage(packageName, ScreenSnapshot(APP, "1.0", screen, nodes))
    }

    @Test
    fun `finds a Reels list showing inside its pager`() {
        assertThat(find(profile())).isEqualTo(reelsPage)
    }

    @Test
    fun `finds nothing while the Reels list is a page beside the one in view`() {
        assertThat(find(profile(reelsInView = false))).isNull()
    }

    @Test
    fun `finds nothing when the Reels list is not inside the pager`() {
        val elsewhere = listOf(node("pager", pagerBounds), node("reels_grid", Bounds(0, 100, 1000, 900)))

        assertThat(find(elsewhere)).isNull()
    }

    @Test
    fun `knows no Reels pages of apps without rules`() {
        assertThat(find(profile(), packageName = "com.other.app")).isNull()
    }

    @Test
    fun `rejects incomplete Reels pages`() {
        assertThrows<IllegalArgumentException> { ReelsPageRule(" ", "reels_grid", "pager") }
        assertThrows<IllegalArgumentException> { ReelsPageRule("reels", "", "pager") }
        assertThrows<IllegalArgumentException> { ReelsPageRule("reels", "reels_grid", "") }
    }

    @Test
    fun `rules may not repeat a Reels page`() {
        assertThrows<IllegalArgumentException> {
            AppRules(
                APP,
                "1.0",
                listOf(SurfaceRule("viewer", listOf(Signal("player")))),
                reelsPages = listOf(reelsPage, reelsPage.copy(viewId = "other")),
            )
        }
    }

    private companion object {
        const val APP = "com.example.app"
    }
}
