package com.waycairn.data.repo

import com.waycairn.data.db.AppSettingDao
import com.waycairn.data.model.AppSetting
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/** Per-app gate config store: CRUD plus streams of all / enabled gated apps. */
class AppSettingRepository(
    private val appSettingDao: AppSettingDao
) {

    fun observeAll(): Flow<List<AppSetting>> = appSettingDao.observeAll()

    fun observeEnabled(): Flow<List<AppSetting>> = appSettingDao.observeEnabled()

    /** Package names of all currently enabled gated apps — what the service watches for. */
    fun enabledPackages(): Flow<List<String>> =
        appSettingDao.observeEnabled().map { list -> list.map { it.packageName } }

    suspend fun get(packageName: String): AppSetting? = withContext(Dispatchers.IO) {
        appSettingDao.getByPackage(packageName)
    }

    suspend fun upsert(setting: AppSetting) = withContext(Dispatchers.IO) {
        appSettingDao.upsert(setting)
    }

    suspend fun delete(packageName: String) = withContext(Dispatchers.IO) {
        appSettingDao.deleteByPackage(packageName)
    }
}
