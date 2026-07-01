package com.waycairn.data.repo

import com.waycairn.data.db.CompletionDao
import com.waycairn.data.db.HabitDao
import com.waycairn.data.model.Habit
import com.waycairn.data.model.HabitCompletion
import com.waycairn.util.DayRange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.LocalDate

/** A habit paired with whether it has a completion within today's local-timezone range. */
data class HabitToday(
    val habit: Habit,
    val doneToday: Boolean
)

/**
 * Single source of truth for habits + their derived "done today", streak, and calendar views.
 * "Today" is recomputed from [DayRange] on every emission, never cached, so day rollover and
 * timezone changes are handled for free. All suspend work runs on [Dispatchers.IO].
 */
class HabitRepository(
    private val habitDao: HabitDao,
    private val completionDao: CompletionDao
) {

    // ---- CRUD ---------------------------------------------------------------

    suspend fun addHabit(habit: Habit): Long = withContext(Dispatchers.IO) {
        habitDao.insert(habit)
    }

    suspend fun updateHabit(habit: Habit) = withContext(Dispatchers.IO) {
        habitDao.update(habit)
    }

    suspend fun deleteHabit(habit: Habit) = withContext(Dispatchers.IO) {
        habitDao.delete(habit)
    }

    suspend fun deleteHabit(habitId: Long) = withContext(Dispatchers.IO) {
        habitDao.deleteById(habitId)
    }

    suspend fun getHabit(habitId: Long): Habit? = withContext(Dispatchers.IO) {
        habitDao.getById(habitId)
    }

    fun observeHabit(habitId: Long): Flow<Habit?> = habitDao.observeById(habitId)

    // ---- Derived reads ------------------------------------------------------

    /** All non-archived habits, each with a derived [HabitToday.doneToday]. */
    fun habitsForToday(): Flow<List<HabitToday>> =
        combine(habitDao.observeActive(), completionDao.observeAll()) { habits, completions ->
            val today = DayRange.today()
            habits.map { habit ->
                val done = completions.any {
                    it.habitId == habit.id &&
                        it.completedAt >= today.startMillis &&
                        it.completedAt < today.endMillis
                }
                HabitToday(habit, done)
            }
        }

    /** Incomplete habits, timed-first ascending by deadline, untimed ("any time") last. */
    fun incompleteToday(): Flow<List<Habit>> =
        habitsForToday().map { list ->
            list.asSequence()
                .filter { !it.doneToday }
                .map { it.habit }
                .sortedWith(
                    compareBy(nullsLast<Int>()) { h: Habit -> h.deadlineMinutes }
                        .thenBy { it.createdAt }
                )
                .toList()
        }

    /** Toggle today's completion: insert one if none exists today, else delete today's row(s). */
    suspend fun toggleCompletionForToday(habitId: Long) = withContext(Dispatchers.IO) {
        val today = DayRange.today()
        val existing = completionDao.countForHabitInRange(habitId, today.startMillis, today.endMillis)
        if (existing == 0) {
            completionDao.insert(HabitCompletion(habitId = habitId))
        } else {
            completionDao.deleteForHabitInRange(habitId, today.startMillis, today.endMillis)
        }
    }

    /** Consecutive days (ending today, or yesterday if today isn't done yet) with >=1 completion. */
    fun perHabitStreak(habitId: Long): Flow<Int> =
        completionDao.observeAll().map { all ->
            val days = all.asSequence()
                .filter { it.habitId == habitId }
                .map { DayRange.localDateOf(it.completedAt) }
                .toHashSet()
            consecutiveDaysEndingNear(days, LocalDate.now())
        }

    /**
     * Consecutive "perfect days" ending today/yesterday, where a perfect day = every habit that
     * existed that day (created on/before it, currently non-archived) has >=1 completion that day.
     * Strict rule per BUILD_PLAN.md §4 flagged decision — flip [isPerfectDay] for the gentle variant.
     */
    fun globalStreak(): Flow<Int> =
        combine(habitDao.observeActive(), completionDao.observeAll()) { habits, completions ->
            if (habits.isEmpty()) return@combine 0

            val completionDaysByHabit: Map<Long, Set<LocalDate>> = completions
                .groupBy { it.habitId }
                .mapValues { entry -> entry.value.mapTo(HashSet()) { DayRange.localDateOf(it.completedAt) } }

            val createdDateByHabit: Map<Long, LocalDate> =
                habits.associate { it.id to DayRange.localDateOf(it.createdAt) }

            val today = LocalDate.now()
            val anchor = when {
                isPerfectDay(today, habits, createdDateByHabit, completionDaysByHabit) -> today
                isPerfectDay(today.minusDays(1), habits, createdDateByHabit, completionDaysByHabit) ->
                    today.minusDays(1)
                else -> return@combine 0
            }

            var streak = 0
            var cursor = anchor
            while (isPerfectDay(cursor, habits, createdDateByHabit, completionDaysByHabit)) {
                streak++
                cursor = cursor.minusDays(1)
            }
            streak
        }

    /** Completion counts keyed by local date within [startMillis, endMillis). */
    suspend fun completionsByDay(startMillis: Long, endMillis: Long): Map<LocalDate, Int> =
        withContext(Dispatchers.IO) {
            completionDao.getInRange(startMillis, endMillis)
                .groupingBy { DayRange.localDateOf(it.completedAt) }
                .eachCount()
        }

    // ---- helpers ------------------------------------------------------------

    private fun consecutiveDaysEndingNear(days: Set<LocalDate>, today: LocalDate): Int {
        val anchor = when {
            days.contains(today) -> today
            days.contains(today.minusDays(1)) -> today.minusDays(1)
            else -> return 0
        }
        var streak = 0
        var cursor = anchor
        while (days.contains(cursor)) {
            streak++
            cursor = cursor.minusDays(1)
        }
        return streak
    }

    private fun isPerfectDay(
        date: LocalDate,
        habits: List<Habit>,
        createdDateByHabit: Map<Long, LocalDate>,
        completionDaysByHabit: Map<Long, Set<LocalDate>>
    ): Boolean {
        val scheduled = habits.filter { (createdDateByHabit[it.id] ?: date).let { c -> !c.isAfter(date) } }
        if (scheduled.isEmpty()) return false
        return scheduled.all { completionDaysByHabit[it.id]?.contains(date) == true }
    }
}
