package io.github.mallma.removereels.detection

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class BoundsTest {
    private val screen = Bounds(0, 0, 1000, 2000)

    @Test
    fun `area is width times height, and zero for an empty rectangle`() {
        assertThat(Bounds(10, 20, 110, 70).area).isEqualTo(5000)
        assertThat(Bounds(100, 100, 50, 300).area).isEqualTo(0)
    }

    @Test
    fun `share is the covered fraction of the screen`() {
        assertThat(screen.shareOf(screen)).isEqualTo(1.0)
        assertThat(Bounds(0, 0, 1000, 500).shareOf(screen)).isEqualTo(0.25)
    }

    @Test
    fun `only the part on the screen counts`() {
        assertThat(Bounds(0, 1000, 1000, 3000).shareOf(screen)).isEqualTo(0.5)
        assertThat(Bounds(0, 2000, 1000, 4000).shareOf(screen)).isEqualTo(0.0)
    }

    @Test
    fun `the overlap of two rectangles, empty when they do not touch`() {
        assertThat(Bounds(0, 0, 600, 600).intersect(Bounds(400, 400, 1000, 1000))).isEqualTo(Bounds(400, 400, 600, 600))
        assertThat(Bounds(0, 0, 100, 100).intersect(Bounds(200, 200, 300, 300)).area).isEqualTo(0)
    }

    @Test
    fun `contains rectangles inside it, edges included`() {
        assertThat(Bounds(10, 10, 20, 20) in screen).isTrue()
        assertThat(screen in screen).isTrue()
        assertThat(Bounds(900, 1900, 1100, 2000) in screen).isFalse()
    }

    @Test
    fun `nothing covers an empty screen`() {
        assertThat(screen.shareOf(Bounds(0, 0, 0, 0))).isEqualTo(0.0)
    }
}
