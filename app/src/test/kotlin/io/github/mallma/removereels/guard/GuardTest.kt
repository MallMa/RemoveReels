package io.github.mallma.removereels.guard

import com.google.common.truth.Truth.assertThat
import io.github.mallma.removereels.budget.Allowance
import io.github.mallma.removereels.budget.AllowanceSchedule
import io.github.mallma.removereels.budget.Allowances
import io.github.mallma.removereels.budget.BlockReason
import io.github.mallma.removereels.budget.TargetApp
import io.github.mallma.removereels.budget.UsageLedger
import io.github.mallma.removereels.budget.WatchTimer
import io.github.mallma.removereels.detection.Bounds
import io.github.mallma.removereels.detection.CanaryResult
import io.github.mallma.removereels.detection.EntryPoint
import io.github.mallma.removereels.detection.ReelsPageRule
import io.github.mallma.removereels.detection.Signal
import io.github.mallma.removereels.detection.SurfaceRule
import io.github.mallma.removereels.storage.AppState
import io.github.mallma.removereels.storage.InMemoryDataStore
import io.github.mallma.removereels.storage.StateStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource

class GuardTest {
    private val today = LocalDate.of(2026, 11, 2)
    private val clock = MutableClock(today.atTime(20, 0).toInstant(ZoneOffset.UTC))
    private val time = TestTimeSource()
    private val actions = RecordingActions()
    private var automaticTimeOn = true

    private val reels = Screen(TargetApp.INSTAGRAM, SurfaceRule("reels", listOf(Signal("clips"))))
    private val shorts = Screen(TargetApp.YOUTUBE, SurfaceRule("shorts", listOf(Signal("reel"))))
    private val youtubeHome = Screen(TargetApp.YOUTUBE, surface = null)
    private val tab = EntryPoint("reels_tab", Bounds(216, 2064, 432, 2190))
    private val instagramFeed = Screen(TargetApp.INSTAGRAM, surface = null, entryPoints = listOf(tab))
    private val reelsPage = ReelsPageRule("profile_reels", viewId = "clips_grid", pager = "profile_pager")
    private val profileOnReels = Screen(TargetApp.INSTAGRAM, surface = null, reelsPage = reelsPage)
    private val profileOnPosts = Screen(TargetApp.INSTAGRAM, surface = null)

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
            val (guard, _) = guard(youtube = 15)

            guard.check(shorts)
            time += 3.seconds
            guard.check(shorts)

