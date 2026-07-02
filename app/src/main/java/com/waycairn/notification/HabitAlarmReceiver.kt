package com.waycairn.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.waycairn.WaycairnApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Phase 6: fires the reminder / missed notifications, and rebuilds the day's alarms when the daily
 * anchor goes off. DB work runs off the main thread via [goAsync].
 */
class HabitAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val type = intent.getStringExtra(AlarmScheduler.EXTRA_TYPE) ?: return
        val habitId = intent.getLongExtra(AlarmScheduler.EXTRA_HABIT_ID, -1L)
        val app = context.applicationContext as WaycairnApp
        val appContext = context.applicationContext

        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                when (type) {
                    AlarmScheduler.TYPE_REMINDER -> {
                        if (habitId > 0 && app.habitRepository.isIncompleteToday(habitId)) {
                            app.habitRepository.getHabit(habitId)?.let { Notifs.postReminder(appContext, it) }
                        }
                    }
                    AlarmScheduler.TYPE_MISSED -> {
                        if (habitId > 0 && app.habitRepository.isIncompleteToday(habitId)) {
                            app.habitRepository.getHabit(habitId)?.let { Notifs.postMissed(appContext, it) }
                        }
                    }
                    AlarmScheduler.TYPE_REBUILD -> {
                        AlarmScheduler.rescheduleAll(appContext)
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }
}
