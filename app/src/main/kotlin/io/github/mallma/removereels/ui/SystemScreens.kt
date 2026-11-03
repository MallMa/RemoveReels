package io.github.mallma.removereels.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.net.toUri
import io.github.mallma.removereels.budget.TargetApp
import io.github.mallma.removereels.packageName

/** Opens Android's accessibility settings, where RemoveReels is listed under the installed apps. */
internal fun Context.openAccessibilitySettings() {
    startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
}

/** Opens Android's date and time settings, where automatic date, time and time zone are switched on. */
internal fun Context.openDateSettings() {
    startActivity(Intent(Settings.ACTION_DATE_SETTINGS))
}

/** Opens RemoveReels' own page in Android's app settings: battery use, restricted settings. */
internal fun Context.openAppDetails() {
    startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)))
}

/** Opens [app], if it is installed. */
internal fun Context.launch(app: TargetApp) {
    packageManager.getLaunchIntentForPackage(app.packageName)?.let(::startActivity)
}

/** Opens [url] in the browser; RemoveReels itself has no internet access. */
internal fun Context.openWebPage(url: String) {
    startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
}
