package io.github.mallma.removereels.budget

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource

class WatchTimerTest {
    private val time = TestTimeSource()
    private val timer = WatchTimer(time)

    @Test
    fun `measures nothing until started`() {
        time += 5.seconds

        assertThat(timer.isRunning).isFalse()
        assertThat(timer.lap()).isEqualTo(Duration.ZERO)
    }

    @Test
    fun `each lap measures the time since the previous one`() {
        timer.start()
        time += 3.seconds
        assertThat(timer.lap()).isEqualTo(3.seconds)

        time += 2.seconds
        assertThat(timer.lap()).isEqualTo(2.seconds)
    }

    @Test
    fun `starting again while running keeps the first start`() {
        timer.start()
        time += 3.seconds
        timer.start()
        time += 1.seconds

        assertThat(timer.lap()).isEqualTo(4.seconds)
    }

    @Test
    fun `stop returns the last lap and ends measuring`() {
        timer.start()
        time += 4.seconds

        assertThat(timer.stop()).isEqualTo(4.seconds)
        assertThat(timer.isRunning).isFalse()

        time += 10.seconds
        assertThat(timer.lap()).isEqualTo(Duration.ZERO)
    }
}
