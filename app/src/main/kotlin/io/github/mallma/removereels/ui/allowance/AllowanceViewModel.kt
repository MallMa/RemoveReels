package io.github.mallma.removereels.ui.allowance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.mallma.removereels.AutomaticTime
import io.github.mallma.removereels.budget.Allowance
import io.github.mallma.removereels.budget.Allowances
import io.github.mallma.removereels.budget.TargetApp
import io.github.mallma.removereels.storage.StateStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
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
    val automaticTimeOn: Boolean,
) {
    val hasChanges: Boolean get() = draft != saved

    val canSave: Boolean get() = hasChanges && automaticTimeOn

    /** Whether [app] can have any time while the other app keeps its draft allowance. */
    fun canAllow(app: TargetApp): Boolean = draft.maximumMinutesFor(app) >= Allowance.STEP_MINUTES
}

internal class AllowanceViewModel(
    private val store: StateStore,
    private val clock: Clock,
    private val automaticTime: AutomaticTime,
) : ViewModel() {
    private val draft = MutableStateFlow<Allowances?>(null)
    private val refreshes = MutableStateFlow(0)

    /** Null until the stored state has been read. */
    val state: StateFlow<AllowanceEditorState?> =
        combine(store.state, draft, refreshes) { state, draft, _ ->
            val today = LocalDate.now(clock)
            val current = state.schedule.allowancesOn(today)
            val saved = state.schedule.changeAfter(today)?.allowances ?: current
            AllowanceEditorState(current, saved, draft ?: saved, today.plusDays(1), automaticTime.isOn())
        }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Reads again what can change outside the app: the date and automatic time. */
    fun refresh() {
        refreshes.update { it + 1 }
    }

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

    /**
     * Saves the draft from the day after today and passes that day to [onSaved]. Refuses while automatic time is off:
     * saving on a date set by hand would make a scheduled change current on the wrong day, so a raised allowance
     * could start early and stay.
     */
    fun save(onSaved: (startsOn: LocalDate) -> Unit) {
        val allowances = state.value?.draft
        if (allowances == null || !automaticTime.isOn()) {
            refresh()
            return
        }
        val today = LocalDate.now(clock)
        viewModelScope.launch {
            store.scheduleChange(allowances, today)
            draft.value = null
            onSaved(today.plusDays(1))
        }
    }

    private fun edit(change: (Allowances) -> Allowances) {
        state.value?.let { draft.value = change(it.draft) }
    }
}
