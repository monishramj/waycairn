package com.waycairn.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.waycairn.data.model.Habit
import com.waycairn.data.repo.HabitRepository
import com.waycairn.ui.components.StreakLevel
import com.waycairn.ui.waycairnApp
import com.waycairn.util.DayRange
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

data class DaySelection(
    val date: LocalDate,
    val completed: List<Habit>,
    /** Habits that existed that day and weren't completed. Empty for today/future (not yet "missed"). */
    val missed: List<Habit>
)

data class CalendarUiState(
    val month: YearMonth = YearMonth.now(),
    val countsByDay: Map<LocalDate, Int> = emptyMap(),
    val selected: DaySelection? = null,
    /** Consecutive days with at least one habit completed (kept-alive streak). */
    val streak: Int = 0,
    /** Today's streak state: none, dim (>=1 habit), or full (all habits). */
    val streakLevel: StreakLevel = StreakLevel.NONE
)

class CalendarViewModel(
    private val repository: HabitRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CalendarUiState())
    val state: StateFlow<CalendarUiState> = _state.asStateFlow()

    /** Live kept-alive streak count + today's streak level, fed from the habits flow. */
    private val streakState: StateFlow<Pair<Int, StreakLevel>> =
        combine(repository.habitsForToday(), repository.anyCompletionStreak()) { habits, streak ->
            val doneCount = habits.count { it.doneToday }
            val level = when {
                habits.isNotEmpty() && doneCount == habits.size -> StreakLevel.FULL
                doneCount >= 1 -> StreakLevel.PARTIAL
                else -> StreakLevel.NONE
            }
            Pair(streak, level)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Pair(0, StreakLevel.NONE))

    init {
        loadMonth(YearMonth.now())
        viewModelScope.launch {
            streakState.collect { (streak, level) ->
                _state.value = _state.value.copy(streak = streak, streakLevel = level)
            }
        }
    }

    fun previousMonth() = loadMonth(_state.value.month.minusMonths(1))

    fun nextMonth() = loadMonth(_state.value.month.plusMonths(1))

    private fun loadMonth(month: YearMonth) {
        _state.value = _state.value.copy(month = month, selected = null)
        viewModelScope.launch {
            val firstDay = month.atDay(1)
            val lastDay = month.atEndOfMonth()
            val start = DayRange.dayFor(firstDay).startMillis
            val end = DayRange.dayFor(lastDay).endMillis
            val counts = repository.completionsByDay(start, end)
            _state.value = _state.value.copy(countsByDay = counts)
        }
    }

    fun selectDay(date: LocalDate) {
        viewModelScope.launch {
            val completed = repository.completedHabitsOn(date)
            // A day in the past can have "missed" habits: ones that existed then but weren't done.
            // Today/future aren't judged yet, so their missed list stays empty.
            val missed = if (date.isBefore(LocalDate.now())) {
                val completedIds = completed.mapTo(HashSet()) { it.id }
                repository.activeHabitsSnapshot()
                    .filter { habit ->
                        habit.id !in completedIds &&
                            !DayRange.localDateOf(habit.createdAt).isAfter(date)
                    }
            } else {
                emptyList()
            }
            _state.value = _state.value.copy(selected = DaySelection(date, completed, missed))
        }
    }

    fun clearSelection() {
        _state.value = _state.value.copy(selected = null)
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { CalendarViewModel(waycairnApp().habitRepository) }
        }
    }
}
