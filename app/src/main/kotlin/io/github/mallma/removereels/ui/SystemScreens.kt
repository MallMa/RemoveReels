package io.github.mallma.removereels.ui

import android.content.Context
import android.content.Intent
import android.provider.Settings

/** Opens Android's accessibility settings, where RemoveReels is listed under the installed apps. */
internal fun Context.openAccessibilitySettings() {
    startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
}

/** Opens Android's date and time settings, where automatic time is switched on. */
internal fun Context.openDateSettings() {
    startActivity(Intent(Settings.ACTION_DATE_SETTINGS))
}
