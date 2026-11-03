package io.github.mallma.removereels.ui.tryit

import com.google.common.truth.Truth.assertThat
import io.github.mallma.removereels.InstalledApps
import io.github.mallma.removereels.budget.Allowance
import io.github.mallma.removereels.budget.AllowanceSchedule
import io.github.mallma.removereels.budget.Allowances
import io.github.mallma.removereels.budget.TargetApp
import io.github.mallma.removereels.budget.UsageLedger
import io.github.mallma.removereels.guard.GuardStatus
import io.github.mallma.removereels.storage.AppState
import io.github.mallma.removereels.storage.InMemoryDataStore
import io.github.mallma.removereels.storage.StateStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class TryItViewModelTest {
    private val status = GuardStatus().apply { serviceConnected() }
    private val clock = Clock.fixed(LocalDate.of(2026, 11, 2).atTime(12, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC)

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(
        instagram: Int = 0,
        youtube: Int = 0,
        installedApps: InstalledApps = InstalledApps { "1.0" },
    ): TryItViewModel {
        val allowances = Allowances(Allowance.ofMinutes(instagram), Allowance.ofMinutes(youtube))
        val store = StateStore(InMemoryDataStore(AppState(AllowanceSchedule(allowances), UsageLedger())))
        return TryItViewModel(status, installedApps, store, clock) { true }
    }

    @Test
    fun `works once a screen is blocked after the step opens`() {
        status.blocked()
        val viewModel = viewModel()
        assertThat(viewModel.state.value?.worked).isFalse()

        status.blocked()

        assertThat(viewModel.state.value)
            .isEqualTo(TryItState(blockingOn = true, app = TargetApp.INSTAGRAM, allowedNow = false, worked = true))
    }

    @Test
    fun `uses YouTube when Instagram is not installed`() {
        val viewModel = viewModel(installedApps = { app -> if (app == TargetApp.YOUTUBE) "1.0" else null })

        assertThat(viewModel.state.value?.app).isEqualTo(TargetApp.YOUTUBE)
    }

    @Test
    fun `uses the app that is blocked right now`() {
        val viewModel = viewModel(instagram = 15)

        assertThat(viewModel.state.value?.app).isEqualTo(TargetApp.YOUTUBE)
    }

    @Test
    fun `says so when every app has time left today`() {
        val state = viewModel(instagram = 15, youtube = 10).state.value

        assertThat(state?.app).isNull()
        assertThat(state?.allowedNow).isTrue()
    }
}
