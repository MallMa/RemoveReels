package io.github.mallma.removereels.detection

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class ScreenSnapshotTest {
    private val snapshot =
        ScreenSnapshot(
            packageName = "com.example.app",
            appVersion = "1.2.3",
            screen = Bounds(0, 0, 1080, 2340),
            nodes =
                listOf(
                    SnapshotNode(
                        "com.example.app:id/player",
                        "android.widget.FrameLayout",
                        Bounds(0, 0, 1080, 2340),
                        true,
                    ),
                    SnapshotNode(
                        "com.example.app:id/tab",
                        "android.widget.Button",
                        Bounds(0, 2200, 200, 2340),
                        true,
                        true,
                    ),
                    SnapshotNode(
                        "com.example.app:id/tab",
                        "android.widget.Button",
                        Bounds(200, 2200, 400, 2340),
                        false,
                    ),
                ),
        )

    @Test
    fun `reads back what it wrote`() {
        assertThat(ScreenSnapshot.fromJson(snapshot.toJson())).isEqualTo(snapshot)
    }

    @Test
    fun `writes one view per line`() {
        val lines = snapshot.toJson().lines()

        assertThat(lines.first()).startsWith("""{"packageName":"com.example.app","appVersion":"1.2.3",""")
        assertThat(lines.drop(1).take(3)).isEqualTo(
            listOf(
                """{"viewId":"com.example.app:id/player","className":"android.widget.FrameLayout",""" +
                    """"bounds":{"left":0,"top":0,"right":1080,"bottom":2340},"visible":true,"selected":false},""",
                """{"viewId":"com.example.app:id/tab","className":"android.widget.Button",""" +
                    """"bounds":{"left":0,"top":2200,"right":200,"bottom":2340},"visible":true,"selected":true},""",
                """{"viewId":"com.example.app:id/tab","className":"android.widget.Button",""" +
                    """"bounds":{"left":200,"top":2200,"right":400,"bottom":2340},"visible":false,"selected":false}""",
            ),
        )
        assertThat(lines.drop(4)).isEqualTo(listOf("]}", ""))
    }

    @Test
    fun `finds every node with a view id`() {
        assertThat(snapshot.findByViewId("com.example.app:id/tab"))
            .containsExactly(
                ScreenNode(Bounds(0, 2200, 200, 2340), isVisibleToUser = true, isSelected = true),
                ScreenNode(Bounds(200, 2200, 400, 2340), isVisibleToUser = false, isSelected = false),
            ).inOrder()
        assertThat(snapshot.findByViewId("com.example.app:id/missing")).isEmpty()
    }
}
