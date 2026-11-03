package io.github.mallma.removereels.ui

import kotlinx.serialization.Serializable

/** The app's screens, as type-safe navigation routes. */
internal sealed interface Route {
    @Serializable
    data object Home : Route
}
