package io.github.mallma.removereels.ui.allowance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.mallma.removereels.budget.Allowance
import io.github.mallma.removereels.budget.Allowances
import io.github.mallma.removereels.budget.TargetApp
import io.github.mallma.removereels.storage.StateStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate

/** The allowance editor: today's allowances, read only, and the ones from tomorrow being edited. */
internal data class AllowanceEditorState(
    val today: Allowances,
    /** What is saved for tomorrow; today's allowances if nothing is scheduled. */
    val saved: Allowances,
    val draft: Allowances,
    val startsOn: LocalDate,
) {
    val hasChanges: Boolean get() = draft != saved

    /** Whether [app] can have any time while the other app keeps its draft allowance. */
    fun canAllow(app: TargetApp): Boolean = draft.maximumMinutesFor(app) >= Allowance.STEP_MINUTES
}

internal class AllowanceViewModel(
    private val store: StateStore,
    private val clock: Clock,
) : ViewModel() {
    private val draft = MutableStateFlow<Allowances?>(null)

    /** Null until the stored state has been read. */
    val state: StateFlow<AllowanceEditorState?> =
        combine(store.state, draft) { state, draft ->
            val today = LocalDate.now(clock)
            val current = state.schedule.allowancesOn(today)
            val saved = state.schedule.changeAfter(today)?.allowances ?: current
            AllowanceEditorState(current, saved, draft ?: saved, today.plusDays(1))
        }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun block(app: TargetApp) = edit { it.with(app, Allowance.BLOCKED) }

    /**
     * Allows [minutes] for [app], rounded to a step and kept within what the other app leaves. Does nothing if the
     * other app already has the whole daily maximum.
     */
    fun allow(
        app: TargetApp,
        minutes: Int,
    ) = edit { draft ->
        val step = Allowance.STEP_MINUTES
        val maximum = draft.maximumMinutesFor(app)
        if (maximum < step) {
            draft
        } else {
            val rounded = (minutes + step / 2) / step * step
            draft.with(app, Allowance.ofMinutes(rounded.coerceIn(step, maximum)))
        }
    }

    /** Saves the draft; like every change, it starts tomorrow. */
    fun save() {
        val allowances = state.value?.draft ?: return
        viewModelScope.launch {
            store.scheduleChange(allowances, LocalDate.now(clock))
            draft.value = null
        }
    }

    private fun edit(change: (Allowances) -> Allowances) {
        state.value?.let { draft.value = change(it.draft) }
    }
}
