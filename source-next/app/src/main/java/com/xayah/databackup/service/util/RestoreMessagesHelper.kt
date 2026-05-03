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

class RestoreMessagesHelper(private val mRestoreProcessRepo: RestoreProcessRepository) {
    companion object {
        private const val TAG = "RestoreMessagesHelper"
    }

    suspend fun start() {
        val smsList = mRestoreProcessRepo.getSmsList()
        val mmsList = mRestoreProcessRepo.getMmsList()
        val totalCount = smsList.size + mmsList.size
        
        // Process SMS
        smsList.forEachIndexed { index, sms ->
            mRestoreProcessRepo.updateMessagesItem {
                copy {
                    ProcessItem.currentIndex set index
                    ProcessItem.msg set "SMS: ${sms.id}"
                    ProcessItem.progress set index.toFloat() / totalCount
                }
            }
            // TODO: Implement actual SMS restoration logic
        }
        
        // Process MMS
        mmsList.forEachIndexed { index, mms ->
            val currentIndex = smsList.size + index
            mRestoreProcessRepo.updateMessagesItem {
                copy {
                    ProcessItem.currentIndex set currentIndex
                    ProcessItem.msg set "MMS: ${mms.id}"
                    ProcessItem.progress set currentIndex.toFloat() / totalCount
                }
            }
            // TODO: Implement actual MMS restoration logic
        }

        mRestoreProcessRepo.updateMessagesItem {
            copy {
                ProcessItem.currentIndex set totalCount
                ProcessItem.msg set application.getString(R.string.finished)
                ProcessItem.progress set 1f
            }
        }
    }
}
