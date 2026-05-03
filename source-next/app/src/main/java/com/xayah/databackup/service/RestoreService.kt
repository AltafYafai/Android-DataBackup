package com.xayah.databackup.service

import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Binder
import android.os.IBinder
import android.os.RemoteException
import com.xayah.databackup.App
import com.xayah.databackup.R
import com.xayah.databackup.data.RestoreProcessRepository
import com.xayah.databackup.service.util.RestoreAppsHelper
import com.xayah.databackup.service.util.RestoreNetworksHelper
import com.xayah.databackup.service.util.RestoreContactsHelper
import com.xayah.databackup.service.util.RestoreCallLogsHelper
import com.xayah.databackup.service.util.RestoreMessagesHelper
import com.xayah.databackup.util.LogHelper
import com.xayah.databackup.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class RestoreService : Service() {
    companion object {
        private const val TAG = "RestoreService"
        private var mService: IRestoreService? = null
        private val mServiceConnection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                mService = IRestoreService.Stub.asInterface(service)
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                mService = null
            }
        }

        private fun bindService(context: Context): IRestoreService? {
            context.bindService(Intent(context, RestoreService::class.java), mServiceConnection, BIND_AUTO_CREATE)
            return mService
        }

        private suspend fun getService(): IRestoreService? {
            return if (mService == null) {
                runCatching { bindService(App.application) }.getOrNull()
            } else {
                mService
            }
        }

        suspend fun start() {
            getService()?.start()
        }
    }

    private val mRestoreProcessRepo: RestoreProcessRepository by inject()
    private val mRestoreAppsHelper: RestoreAppsHelper by inject()
    private val mRestoreNetworksHelper: RestoreNetworksHelper by inject()
    private val mRestoreContactsHelper: RestoreContactsHelper by inject()
    private val mRestoreCallLogsHelper: RestoreCallLogsHelper by inject()
    private val mRestoreMessagesHelper: RestoreMessagesHelper by inject()

    private val mBinder = object : IRestoreService.Stub() {
        override fun start() {
            mScope.launch {
                runRestore()
            }
        }
    }

    private val mJob = SupervisorJob()
    private val mScope = CoroutineScope(Dispatchers.IO + mJob)

    override fun onBind(intent: Intent?): IBinder = mBinder

    override fun onCreate() {
        super.onCreate()
        LogHelper.i(TAG, "onCreate", "RestoreService created")
    }

    override fun onDestroy() {
        super.onDestroy()
        mJob.cancel()
        LogHelper.i(TAG, "onDestroy", "RestoreService destroyed")
    }

    private suspend fun runRestore() {
        LogHelper.i(TAG, "runRestore", "Starting restoration process")
        val notificationId = NotificationHelper.getRestoreNotificationId()
        startForeground(notificationId, NotificationHelper.getRestoreNotification(this, getString(R.string.restoring), 0f))

        runCatching {
            if (mRestoreProcessRepo.getAppsItem().value.isSelected) {
                mRestoreAppsHelper.start()
            }
            if (mRestoreProcessRepo.getNetworksItem().value.isSelected) {
                mRestoreNetworksHelper.start()
            }
            if (mRestoreProcessRepo.getContactsItem().value.isSelected) {
                mRestoreContactsHelper.start()
            }
            if (mRestoreProcessRepo.getCallLogsItem().value.isSelected) {
                mRestoreCallLogsHelper.start()
            }
            if (mRestoreProcessRepo.getMessagesItem().value.isSelected) {
                mRestoreMessagesHelper.start()
            }
        }.onFailure {
            LogHelper.e(TAG, "runRestore", "Restoration failed", it)
        }

        LogHelper.i(TAG, "runRestore", "Restoration process finished")
        stopForeground(STOP_FOREGROUND_DETACH)
        NotificationHelper.notifyRestoreFinished(this)
        stopSelf()
    }
}

interface IRestoreService : android.os.IInterface {
    fun start()
    abstract class Stub : Binder(), IRestoreService {
        override fun asBinder(): IBinder = this
        companion object {
            fun asInterface(binder: IBinder?): IRestoreService? {
                if (binder == null) return null
                return binder as? IRestoreService ?: object : IRestoreService {
                    override fun start() {
                        val data = android.os.Parcel.obtain()
                        try {
                            data.writeInterfaceToken("IRestoreService")
                            binder.transact(1, data, null, android.os.IBinder.FLAG_ONEWAY)
                        } finally {
                            data.recycle()
                        }
                    }
                    override fun asBinder(): IBinder = binder
                }
            }
        }
        override fun onTransact(code: Int, data: android.os.Parcel, reply: android.os.Parcel?, flags: Int): Boolean {
            when (code) {
                1 -> {
                    data.enforceInterface("IRestoreService")
                    start()
                    return true
                }
            }
            return super.onTransact(code, data, reply, flags)
        }
    }
}
