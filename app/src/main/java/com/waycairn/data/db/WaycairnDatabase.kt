package com.waycairn.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.waycairn.data.model.AppSetting
import com.waycairn.data.model.Habit
import com.waycairn.data.model.HabitCompletion

@Database(
    entities = [Habit::class, HabitCompletion::class, AppSetting::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class WaycairnDatabase : RoomDatabase() {

    abstract fun habitDao(): HabitDao
    abstract fun completionDao(): CompletionDao
    abstract fun appSettingDao(): AppSettingDao

    companion object {
        private const val DB_NAME = "waycairn.db"

        @Volatile
        private var INSTANCE: WaycairnDatabase? = null

        fun getInstance(context: Context): WaycairnDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    WaycairnDatabase::class.java,
                    DB_NAME
                ).build().also { INSTANCE = it }
            }
    }
}
