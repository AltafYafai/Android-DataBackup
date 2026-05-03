package com.xayah.databackup.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.xayah.databackup.database.entity.Mms
import com.xayah.databackup.database.entity.Sms
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Upsert(entity = Sms::class)
    suspend fun upsertSms(messages: List<Sms>)

    @Query("SELECT * from messages_sms WHERE isRestore = :isRestore")
    fun loadFlowSms(isRestore: Boolean = false): Flow<List<Sms>>

    @Query("UPDATE messages_sms SET selected = :selected WHERE id = :id AND isRestore = :isRestore")
    suspend fun selectSms(id: Long, selected: Boolean, isRestore: Boolean = false)

    @Query("UPDATE messages_sms SET selected = :selected WHERE (id) in (:ids) AND isRestore = :isRestore")
    suspend fun selectAllSms(ids: List<Long>, selected: Boolean, isRestore: Boolean = false)

    @Query("DELETE FROM messages_sms WHERE isRestore = 1")
    suspend fun deleteRestoreSms()

    @Upsert(entity = Mms::class)
    suspend fun upsertMms(messages: List<Mms>)

    @Query("SELECT * from messages_mms WHERE isRestore = :isRestore")
    fun loadFlowMms(isRestore: Boolean = false): Flow<List<Mms>>

    @Query("UPDATE messages_mms SET selected = :selected WHERE id = :id AND isRestore = :isRestore")
    suspend fun selectMms(id: Long, selected: Boolean, isRestore: Boolean = false)

    @Query("UPDATE messages_mms SET selected = :selected WHERE (id) in (:ids) AND isRestore = :isRestore")
    suspend fun selectAllMms(ids: List<Long>, selected: Boolean, isRestore: Boolean = false)

    @Query("DELETE FROM messages_mms WHERE isRestore = 1")
    suspend fun deleteRestoreMms()
}
