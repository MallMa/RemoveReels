package io.github.mallma.removereels.ui

import kotlinx.serialization.Serializable

/** The app's screens, as type-safe navigation routes. */
internal sealed interface Route {
    @Serializable
    data object Welcome : Route

    @Serializable
    data object HowItWorks : Route

    @Serializable
    data object Notifications : Route

    /** [inOnboarding] leads on to the next step; otherwise the screen returns where it came from. */
    @Serializable
    data class TurnOn(
        val inOnboarding: Boolean,
    ) : Route

    @Serializable
    data class KeepRunning(
        val inOnboarding: Boolean,
    ) : Route

    @Serializable
    data class TestBlocking(
        val inOnboarding: Boolean,
    ) : Route

    @Serializable
    data object Home : Route

    @Serializable
    data object Allowance : Route

    @Serializable
    data object Settings : Route

    @Serializable
    data object About : Route
}
