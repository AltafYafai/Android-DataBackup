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

class RestoreCallLogsHelper(private val mRestoreProcessRepo: RestoreProcessRepository) {
    companion object {
        private const val TAG = "RestoreCallLogsHelper"
    }

    suspend fun start() {
        val callLogs = mRestoreProcessRepo.getCallLogs()
        callLogs.forEachIndexed { index, callLog ->
            mRestoreProcessRepo.updateCallLogsItem {
                copy {
                    ProcessItem.currentIndex set index
                    ProcessItem.msg set callLog.id.toString()
                    ProcessItem.progress set index.toFloat() / callLogs.size
                }
            }
            // TODO: Implement actual call logs restoration logic
        }

        mRestoreProcessRepo.updateCallLogsItem {
            copy {
                ProcessItem.currentIndex set callLogs.size
                ProcessItem.msg set application.getString(R.string.finished)
                ProcessItem.progress set 1f
            }
        }
    }
}
