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

class RestoreAppsHelper(private val mRestoreProcessRepo: RestoreProcessRepository) {
    companion object {
        private const val TAG = "RestoreAppsHelper"
    }

    suspend fun start() {
        val apps = mRestoreProcessRepo.getApps()
        apps.forEachIndexed { index, app ->
            mRestoreProcessRepo.updateAppsItem {
                copy {
                    ProcessItem.currentIndex set index
                    ProcessItem.msg set app.info.label
                    ProcessItem.progress set index.toFloat() / apps.size
                }
            }
            // TODO: Implement actual apps restoration logic
        }

        mRestoreProcessRepo.updateAppsItem {
            copy {
                ProcessItem.currentIndex set apps.size
                ProcessItem.msg set application.getString(R.string.finished)
                ProcessItem.progress set 1f
            }
        }
    }
}
