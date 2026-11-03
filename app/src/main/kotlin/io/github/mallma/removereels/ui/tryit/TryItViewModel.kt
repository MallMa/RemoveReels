package io.github.mallma.removereels.ui.tryit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.mallma.removereels.InstalledApps
import io.github.mallma.removereels.budget.TargetApp
import io.github.mallma.removereels.guard.GuardStatus
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** A block the user triggers on purpose, so that the first one is no surprise. */
internal data class TryItState(
    val blockingOn: Boolean,
    /** The app to try it in: Instagram, or YouTube if Instagram is not installed. */
    val app: TargetApp?,
    /** A screen was blocked since this step opened. */
    val worked: Boolean,
)

internal class TryItViewModel(
    status: GuardStatus,
    installedApps: InstalledApps,
) : ViewModel() {
    private val blocksBefore = status.blocks.value
    private val app = TargetApp.entries.firstOrNull { installedApps.versionOf(it) != null }

    val state: StateFlow<TryItState> =
        combine(status.running, status.blocks) { running, blocks -> TryItState(running, app, blocks > blocksBefore) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, TryItState(status.running.value, app, worked = false))
}
