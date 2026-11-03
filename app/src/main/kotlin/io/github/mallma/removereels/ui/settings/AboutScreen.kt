package io.github.mallma.removereels.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.mallma.removereels.R
import io.github.mallma.removereels.budget.TargetApp
import io.github.mallma.removereels.container
import io.github.mallma.removereels.packageName
import io.github.mallma.removereels.ui.AppIcon
import io.github.mallma.removereels.ui.SimpleScreen
import io.github.mallma.removereels.ui.label
import io.github.mallma.removereels.ui.openWebPage

private const val SOURCE_URL = "https://github.com/MallMa/RemoveReels"
private const val LICENSE_URL = "$SOURCE_URL/blob/main/LICENSE"

@Composable
internal fun AboutRoute(onBack: () -> Unit) {
    val context = LocalContext.current
    val container = context.container
    val versions =
        TargetApp.entries.mapNotNull { app ->
            container.detector.rulesFor(app.packageName)?.let {
                TestedVersion(app, tested = it.testedVersion, installed = container.installedApps.versionOf(app))
            }
        }
    AboutScreen(
        appVersion = container.appVersion,
        testedVersions = versions,
        onOpenSource = { context.openWebPage(SOURCE_URL) },
        onOpenLicense = { context.openWebPage(LICENSE_URL) },
        onBack = onBack,
    )
}

@Composable
internal fun AboutScreen(
    appVersion: String,
    testedVersions: List<TestedVersion>,
    onOpenSource: () -> Unit,
    onOpenLicense: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SimpleScreen(title = stringResource(R.string.settings_about), onBack = onBack, modifier = modifier) {
        Text(stringResource(R.string.about_version, appVersion), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.about_privacy), style = MaterialTheme.typography.bodyMedium)
        Column {
            TextButton(onClick = onOpenSource) { Text(stringResource(R.string.about_source)) }
            TextButton(onClick = onOpenLicense) { Text(stringResource(R.string.about_license)) }
        }
        Text(stringResource(R.string.about_libraries), style = MaterialTheme.typography.bodyMedium)
        Text(stringResource(R.string.about_tested_versions), style = MaterialTheme.typography.titleMedium)
        testedVersions.forEach { TestedVersionRow(it) }
    }
}

@Composable
private fun TestedVersionRow(version: TestedVersion) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        AppIcon(version.app, size = 32.dp)
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(stringResource(version.app.label), style = MaterialTheme.typography.bodyLarge)
            Text(
                stringResource(R.string.about_tested_with, version.tested),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text =
                    when (version.installed) {
                        null -> stringResource(R.string.home_not_installed)
                        version.tested -> stringResource(R.string.about_installed_same, version.installed)
                        else -> stringResource(R.string.about_installed_other, version.installed)
                    },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
