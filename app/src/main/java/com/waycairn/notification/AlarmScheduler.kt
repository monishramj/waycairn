package com.waycairn.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.waycairn.WaycairnApp
import com.waycairn.data.model.Habit
import com.waycairn.util.DayRange

/**
 * Phase 6: exact alarms for timed habits, plus the daily rebuild anchor.
 *
 * [AlarmManager.setExactAndAllowWhileIdle] fires ONCE, so every alarm must be re-scheduled. We do
 * that by (a) rebuilding on habit create/edit/delete, (b) a daily anchor alarm at 00:01 that
 * rebuilds the day and re-arms itself, and (c) a boot rebuild. Without these, reminders silently
 * stop after the first day.
 */
object AlarmScheduler {

    private const val TAG = "WaycairnAlarms"

    const val EXTRA_TYPE = "type"
    const val EXTRA_HABIT_ID = "habitId"

    const val TYPE_REMINDER = "reminder"
    const val TYPE_MISSED = "missed"
    const val TYPE_REBUILD = "rebuild"

    private const val REMINDER_LEAD_MINUTES = 30

    // Request-code scheme mirrors Notifs' ID scheme so each (habit, type) has a stable slot.
    private const val REMINDER_RC_BASE = 100_000
    private const val MISSED_RC_BASE = 200_000
    private const val ANCHOR_RC = 900_001

    /** Rebuild today's alarms for all timed habits and (re)arm the daily anchor. */
    suspend fun rescheduleAll(context: Context) {
        val app = context.applicationContext as WaycairnApp
        val habits = app.habitRepository.activeHabitsSnapshot()
        rescheduleAll(context, habits)
    }

    fun rescheduleAll(context: Context, habits: List<Habit>) {
        val appContext = context.applicationContext
        val now = System.currentTimeMillis()
        val today = DayRange.today()

        habits.forEach { habit ->
            val deadline = habit.deadlineMinutes ?: run {
                cancelHabit(appContext, habit.id)
                return@forEach
            }
            val deadlineAt = today.startMillis + deadline * 60_000L
            val reminderAt = deadlineAt - REMINDER_LEAD_MINUTES * 60_000L

            // Always cancel first so an edited deadline doesn't leave a stale alarm.
            cancelHabit(appContext, habit.id)

            if (reminderAt > now) {
                setExact(appContext, reminderAt, alarmIntent(appContext, TYPE_REMINDER, habit.id, reminderRequestCode(habit.id)))
            }
            if (deadlineAt > now) {
                setExact(appContext, deadlineAt, alarmIntent(appContext, TYPE_MISSED, habit.id, missedRequestCode(habit.id)))
            }
        }

        armDailyAnchor(appContext)
    }

    /** Cancel both alarms for a single habit (used on delete and before re-scheduling). */
    fun cancelHabit(context: Context, habitId: Long) {
        val appContext = context.applicationContext
        val am = appContext.getSystemService(AlarmManager::class.java)
        am.cancel(alarmIntent(appContext, TYPE_REMINDER, habitId, reminderRequestCode(habitId)))
        am.cancel(alarmIntent(appContext, TYPE_MISSED, habitId, missedRequestCode(habitId)))
    }

    /** Arm the next 00:01 anchor. On fire the receiver rebuilds the day and re-arms this. */
    fun armDailyAnchor(context: Context) {
        val appContext = context.applicationContext
        // endMillis is midnight tomorrow; +1 minute = 00:01.
        val anchorAt = DayRange.today().endMillis + 60_000L
        setExact(
            appContext,
            anchorAt,
            alarmIntent(appContext, TYPE_REBUILD, habitId = -1L, requestCode = ANCHOR_RC)
        )
    }

    private fun setExact(context: Context, triggerAt: Long, pendingIntent: PendingIntent) {
        val am = context.getSystemService(AlarmManager::class.java)
        try {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        } catch (e: SecurityException) {
            // "Alarms & reminders" not allowed — fall back to an inexact idle alarm so we still fire.
            Log.w(TAG, "Exact alarm denied; falling back to inexact", e)
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        }
    }

    private fun alarmIntent(
        context: Context,
        type: String,
        habitId: Long,
        requestCode: Int
    ): PendingIntent {
        val intent = Intent(context, HabitAlarmReceiver::class.java).apply {
            action = "com.waycairn.ALARM.$type"
            putExtra(EXTRA_TYPE, type)
            putExtra(EXTRA_HABIT_ID, habitId)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun reminderRequestCode(habitId: Long): Int = (REMINDER_RC_BASE + habitId).toInt()
    private fun missedRequestCode(habitId: Long): Int = (MISSED_RC_BASE + habitId).toInt()
}
