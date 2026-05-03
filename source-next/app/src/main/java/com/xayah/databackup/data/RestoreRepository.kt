package com.xayah.databackup.data

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.adapter
import com.xayah.databackup.adapter.WifiConfigurationAdapter
import com.xayah.databackup.database.entity.App
import com.xayah.databackup.database.entity.CallLog
import com.xayah.databackup.database.entity.Contact
import com.xayah.databackup.database.entity.Mms
import com.xayah.databackup.database.entity.Network
import com.xayah.databackup.database.entity.Sms
import com.xayah.databackup.entity.BackupConfig
import com.xayah.databackup.rootservice.RemoteRootService
import com.xayah.databackup.util.DatabaseHelper
import com.xayah.databackup.util.LogHelper
import com.xayah.databackup.util.PathHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class RestoreRepository {
    companion object {
        private const val TAG = "RestoreRepository"
    }

    private val moshi: Moshi = Moshi.Builder().add(WifiConfigurationAdapter()).build()

    private val _selectedConfig = MutableStateFlow<BackupConfig?>(null)
    val selectedConfig: Flow<BackupConfig?> = _selectedConfig.asStateFlow()

    val restoreApps: Flow<List<App>> = DatabaseHelper.appDao.loadFlowApps(isRestore = true)
    val restoreAppsSelected: Flow<List<App>> = restoreApps.map { apps -> apps.filter { it.isSelected } }

    val restoreNetworks: Flow<List<Network>> = DatabaseHelper.networkDao.loadFlowNetworks(isRestore = true)
    val restoreNetworksSelected: Flow<List<Network>> = restoreNetworks.map { networks -> networks.filter { it.selected } }

    val restoreContacts: Flow<List<Contact>> = DatabaseHelper.contactDao.loadFlowContacts(isRestore = true)
    val restoreContactsSelected: Flow<List<Contact>> = restoreContacts.map { contacts -> contacts.filter { it.selected } }

    val restoreCallLogs: Flow<List<CallLog>> = DatabaseHelper.callLogDao.loadFlowCallLogs(isRestore = true)
    val restoreCallLogsSelected: Flow<List<CallLog>> = restoreCallLogs.map { callLogs -> callLogs.filter { it.selected } }

    val restoreSmsList: Flow<List<Sms>> = DatabaseHelper.messageDao.loadFlowSms(isRestore = true)
    val restoreSmsListSelected: Flow<List<Sms>> = restoreSmsList.map { smsList -> smsList.filter { it.selected } }

    val restoreMmsList: Flow<List<Mms>> = DatabaseHelper.messageDao.loadFlowMms(isRestore = true)
    val restoreMmsListSelected: Flow<List<Mms>> = restoreMmsList.map { mmsList -> mmsList.filter { it.selected } }

    suspend fun selectConfig(config: BackupConfig?) {
        _selectedConfig.emit(config)
        if (config != null) {
            scanBackupContent(config)
        } else {
            clearRestoreDatabase()
        }
    }

    private suspend fun clearRestoreDatabase() {
        DatabaseHelper.appDao.deleteRestoreItems()
        DatabaseHelper.networkDao.deleteRestoreItems()
        DatabaseHelper.callLogDao.deleteRestoreItems()
        DatabaseHelper.contactDao.deleteRestoreItems()
        DatabaseHelper.messageDao.deleteRestoreSms()
        DatabaseHelper.messageDao.deleteRestoreMms()
    }

    private suspend fun scanBackupContent(config: BackupConfig) {
        withContext(Dispatchers.IO) {
            clearRestoreDatabase()

            // Scan Networks
            runCatching {
                val networksJson = RemoteRootService.readText(PathHelper.getBackupNetworksConfigFilePath(config.path))
                if (networksJson.isNotEmpty()) {
                    val listType = Types.newParameterizedType(List::class.java, Network::class.java)
                    val networks = moshi.adapter<List<Network>>(listType).fromJson(networksJson)
                    networks?.forEach { it.isRestore = true; it.selected = true }
                    if (networks != null) DatabaseHelper.networkDao.upsert(networks)
                }
            }.onFailure { LogHelper.e(TAG, "scanBackupContent", "Failed to scan networks", it) }

            // Scan Contacts
            runCatching {
                val contactsJson = RemoteRootService.readText(PathHelper.getBackupContactsConfigFilePath(config.path))
                if (contactsJson.isNotEmpty()) {
                    val listType = Types.newParameterizedType(List::class.java, Contact::class.java)
                    val contacts = moshi.adapter<List<Contact>>(listType).fromJson(contactsJson)
                    contacts?.forEach { it.isRestore = true; it.selected = true }
                    if (contacts != null) DatabaseHelper.contactDao.upsert(contacts)
                }
            }.onFailure { LogHelper.e(TAG, "scanBackupContent", "Failed to scan contacts", it) }

            // Scan Call Logs
            runCatching {
                val callLogsJson = RemoteRootService.readText(PathHelper.getBackupCallLogsConfigFilePath(config.path))
                if (callLogsJson.isNotEmpty()) {
                    val listType = Types.newParameterizedType(List::class.java, CallLog::class.java)
                    val callLogs = moshi.adapter<List<CallLog>>(listType).fromJson(callLogsJson)
                    callLogs?.forEach { it.isRestore = true; it.selected = true }
                    if (callLogs != null) DatabaseHelper.callLogDao.upsert(callLogs)
                }
            }.onFailure { LogHelper.e(TAG, "scanBackupContent", "Failed to scan call logs", it) }

            // Scan SMS
            runCatching {
                val smsJson = RemoteRootService.readText(PathHelper.getBackupMessagesSmsConfigFilePath(config.path))
                if (smsJson.isNotEmpty()) {
                    val listType = Types.newParameterizedType(List::class.java, Sms::class.java)
                    val smsList = moshi.adapter<List<Sms>>(listType).fromJson(smsJson)
                    smsList?.forEach { it.isRestore = true; it.selected = true }
                    if (smsList != null) DatabaseHelper.messageDao.upsertSms(smsList)
                }
            }.onFailure { LogHelper.e(TAG, "scanBackupContent", "Failed to scan SMS", it) }

            // Scan MMS
            runCatching {
                val mmsJson = RemoteRootService.readText(PathHelper.getBackupMessagesMmsConfigFilePath(config.path))
                if (mmsJson.isNotEmpty()) {
                    val listType = Types.newParameterizedType(List::class.java, Mms::class.java)
                    val mmsList = moshi.adapter<List<Mms>>(listType).fromJson(mmsJson)
                    mmsList?.forEach { it.isRestore = true; it.selected = true }
                    if (mmsList != null) DatabaseHelper.messageDao.upsertMms(mmsList)
                }
            }.onFailure { LogHelper.e(TAG, "scanBackupContent", "Failed to scan MMS", it) }

            // Scan Apps
            runCatching {
                val appsDir = "${config.path}/apps"
                val packages = RemoteRootService.listFilePaths(appsDir, listFiles = false, listDirs = true)
                val apps = packages.mapNotNull {
                    val jsonPath = "${it.path}/restore.json"
                    val json = RemoteRootService.readText(jsonPath)
                    if (json.isNotEmpty()) {
                        moshi.adapter<App>(App::class.java).fromJson(json)?.apply {
                            this.isRestore = true
                            // Default all components to selected for restoration
                            this.option.apk = true
                            this.option.internalData = true
                            this.option.externalData = true
                            this.option.additionalData = true
                        }
                    } else {
                        // Fallback if restore.json is missing
                        App(
                            packageName = PathHelper.getChildPath(it.path),
                            userId = 0,
                            isRestore = true,
                            info = com.xayah.databackup.database.entity.Info(label = PathHelper.getChildPath(it.path)),
                            option = com.xayah.databackup.database.entity.Option(apk = true, internalData = true, externalData = true, additionalData = true),
                            storage = com.xayah.databackup.database.entity.Storage()
                        )
                    }
                }
                DatabaseHelper.appDao.upsert(apps)
            }.onFailure { LogHelper.e(TAG, "scanBackupContent", "Failed to scan apps", it) }
        }
    }
}
