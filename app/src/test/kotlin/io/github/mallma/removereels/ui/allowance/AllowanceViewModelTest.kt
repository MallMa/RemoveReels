package io.github.mallma.removereels.ui.allowance

import com.google.common.truth.Truth.assertThat
import io.github.mallma.removereels.budget.Allowance
import io.github.mallma.removereels.budget.AllowanceSchedule
import io.github.mallma.removereels.budget.Allowances
import io.github.mallma.removereels.budget.TargetApp
import io.github.mallma.removereels.budget.UsageLedger
import io.github.mallma.removereels.storage.AppState
import io.github.mallma.removereels.storage.InMemoryDataStore
import io.github.mallma.removereels.storage.StateStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class AllowanceViewModelTest {
    private val today = LocalDate.of(2026, 11, 2)
    private val clock = Clock.fixed(today.atTime(12, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC)

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun allowances(
        instagram: Int,
        youtube: Int,
    ) = Allowances(Allowance.ofMinutes(instagram), Allowance.ofMinutes(youtube))

    private var automaticTimeOn = true

    private fun editor(schedule: AllowanceSchedule = AllowanceSchedule.INITIAL): Pair<AllowanceViewModel, StateStore> {
        val store = StateStore(InMemoryDataStore(AppState(schedule, UsageLedger())))
        return AllowanceViewModel(store, clock) { automaticTimeOn } to store
    }

    private suspend fun AllowanceViewModel.current() = state.filterNotNull().first()

    @Test
    fun `starts from what is saved for tomorrow`() =
        runTest {
            val schedule = AllowanceSchedule(allowances(0, 15)).scheduleChange(allowances(20, 15), today)
            val (editor, _) = editor(schedule)

            val state = editor.current()
            assertThat(state.today).isEqualTo(allowances(0, 15))
            assertThat(state.draft).isEqualTo(allowances(20, 15))
            assertThat(state.startsOn).isEqualTo(today.plusDays(1))
            assertThat(state.hasChanges).isFalse()
        }

    @Test
    fun `allowing rounds to five minutes and stays within what the other app leaves`() =
        runTest {
            val (editor, _) = editor(AllowanceSchedule(allowances(0, 30)))

            editor.allow(TargetApp.INSTAGRAM, 12)
            assertThat(editor.current().draft[TargetApp.INSTAGRAM]).isEqualTo(Allowance.ofMinutes(10))

            editor.allow(TargetApp.INSTAGRAM, 40)
            assertThat(editor.current().draft[TargetApp.INSTAGRAM]).isEqualTo(Allowance.ofMinutes(15))

            editor.allow(TargetApp.INSTAGRAM, 0)
            assertThat(editor.current().draft[TargetApp.INSTAGRAM]).isEqualTo(Allowance.ofMinutes(5))
        }

    @Test
    fun `an app cannot be allowed while the other has the whole maximum`() =
        runTest {
            val (editor, _) = editor(AllowanceSchedule(allowances(0, 45)))

            editor.allow(TargetApp.INSTAGRAM, 10)

            val state = editor.current()
            assertThat(state.canAllow(TargetApp.INSTAGRAM)).isFalse()
            assertThat(state.draft[TargetApp.INSTAGRAM]).isEqualTo(Allowance.BLOCKED)
        }

    @Test
    fun `blocking an app frees its minutes for the other`() =
        runTest {
            val (editor, _) = editor(AllowanceSchedule(allowances(30, 15)))

            editor.block(TargetApp.INSTAGRAM)

            val state = editor.current()
            assertThat(state.draft).isEqualTo(allowances(0, 15))
            assertThat(state.draft.maximumMinutesFor(TargetApp.YOUTUBE)).isEqualTo(45)
            assertThat(state.hasChanges).isTrue()
        }

    @Test
    fun `saving schedules the draft for tomorrow and leaves today alone`() =
        runTest {
            val (editor, store) = editor()

            var startsOn: LocalDate? = null

            editor.allow(TargetApp.YOUTUBE, 20)
            editor.save { startsOn = it }

            val schedule = store.state.first().schedule
            assertThat(schedule.allowancesOn(today)).isEqualTo(Allowances.BLOCKED)
            assertThat(schedule.allowancesOn(today.plusDays(1))).isEqualTo(allowances(0, 20))
            assertThat(editor.current().hasChanges).isFalse()
            assertThat(startsOn).isEqualTo(today.plusDays(1))
        }

    @Test
    fun `refuses to save while automatic time is off`() =
        runTest {
            val (editor, store) = editor()
            automaticTimeOn = false
            var saved = false

            editor.allow(TargetApp.YOUTUBE, 20)
            editor.save { saved = true }

            assertThat(saved).isFalse()
            assertThat(store.state.first().schedule).isEqualTo(AllowanceSchedule.INITIAL)
            assertThat(editor.current().automaticTimeOn).isFalse()
            assertThat(editor.current().canSave).isFalse()
        }
}
