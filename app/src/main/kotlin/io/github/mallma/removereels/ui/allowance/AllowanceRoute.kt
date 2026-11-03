package io.github.mallma.removereels.ui.allowance

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.mallma.removereels.R
import io.github.mallma.removereels.container
import io.github.mallma.removereels.ui.shortDate
import kotlinx.coroutines.launch

/** [AllowanceScreen] with its view model; confirms each save with the day the change starts. */
@Composable
internal fun AllowanceRoute(onBack: () -> Unit) {
    val context = LocalContext.current
    val viewModel = viewModel { AllowanceViewModel(context.container.stateStore, context.container.clock) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    state?.let {
        val saved = stringResource(R.string.allowance_saved, shortDate(it.startsOn))
        AllowanceScreen(
            state = it,
            onBlock = viewModel::block,
            onAllow = viewModel::allow,
            onSave = {
                viewModel.save()
                scope.launch { snackbarHostState.showSnackbar(saved) }
            },
            onBack = onBack,
            snackbarHostState = snackbarHostState,
        )
    }
}
