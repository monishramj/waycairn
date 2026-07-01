package com.waycairn.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    // minutes-since-midnight, e.g. 18:00 = 1080. null = "any time today".
    val deadlineMinutes: Int? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val archived: Boolean = false
)
