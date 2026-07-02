package com.waycairn.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.InputMethodManager
import com.waycairn.WaycairnApp
import com.waycairn.data.model.AppSetting
import com.waycairn.data.model.Habit
import com.waycairn.data.model.TriggerMode
import com.waycairn.notification.UnlockReceiver
import com.waycairn.service.overlay.OverlayController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Phase 5: foreground-app detection wired to the real gate trigger.
 *
 * The accessibility callback runs on the main thread, so Room is never queried there — DB reads
 * happen on [Dispatchers.IO] via [serviceScope], and WindowManager work is switched back to the
 * main thread before touching [OverlayController].
 */
class WaycairnAccessibilityService : AccessibilityService() {

    /** Package name of the latest foreground window event (may not have settled yet). */
    private var currentForegroundPackage: String? = null

    /**
     * The last package that survived the [FOREGROUND_SETTLE_MS] window — i.e. a real, settled
     * foreground app rather than a transient flash. Session state is keyed off this.
     */
    private var lastSettledPackage: String? = null

    /** True once we've shown the overlay for the current foreground session (debounce). */
    private var shownForCurrentSession = false

    /** Pending "did this foreground actually settle?" check. Canceled by the next window event. */
    private var settleJob: Job? = null

    /** The running AFTER_DELAY timer, if any. Canceled on app switch. */
    private var sessionTimer: SessionTimer? = null

    /**
     * Enabled input-method (keyboard) packages. A keyboard renders its own window on top of the
     * host app, firing TYPE_WINDOW_STATE_CHANGED even though the user never left the app — so it
     * must never be treated as a foreground switch. Read from the system, so it's correct for any
     * keyboard (Samsung Honeyboard, Gboard, SwiftKey, …), not a hardcoded list.
     */
    private val imePackages: Set<String> by lazy { loadImePackages() }

    private var serviceScope = newScope()

    /** Dynamically-registered ACTION_USER_PRESENT receiver (manifest registration is unreliable). */
    private var unlockReceiver: UnlockReceiver? = null

    private val app: WaycairnApp get() = application as WaycairnApp

    override fun onCreate() {
        super.onCreate()
        registerUnlockReceiver()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        if (!serviceScope.isActive) serviceScope = newScope()
        registerUnlockReceiver()
    }

    private fun registerUnlockReceiver() {
        if (unlockReceiver != null) return
        val receiver = UnlockReceiver()
        // Use applicationContext so the registration survives accessibility rebinds; unregister only
        // in onDestroy (not onUnbind — that can fire without destroying the service).
        applicationContext.registerReceiver(
            receiver,
            IntentFilter(Intent.ACTION_USER_PRESENT),
            Context.RECEIVER_NOT_EXPORTED
        )
        unlockReceiver = receiver
        Log.d(TAG, "UnlockReceiver registered")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val pkg = event.packageName?.toString() ?: return

        // Ignore our own overlay/UI and the system UI shell — neither is a "foreground app" the
        // interceptor should react to.
        if (pkg == packageName || pkg == SYSTEM_UI_PACKAGE) return

        // A keyboard popping up/down is not an app switch; ignore it so it can't reset the session.
        if (pkg in imePackages) return

        // Only react when the foreground window actually changes.
        if (pkg == currentForegroundPackage) return

        currentForegroundPackage = pkg
        Log.d(TAG, "Foreground package: $pkg")

        // Don't act immediately: transient flashes (e.g. a brief launcher blip while switching
        // tabs) fire and bounce back within a few hundred ms. Wait for the foreground to settle;
        // if another window event arrives first, this job is canceled and never resets the session.
        settleJob?.cancel()
        settleJob = serviceScope.launch {
            delay(FOREGROUND_SETTLE_MS)
            onForegroundSettled(pkg)
        }
    }

    /** Runs [FOREGROUND_SETTLE_MS] after a window change if no newer change superseded it. */
    private fun onForegroundSettled(pkg: String) {
        if (pkg != currentForegroundPackage) return // a newer event already superseded this one
        if (pkg == lastSettledPackage) return        // same app re-settled after a transient blip

        // A genuinely new foreground app has settled — this is a real session boundary. Reset the
        // debounce and cancel any pending AFTER_DELAY so the next monitored app starts fresh.
        lastSettledPackage = pkg
        shownForCurrentSession = false
        sessionTimer?.cancel()
        sessionTimer = null

        handleForeground(pkg)
    }

    private fun handleForeground(pkg: String) {
        serviceScope.launch {
            val setting = withContext(Dispatchers.IO) { app.appSettingRepository.get(pkg) }
            if (setting == null || !setting.enabled) return@launch

            // Debounce: already handled this session.
            if (shownForCurrentSession) return@launch

            val incomplete = currentIncomplete()
            if (!shouldShow(incomplete, setting)) return@launch

            when (setting.triggerMode) {
                TriggerMode.ON_OPEN -> showOverlay(incomplete)
                TriggerMode.AFTER_DELAY -> startDelayTimer(setting, pkg)
            }
        }
    }

    private fun startDelayTimer(setting: AppSetting, pkg: String) {
        sessionTimer = SessionTimer(serviceScope, setting.delayMinutes) {
            // Only fire if the same monitored app is still the settled foreground. Using
            // lastSettledPackage means transient keyboard/launcher blips don't falsely cancel it.
            if (lastSettledPackage != pkg || shownForCurrentSession) return@SessionTimer
            val again = currentIncomplete()
            if (shouldShow(again, setting)) showOverlay(again)
        }.also { it.start() }
    }

    /** Non-empty incomplete list -> show. Empty -> show only if [AppSetting.showAfterComplete]. */
    private fun shouldShow(incomplete: List<Habit>, setting: AppSetting): Boolean =
        if (incomplete.isNotEmpty()) true else setting.showAfterComplete

    private suspend fun currentIncomplete(): List<Habit> =
        withContext(Dispatchers.IO) { app.habitRepository.incompleteToday().first() }

    private suspend fun showOverlay(habits: List<Habit>) {
        val message = withContext(Dispatchers.IO) { app.settingsStore.globalMessage.first() }
        shownForCurrentSession = true
        withContext(Dispatchers.Main) {
            OverlayController.show(applicationContext, message, habits)
        }
    }

    override fun onInterrupt() {
        // No-op: we do not hold any interruptible feedback.
    }

    override fun onDestroy() {
        settleJob?.cancel()
        settleJob = null
        sessionTimer?.cancel()
        sessionTimer = null
        unlockReceiver?.let {
            runCatching { applicationContext.unregisterReceiver(it) }
            Log.d(TAG, "UnlockReceiver unregistered")
        }
        unlockReceiver = null
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun newScope() = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /** Package names of every enabled keyboard/IME, plus the current default, for robustness. */
    private fun loadImePackages(): Set<String> {
        val result = mutableSetOf<String>()
        (getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)
            ?.enabledInputMethodList
            ?.forEach { it.packageName?.let(result::add) }
        Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
            ?.substringBefore('/')
            ?.takeIf { it.isNotBlank() }
            ?.let(result::add)
        Log.d(TAG, "IME packages ignored: $result")
        return result
    }

    companion object {
        private const val TAG = "WaycairnA11y"
        private const val SYSTEM_UI_PACKAGE = "com.android.systemui"

        /**
         * How long a foreground window must persist before we treat it as a real switch. Long
         * enough to swallow transient blips seen during in-app tab switches, short enough to be
         * imperceptible when genuinely opening a gated app.
         */
        private const val FOREGROUND_SETTLE_MS = 500L
    }
}
