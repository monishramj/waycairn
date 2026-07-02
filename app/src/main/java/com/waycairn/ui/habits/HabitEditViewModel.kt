package com.waycairn.ui.habits

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.waycairn.data.model.Habit
import com.waycairn.data.repo.HabitRepository
import com.waycairn.notification.AlarmScheduler
import com.waycairn.ui.nav.Routes
import com.waycairn.ui.waycairnApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HabitEditState(
    val loaded: Boolean = false,
    val title: String = "",
    val description: String = "",
    val deadlineMinutes: Int? = null
) {
    val canSave: Boolean get() = title.isNotBlank()
}

class HabitEditViewModel(
    private val repository: HabitRepository,
    private val appContext: Context,
    private val habitId: Long
) : ViewModel() {

    val isEditing: Boolean = habitId > 0

    private val _state = MutableStateFlow(HabitEditState())
    val state: StateFlow<HabitEditState> = _state.asStateFlow()

    init {
        if (isEditing) {
            viewModelScope.launch {
                val existing = repository.getHabit(habitId)
                _state.value = if (existing != null) {
                    HabitEditState(
                        loaded = true,
                        title = existing.title,
                        description = existing.description,
                        deadlineMinutes = existing.deadlineMinutes
                    )
                } else {
                    HabitEditState(loaded = true)
                }
            }
        } else {
            _state.value = HabitEditState(loaded = true)
        }
    }

    fun onTitleChange(value: String) {
        _state.value = _state.value.copy(title = value)
    }

    fun onDescriptionChange(value: String) {
        _state.value = _state.value.copy(description = value)
    }

    fun setDeadline(minutes: Int?) {
        _state.value = _state.value.copy(deadlineMinutes = minutes)
    }

    fun save(onDone: () -> Unit) {
        val current = _state.value
        if (!current.canSave) return
        viewModelScope.launch {
            if (isEditing) {
                val existing = repository.getHabit(habitId)
                if (existing != null) {
                    repository.updateHabit(
                        existing.copy(
                            title = current.title.trim(),
                            description = current.description.trim(),
                            deadlineMinutes = current.deadlineMinutes
                        )
                    )
                }
            } else {
                repository.addHabit(
                    Habit(
                        title = current.title.trim(),
                        description = current.description.trim(),
                        deadlineMinutes = current.deadlineMinutes
                    )
                )
            }
            // Habit create/edit changes today's timed alarms — rebuild them.
            AlarmScheduler.rescheduleAll(appContext)
            onDone()
        }
    }

    fun delete(onDone: () -> Unit) {
        if (!isEditing) {
            onDone()
            return
        }
        viewModelScope.launch {
            repository.deleteHabit(habitId)
            // Drop the deleted habit's alarms, then rebuild the rest of today's.
            AlarmScheduler.cancelHabit(appContext, habitId)
            AlarmScheduler.rescheduleAll(appContext)
            onDone()
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val handle = createSavedStateHandle()
                val id = handle.get<Long>(Routes.ARG_HABIT_ID) ?: -1L
                val app = waycairnApp()
                HabitEditViewModel(app.habitRepository, app, id)
            }
        }
    }
}
