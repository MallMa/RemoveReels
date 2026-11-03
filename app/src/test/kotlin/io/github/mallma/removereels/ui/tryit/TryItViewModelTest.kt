package io.github.mallma.removereels.ui.tryit

import com.google.common.truth.Truth.assertThat
import io.github.mallma.removereels.InstalledApps
import io.github.mallma.removereels.budget.TargetApp
import io.github.mallma.removereels.guard.GuardStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TryItViewModelTest {
    private val status = GuardStatus().apply { serviceConnected() }

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `works once a screen is blocked after the step opens`() {
        status.blocked()
        val viewModel = TryItViewModel(status, InstalledApps { "1.0" })
        assertThat(viewModel.state.value.worked).isFalse()

        status.blocked()

        assertThat(
            viewModel.state.value,
        ).isEqualTo(TryItState(blockingOn = true, app = TargetApp.INSTAGRAM, worked = true))
    }

    @Test
    fun `uses YouTube when Instagram is not installed`() {
        val viewModel = TryItViewModel(status, InstalledApps { app -> if (app == TargetApp.YOUTUBE) "1.0" else null })

        assertThat(viewModel.state.value.app).isEqualTo(TargetApp.YOUTUBE)
    }
}
