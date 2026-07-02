package com.waycairn.ui.habits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.waycairn.data.model.Habit
import com.waycairn.data.repo.HabitRepository
import com.waycairn.ui.nav.Routes
import com.waycairn.ui.waycairnApp
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class DayDot(val date: LocalDate, val done: Boolean)

data class HabitDetailUiState(
    val habit: Habit? = null,
    val streak: Int = 0,
    val bestStreak: Int = 0,
    val lastDays: List<DayDot> = emptyList()
)

class HabitDetailViewModel(
    repository: HabitRepository,
    private val habitId: Long
) : ViewModel() {

    val uiState: StateFlow<HabitDetailUiState> =
        combine(
            repository.observeHabit(habitId),
            repository.perHabitStreak(habitId),
            repository.perHabitBestStreak(habitId),
            repository.habitCompletionDates(habitId)
        ) { habit, streak, best, dates ->
            val today = LocalDate.now()
            val lastDays = (0 until STRIP_DAYS).map { i ->
                val date = today.minusDays((STRIP_DAYS - 1 - i).toLong())
                DayDot(date = date, done = dates.contains(date))
            }
            HabitDetailUiState(
                habit = habit,
                streak = streak,
                bestStreak = maxOf(best, streak),
                lastDays = lastDays
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HabitDetailUiState()
        )

    companion object {
        const val STRIP_DAYS = 14

        val Factory = viewModelFactory {
            initializer {
                val handle = createSavedStateHandle()
                val id = handle.get<Long>(Routes.ARG_HABIT_ID) ?: -1L
                HabitDetailViewModel(waycairnApp().habitRepository, id)
            }
        }
    }
}
