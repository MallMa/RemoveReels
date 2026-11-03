package io.github.mallma.removereels.ui.onboarding

import android.Manifest
import android.content.Context
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.mallma.removereels.container
import io.github.mallma.removereels.ui.openAppDetails
import io.github.mallma.removereels.ui.turnOnBlocking

/** [NotificationsStep] with Android's permission request; continues whatever the answer. */
@Composable
internal fun NotificationsRoute(onContinue: () -> Unit) {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED,
        )
    }
    val request =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
            granted = it
            onContinue()
        }
    NotificationsStep(
        granted = granted,
        onAllow = { request.launch(Manifest.permission.POST_NOTIFICATIONS) },
        onContinue = onContinue,
    )
}

/** [TurnOnStep], which moves on by itself once the service is connected. */
@Composable
internal fun TurnOnRoute(onContinue: () -> Unit) {
    val context = LocalContext.current
    val blockingOn by context.container.guardStatus.running
        .collectAsStateWithLifecycle()
    val installedFromFile = remember { context.installedFromFile() }
    TurnOnStep(
        blockingOn = blockingOn,
        installedFromFile = installedFromFile,
        onOpenSettings = context::turnOnBlocking,
        onOpenAppInfo = context::openAppDetails,
        onContinue = onContinue,
    )
}

/** Installed from a downloaded or local APK, for which Android restricts the accessibility switch. */
private fun Context.installedFromFile(): Boolean =
    packageManager.getInstallSourceInfo(packageName).packageSource in
        setOf(PackageInstaller.PACKAGE_SOURCE_DOWNLOADED_FILE, PackageInstaller.PACKAGE_SOURCE_LOCAL_FILE)
