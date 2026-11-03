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
    fun `nothing covers an empty screen`() {
        assertThat(screen.shareOf(Bounds(0, 0, 0, 0))).isEqualTo(0.0)
    }
}
