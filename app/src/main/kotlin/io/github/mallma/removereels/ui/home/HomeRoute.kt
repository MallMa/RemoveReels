package io.github.mallma.removereels.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.mallma.removereels.container
import io.github.mallma.removereels.ui.openAccessibilitySettings
import io.github.mallma.removereels.ui.openDateSettings

/** [HomeScreen] with its view model; re-reads the system state whenever the screen comes back into view. */
@Composable
internal fun HomeRoute(
    onOpenAllowance: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val context = LocalContext.current
    val viewModel =
        viewModel {
            val container = context.container
            HomeViewModel(
                container.stateStore,
                container.guardStatus,
                container.clock,
                container.automaticTime,
                container.installedApps,
            )
        }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }
    val state by viewModel.state.collectAsStateWithLifecycle()
    state?.let {
        HomeScreen(
            state = it,
            onTurnOn = context::openAccessibilitySettings,
            onOpenDateSettings = context::openDateSettings,
            onOpenAllowance = onOpenAllowance,
            onOpenSettings = onOpenSettings,
        )
    }
}
