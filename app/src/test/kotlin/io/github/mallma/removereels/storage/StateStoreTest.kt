package io.github.mallma.removereels.storage

import com.google.common.truth.Truth.assertThat
import io.github.mallma.removereels.budget.Allowance
import io.github.mallma.removereels.budget.Allowances
import io.github.mallma.removereels.budget.TargetApp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.LocalDate
import kotlin.time.Duration.Companion.seconds

class StateStoreTest {
    private val today = LocalDate.of(2026, 11, 2)
    private val store = StateStore(InMemoryDataStore(AppState.INITIAL))

    @Test
    fun `a new store blocks everything`() =
        runTest {
            assertThat(store.state.first()).isEqualTo(AppState.INITIAL)
        }

    @Test
    fun `a scheduled change starts tomorrow`() =
        runTest {
            val allowances = Allowances(Allowance.ofMinutes(10), Allowance.ofMinutes(5))
            store.scheduleChange(allowances, today)

            val schedule = store.state.first().schedule
            assertThat(schedule.allowancesOn(today)).isEqualTo(Allowances.BLOCKED)
            assertThat(schedule.allowancesOn(today.plusDays(1))).isEqualTo(allowances)
        }

    @Test
    fun `stale rules are remembered per app until they fit again`() =
        runTest {
            store.setRulesStale(TargetApp.INSTAGRAM, stale = true)
            assertThat(store.state.first().staleRules).containsExactly(TargetApp.INSTAGRAM)

            store.setRulesStale(TargetApp.INSTAGRAM, stale = false)
            assertThat(store.state.first().staleRules).isEmpty()
        }

    @Test
    fun `the time left shows until it is switched off`() =
        runTest {
            assertThat(store.state.first().showTimeLeft).isTrue()

            store.setShowTimeLeft(false)

            assertThat(store.state.first().showTimeLeft).isFalse()
        }

    @Test
    fun `recorded usage adds up`() =
        runTest {
            store.recordUsage(TargetApp.YOUTUBE, today, 3.seconds)
            store.recordUsage(TargetApp.YOUTUBE, today, 4.seconds)

            val usage = store.state.first().usage
            assertThat(usage.usedOn(today, TargetApp.YOUTUBE)).isEqualTo(7.seconds)
        }
}
