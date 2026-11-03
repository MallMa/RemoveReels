package io.github.mallma.removereels.ui.allowance

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.mallma.removereels.R
import io.github.mallma.removereels.container
import io.github.mallma.removereels.ui.formatShort
import io.github.mallma.removereels.ui.openDateSettings
import kotlinx.coroutines.launch

/** [AllowanceScreen] with its view model; confirms each save with the day the change starts. */
@Composable
internal fun AllowanceRoute(onBack: () -> Unit) {
    val context = LocalContext.current
    val viewModel =
        viewModel {
            AllowanceViewModel(context.container.stateStore, context.container.clock, context.container.automaticTime)
        }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current
    val locale = LocalConfiguration.current.locales[0]
    val scope = rememberCoroutineScope()
    state?.let {
        AllowanceScreen(
            state = it,
            onBlock = viewModel::block,
            onAllow = viewModel::allow,
            onSave = {
                viewModel.save { startsOn ->
                    val message = resources.getString(R.string.allowance_saved, startsOn.formatShort(locale))
                    scope.launch { snackbarHostState.showSnackbar(message) }
                }
            },
            onOpenDateSettings = context::openDateSettings,
            onBack = onBack,
            snackbarHostState = snackbarHostState,
        )
    }
}
