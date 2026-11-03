package io.github.mallma.removereels.storage

import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import io.github.mallma.removereels.budget.Allowances
import io.github.mallma.removereels.budget.TargetApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.time.LocalDate
import kotlin.time.Duration

/** The app's single source of truth, shared by the accessibility service and the UI. */
internal class StateStore(
    private val dataStore: DataStore<AppState>,
) {
    val state: Flow<AppState> get() = dataStore.data

    /** Schedules new allowances; they start the day after [today]. */
    suspend fun scheduleChange(
        allowances: Allowances,
        today: LocalDate,
    ) {
        dataStore.updateData { it.copy(schedule = it.schedule.scheduleChange(allowances, today)) }
    }

    suspend fun recordUsage(
        app: TargetApp,
        date: LocalDate,
        watched: Duration,
    ) {
        dataStore.updateData { it.copy(usage = it.usage.record(date, app, watched)) }
    }

    /** Remembers whether [app]'s rules look stale. DataStore skips the write when nothing changes. */
    suspend fun setRulesStale(
        app: TargetApp,
        stale: Boolean,
    ) {
        dataStore.updateData { it.copy(staleRules = if (stale) it.staleRules + app else it.staleRules - app) }
    }

    suspend fun setShowTimeLeft(show: Boolean) {
        dataStore.updateData { it.copy(showTimeLeft = show) }
    }

    suspend fun finishOnboarding() {
        dataStore.updateData { it.copy(onboardingDone = true) }
    }

    companion object {
        /** A store in [file]. A corrupt file is replaced by [AppState.INITIAL], which never loosens the rules. */
        fun create(
            file: File,
            scope: CoroutineScope,
        ) = StateStore(
            DataStoreFactory.create(
                serializer = AppStateSerializer,
                corruptionHandler = ReplaceFileCorruptionHandler { AppState.INITIAL },
                scope = scope,
                produceFile = { file },
            ),
        )
    }
}
