package io.github.mallma.removereels.storage

import io.github.mallma.removereels.budget.AllowanceSchedule
import io.github.mallma.removereels.budget.UsageLedger

/** Everything the app remembers. Clearing the app's storage brings back [INITIAL], which blocks everything. */
internal data class AppState(
    val schedule: AllowanceSchedule,
    val usage: UsageLedger,
) {
    companion object {
        val INITIAL = AppState(AllowanceSchedule.INITIAL, UsageLedger())
    }
}
