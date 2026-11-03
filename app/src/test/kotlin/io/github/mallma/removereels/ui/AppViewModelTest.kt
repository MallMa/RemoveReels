package io.github.mallma.removereels.ui

import com.google.common.truth.Truth.assertThat
import io.github.mallma.removereels.storage.AppState
import io.github.mallma.removereels.storage.InMemoryDataStore
import io.github.mallma.removereels.storage.StateStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModelTest {
    private val store = StateStore(InMemoryDataStore(AppState.INITIAL))
    private var scheduled = 0

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `the blocking check starts only once onboarding is done`() =
        runTest {
            val viewModel = AppViewModel(store) { scheduled++ }
            assertThat(viewModel.onboardingDone.value).isFalse()
            assertThat(scheduled).isEqualTo(0)

            viewModel.finishOnboarding()

            assertThat(viewModel.onboardingDone.value).isTrue()
            assertThat(scheduled).isEqualTo(1)
        }
}
