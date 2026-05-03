package com.xayah.databackup.service.util

import arrow.optics.copy
import com.xayah.databackup.App.Companion.application
import com.xayah.databackup.R
import com.xayah.databackup.data.RestoreProcessRepository
import com.xayah.databackup.data.ProcessItem
import com.xayah.databackup.data.currentIndex
import com.xayah.databackup.data.msg
import com.xayah.databackup.data.progress
import com.xayah.databackup.util.LogHelper

class RestoreContactsHelper(private val mRestoreProcessRepo: RestoreProcessRepository) {
    companion object {
        private const val TAG = "RestoreContactsHelper"
    }

    suspend fun start() {
        val contacts = mRestoreProcessRepo.getContacts()
        contacts.forEachIndexed { index, contact ->
            mRestoreProcessRepo.updateContactsItem {
                copy {
                    ProcessItem.currentIndex set index
                    ProcessItem.msg set contact.id.toString()
                    ProcessItem.progress set index.toFloat() / contacts.size
                }
            }
            // TODO: Implement actual contacts restoration logic
        }

        mRestoreProcessRepo.updateContactsItem {
            copy {
                ProcessItem.currentIndex set contacts.size
                ProcessItem.msg set application.getString(R.string.finished)
                ProcessItem.progress set 1f
            }
        }
    }
}
