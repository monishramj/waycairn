package com.waycairn.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.waycairn.data.model.HabitCompletion
import kotlinx.coroutines.flow.Flow

@Dao
interface CompletionDao {

    @Insert
    suspend fun insert(completion: HabitCompletion): Long

    // Delete today's completion row(s) for a habit (range is [start, end)).
    @Query(
        "DELETE FROM completions " +
            "WHERE habitId = :habitId AND completedAt >= :startMillis AND completedAt < :endMillis"
    )
    suspend fun deleteForHabitInRange(habitId: Long, startMillis: Long, endMillis: Long)

    @Query(
        "SELECT COUNT(*) FROM completions " +
            "WHERE habitId = :habitId AND completedAt >= :startMillis AND completedAt < :endMillis"
    )
    suspend fun countForHabitInRange(habitId: Long, startMillis: Long, endMillis: Long): Int

    @Query("SELECT * FROM completions WHERE completedAt >= :startMillis AND completedAt < :endMillis")
    suspend fun getInRange(startMillis: Long, endMillis: Long): List<HabitCompletion>

    @Query("SELECT completedAt FROM completions WHERE habitId = :habitId ORDER BY completedAt DESC")
    suspend fun completionTimesForHabit(habitId: Long): List<Long>

    // UI-facing stream of every completion; repositories derive per-day / per-habit views from it.
    @Query("SELECT * FROM completions")
    fun observeAll(): Flow<List<HabitCompletion>>
}
