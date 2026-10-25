package com.xayah.databackup.receiver

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.IBinder

/**
 * Empty broadcast receiver to fulfill Android RoleManager requirements
 * for android.app.role.SMS so BackupVault can temporarily hold the SMS role
 * during SMS restore operations.
 */
class SmsDeliverReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        // No-op
    }
}

/**
 * Empty WAP push receiver to fulfill Android RoleManager requirements.
 */
class WapPushReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        // No-op
    }
}

/**
 * Headless SMS service to fulfill Android RoleManager requirements for ACTION_RESPOND_WITH_MESSAGE.
 */
class HeadlessSmsSendService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null
}
