package io.github.mallma.removereels.ui.home

import io.github.mallma.removereels.InstalledApps
import io.github.mallma.removereels.budget.Allowance
import io.github.mallma.removereels.budget.TargetApp
import io.github.mallma.removereels.storage.AppState
import java.time.LocalDate
import kotlin.time.Duration

/** Everything the Home screen shows. */
internal data class HomeState(
    val blockingOn: Boolean,
    val automaticTimeOn: Boolean,
    val cards: List<AppCard>,
) {
    /** Today's allowances of both apps together. */
    val allowedToday: Duration get() = cards.fold(Duration.ZERO) { total, card -> total + card.today.duration }

    /** Today's use of both apps together, up to each app's allowance. */
    val usedToday: Duration get() = cards.fold(Duration.ZERO) { total, card -> total + card.used }
}

/** One app's card: today's allowance and use, and the allowance from tomorrow if it changes. */
internal data class AppCard(
    val app: TargetApp,
    val installed: Boolean,
    val rulesStale: Boolean,
    val today: Allowance,
    val used: Duration,
    val fromTomorrow: Allowance?,
) {
    val left: Duration get() = (today.duration - used).coerceAtLeast(Duration.ZERO)
}

internal fun homeState(
    state: AppState,
    today: LocalDate,
    blockingOn: Boolean,
    automaticTimeOn: Boolean,
    installedApps: InstalledApps,
): HomeState {
    val allowances = state.schedule.allowancesOn(today)
    val change = state.schedule.changeAfter(today)?.allowances
    return HomeState(
        blockingOn = blockingOn,
        automaticTimeOn = automaticTimeOn,
        cards =
            TargetApp.entries.map { app ->
                AppCard(
                    app = app,
                    installed = installedApps.versionOf(app) != null,
                    rulesStale = app in state.staleRules,
                    today = allowances[app],
                    used = state.usage.usedOn(today, app).coerceAtMost(allowances[app].duration),
                    fromTomorrow = change?.get(app)?.takeIf { it != allowances[app] },
                )
            },
    )
}
