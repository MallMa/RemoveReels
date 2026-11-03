package io.github.mallma.removereels.budget

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class UsageLedgerTest {
    private val today = LocalDate.of(2026, 11, 2)

    @Test
    fun `nothing is used on a day without records`() {
        assertThat(UsageLedger().usedOn(today, TargetApp.INSTAGRAM)).isEqualTo(Duration.ZERO)
    }

    @Test
    fun `records add up per app and day`() {
        val ledger =
            UsageLedger()
                .record(today, TargetApp.INSTAGRAM, 30.seconds)
                .record(today, TargetApp.INSTAGRAM, 90.seconds)
                .record(today, TargetApp.YOUTUBE, 5.seconds)
                .record(today.plusDays(1), TargetApp.INSTAGRAM, 1.minutes)

        assertThat(ledger.usedOn(today, TargetApp.INSTAGRAM)).isEqualTo(2.minutes)
        assertThat(ledger.usedOn(today, TargetApp.YOUTUBE)).isEqualTo(5.seconds)
        assertThat(ledger.usedOn(today.plusDays(1), TargetApp.INSTAGRAM)).isEqualTo(1.minutes)
    }

    @Test
    fun `keeps the last seven days`() {
        val ledger =
            (0L..9L).fold(UsageLedger()) { ledger, day ->
                ledger.record(today.plusDays(day), TargetApp.YOUTUBE, 1.minutes)
            }

        assertThat(ledger.days.keys).containsExactlyElementsIn((3L..9L).map(today::plusDays))
    }

    @Test
    fun `rejects negative time`() {
        assertThrows<IllegalArgumentException> { UsageLedger().record(today, TargetApp.YOUTUBE, (-1).seconds) }
    }
}
