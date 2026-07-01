package com.waycairn.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.waycairn.data.prefs.SettingsStore
import com.waycairn.data.prefs.ThemeMode
import com.waycairn.ui.waycairnApp
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val store: SettingsStore
) : ViewModel() {

    val globalMessage: StateFlow<String> =
        store.globalMessage.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            SettingsStore.DEFAULT_GLOBAL_MESSAGE
        )

    val themeMode: StateFlow<ThemeMode> =
        store.themeMode.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            ThemeMode.SYSTEM
        )

    fun setGlobalMessage(value: String) {
        viewModelScope.launch { store.setGlobalMessage(value) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { store.setThemeMode(mode) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { SettingsViewModel(waycairnApp().settingsStore) }
        }
    }
}
