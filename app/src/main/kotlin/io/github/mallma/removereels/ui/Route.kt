package io.github.mallma.removereels.ui

import kotlinx.serialization.Serializable

/** The app's screens, as type-safe navigation routes. */
internal sealed interface Route {
    @Serializable
    data object Home : Route

    @Serializable
    data object Allowance : Route

    @Serializable
    data object Settings : Route

    @Serializable
    data object TestBlocking : Route

    @Serializable
    data object KeepRunning : Route

    @Serializable
    data object About : Route
}
