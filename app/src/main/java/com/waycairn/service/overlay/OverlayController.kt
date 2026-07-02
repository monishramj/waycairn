package com.waycairn.service.overlay

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.waycairn.MainActivity
import com.waycairn.data.model.Habit

/**
 * Phase 5: builds a ComposeView hosting [OverlayContent] with real data, attaches an
 * [OverlayLifecycleOwner], and adds it to the WindowManager as a full-screen application overlay.
 * All add/remove happens on the main thread and double-adds are guarded against.
 */
object OverlayController {

    private const val TAG = "WaycairnOverlay"

    private val mainHandler = Handler(Looper.getMainLooper())

    private var composeView: ComposeView? = null
    private var lifecycleOwner: OverlayLifecycleOwner? = null

    val isShowing: Boolean get() = composeView != null

    fun show(context: Context, message: String, habits: List<Habit>) {
        val appContext = context.applicationContext
        runOnMain {
            if (composeView != null) {
                Log.d(TAG, "show() ignored — overlay already added")
                return@runOnMain
            }

            val windowManager =
                appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager

            val owner = OverlayLifecycleOwner().apply { onCreate() }

            val view = ComposeView(appContext).apply {
                setViewTreeLifecycleOwner(owner)
                setViewTreeViewModelStoreOwner(owner)
                setViewTreeSavedStateRegistryOwner(owner)
                setContent {
                    OverlayContent(
                        message = message,
                        habits = habits,
                        onHabitClick = { habitId ->
                            launchHabitDetail(appContext, habitId)
                            remove()
                        },
                        onContinue = { remove() }
                    )
                }
            }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
            )

            composeView = view
            lifecycleOwner = owner
            windowManager.addView(view, params)
            Log.d(TAG, "Overlay added")
        }
    }

    fun remove(context: Context) {
        runOnMain { detach(context.applicationContext) }
    }

    /** Convenience for callers inside the overlay that don't hold a Context. */
    fun remove() {
        val view = composeView ?: return
        runOnMain { detach(view.context.applicationContext) }
    }

    private fun detach(context: Context) {
        val view = composeView ?: return
        val windowManager =
            context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        try {
            windowManager.removeView(view)
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "removeView failed — view not attached", e)
        }
        lifecycleOwner?.onDestroy()
        composeView = null
        lifecycleOwner = null
        Log.d(TAG, "Overlay removed")
    }

    /** Opens MainActivity deep-linked to a habit's detail. Launched from a non-activity context. */
    private fun launchHabitDetail(context: Context, habitId: Long) {
        val intent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(MainActivity.EXTRA_HABIT_ID, habitId)
        }
        context.startActivity(intent)
    }

    private fun runOnMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            block()
        } else {
            mainHandler.post(block)
        }
    }
}
