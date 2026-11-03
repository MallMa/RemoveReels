package io.github.mallma.removereels.guard

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class GuardStatusTest {
    private val status = GuardStatus()

    @Test
    fun `a turn-on request is taken once`() {
        status.requestTurnOn()

        assertThat(status.takeTurnOnRequest()).isTrue()
        assertThat(status.takeTurnOnRequest()).isFalse()
    }

    @Test
    fun `without a request the service does not bring the app back`() {
        assertThat(status.takeTurnOnRequest()).isFalse()
    }
}
