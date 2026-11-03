package io.github.mallma.removereels

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import io.github.mallma.removereels.budget.TargetApp

/** The Android package of each app RemoveReels watches. */
internal val TargetApp.packageName: String
    get() =
        when (this) {
            TargetApp.INSTAGRAM -> "com.instagram.android"
            TargetApp.YOUTUBE -> "com.google.android.youtube"
        }

internal fun targetAppOf(packageName: CharSequence?): TargetApp? =
    TargetApp.entries.firstOrNull { it.packageName == packageName?.toString() }

/** The installed package of [app], or null if it is not installed; visible through the manifest's `<queries>`. */
internal fun PackageManager.packageInfoOf(app: TargetApp): PackageInfo? =
    try {
        getPackageInfo(app.packageName, PackageManager.PackageInfoFlags.of(0))
    } catch (ignored: PackageManager.NameNotFoundException) {
        null
    }
