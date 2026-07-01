package com.waycairn.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_settings")
data class AppSetting(
    @PrimaryKey val packageName: String,
    val triggerMode: TriggerMode = TriggerMode.ON_OPEN,
    val delayMinutes: Int = 0,            // used only when AFTER_DELAY
    val showAfterComplete: Boolean = false,
    val perAppMessage: String? = null,    // reserved for later; v1 ignores, uses global
    val enabled: Boolean = true
)

enum class TriggerMode { ON_OPEN, AFTER_DELAY }
