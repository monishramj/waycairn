package com.waycairn.service

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.waycairn.service.overlay.OverlayController

/**
 * Phase 3: foreground app detection only.
 *
 * Listens for window-state changes and tracks the current foreground package. Later phases use
 * transitions in [currentForegroundPackage] to detect when a gated app is entered (leave/return).
 */
class WaycairnAccessibilityService : AccessibilityService() {

    /** Package name of the app currently in the foreground, or null if unknown. */
    private var currentForegroundPackage: String? = null

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val pkg = event.packageName?.toString() ?: return

        // Ignore our own overlay/UI and the system UI shell — neither is a "foreground app" the
        // interceptor should react to.
        if (pkg == packageName || pkg == SYSTEM_UI_PACKAGE) return

        if (pkg != currentForegroundPackage) {
            currentForegroundPackage = pkg
            Log.d(TAG, "Foreground package: $pkg")

            // TEMP dev trigger (Phase 4): fire the overlay when Chrome comes to the foreground so
            // the friction screen can be tested live. Real trigger logic lands in Phase 5.
            if (pkg == DEV_TRIGGER_PACKAGE) {
                OverlayController.show(this)
            }
        }
    }

    override fun onInterrupt() {
        // No-op: we do not hold any interruptible feedback.
    }

    companion object {
        private const val TAG = "WaycairnA11y"
        private const val SYSTEM_UI_PACKAGE = "com.android.systemui"
        private const val DEV_TRIGGER_PACKAGE = "com.android.chrome"
    }
}
