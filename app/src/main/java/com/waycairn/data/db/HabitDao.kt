package com.waycairn.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.waycairn.data.model.Habit
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(habit: Habit): Long

    @Update
    suspend fun update(habit: Habit)

    @Delete
    suspend fun delete(habit: Habit)

    @Query("DELETE FROM habits WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM habits WHERE id = :id")
    suspend fun getById(id: Long): Habit?

    @Query("SELECT * FROM habits WHERE id = :id")
    fun observeById(id: Long): Flow<Habit?>

    // Active habits ordered timed-first (by deadline) then untimed, stable by creation time.
    @Query(
        "SELECT * FROM habits WHERE archived = 0 " +
            "ORDER BY (deadlineMinutes IS NULL), deadlineMinutes ASC, createdAt ASC"
    )
    fun observeActive(): Flow<List<Habit>>

    @Query("SELECT * FROM habits WHERE archived = 0")
    suspend fun getActive(): List<Habit>

    @Query("SELECT COUNT(*) FROM habits")
    suspend fun count(): Int
}
