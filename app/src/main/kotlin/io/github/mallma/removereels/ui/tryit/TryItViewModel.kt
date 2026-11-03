package io.github.mallma.removereels.ui.tryit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.mallma.removereels.AutomaticTime
import io.github.mallma.removereels.InstalledApps
import io.github.mallma.removereels.budget.BlockPolicy
import io.github.mallma.removereels.budget.Decision
import io.github.mallma.removereels.budget.TargetApp
import io.github.mallma.removereels.guard.GuardStatus
import io.github.mallma.removereels.storage.StateStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.LocalDate

/** A block the user triggers on purpose, so that the first one is no surprise. */
internal data class TryItState(
    val blockingOn: Boolean,
    /** An installed app whose Reels or Shorts are blocked right now, Instagram first. */
    val app: TargetApp?,
    /** Every installed app has time left today, so it would show the time left instead of a block. */
    val allowedNow: Boolean,
    /** A screen was blocked since this step opened. */
    val worked: Boolean,
)

internal class TryItViewModel(
    status: GuardStatus,
    installedApps: InstalledApps,
    store: StateStore,
    clock: Clock,
    automaticTime: AutomaticTime,
) : ViewModel() {
    private val blocksBefore = status.blocks.value
    private val installed = TargetApp.entries.filter { installedApps.versionOf(it) != null }

    /** Null until the stored state has been read. */
    val state: StateFlow<TryItState?> =
        combine(status.running, status.blocks, store.state) { running, blocks, state ->
            val today = LocalDate.now(clock)
            val blocked =
                installed.filter { app ->
                    BlockPolicy.decide(app, today, state.schedule, state.usage, automaticTime.isOn()) is Decision.Block
                }
            TryItState(
                blockingOn = running,
                app = blocked.firstOrNull(),
                allowedNow = installed.isNotEmpty() && blocked.isEmpty(),
                worked = blocks > blocksBefore,
            )
        }.stateIn(viewModelScope, SharingStarted.Eagerly, null)
}
