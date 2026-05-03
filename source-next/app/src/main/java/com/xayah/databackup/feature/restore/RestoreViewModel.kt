package com.xayah.databackup.feature.restore

import androidx.lifecycle.viewModelScope
import com.xayah.databackup.data.BackupConfigRepository
import com.xayah.databackup.data.RestoreProcessRepository
import com.xayah.databackup.data.RestoreRepository
import com.xayah.databackup.entity.BackupConfig
import com.xayah.databackup.util.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RestoreUiState(
    val isLoading: Boolean = false,
    val configs: List<BackupConfig> = listOf(),
    val selectedIndex: Int = -1,
    val appsCount: Pair<Int, Int> = 0 to 0,
    val networksCount: Pair<Int, Int> = 0 to 0,
    val contactsCount: Pair<Int, Int> = 0 to 0,
    val callLogsCount: Pair<Int, Int> = 0 to 0,
    val messagesCount: Pair<Int, Int> = 0 to 0,
)

class RestoreViewModel(
    private val backupConfigRepo: BackupConfigRepository,
    private val restoreRepo: RestoreRepository,
    private val restoreProcessRepo: RestoreProcessRepository,
) : BaseViewModel() {
    private val _uiState = MutableStateFlow(RestoreUiState())
    val uiState: StateFlow<RestoreUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            backupConfigRepo.configs.collect { configs ->
                _uiState.update { it.copy(configs = configs) }
            }
        }

        viewModelScope.launch {
            combine(
                restoreRepo.restoreApps,
                restoreRepo.restoreAppsSelected,
                restoreRepo.restoreNetworks,
                restoreRepo.restoreNetworksSelected,
                restoreRepo.restoreContacts,
                restoreRepo.restoreContactsSelected,
                restoreRepo.restoreCallLogs,
                restoreRepo.restoreCallLogsSelected,
                restoreRepo.restoreSmsList,
                restoreRepo.restoreSmsListSelected,
                restoreRepo.restoreMmsList,
                restoreRepo.restoreMmsListSelected,
            ) { args ->
                val apps = args[0] as List<*>
                val appsSelected = args[1] as List<*>
                val networks = args[2] as List<*>
                val networksSelected = args[3] as List<*>
                val contacts = args[4] as List<*>
                val contactsSelected = args[5] as List<*>
                val callLogs = args[6] as List<*>
                val callLogsSelected = args[7] as List<*>
                val sms = args[8] as List<*>
                val smsSelected = args[9] as List<*>
                val mms = args[10] as List<*>
                val mmsSelected = args[11] as List<*>

                _uiState.update {
                    it.copy(
                        appsCount = appsSelected.size to apps.size,
                        networksCount = networksSelected.size to networks.size,
                        contactsCount = contactsSelected.size to contacts.size,
                        callLogsCount = callLogsSelected.size to callLogs.size,
                        messagesCount = (smsSelected.size + mmsSelected.size) to (sms.size + mms.size)
                    )
                }
            }.collect {}
        }
    }

    fun loadConfigs() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            backupConfigRepo.loadBackupConfigsFromLocal()
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun selectBackup(index: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(selectedIndex = index) }
            if (index >= 0 && index < _uiState.value.configs.size) {
                restoreRepo.selectConfig(_uiState.value.configs[index])
            } else {
                restoreRepo.selectConfig(null)
            }
        }
    }

    fun resetProcessRepo() {
        restoreProcessRepo.reset()
    }
}
