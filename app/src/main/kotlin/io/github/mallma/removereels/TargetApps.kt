package io.github.mallma.removereels

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
