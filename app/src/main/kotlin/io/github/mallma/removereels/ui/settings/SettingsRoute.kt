package io.github.mallma.removereels.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.mallma.removereels.container

/** [SettingsScreen] with its view model. */
@Composable
internal fun SettingsRoute(
    onOpenAllowance: () -> Unit,
    onTestBlocking: () -> Unit,
    onKeepRunning: () -> Unit,
    onAbout: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val viewModel = viewModel { SettingsViewModel(context.container.stateStore) }
    val showTimeLeft by viewModel.showTimeLeft.collectAsStateWithLifecycle()
    showTimeLeft?.let {
        SettingsScreen(
            showTimeLeft = it,
            onShowTimeLeftChange = viewModel::setShowTimeLeft,
            onOpenAllowance = onOpenAllowance,
            onTestBlocking = onTestBlocking,
            onKeepRunning = onKeepRunning,
            onAbout = onAbout,
            onBack = onBack,
        )
    }
}