            assertThat(guard.isWatching).isTrue()
            assertThat(actions.timeLeft).containsExactly(15.minutes, 15.minutes - 3.seconds).inOrder()
        }

    @Test
    fun `saves the watching time every five seconds, not on every check`() =
        runTest {
            val (guard, store) = guard(youtube = 15)

            guard.check(shorts)
            repeat(4) {
                time += 1.seconds
                guard.check(shorts)
            }
            assertThat(store.usedToday(TargetApp.YOUTUBE)).isEqualTo(Duration.ZERO)

            time += 1.seconds
            guard.check(shorts)
            assertThat(store.usedToday(TargetApp.YOUTUBE)).isEqualTo(5.seconds)
        }

    @Test
    fun `time before midnight counts for that day, after it for the next`() =
        runTest {
            val (guard, store) = guard(youtube = 15)
            clock.now = today.atTime(23, 59, 57).toInstant(ZoneOffset.UTC)

            guard.check(shorts)
            repeat(6) {
                time += 1.seconds
                clock.now = clock.now.plusSeconds(1)
                guard.check(shorts)
            }
            guard.check(null)

            val usage = store.state.first().usage
            assertThat(usage.usedOn(today, TargetApp.YOUTUBE)).isEqualTo(2.seconds)
            assertThat(usage.usedOn(today.plusDays(1), TargetApp.YOUTUBE)).isEqualTo(4.seconds)
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
    fun `a failed save still ends the session`() =
        runTest {
            val state =
                AppState(AllowanceSchedule(Allowances(Allowance.BLOCKED, Allowance.ofMinutes(15))), UsageLedger())
            val dataStore = InMemoryDataStore(state)
            val guard = Guard(clock, { true }, StateStore(dataStore), WatchTimer(time), actions)
            guard.check(shorts)
            dataStore.failWrites = true
            time += 2.seconds

            assertThrows(IOException::class.java) { runBlocking { guard.check(null) } }

            assertThat(guard.isWatching).isFalse()
            assertThat(actions.timeLeftHidden).isEqualTo(1)
        }

    @Test
    fun `covers the entry points while the app is blocked`() =
        runTest {
            val (guard, _) = guard(instagram = 0)

            guard.check(instagramFeed)

            assertThat(actions.covers.last()).containsExactly(tab)
            assertThat(guard.isCovering).isTrue()
        }

    @Test
    fun `leaves the entry points visible while the allowance has time left`() =
        runTest {
            val (guard, _) = guard(youtube = 15)

            guard.check(youtubeHome.copy(entryPoints = listOf(tab)))

            assertThat(actions.covers.last()).isEmpty()
            assertThat(guard.isCovering).isFalse()
        }

    @Test
    fun `removes the covers over a blocked surface and when the app leaves`() =
        runTest {
            val (guard, _) = guard(instagram = 0)
            guard.check(instagramFeed)

            guard.check(reels.copy(entryPoints = listOf(tab)))
            assertThat(actions.covers.last()).isEmpty()

            guard.check(instagramFeed)
            guard.check(null)
            assertThat(actions.covers.last()).isEmpty()
            assertThat(guard.isCovering).isFalse()
        }

    @Test
    fun `leaves a page that lists only Reels while the app is blocked`() =
        runTest {
            val (guard, _) = guard(instagram = 0)

            guard.check(profileOnReels)

            assertThat(actions.left).containsExactly(reelsPage)
        }

    @Test
    fun `leaves a Reels page once each time it comes into view`() =
        runTest {
            val (guard, _) = guard(instagram = 0)

            guard.check(profileOnReels)
            guard.check(profileOnReels)
            assertThat(actions.left).hasSize(1)

            guard.check(profileOnPosts)
            guard.check(profileOnReels)
            assertThat(actions.left).hasSize(2)
        }

    @Test
    fun `stays on a Reels page while the allowance has time left`() =
        runTest {
            val (guard, _) = guard(youtube = 15)

            guard.check(youtubeHome.copy(reelsPage = reelsPage))

            assertThat(actions.left).isEmpty()
        }

    @Test
    fun `blocks while automatic date and time is off`() =
        runTest {
            val (guard, _) = guard(youtube = 15)
            automaticTimeOn = false

            guard.check(shorts)

            assertThat(actions.blocks).containsExactly(Block(shorts, BlockReason.AutomaticTimeOff))
        }

    @Test
    fun `rules count as stale only after repeated stale checks`() =
        runTest {
            val (guard, store) = guard()
            val staleFeed = Screen(TargetApp.INSTAGRAM, surface = null, canary = CanaryResult.STALE)

            repeat(7) { guard.check(staleFeed) }
            assertThat(store.state.first().staleRules).isEmpty()

            guard.check(staleFeed)
            assertThat(store.state.first().staleRules).containsExactly(TargetApp.INSTAGRAM)
        }

    @Test
    fun `one healthy check clears stale rules`() =
        runTest {
            val (guard, store) = guard()
            repeat(8) { guard.check(Screen(TargetApp.INSTAGRAM, surface = null, canary = CanaryResult.STALE)) }

            guard.check(Screen(TargetApp.INSTAGRAM, surface = null, canary = CanaryResult.HEALTHY))

            assertThat(store.state.first().staleRules).isEmpty()
        }

    @Test
    fun `a healthy check resets the count of stale checks`() =
        runTest {
            val (guard, store) = guard()
            val stale = Screen(TargetApp.INSTAGRAM, surface = null, canary = CanaryResult.STALE)

            repeat(7) { guard.check(stale) }
            guard.check(Screen(TargetApp.INSTAGRAM, surface = null, canary = CanaryResult.HEALTHY))
            repeat(7) { guard.check(stale) }

            assertThat(store.state.first().staleRules).isEmpty()
        }

    private suspend fun StateStore.usedToday(app: TargetApp) = state.first().usage.usedOn(today, app)

    /** A clock the test moves by hand, in UTC. */
    private class MutableClock(
        var now: Instant,
    ) : Clock() {
        override fun getZone(): ZoneId = ZoneOffset.UTC

        override fun withZone(zone: ZoneId): Clock = this

        override fun instant(): Instant = now
    }

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

        /** The entry points of each call to [cover]. */
        val covers = mutableListOf<List<EntryPoint>>()

        override fun cover(entryPoints: List<EntryPoint>) {
            covers += entryPoints
        }

        /** The Reels pages left, in order. */
        val left = mutableListOf<ReelsPageRule>()

        override fun leave(
            app: TargetApp,
            page: ReelsPageRule,
        ) {
            left += page
        }
    }
}
