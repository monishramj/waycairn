package com.waycairn.ui.habits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.waycairn.data.repo.HabitRepository
import com.waycairn.data.repo.HabitToday
import com.waycairn.ui.waycairnApp
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HabitListUiState(
    val habits: List<HabitToday> = emptyList(),
    val globalStreak: Int = 0
)

class HabitListViewModel(
    private val repository: HabitRepository
) : ViewModel() {

    val uiState: StateFlow<HabitListUiState> =
        combine(repository.habitsForToday(), repository.globalStreak()) { habits, streak ->
            HabitListUiState(habits = habits, globalStreak = streak)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HabitListUiState()
        )

    fun toggle(habitId: Long) {
        viewModelScope.launch { repository.toggleCompletionForToday(habitId) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { HabitListViewModel(waycairnApp().habitRepository) }
        }
    }
}
