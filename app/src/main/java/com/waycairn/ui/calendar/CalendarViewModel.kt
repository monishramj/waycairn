package com.waycairn.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.waycairn.data.model.Habit
import com.waycairn.data.repo.HabitRepository
import com.waycairn.ui.waycairnApp
import com.waycairn.util.DayRange
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

data class DaySelection(
    val date: LocalDate,
    val habits: List<Habit>
)

data class CalendarUiState(
    val month: YearMonth = YearMonth.now(),
    val countsByDay: Map<LocalDate, Int> = emptyMap(),
    val selected: DaySelection? = null
)

class CalendarViewModel(
    private val repository: HabitRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CalendarUiState())
    val state: StateFlow<CalendarUiState> = _state.asStateFlow()

    init {
        loadMonth(YearMonth.now())
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
            val habits = repository.completedHabitsOn(date)
            _state.value = _state.value.copy(selected = DaySelection(date, habits))
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
