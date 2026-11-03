package io.github.mallma.removereels.guard.overlay

import android.accessibilityservice.AccessibilityService
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import io.github.mallma.removereels.detection.EntryPoint
import kotlin.math.roundToInt

/**
 * Opaque windows over Reels and Shorts entry points (ADR 0008). Each hides one entry point and swallows taps on it.
 * They are plain Views rather than Compose, because they move whenever the app's layout does.
 */
internal class EntryPointCovers(
    private val service: AccessibilityService,
) {
    private val windowManager = service.getSystemService(WindowManager::class.java)
    private val covers = mutableListOf<View>()
    private var shown = emptyList<EntryPoint>()

    /** Covers exactly [entryPoints], reusing the windows already shown. */
    fun show(entryPoints: List<EntryPoint>) {
        if (entryPoints == shown) return
        while (covers.size > entryPoints.size) windowManager.removeView(covers.removeAt(covers.lastIndex))
        val darkTheme = service.resources.configuration.isNightModeActive
        entryPoints.forEachIndexed { index, entryPoint ->
            val cover = covers.getOrNull(index)
            if (cover == null) {
                covers += newCover().also { windowManager.addView(it, layout(entryPoint)) }
            } else {
                windowManager.updateViewLayout(cover, layout(entryPoint))
            }
            covers[index].setBackgroundColor(entryPoint.color.argb(darkTheme))
        }
        shown = entryPoints
    }

    fun hide() = show(emptyList())

    private fun newCover() = View(service).apply { importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO }

    private fun layout(entryPoint: EntryPoint): WindowManager.LayoutParams {
        val inset = (entryPoint.insetTopDp * service.resources.displayMetrics.density).roundToInt()
        val area = entryPoint.bounds.copy(top = entryPoint.bounds.top + inset)
        return WindowManager
            .LayoutParams(
                area.right - area.left,
                area.bottom - area.top,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.OPAQUE,
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = area.left
                y = area.top
            }
    }
}
