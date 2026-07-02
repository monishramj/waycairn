package com.waycairn

import android.app.Application
import com.waycairn.data.db.WaycairnDatabase
import com.waycairn.data.model.Habit
import com.waycairn.data.prefs.SettingsStore
import com.waycairn.data.repo.AppSettingRepository
import com.waycairn.data.repo.HabitRepository
import com.waycairn.notification.AlarmScheduler
import com.waycairn.notification.Notifs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class WaycairnApp : Application() {

    // App-wide singletons. Lazily built so the DB isn't touched on the main thread at startup.
    val database: WaycairnDatabase by lazy { WaycairnDatabase.getInstance(this) }

    val habitRepository: HabitRepository by lazy {
        HabitRepository(database.habitDao(), database.completionDao())
    }

    val appSettingRepository: AppSettingRepository by lazy {
        AppSettingRepository(database.appSettingDao())
    }

    val settingsStore: SettingsStore by lazy { SettingsStore(this) }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        Notifs.createChannels(this)
        appScope.launch {
            if (BuildConfig.DEBUG) seedIfFirstRun()
            // Rebuild today's alarms on every cold start (also re-arms the daily 00:01 anchor).
            AlarmScheduler.rescheduleAll(this@WaycairnApp)
        }
    }

    /** Insert 3 sample habits (one timed 18:00, two untimed) the first time the DB is empty. */
    private suspend fun seedIfFirstRun() {
        val habitDao = database.habitDao()
        if (habitDao.count() > 0) return
        habitDao.insert(
            Habit(
                title = "Evening reflection",
                description = "Two lines on how the day went.",
                deadlineMinutes = 18 * 60 // 18:00
            )
        )
        habitDao.insert(Habit(title = "Drink water"))
        habitDao.insert(Habit(title = "Step outside"))
    }
}
