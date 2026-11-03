package io.github.mallma.removereels.guard.overlay

import android.accessibilityservice.AccessibilityService
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import io.github.mallma.removereels.ui.theme.RemoveReelsTheme

/**
 * A Compose window over other apps. As an accessibility overlay it needs no "draw over other apps" permission, and it
 * never takes focus, so the app underneath stays the active window and keeps receiving the back key.
 */
internal class OverlayWindow(
    private val service: AccessibilityService,
    private val layout: WindowManager.LayoutParams,
) : LifecycleOwner,
    SavedStateRegistryOwner {
    private val windowManager = service.getSystemService(WindowManager::class.java)
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateController = SavedStateRegistryController.create(this)
    private var view: ComposeView? = null

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry

    init {
        savedStateController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
    }

    /** Shows the window with [content], or replaces the content if it is already showing. */
    fun show(content: @Composable () -> Unit) {
        val view =
            view ?: ComposeView(service).also {
                it.setViewTreeLifecycleOwner(this)
                it.setViewTreeSavedStateRegistryOwner(this)
                windowManager.addView(it, layout)
                view = it
            }
        view.setContent { RemoveReelsTheme(content = content) }
    }

    fun hide() {
        view?.let(windowManager::removeView)
        view = null
    }

    /** Hides the window for good; it cannot be shown again. */
    fun destroy() {
        hide()
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
    }

    companion object {
        /** Covers the whole screen and catches every touch. */
        fun fullScreen() =
            WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT,
            )

        /** As small as its content, at the top centre; touches beside it reach the app. */
        fun topCentre() =
            WindowManager
                .LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                    PixelFormat.TRANSLUCENT,
                ).apply { gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL }
    }
}
