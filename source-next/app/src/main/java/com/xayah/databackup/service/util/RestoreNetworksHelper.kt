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

class RestoreNetworksHelper(private val mRestoreProcessRepo: RestoreProcessRepository) {
    companion object {
        private const val TAG = "RestoreNetworksHelper"
    }

    suspend fun start() {
        val networks = mRestoreProcessRepo.getNetworks()
        networks.forEachIndexed { index, network ->
            mRestoreProcessRepo.updateNetworksItem {
                copy {
                    ProcessItem.currentIndex set index
                    ProcessItem.msg set network.ssid
                    ProcessItem.progress set index.toFloat() / networks.size
                }
            }
            // TODO: Implement actual network restoration logic
        }

        mRestoreProcessRepo.updateNetworksItem {
            copy {
                ProcessItem.currentIndex set networks.size
                ProcessItem.msg set application.getString(R.string.finished)
                ProcessItem.progress set 1f
            }
        }
    }
}
