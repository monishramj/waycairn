package com.waycairn.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.waycairn.MainActivity
import com.waycairn.R
import com.waycairn.data.model.Habit
import com.waycairn.ui.components.deadlineLabel

/**
 * Phase 6: notification channels, stable notification IDs, and the builders that post them.
 *
 * Every tap opens [MainActivity] deep-linked to the relevant habit (via [MainActivity.EXTRA_HABIT_ID])
 * — tapping never silently completes a habit.
 */
object Notifs {

    const val CHANNEL_REMINDERS = "reminders"
    const val CHANNEL_MISSED = "missed"
    const val CHANNEL_UNLOCK = "unlock_nudges"

    // Stable ID scheme. Per-habit reminder/missed IDs are derived from the habit id so a re-post
    // replaces (rather than stacks) the same habit's notification. Unlock has one fixed slot.
    private const val REMINDER_ID_BASE = 100_000
    private const val MISSED_ID_BASE = 200_000
    const val UNLOCK_NOTIF_ID = 300_001

    fun reminderNotifId(habitId: Long): Int = (REMINDER_ID_BASE + habitId).toInt()
    fun missedNotifId(habitId: Long): Int = (MISSED_ID_BASE + habitId).toInt()

    fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_REMINDERS,
                "Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Nudges 30 minutes before a timed habit's deadline." }
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_MISSED,
                "Missed deadlines",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "When a timed habit's deadline passes uncompleted." }
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_UNLOCK,
                "Unlock nudges",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Occasional reminder of any-time habits still left today." }
        )
    }

    fun postReminder(context: Context, habit: Habit) {
        val notification = baseBuilder(context, CHANNEL_REMINDERS, habit.id)
            .setContentTitle("Coming up: ${habit.title}")
            .setContentText("Due at ${deadlineLabel(habit.deadlineMinutes)} — still unfinished.")
            .build()
        notify(context, reminderNotifId(habit.id), notification)
    }

    fun postMissed(context: Context, habit: Habit) {
        val notification = baseBuilder(context, CHANNEL_MISSED, habit.id)
            .setContentTitle("You missed: ${habit.title}")
            .setContentText("The ${deadlineLabel(habit.deadlineMinutes)} deadline passed today.")
            .build()
        notify(context, missedNotifId(habit.id), notification)
    }

    /**
     * Post/replace the every-3rd-unlock nudge listing incomplete untimed habits. Uses a fixed ID and
     * [NotificationCompat.Builder.setOnlyAlertOnce] so replacing the list doesn't re-buzz.
     */
    fun postUnlock(context: Context, untimedHabits: List<Habit>) {
        if (untimedHabits.isEmpty()) return
        val target = untimedHabits.first().id
        val text = untimedHabits.joinToString(", ") { it.title }
        val notification = baseBuilder(context, CHANNEL_UNLOCK, target)
            .setContentTitle("A few stones still to stack")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setOnlyAlertOnce(true)
            .build()
        notify(context, UNLOCK_NOTIF_ID, notification)
    }

    private fun baseBuilder(
        context: Context,
        channelId: String,
        habitId: Long
    ): NotificationCompat.Builder =
        NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setAutoCancel(true)
            .setContentIntent(habitTapIntent(context, habitId))

    private fun habitTapIntent(context: Context, habitId: Long): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(MainActivity.EXTRA_HABIT_ID, habitId)
        }
        return PendingIntent.getActivity(
            context,
            habitId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun notify(context: Context, id: Int, notification: android.app.Notification) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return
        try {
            manager.notify(id, notification)
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS not granted — silently skip.
        }
    }
}
