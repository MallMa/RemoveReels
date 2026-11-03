package io.github.mallma.removereels.detection

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class SurfaceDetectorTest {
    private val rules =
        AppRules(
            packageName = APP,
            surfaces =
                listOf(
                    SurfaceRule(
                        name = "viewer_from_chat",
                        anyOf = listOf(Signal("player", minScreenShare = 0.6)),
                        allOf = listOf(Signal("chat_sender")),
                        openedFromMessages = true,
                    ),
                    SurfaceRule("viewer", anyOf = listOf(Signal("player", minScreenShare = 0.6), Signal("player_alt"))),
                    SurfaceRule("tab", anyOf = listOf(Signal("short_tab", selected = true))),
                ),
            canary = Canary(anchorViewId = "tab_bar", expectedViewId = "short_tab"),
        )
    private val detector = SurfaceDetector(listOf(rules))

    @Test
    fun `finds a surface whose view covers enough of the screen`() {
        val tree = screen(node("player", Bounds(0, 0, 1000, 1800)))

        assertThat(detector.findSurface(APP, tree)?.name).isEqualTo("viewer")
    }

    @Test
    fun `ignores the same view as a small preview`() {
        val tree = screen(node("player", Bounds(0, 500, 1000, 1000)))

        assertThat(detector.findSurface(APP, tree)).isNull()
    }

    @Test
    fun `ignores a view the user cannot see`() {
        val tree = screen(node("player", SCREEN, visible = false))

        assertThat(detector.findSurface(APP, tree)).isNull()
    }

    @Test
    fun `any signal of a surface is enough`() {
        val tree = screen(node("player_alt", Bounds(0, 0, 10, 10)))

        assertThat(detector.findSurface(APP, tree)?.name).isEqualTo("viewer")
    }

    @Test
    fun `a surface can require further views, and the first matching surface wins`() {
        val tree = screen(node("player", SCREEN), node("chat_sender"))

        assertThat(detector.findSurface(APP, tree)?.name).isEqualTo("viewer_from_chat")
        assertThat(detector.findSurface(APP, tree)?.openedFromMessages).isTrue()
    }

    @Test
    fun `a signal can require a selection state`() {
        assertThat(detector.findSurface(APP, screen(node("short_tab", selected = true)))?.name).isEqualTo("tab")
        assertThat(detector.findSurface(APP, screen(node("short_tab", selected = false)))).isNull()
    }

    @Test
    fun `knows nothing about apps without rules`() {
        val tree = screen(node("player", SCREEN))

        assertThat(detector.findSurface("com.other.app", tree)).isNull()
        assertThat(detector.checkCanary("com.other.app", tree)).isEqualTo(CanaryResult.NOT_APPLICABLE)
        assertThat(detector.rulesFor("com.other.app")).isNull()
        assertThat(detector.rulesFor(APP)).isEqualTo(rules)
        assertThat(detector.packageNames).containsExactly(APP)
    }

    @Test
    fun `the canary is healthy when the expected view shows with its anchor`() {
        val tree = screen(node("tab_bar"), node("short_tab"))

        assertThat(detector.checkCanary(APP, tree)).isEqualTo(CanaryResult.HEALTHY)
    }

    @Test
    fun `the canary is stale when the anchor shows without the expected view`() {
        val tree = screen(node("tab_bar"), node("short_tab", visible = false))

        assertThat(detector.checkCanary(APP, tree)).isEqualTo(CanaryResult.STALE)
    }

    @Test
    fun `the canary does not apply without its anchor`() {
        assertThat(detector.checkCanary(APP, screen(node("short_tab")))).isEqualTo(CanaryResult.NOT_APPLICABLE)
    }

    @Test
    fun `the canary does not apply to an app without one`() {
        val detector = SurfaceDetector(listOf(rules.copy(canary = null)))

        assertThat(detector.checkCanary(APP, screen(node("tab_bar")))).isEqualTo(CanaryResult.NOT_APPLICABLE)
    }

    @Test
    fun `rejects two rule lists for the same app`() {
        assertThrows<IllegalArgumentException> { SurfaceDetector(listOf(rules, rules)) }
    }

    private fun screen(vararg nodes: SnapshotNode) = ScreenSnapshot(APP, "1.0", SCREEN, nodes.toList())

    private fun node(
        viewId: String,
        bounds: Bounds = Bounds(0, 1900, 200, 2000),
        visible: Boolean = true,
        selected: Boolean = false,
    ) = SnapshotNode("$APP:id/$viewId", "android.view.View", bounds, visible, selected)

    private companion object {
        const val APP = "com.example.app"
        val SCREEN = Bounds(0, 0, 1000, 2000)
    }
}
