package io.github.mallma.removereels.storage

import androidx.datastore.core.CorruptionException
import com.google.common.truth.Truth.assertThat
import io.github.mallma.removereels.budget.Allowance
import io.github.mallma.removereels.budget.AllowanceSchedule
import io.github.mallma.removereels.budget.Allowances
import io.github.mallma.removereels.budget.TargetApp
import io.github.mallma.removereels.budget.UsageLedger
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

class AppStateSerializerTest {
    private val today = LocalDate.of(2026, 11, 2)

    @Test
    fun `a written state reads back unchanged`() =
        runTest {
            val state =
                AppState(
                    schedule =
                        AllowanceSchedule(Allowances(Allowance.BLOCKED, Allowance.ofMinutes(15)))
                            .scheduleChange(Allowances(Allowance.ofMinutes(20), Allowance.ofMinutes(25)), today),
                    usage =
                        UsageLedger()
                            .record(today, TargetApp.YOUTUBE, 7.minutes + 250.milliseconds)
                            .record(today.minusDays(1), TargetApp.INSTAGRAM, 3.minutes),
                )

            assertThat(read(write(state))).isEqualTo(state)
        }

    @Test
    fun `missing fields read as blocked`() =
        runTest {
            assertThat(read("{}")).isEqualTo(AppState.INITIAL)
        }

    @Test
    fun `unreadable JSON is corrupt`() {
        assertCorrupt("not json")
    }

    @Test
    fun `a date that is not ISO is corrupt`() {
        assertCorrupt("""{"usage":[{"date":"2.11.2026"}]}""")
    }

    @Test
    fun `allowances that break the rules are corrupt`() {
        assertCorrupt("""{"schedule":{"current":{"instagramMinutes":30,"youtubeMinutes":30}}}""")
        assertCorrupt("""{"schedule":{"current":{"instagramMinutes":7}}}""")
    }

    private fun assertCorrupt(text: String) {
        assertThrows(CorruptionException::class.java) { runTest { read(text) } }
    }

    private suspend fun write(state: AppState): String =
        ByteArrayOutputStream().also { AppStateSerializer.writeTo(state, it) }.toString(Charsets.UTF_8)

    private suspend fun read(text: String): AppState =
        AppStateSerializer.readFrom(ByteArrayInputStream(text.toByteArray()))
}
