package io.github.mallma.removereels.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.mallma.removereels.storage.StateStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal class SettingsViewModel(
    private val store: StateStore,
) : ViewModel() {
    /** Null until the stored state has been read. */
    val showTimeLeft: StateFlow<Boolean?> =
        store.state.map { it.showTimeLeft }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun setShowTimeLeft(show: Boolean) {
        viewModelScope.launch { store.setShowTimeLeft(show) }
    }
}
