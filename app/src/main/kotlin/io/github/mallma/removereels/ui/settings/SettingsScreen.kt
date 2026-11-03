package io.github.mallma.removereels.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import io.github.mallma.removereels.R
import io.github.mallma.removereels.ui.SimpleScreen

/** Rarely needed options. There is deliberately none that pauses or disables blocking. */
@Composable
internal fun SettingsScreen(
    showTimeLeft: Boolean,
    onShowTimeLeftChange: (Boolean) -> Unit,
    onOpenAllowance: () -> Unit,
    onTestBlocking: () -> Unit,
    onKeepRunning: () -> Unit,
    onAbout: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SimpleScreen(title = stringResource(R.string.settings_title), onBack = onBack, modifier = modifier) {
        Column {
            LinkRow(R.string.home_daily_allowance, onOpenAllowance)
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_show_time_left)) },
                supportingContent = { Text(stringResource(R.string.settings_show_time_left_detail)) },
                trailingContent = { Switch(checked = showTimeLeft, onCheckedChange = null) },
                modifier =
                    Modifier.toggleable(
                        value = showTimeLeft,
                        role = Role.Switch,
                        onValueChange = onShowTimeLeftChange,
                    ),
            )
            LinkRow(R.string.settings_test_blocking, onTestBlocking)
            LinkRow(R.string.settings_keep_running, onKeepRunning)
            LinkRow(R.string.settings_about, onAbout)
        }
    }
}

@Composable
private fun LinkRow(
    text: Int,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(stringResource(text)) },
        trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
        modifier = Modifier.clickable(onClick = onClick),
    )
}
