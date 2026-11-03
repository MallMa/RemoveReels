package io.github.mallma.removereels.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.mallma.removereels.AutomaticTime
import io.github.mallma.removereels.InstalledApps
import io.github.mallma.removereels.guard.GuardStatus
import io.github.mallma.removereels.storage.StateStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.Clock
import java.time.LocalDate

internal class HomeViewModel(
    store: StateStore,
    status: GuardStatus,
    clock: Clock,
    automaticTime: AutomaticTime,
    installedApps: InstalledApps,
) : ViewModel() {
    private val refreshes = MutableStateFlow(0)

    /** Null until the stored state has been read. */
    val state: StateFlow<HomeState?> =
        combine(store.state, status.running, refreshes) { state, running, _ ->
            homeState(state, LocalDate.now(clock), running, automaticTime.isOn(), installedApps)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    /** Reads again what can change outside the app: the date, automatic time and the installed apps. */
    fun refresh() {
        refreshes.update { it + 1 }
    }

    private companion object {
        /** Keeps the state through a configuration change. */
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
