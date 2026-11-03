package io.github.mallma.removereels.budget

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class AllowancesTest {
    @ParameterizedTest
    @CsvSource("0, 0", "45, 0", "0, 45", "20, 25", "30, 15")
    fun `accepts a total of up to 45 minutes`(
        instagram: Int,
        youtube: Int,
    ) {
        val allowances = allowances(instagram, youtube)

        assertThat(allowances[TargetApp.INSTAGRAM].minutes).isEqualTo(instagram)
        assertThat(allowances[TargetApp.YOUTUBE].minutes).isEqualTo(youtube)
    }

    @ParameterizedTest
    @CsvSource("45, 5", "25, 25", "5, 45")
    fun `rejects a total above 45 minutes`(
        instagram: Int,
        youtube: Int,
    ) {
        assertThrows<IllegalArgumentException> { allowances(instagram, youtube) }
    }

    @Test
    fun `both apps start blocked`() {
        assertThat(Allowances.BLOCKED[TargetApp.INSTAGRAM]).isEqualTo(Allowance.BLOCKED)
        assertThat(Allowances.BLOCKED[TargetApp.YOUTUBE]).isEqualTo(Allowance.BLOCKED)
    }

    @Test
    fun `with replaces one app's allowance`() {
        val start = allowances(instagram = 10, youtube = 15)

        assertThat(start.with(TargetApp.INSTAGRAM, Allowance.ofMinutes(30))).isEqualTo(allowances(30, 15))
        assertThat(start.with(TargetApp.YOUTUBE, Allowance.BLOCKED)).isEqualTo(allowances(10, 0))
    }

    @Test
    fun `with fails when the total would exceed 45 minutes`() {
        assertThrows<IllegalArgumentException> {
            allowances(instagram = 20, youtube = 0).with(TargetApp.YOUTUBE, Allowance.ofMinutes(30))
        }
    }

    @Test
    fun `each app can have what the other app leaves`() {
        val allowances = allowances(instagram = 20, youtube = 10)

        assertThat(allowances.maximumMinutesFor(TargetApp.INSTAGRAM)).isEqualTo(35)
        assertThat(allowances.maximumMinutesFor(TargetApp.YOUTUBE)).isEqualTo(25)
    }
}
