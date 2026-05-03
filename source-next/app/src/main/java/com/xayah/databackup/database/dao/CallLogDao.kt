package com.xayah.databackup.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.xayah.databackup.database.entity.CallLog
import kotlinx.coroutines.flow.Flow

@Dao
interface CallLogDao {
    @Upsert(entity = CallLog::class)
    suspend fun upsert(callLogs: List<CallLog>)

    @Query("SELECT * from call_logs WHERE isRestore = :isRestore")
    fun loadFlowCallLogs(isRestore: Boolean = false): Flow<List<CallLog>>

    @Query("UPDATE call_logs SET selected = :selected WHERE id = :id AND isRestore = :isRestore")
    suspend fun selectCallLog(id: Long, selected: Boolean, isRestore: Boolean = false)

    @Query("UPDATE call_logs SET selected = :selected WHERE (id) in (:ids) AND isRestore = :isRestore")
    suspend fun selectAllCallLogs(ids: List<Long>, selected: Boolean, isRestore: Boolean = false)

    @Query("DELETE FROM call_logs WHERE isRestore = 1")
    suspend fun deleteRestoreItems()
}
