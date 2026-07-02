package com.waycairn.ui.apps

import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.waycairn.data.model.AppSetting
import com.waycairn.data.model.TriggerMode
import com.waycairn.data.repo.AppSettingRepository
import com.waycairn.ui.waycairnApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class LaunchableApp(
    val packageName: String,
    val label: String,
    val icon: ImageBitmap?
)

class AppPickerViewModel(
    application: Application,
    private val repository: AppSettingRepository
) : AndroidViewModel(application) {

    private val _apps = MutableStateFlow<List<LaunchableApp>>(emptyList())
    val apps: StateFlow<List<LaunchableApp>> = _apps.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    /** packageName -> its saved gate config (present == gated). */
    val gated: StateFlow<Map<String, AppSetting>> =
        repository.observeAll()
            .map { list -> list.associateBy { it.packageName } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    init {
        loadInstalledApps()
    }

    private fun loadInstalledApps() {
        viewModelScope.launch {
            val ownPackage = getApplication<Application>().packageName
            val loaded = withContext(Dispatchers.IO) {
                val pm = getApplication<Application>().packageManager
                val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                val launcherPackage = pm.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
                    ?.activityInfo?.packageName
                val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                pm.queryIntentActivities(intent, 0)
                    .asSequence()
                    .map { it.activityInfo.packageName }
                    .filter { it != ownPackage && it != launcherPackage }
                    .distinct()
                    .mapNotNull { pkg ->
                        runCatching {
                            val appInfo = pm.getApplicationInfo(pkg, 0)
                            val label = pm.getApplicationLabel(appInfo).toString()
                            val icon = runCatching {
                                pm.getApplicationIcon(appInfo).toBitmap(96, 96).asImageBitmap()
                            }.getOrNull()
                            LaunchableApp(pkg, label, icon)
                        }.getOrNull()
                    }
                    .sortedBy { it.label.lowercase() }
                    .toList()
            }
            _apps.value = loaded
            _loading.value = false
        }
    }

    fun setGated(packageName: String, gated: Boolean) {
        viewModelScope.launch {
            if (gated) {
                val existing = repository.get(packageName)
                repository.upsert(existing?.copy(enabled = true) ?: AppSetting(packageName = packageName))
            } else {
                repository.delete(packageName)
            }
        }
    }

    fun updateConfig(
        packageName: String,
        triggerMode: TriggerMode,
        delayMinutes: Int,
        showAfterComplete: Boolean
    ) {
        viewModelScope.launch {
            val existing = repository.get(packageName) ?: AppSetting(packageName = packageName)
            repository.upsert(
                existing.copy(
                    triggerMode = triggerMode,
                    delayMinutes = delayMinutes,
                    showAfterComplete = showAfterComplete,
                    enabled = true
                )
            )
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                AppPickerViewModel(waycairnApp(), waycairnApp().appSettingRepository)
            }
        }
    }
}
