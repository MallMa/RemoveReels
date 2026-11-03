package io.github.mallma.removereels.guard

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds

class BackOutTest {
    /** Leaves the blocked screen after [stepsToLeave] steps; never, if null. */
    private class FakeNavigation(
        private val stepsToLeave: Int?,
    ) : Navigation {
        val steps = mutableListOf<String>()

        override fun back() {
            steps += "back"
        }

        override fun tap(viewId: String) {
            steps += "tap $viewId"
        }

        override fun home() {
            steps += "home"
        }

        override fun isOnSurface() = stepsToLeave == null || steps.size < stepsToLeave
    }

    private suspend fun leave(
        stepsToLeave: Int?,
        homeTab: String? = "app:id/home_tab",
    ): List<String> {
        val navigation = FakeNavigation(stepsToLeave)
        BackOut(navigation, settle = 400.milliseconds).leave(homeTab)
        return navigation.steps
    }

    @Test
    fun `one back press is usually enough`() =
        runTest {
            assertThat(leave(stepsToLeave = 1)).containsExactly("back")
        }

    @Test
    fun `presses back up to three times`() =
        runTest {
            assertThat(leave(stepsToLeave = 3)).containsExactly("back", "back", "back")
        }

    @Test
    fun `then taps the home tab`() =
        runTest {
            assertThat(leave(stepsToLeave = 4)).containsExactly("back", "back", "back", "tap app:id/home_tab").inOrder()
        }

    @Test
    fun `goes to the launcher as a last resort`() =
        runTest {
            assertThat(leave(stepsToLeave = null))
                .containsExactly("back", "back", "back", "tap app:id/home_tab", "home")
                .inOrder()
        }

    @Test
    fun `skips the home tab when the app has none`() =
        runTest {
            assertThat(
                leave(stepsToLeave = null, homeTab = null),
            ).containsExactly("back", "back", "back", "home").inOrder()
        }
}
