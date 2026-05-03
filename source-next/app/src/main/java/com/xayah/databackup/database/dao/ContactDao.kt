package com.xayah.databackup.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.xayah.databackup.database.entity.Contact
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {
    @Upsert(entity = Contact::class)
    suspend fun upsert(contacts: List<Contact>)

    @Query("SELECT * from contacts WHERE isRestore = :isRestore")
    fun loadFlowContacts(isRestore: Boolean = false): Flow<List<Contact>>

    @Query("UPDATE contacts SET selected = :selected WHERE id = :id AND isRestore = :isRestore")
    suspend fun selectContact(id: Long, selected: Boolean, isRestore: Boolean = false)

    @Query("UPDATE contacts SET selected = :selected WHERE (id) in (:ids) AND isRestore = :isRestore")
    suspend fun selectAllContacts(ids: List<Long>, selected: Boolean, isRestore: Boolean = false)

    @Query("DELETE FROM contacts WHERE isRestore = 1")
    suspend fun deleteRestoreItems()
}
