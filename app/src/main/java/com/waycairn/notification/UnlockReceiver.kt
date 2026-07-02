package com.waycairn.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.waycairn.WaycairnApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Phase 6: every-3rd-unlock nudge for any-time (untimed) habits.
 *
 * Registered dynamically from the running AccessibilityService — [Intent.ACTION_USER_PRESENT] is
 * not reliably delivered to a manifest-declared receiver on modern Android.
 */
class UnlockReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_USER_PRESENT) return

        val app = context.applicationContext as WaycairnApp
        val appContext = context.applicationContext

        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val count = app.settingsStore.incrementUnlockCount()
                Log.d(TAG, "Unlock count=$count")
                if (count % 3 != 0) return@launch

                val untimedIncomplete = app.habitRepository.incompleteTodaySnapshot()
                    .filter { it.deadlineMinutes == null }
                if (untimedIncomplete.isEmpty()) {
                    Log.d(TAG, "Every-3rd unlock reached but no incomplete untimed habits — skipping")
                    return@launch
                }
                Log.d(TAG, "Posting unlock nudge for ${untimedIncomplete.size} untimed habit(s)")
                Notifs.postUnlock(appContext, untimedIncomplete)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private const val TAG = "WaycairnUnlock"
    }
}
