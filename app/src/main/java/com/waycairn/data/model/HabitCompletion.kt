package com.waycairn.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// Completion is a log, not a flag: a habit is "done today" iff a row exists whose
// completedAt falls within today's local-timezone range. See BUILD_PLAN.md §4.
@Entity(
    tableName = "completions",
    foreignKeys = [ForeignKey(
        entity = Habit::class,
        parentColumns = ["id"],
        childColumns = ["habitId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("habitId"), Index("completedAt")]
)
data class HabitCompletion(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val habitId: Long,
    val completedAt: Long = System.currentTimeMillis()
)
