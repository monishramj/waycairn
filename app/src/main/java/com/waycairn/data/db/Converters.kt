package com.waycairn.data.db

import androidx.room.TypeConverter
import com.waycairn.data.model.TriggerMode

class Converters {
    @TypeConverter
    fun triggerModeToString(mode: TriggerMode): String = mode.name

    @TypeConverter
    fun stringToTriggerMode(value: String): TriggerMode = TriggerMode.valueOf(value)
}
