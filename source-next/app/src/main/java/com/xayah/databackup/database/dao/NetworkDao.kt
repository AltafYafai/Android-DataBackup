package com.xayah.databackup.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.xayah.databackup.database.entity.Network
import kotlinx.coroutines.flow.Flow

@Dao
interface NetworkDao {
    @Upsert(entity = Network::class)
    suspend fun upsert(networks: List<Network>)

    @Query("SELECT * from networks WHERE isRestore = :isRestore")
    fun loadFlowNetworks(isRestore: Boolean = false): Flow<List<Network>>

    @Query("UPDATE networks SET selected = :selected WHERE id = :id AND isRestore = :isRestore")
    suspend fun selectNetwork(id: Int, selected: Boolean, isRestore: Boolean = false)

    @Query("UPDATE networks SET selected = :selected WHERE (id) in (:ids) AND isRestore = :isRestore")
    suspend fun selectAllNetworks(ids: List<Int>, selected: Boolean, isRestore: Boolean = false)

    @Query("DELETE FROM networks WHERE isRestore = 1")
    suspend fun deleteRestoreItems()
}
