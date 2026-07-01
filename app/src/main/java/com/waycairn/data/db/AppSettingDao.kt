package com.waycairn.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.waycairn.data.model.AppSetting
import kotlinx.coroutines.flow.Flow

@Dao
interface AppSettingDao {

    @Upsert
    suspend fun upsert(setting: AppSetting)

    @Query("DELETE FROM app_settings WHERE packageName = :packageName")
    suspend fun deleteByPackage(packageName: String)

    @Query("SELECT * FROM app_settings WHERE packageName = :packageName")
    suspend fun getByPackage(packageName: String): AppSetting?

    @Query("SELECT * FROM app_settings ORDER BY packageName ASC")
    fun observeAll(): Flow<List<AppSetting>>

    @Query("SELECT * FROM app_settings WHERE enabled = 1 ORDER BY packageName ASC")
    fun observeEnabled(): Flow<List<AppSetting>>
}
