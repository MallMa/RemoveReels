package io.github.mallma.removereels.ui.keeprunning

import android.os.Build
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import io.github.mallma.removereels.R
import io.github.mallma.removereels.ui.SimpleScreen
import io.github.mallma.removereels.ui.openAppDetails

@Composable
internal fun KeepRunningRoute(onBack: () -> Unit) {
    val context = LocalContext.current
    KeepRunningScreen(PhoneMaker.of(Build.MANUFACTURER), onOpenAppSettings = context::openAppDetails, onBack = onBack)
}

/** How to keep the phone's battery saver from stopping RemoveReels. */
@Composable
internal fun KeepRunningScreen(
    maker: PhoneMaker,
    onOpenAppSettings: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SimpleScreen(title = stringResource(R.string.settings_keep_running), onBack = onBack, modifier = modifier) {
        Text(stringResource(R.string.keep_running_intro), style = MaterialTheme.typography.bodyLarge)
        stringArrayResource(maker.steps).forEachIndexed { index, step ->
            Text(
                stringResource(R.string.keep_running_step, index + 1, step),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        Button(onClick = onOpenAppSettings) { Text(stringResource(R.string.keep_running_open_app_settings)) }
    }
}
