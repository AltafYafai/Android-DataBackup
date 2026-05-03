package com.xayah.databackup.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.xayah.databackup.database.entity.App
import com.xayah.databackup.database.entity.AppInfo
import com.xayah.databackup.database.entity.AppStorage
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Upsert(entity = App::class)
    suspend fun upsert(apps: List<App>)

    @Upsert(entity = App::class)
    suspend fun upsertInfo(apps: List<AppInfo>)

    @Upsert(entity = App::class)
    suspend fun upsertStorage(apps: List<AppStorage>)

    @Query("SELECT * from apps WHERE isRestore = :isRestore")
    fun loadFlowApps(isRestore: Boolean = false): Flow<List<App>>

    @Query("UPDATE apps SET option_apk = :selected WHERE packageName = :packageName AND userId = :userId AND isRestore = :isRestore")
    suspend fun selectApk(packageName: String, userId: Int, selected: Boolean, isRestore: Boolean = false)

    @Query("UPDATE apps SET option_internalData = :selected WHERE packageName = :packageName AND userId = :userId AND isRestore = :isRestore")
    suspend fun selectInternalData(packageName: String, userId: Int, selected: Boolean, isRestore: Boolean = false)

    @Query("UPDATE apps SET option_externalData = :selected WHERE packageName = :packageName AND userId = :userId AND isRestore = :isRestore")
    suspend fun selectExternalData(packageName: String, userId: Int, selected: Boolean, isRestore: Boolean = false)

    @Query("UPDATE apps SET option_additionalData = :selected WHERE packageName = :packageName AND userId = :userId AND isRestore = :isRestore")
    suspend fun selectAdditionalData(packageName: String, userId: Int, selected: Boolean, isRestore: Boolean = false)

    @Query("UPDATE apps SET option_apk = :selected, option_internalData = :selected, option_externalData = :selected, option_additionalData = :selected WHERE packageName = :packageName AND userId = :userId AND isRestore = :isRestore")
    suspend fun selectAll(packageName: String, userId: Int, selected: Boolean, isRestore: Boolean = false)

    @Query("UPDATE apps SET option_apk = :selected WHERE (packageName || '-' || userId) in (:keys) AND isRestore = :isRestore")
    suspend fun selectAllApk(keys: List<String>, selected: Boolean, isRestore: Boolean = false)

    @Query("UPDATE apps SET option_internalData = :selected, option_externalData = :selected, option_additionalData = :selected WHERE (packageName || '-' || userId) in (:keys) AND isRestore = :isRestore")
    suspend fun selectAllData(keys: List<String>, selected: Boolean, isRestore: Boolean = false)

    @Query("UPDATE apps SET option_internalData = :selected WHERE (packageName || '-' || userId) in (:keys) AND isRestore = :isRestore")
    suspend fun selectAllIntData(keys: List<String>, selected: Boolean, isRestore: Boolean = false)

    @Query("UPDATE apps SET option_externalData = :selected WHERE (packageName || '-' || userId) in (:keys) AND isRestore = :isRestore")
    suspend fun selectAllExtData(keys: List<String>, selected: Boolean, isRestore: Boolean = false)

    @Query("UPDATE apps SET option_additionalData = :selected WHERE (packageName || '-' || userId) in (:keys) AND isRestore = :isRestore")
    suspend fun selectAllAddlData(keys: List<String>, selected: Boolean, isRestore: Boolean = false)

    @Query("DELETE FROM apps WHERE isRestore = 1")
    suspend fun deleteRestoreItems()
}
