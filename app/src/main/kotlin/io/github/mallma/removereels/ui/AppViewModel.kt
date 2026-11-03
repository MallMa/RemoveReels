package io.github.mallma.removereels.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.mallma.removereels.storage.StateStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Whether the app starts with onboarding or on Home. Once onboarding is done, [scheduleBlockingCheck] starts the
 * "Blocking is off" check; before that, the notification would only interrupt onboarding's own Turn on step.
 */
internal class AppViewModel(
    private val store: StateStore,
    scheduleBlockingCheck: () -> Unit,
) : ViewModel() {
    /** Null until the stored state has been read. */
    val onboardingDone: StateFlow<Boolean?> =
        store.state.map { it.onboardingDone }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        viewModelScope.launch {
            onboardingDone.first { it == true }
            scheduleBlockingCheck()
        }
    }

    fun finishOnboarding() {
        viewModelScope.launch { store.finishOnboarding() }
    }
}
