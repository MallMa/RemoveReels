package io.github.mallma.removereels.guard

import com.google.common.truth.Truth.assertThat
import io.github.mallma.removereels.budget.Allowance
import io.github.mallma.removereels.budget.AllowanceSchedule
import io.github.mallma.removereels.budget.Allowances
import io.github.mallma.removereels.budget.BlockReason
import io.github.mallma.removereels.budget.TargetApp
import io.github.mallma.removereels.budget.UsageLedger
import io.github.mallma.removereels.budget.WatchTimer
import io.github.mallma.removereels.detection.Signal
import io.github.mallma.removereels.detection.SurfaceRule
import io.github.mallma.removereels.storage.AppState
import io.github.mallma.removereels.storage.InMemoryDataStore
import io.github.mallma.removereels.storage.StateStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource

class GuardTest {
    private val today = LocalDate.of(2026, 11, 2)
    private val clock = Clock.fixed(today.atTime(20, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC)
    private val time = TestTimeSource()
    private val actions = RecordingActions()
    private var automaticTimeOn = true

    private val reels = Screen(TargetApp.INSTAGRAM, SurfaceRule("reels", listOf(Signal("clips"))))
    private val shorts = Screen(TargetApp.YOUTUBE, SurfaceRule("shorts", listOf(Signal("reel"))))
    private val youtubeHome = Screen(TargetApp.YOUTUBE, surface = null)

    private fun guard(
        instagram: Int = 0,
        youtube: Int = 15,
        used: Duration = Duration.ZERO,
    ): Pair<Guard, StateStore> {
        val state =
            AppState(
                schedule = AllowanceSchedule(Allowances(Allowance.ofMinutes(instagram), Allowance.ofMinutes(youtube))),
                usage = UsageLedger().record(today, TargetApp.YOUTUBE, used),
            )
        val store = StateStore(InMemoryDataStore(state))
        return Guard(clock, { automaticTimeOn }, store, WatchTimer(time), actions) to store
    }

    @Test
    fun `blocks Reels without an allowance`() =
        runTest {
            val (guard, _) = guard()

            guard.check(reels)

            assertThat(actions.blocks).containsExactly(Block(reels, BlockReason.NoAllowance))
            assertThat(guard.isWatching).isFalse()
        }

    @Test
    fun `counts the time an allowed screen stays open`() =
        runTest {
            val (guard, store) = guard(youtube = 15)

            guard.check(shorts)
            time += 3.seconds
            guard.check(shorts)

            assertThat(guard.isWatching).isTrue()
            assertThat(store.usedToday(TargetApp.YOUTUBE)).isEqualTo(3.seconds)
            assertThat(actions.timeLeft).containsExactly(15.minutes, 15.minutes - 3.seconds).inOrder()
        }

    @Test
    fun `stops counting when the screen leaves Shorts`() =
        runTest {
            val (guard, store) = guard(youtube = 15)

            guard.check(shorts)
            time += 5.seconds
            guard.check(youtubeHome)
            time += 30.seconds
            guard.check(youtubeHome)

            assertThat(guard.isWatching).isFalse()
            assertThat(store.usedToday(TargetApp.YOUTUBE)).isEqualTo(5.seconds)
            assertThat(actions.timeLeftHidden).isEqualTo(1)
            assertThat(actions.surfaceGoneCount).isEqualTo(2)
        }

    @Test
    fun `stops counting when the app goes to the background or the screen turns off`() =
        runTest {
            val (guard, store) = guard(youtube = 15)

            guard.check(shorts)
            time += 2.seconds
            guard.check(null)
            time += 1.minutes

            assertThat(guard.isWatching).isFalse()
            assertThat(store.usedToday(TargetApp.YOUTUBE)).isEqualTo(2.seconds)
        }

    @Test
    fun `blocks when the allowance runs out while watching`() =
        runTest {
            val (guard, _) = guard(youtube = 15, used = 15.minutes - 1.seconds)

            guard.check(shorts)
            time += 2.seconds
            guard.check(shorts)

            assertThat(actions.blocks).containsExactly(Block(shorts, BlockReason.UsedUp(Allowance.ofMinutes(15))))
            assertThat(guard.isWatching).isFalse()
            assertThat(actions.timeLeftHidden).isEqualTo(1)
        }

    @Test
    fun `switching apps ends the first app's session`() =
        runTest {
            val (guard, store) = guard(instagram = 10, youtube = 15)

            guard.check(shorts)
            time += 4.seconds
            guard.check(reels)
            time += 6.seconds
            guard.check(null)

            assertThat(store.usedToday(TargetApp.YOUTUBE)).isEqualTo(4.seconds)
            assertThat(store.usedToday(TargetApp.INSTAGRAM)).isEqualTo(6.seconds)
        }

    @Test
    fun `blocks while automatic date and time is off`() =
        runTest {
            val (guard, _) = guard(youtube = 15)
            automaticTimeOn = false

            guard.check(shorts)

            assertThat(actions.blocks).containsExactly(Block(shorts, BlockReason.AutomaticTimeOff))
        }

    private suspend fun StateStore.usedToday(app: TargetApp) = state.first().usage.usedOn(today, app)

    private data class Block(
        val screen: Screen,
        val reason: BlockReason,
    )

    private class RecordingActions : GuardActions {
        val blocks = mutableListOf<Block>()
        val timeLeft = mutableListOf<Duration>()
        var timeLeftHidden = 0
        var surfaceGoneCount = 0

        override fun showTimeLeft(
            app: TargetApp,
            remaining: Duration,
        ) {
            timeLeft += remaining
        }

        override fun hideTimeLeft() {
            timeLeftHidden++
        }

        override suspend fun block(
            app: TargetApp,
            surface: SurfaceRule,
            reason: BlockReason,
        ) {
            blocks += Block(Screen(app, surface), reason)
        }

        override fun surfaceGone() {
            surfaceGoneCount++
        }
    }
}
