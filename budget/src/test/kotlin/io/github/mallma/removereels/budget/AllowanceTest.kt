package io.github.mallma.removereels.budget

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.time.Duration.Companion.minutes

class AllowanceTest {
    @ParameterizedTest
    @ValueSource(ints = [5, 10, 15, 20, 25, 30, 35, 40, 45])
    fun `accepts 5 to 45 minutes in steps of 5`(minutes: Int) {
        val allowance = Allowance.ofMinutes(minutes)

        assertThat(allowance.minutes).isEqualTo(minutes)
        assertThat(allowance.duration).isEqualTo(minutes.minutes)
        assertThat(allowance.isBlocked).isFalse()
    }

    @ParameterizedTest
    @ValueSource(ints = [-5, 1, 4, 12, 44, 50, 60])
    fun `rejects any other number of minutes`(minutes: Int) {
        assertThrows<IllegalArgumentException> { Allowance.ofMinutes(minutes) }
    }

    @Test
    fun `zero minutes is blocked`() {
        assertThat(Allowance.ofMinutes(0)).isEqualTo(Allowance.BLOCKED)
        assertThat(Allowance.BLOCKED.isBlocked).isTrue()
        assertThat(Allowance.BLOCKED.duration).isEqualTo(0.minutes)
    }
}
