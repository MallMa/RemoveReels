package io.github.mallma.removereels.detection

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class ScreenSnapshotTest {
    private val root = Bounds(0, 0, 1080, 2340)
    private val tabBar = Bounds(0, 2200, 1080, 2340)

    /** A window with a player and a tab bar without an id, which holds two tabs. */
    private val snapshot =
        ScreenSnapshot(
            packageName = "com.example.app",
            appVersion = "1.2.3",
            screen = root,
            nodes =
                listOf(
                    SnapshotNode(className = "android.widget.FrameLayout", bounds = root, visible = true),
                    SnapshotNode("com.example.app:id/player", "android.widget.FrameLayout", root, true, parent = 0),
                    SnapshotNode(
                        className = "android.widget.LinearLayout",
                        bounds = tabBar,
                        visible = true,
                        parent = 0,
                    ),
                    SnapshotNode(
                        "com.example.app:id/tab",
                        "android.widget.Button",
                        Bounds(0, 2200, 200, 2340),
                        true,
                        true,
                        2,
                    ),
                    SnapshotNode(
                        "com.example.app:id/tab",
                        "android.widget.Button",
                        Bounds(200, 2200, 400, 2340),
                        false,
                        parent = 2,
                    ),
                ),
        )

    @Test
    fun `reads back what it wrote`() {
        assertThat(ScreenSnapshot.fromJson(snapshot.toJson())).isEqualTo(snapshot)
    }

    @Test
    fun `writes one view per line, leaving out a missing id and parent`() {
        val lines = snapshot.toJson().lines()

        assertThat(lines.first()).startsWith("""{"packageName":"com.example.app","appVersion":"1.2.3",""")
        assertThat(lines[1]).isEqualTo(
            """{"className":"android.widget.FrameLayout",""" +
                """"bounds":{"left":0,"top":0,"right":1080,"bottom":2340},"visible":true,"selected":false},""",
        )
        assertThat(lines[4]).isEqualTo(
            """{"viewId":"com.example.app:id/tab","className":"android.widget.Button",""" +
                """"bounds":{"left":0,"top":2200,"right":200,"bottom":2340},""" +
                """"visible":true,"selected":true,"parent":2},""",
        )
        assertThat(lines.takeLast(2)).isEqualTo(listOf("]}", ""))
    }

    @Test
    fun `finds every view with an id, in the order recorded`() {
        val tabs = snapshot.findByViewId("com.example.app:id/tab")

        assertThat(
            tabs.map { it.bounds },
        ).containsExactly(Bounds(0, 2200, 200, 2340), Bounds(200, 2200, 400, 2340)).inOrder()
        assertThat(tabs.map { it.isVisibleToUser }).containsExactly(true, false).inOrder()
        assertThat(tabs.map { it.isSelected }).containsExactly(true, false).inOrder()
        assertThat(snapshot.findByViewId("com.example.app:id/missing")).isEmpty()
    }

    @Test
    fun `leads from a view to the views that contain it`() {
        val tab = snapshot.findByViewId("com.example.app:id/tab").first()

        assertThat(tab.parent?.bounds).isEqualTo(tabBar)
        assertThat(tab.parent?.parent?.bounds).isEqualTo(root)
        assertThat(tab.parent?.parent?.parent).isNull()
    }
}
