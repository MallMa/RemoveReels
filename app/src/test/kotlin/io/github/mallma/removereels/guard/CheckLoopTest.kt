package io.github.mallma.removereels.guard

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class CheckLoopTest {
    private val checks = mutableListOf<Long>()
    private var ticking = false

    private fun TestScope.startLoop(): CheckLoop {
        val loop =
            CheckLoop(
                timeSource = testScheduler.timeSource,
                spacing = 250.milliseconds,
                tick = 1.seconds,
                ticking = { ticking },
                check = { checks += currentTime },
            )
        backgroundScope.launch { loop.run() }
        runCurrent()
        return loop
    }

    @Test
    fun `checks a new window at once`() =
        runTest {
            val loop = startLoop()
            loop.request(urgent = false)
            runCurrent()
            advanceTimeBy(100.milliseconds)

            loop.request(urgent = true)
            runCurrent()

            assertThat(checks).containsExactly(0L, 100L).inOrder()
        }

    @Test
    fun `content changes are checked at most every 250 ms`() =
        runTest {
            val loop = startLoop()
            repeat(10) {
                loop.request(urgent = false)
                advanceTimeBy(50.milliseconds)
            }
            runCurrent()

            assertThat(checks).containsExactly(0L, 250L, 500L).inOrder()
        }

    @Test
    fun `ticks every second while watching, without events`() =
        runTest {
            ticking = true
            startLoop()

            advanceTimeBy(3.seconds + 1.milliseconds)

            assertThat(checks).containsExactly(1000L, 2000L, 3000L).inOrder()
        }

    @Test
    fun `waits for events while not watching`() =
        runTest {
            startLoop()

            advanceTimeBy(10.seconds)

            assertThat(checks).isEmpty()
        }
}
