package com.xayah.databackup.data

import com.xayah.databackup.App.Companion.application
import com.xayah.databackup.R
import com.xayah.databackup.database.entity.App
import com.xayah.databackup.database.entity.CallLog
import com.xayah.databackup.database.entity.Contact
import com.xayah.databackup.database.entity.Mms
import com.xayah.databackup.database.entity.Network
import com.xayah.databackup.database.entity.Sms
import com.xayah.databackup.entity.BackupConfig
import com.xayah.databackup.service.RestoreService
import com.xayah.databackup.util.ShellHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update

class RestoreProcessRepository(
    private val mRestoreRepo: RestoreRepository,
) {
    companion object {
        private const val TAG = "RestoreProcessRepository"
    }

    var mIsCanceled: Boolean = false
        private set

    private var _backupConfig: BackupConfig = BackupConfig()

    private var _appsItem: MutableStateFlow<ProcessItem> = MutableStateFlow(ProcessItem())
    private var _apps: List<App> = listOf()
    private var _processAppItems: MutableStateFlow<List<ProcessAppItem>> = MutableStateFlow(listOf())

    private var _networksItem: MutableStateFlow<ProcessItem> = MutableStateFlow(ProcessItem())
    private var _networks: List<Network> = listOf()

    private var _contactsItem: MutableStateFlow<ProcessItem> = MutableStateFlow(ProcessItem())
    private var _contacts: List<Contact> = listOf()

    private var _callLogsItem: MutableStateFlow<ProcessItem> = MutableStateFlow(ProcessItem())
    private var _callLogs: List<CallLog> = listOf()

    private var _messagesItem: MutableStateFlow<ProcessItem> = MutableStateFlow(ProcessItem())
    private var _smsList: List<Sms> = listOf()
    private var _mmsList: List<Mms> = listOf()

    suspend fun loadAppsProcessItems() {
        _apps = mRestoreRepo.restoreAppsSelected.first()
        _appsItem.update {
            it.copy(
                isLoading = false,
                isSelected = _apps.isNotEmpty(),
                currentIndex = 0,
                totalCount = _apps.size,
                progress = 0f
            )
        }
    }

    suspend fun loadNetworksProcessItems() {
        _networks = mRestoreRepo.restoreNetworksSelected.first()
        _networksItem.update {
            it.copy(
                isLoading = false,
                isSelected = _networks.isNotEmpty(),
                currentIndex = 0,
                totalCount = _networks.size,
                progress = 0f
            )
        }
    }

    suspend fun loadContactsProcessItems() {
        _contacts = mRestoreRepo.restoreContactsSelected.first()
        _contactsItem.update {
            it.copy(
                isLoading = false,
                isSelected = _contacts.isNotEmpty(),
                currentIndex = 0,
                totalCount = _contacts.size,
                progress = 0f
            )
        }
    }

    suspend fun loadCallLogsProcessItems() {
        _callLogs = mRestoreRepo.restoreCallLogsSelected.first()
        _callLogsItem.update {
            it.copy(
                isLoading = false,
                isSelected = _callLogs.isNotEmpty(),
                currentIndex = 0,
                totalCount = _callLogs.size,
                progress = 0f
            )
        }
    }

    suspend fun loadMessagesProcessItems() {
        _smsList = mRestoreRepo.restoreSmsListSelected.first()
        _mmsList = mRestoreRepo.restoreMmsListSelected.first()
        _messagesItem.update {
            it.copy(
                isLoading = false,
                isSelected = (_smsList.size + _mmsList.size) > 0,
                currentIndex = 0,
                totalCount = _smsList.size + _mmsList.size,
                progress = 0f
            )
        }
    }

    private suspend fun loadProcessItems() {
        clearProcessAppItems()
        loadAppsProcessItems()
        loadNetworksProcessItems()
        loadContactsProcessItems()
        loadCallLogsProcessItems()
        loadMessagesProcessItems()
    }

    private suspend fun loadBackupPath() {
        _backupConfig = mRestoreRepo.selectedConfig.first() ?: BackupConfig()
    }

    suspend fun cancel() {
        if (mIsCanceled.not()) {
            mIsCanceled = true
            ShellHelper.killRootService()
        }
    }

    suspend fun onStart() {
        mIsCanceled = false
        loadBackupPath()
        loadProcessItems()
        RestoreService.start()
    }

    fun getBackupConfig(): BackupConfig {
        return _backupConfig
    }

    fun getAppsItem(): MutableStateFlow<ProcessItem> {
        return _appsItem
    }

    fun getNetworksItem(): MutableStateFlow<ProcessItem> {
        return _networksItem
    }

    fun getContactsItem(): MutableStateFlow<ProcessItem> {
        return _contactsItem
    }

    fun getCallLogsItem(): MutableStateFlow<ProcessItem> {
        return _callLogsItem
    }

    fun getMessagesItem(): MutableStateFlow<ProcessItem> {
        return _messagesItem
    }

    fun getApps(): List<App> {
        return _apps
    }

    fun getNetworks(): List<Network> {
        return _networks
    }

    fun getContacts(): List<Contact> {
        return _contacts
    }

    fun getCallLogs(): List<CallLog> {
        return _callLogs
    }

    fun getSmsList(): List<Sms> {
        return _smsList
    }

    fun getMmsList(): List<Mms> {
        return _mmsList
    }

    fun reset() {
        updateAppsItem { ProcessItem() }
        updateNetworksItem { ProcessItem() }
        updateContactsItem { ProcessItem() }
        updateCallLogsItem { ProcessItem() }
        updateMessagesItem { ProcessItem() }
        clearProcessAppItems()
    }

    fun clearProcessAppItems() {
        _processAppItems.value = listOf()
    }

    fun getProcessAppItems(): MutableStateFlow<List<ProcessAppItem>> {
        return _processAppItems
    }

    fun updateAppsItem(onUpdate: ProcessItem.() -> ProcessItem) {
        _appsItem.value = onUpdate(_appsItem.value)
    }

    fun updateNetworksItem(onUpdate: ProcessItem.() -> ProcessItem) {
        _networksItem.value = onUpdate(_networksItem.value)
    }

    fun updateContactsItem(onUpdate: ProcessItem.() -> ProcessItem) {
        _contactsItem.value = onUpdate(_contactsItem.value)
    }

    fun updateCallLogsItem(onUpdate: ProcessItem.() -> ProcessItem) {
        _callLogsItem.value = onUpdate(_callLogsItem.value)
    }

    fun updateMessagesItem(onUpdate: ProcessItem.() -> ProcessItem) {
        _messagesItem.value = onUpdate(_messagesItem.value)
    }

    fun addProcessAppItem(item: ProcessAppItem) {
        _processAppItems.update {
            val items = it.toMutableList()
            items.add(item)
            items
        }
    }

    fun updateProcessAppItem(onUpdate: ProcessAppItem.() -> ProcessAppItem) {
        val currentList = _processAppItems.value
        val newList = currentList.mapIndexed { index, item ->
            if (index == currentList.size - 1) {
                onUpdate(item)
            } else {
                item
            }
        }
        _processAppItems.value = newList
    }
}
