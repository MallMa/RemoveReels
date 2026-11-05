package io.github.mallma.removereels.guard

import android.accessibilityservice.AccessibilityService
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.view.Gravity
import android.view.View
import android.view.WindowManager

/** How the [CoverProbe] draws its covers. */
internal interface ProbeCovers {
    fun show(rects: List<Rect>)

    fun hide()
}

/** One window per cover, moved by the window manager as H1's covers are; [touchable] ones swallow touches. */
internal class WindowCovers(
    private val service: AccessibilityService,
    private val touchable: Boolean,
    private val color: Int,
) : ProbeCovers {
    private val windowManager = service.getSystemService(WindowManager::class.java)
    private val windows = mutableListOf<View>()

    override fun show(rects: List<Rect>) {
        val shown = rects.filterNot { it.isEmpty }
        while (windows.size > shown.size) windowManager.removeView(windows.removeAt(windows.lastIndex))
        shown.forEachIndexed { index, rect ->
            val window = windows.getOrNull(index)
            if (window == null) {
                windows += newWindow().also { windowManager.addView(it, params(rect)) }
            } else {
                windowManager.updateViewLayout(window, params(rect))
            }
        }
    }

    override fun hide() = show(emptyList())

    private fun newWindow() =
        View(service).apply {
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            setBackgroundColor(color)
        }

    private fun params(rect: Rect) =
        WindowManager
            .LayoutParams(
                rect.width(),
                rect.height(),
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    (if (touchable) 0 else WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE),
                PixelFormat.OPAQUE,
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = rect.left
                y = rect.top
            }
}

/** One window over the whole screen that lets every touch through and draws all covers itself. */
internal class CanvasCovers(
    service: AccessibilityService,
    color: Int,
) : ProbeCovers {
    private val windowManager = service.getSystemService(WindowManager::class.java)
    private var rects = emptyList<Rect>()
    private val paint = Paint().apply { this.color = color }
    private val origin = IntArray(2)
    private val view =
        object : View(service) {
            override fun onDraw(canvas: Canvas) {
                getLocationOnScreen(origin)
                canvas.translate(-origin[0].toFloat(), -origin[1].toFloat())
                rects.forEach { canvas.drawRect(it, paint) }
            }
        }.apply { importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO }
    private var attached = false

    override fun show(rects: List<Rect>) {
        this.rects = rects
        if (!attached) {
            windowManager.addView(view, params())
            attached = true
        }
        view.invalidate()
    }

    override fun hide() {
        if (attached) windowManager.removeView(view)
        attached = false
    }

    private fun params() =
        WindowManager
            .LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT,
            ).apply { gravity = Gravity.TOP or Gravity.START }
}
