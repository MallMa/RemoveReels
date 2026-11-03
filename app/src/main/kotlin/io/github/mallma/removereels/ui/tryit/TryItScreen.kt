package io.github.mallma.removereels.ui.tryit

import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.mallma.removereels.R
import io.github.mallma.removereels.budget.TargetApp
import io.github.mallma.removereels.container
import io.github.mallma.removereels.ui.SimpleScreen
import io.github.mallma.removereels.ui.launch
import io.github.mallma.removereels.ui.openAccessibilitySettings

/** [TryItScreen] with its view model; [onDone] leaves the step, whether or not the block was tried. */
@Composable
internal fun TryItRoute(
    onDone: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val viewModel = viewModel { TryItViewModel(context.container.guardStatus, context.container.installedApps) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    TryItScreen(
        state = state,
        onOpenApp = context::launch,
        onTurnOn = context::openAccessibilitySettings,
        onDone = onDone,
        onBack = onBack,
    )
}

@Composable
internal fun TryItScreen(
    state: TryItState,
    onOpenApp: (TargetApp) -> Unit,
    onTurnOn: () -> Unit,
    onDone: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SimpleScreen(title = stringResource(R.string.try_it_title), onBack = onBack, modifier = modifier) {
        when {
            state.worked -> {
                Text(stringResource(R.string.try_it_worked), style = MaterialTheme.typography.titleMedium)
                Button(onClick = onDone) { Text(stringResource(R.string.try_it_done)) }
            }

            !state.blockingOn -> {
                Text(stringResource(R.string.try_it_turn_on_first), style = MaterialTheme.typography.bodyLarge)
                Button(onClick = onTurnOn) { Text(stringResource(R.string.home_turn_on)) }
                OutlinedButton(onClick = onDone) { Text(stringResource(R.string.try_it_skip)) }
            }

            state.app == null -> {
                Text(stringResource(R.string.try_it_no_app), style = MaterialTheme.typography.bodyLarge)
                Button(onClick = onDone) { Text(stringResource(R.string.try_it_done)) }
            }

            else -> {
                Text(stringResource(state.app.tryItInstruction), style = MaterialTheme.typography.bodyLarge)
                Button(onClick = { onOpenApp(state.app) }) { Text(stringResource(state.app.openLabel)) }
                OutlinedButton(onClick = onDone) { Text(stringResource(R.string.try_it_skip)) }
            }
        }
    }
}

private val TargetApp.tryItInstruction: Int
    get() =
        when (this) {
            TargetApp.INSTAGRAM -> R.string.try_it_instagram
            TargetApp.YOUTUBE -> R.string.try_it_youtube
        }

private val TargetApp.openLabel: Int
    get() =
        when (this) {
            TargetApp.INSTAGRAM -> R.string.try_it_open_instagram
            TargetApp.YOUTUBE -> R.string.try_it_open_youtube
        }
