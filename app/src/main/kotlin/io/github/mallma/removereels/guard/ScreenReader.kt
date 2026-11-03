package io.github.mallma.removereels.guard

import android.accessibilityservice.AccessibilityService
import android.os.PowerManager
import io.github.mallma.removereels.budget.TargetApp
import io.github.mallma.removereels.detection.SurfaceDetector
import io.github.mallma.removereels.packageName
import io.github.mallma.removereels.targetAppOf

/** Reads what Instagram or YouTube shows, through the accessibility service, as the guard's [Screen]. */
internal class ScreenReader(
    private val service: AccessibilityService,
    private val detector: SurfaceDetector,
) {
    private val power = service.getSystemService(PowerManager::class.java)

    /** Null while the screen is off or neither app is in front. */
    fun read(): Screen? =
        appInFront()?.let { (app, tree) ->
            Screen(
                app = app,
                surface = detector.findSurface(app.packageName, tree),
                canary = detector.checkCanary(app.packageName, tree),
                entryPoints = detector.findEntryPoints(app.packageName, tree),
            )
        }

    /** Whether a blocked surface is in front: the quicker question the back-out asks after each step. */
    fun isOnSurface(): Boolean =
        appInFront()?.let { (app, tree) -> detector.findSurface(app.packageName, tree) != null } ?: false

    private fun appInFront(): Pair<TargetApp, AccessibilityScreenTree>? {
        val root = service.rootInActiveWindow?.takeIf { power.isInteractive }
        val app = targetAppOf(root?.packageName)
        return if (root == null || app == null) null else app to AccessibilityScreenTree(root)
    }
}
