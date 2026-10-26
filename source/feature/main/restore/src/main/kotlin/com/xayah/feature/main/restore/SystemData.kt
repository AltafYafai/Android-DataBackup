package com.xayah.feature.main.restore

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Message
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.xayah.core.data.repository.SystemDataRepo
import com.xayah.core.model.SystemBackupSnapshot
import com.xayah.core.model.SystemDataCategoryInfo
import com.xayah.core.model.SystemDataType
import com.xayah.core.model.WifiNetworkItem
import com.xayah.core.ui.theme.ThemedColorSchemeKeyTokens
import com.xayah.core.ui.theme.value
import com.xayah.core.ui.token.SizeTokens
import com.xayah.core.ui.util.LocalNavController
import com.xayah.core.util.DateUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SystemDataViewModel @Inject constructor(
    private val systemDataRepo: SystemDataRepo,
) : ViewModel() {

    private val _categories = MutableStateFlow<List<SystemDataCategoryInfo>>(emptyList())
    val categories: StateFlow<List<SystemDataCategoryInfo>> = _categories.asStateFlow()

    private val _selectedTypes = MutableStateFlow<Set<SystemDataType>>(
        setOf(SystemDataType.CALL_LOG, SystemDataType.SMS, SystemDataType.CONTACTS, SystemDataType.WIFI)
    )
    val selectedTypes: StateFlow<Set<SystemDataType>> = _selectedTypes.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _processingTitle = MutableStateFlow("")
    val processingTitle: StateFlow<String> = _processingTitle.asStateFlow()

    private val _processingMessage = MutableStateFlow("")
    val processingMessage: StateFlow<String> = _processingMessage.asStateFlow()

    private val _processingCompleted = MutableStateFlow(false)
    val processingCompleted: StateFlow<Boolean> = _processingCompleted.asStateFlow()

    private val _processingSuccess = MutableStateFlow(true)
    val processingSuccess: StateFlow<Boolean> = _processingSuccess.asStateFlow()

    private val _wifiNetworks = MutableStateFlow<List<WifiNetworkItem>?>(null)
    val wifiNetworks: StateFlow<List<WifiNetworkItem>?> = _wifiNetworks.asStateFlow()

    // Restore timestamp selection state
    private val _restoreTargetType = MutableStateFlow<SystemDataType?>(null)
    val restoreTargetType: StateFlow<SystemDataType?> = _restoreTargetType.asStateFlow()

    private val _isBatchRestore = MutableStateFlow(false)
    val isBatchRestore: StateFlow<Boolean> = _isBatchRestore.asStateFlow()

    private val _availableSnapshots = MutableStateFlow<List<SystemBackupSnapshot>>(emptyList())
    val availableSnapshots: StateFlow<List<SystemBackupSnapshot>> = _availableSnapshots.asStateFlow()

    private val _selectedTimestamp = MutableStateFlow<Long?>(null)
    val selectedTimestamp: StateFlow<Long?> = _selectedTimestamp.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            val cats = systemDataRepo.loadCategoriesInfo()
            _categories.value = cats
            _isLoading.value = false
        }
    }

    fun toggleSelection(type: SystemDataType) {
        val current = _selectedTypes.value.toMutableSet()
        if (current.contains(type)) {
            current.remove(type)
        } else {
            current.add(type)
        }
        _selectedTypes.value = current
    }

    fun selectAll(select: Boolean) {
        if (select) {
            _selectedTypes.value = setOf(SystemDataType.CALL_LOG, SystemDataType.SMS, SystemDataType.CONTACTS, SystemDataType.WIFI)
        } else {
            _selectedTypes.value = emptySet()
        }
    }

    fun backupSelected() {
        val targets = _selectedTypes.value
        if (targets.isEmpty()) return
        val timestamp = System.currentTimeMillis()
        executeOperation(isBackup = true, types = targets, timestamp = timestamp)
    }

    fun backupSingle(type: SystemDataType) {
        val timestamp = System.currentTimeMillis()
        executeOperation(isBackup = true, types = setOf(type), timestamp = timestamp)
    }

    fun openRestoreSingle(type: SystemDataType) {
        viewModelScope.launch {
            _isLoading.value = true
            val snaps = systemDataRepo.getSnapshotsForType(type)
            _availableSnapshots.value = snaps
            _selectedTimestamp.value = snaps.firstOrNull()?.timestamp
            _restoreTargetType.value = type
            _isBatchRestore.value = false
            _isLoading.value = false
        }
    }

    fun openRestoreBatch() {
        val targets = _selectedTypes.value
        if (targets.isEmpty()) return
        viewModelScope.launch {
            _isLoading.value = true
            val allSnaps = systemDataRepo.listSnapshots()
            val filtered = allSnaps.filter { snap ->
                targets.any { snap.hasType(it) }
            }
            _availableSnapshots.value = filtered
            _selectedTimestamp.value = filtered.firstOrNull()?.timestamp
            _isBatchRestore.value = true
            _restoreTargetType.value = null
            _isLoading.value = false
        }
    }

    fun selectTimestamp(timestamp: Long) {
        _selectedTimestamp.value = timestamp
    }

    fun dismissRestoreDialog() {
        _restoreTargetType.value = null
        _isBatchRestore.value = false
        _availableSnapshots.value = emptyList()
        _selectedTimestamp.value = null
    }

    fun deleteSnapshot(timestamp: Long) {
        viewModelScope.launch {
            systemDataRepo.deleteSnapshot(timestamp)
            val type = _restoreTargetType.value
            val isBatch = _isBatchRestore.value
            val updated = if (type != null) {
                systemDataRepo.getSnapshotsForType(type)
            } else if (isBatch) {
                val targets = _selectedTypes.value
                systemDataRepo.listSnapshots().filter { snap -> targets.any { snap.hasType(it) } }
            } else {
                emptyList()
            }
            _availableSnapshots.value = updated
            if (_selectedTimestamp.value == timestamp) {
                _selectedTimestamp.value = updated.firstOrNull()?.timestamp
            }
            refresh()
        }
    }

    fun confirmRestore() {
        val ts = _selectedTimestamp.value ?: return
        val targetType = _restoreTargetType.value
        val isBatch = _isBatchRestore.value
        dismissRestoreDialog()

        if (targetType != null) {
            executeOperation(isBackup = false, types = setOf(targetType), timestamp = ts)
        } else if (isBatch) {
            val targets = _selectedTypes.value
            executeOperation(isBackup = false, types = targets, timestamp = ts)
        }
    }

    private fun executeOperation(isBackup: Boolean, types: Set<SystemDataType>, timestamp: Long) {
        viewModelScope.launch {
            _isProcessing.value = true
            _processingCompleted.value = false
            _processingSuccess.value = true
            _processingTitle.value = if (isBackup) "Backing up System Data..." else "Restoring System Data..."

            val results = mutableListOf<String>()
            var overallSuccess = true

            // For restore, find snapshot to know what was saved at this timestamp
            val snapshot = if (!isBackup) {
                systemDataRepo.listSnapshots().firstOrNull { it.timestamp == timestamp }
            } else null

            types.forEach { type ->
                when (type) {
                    SystemDataType.CALL_LOG -> {
                        if (!isBackup && snapshot != null && !snapshot.hasType(type)) {
                            results.add("ℹ Call Logs: Not saved in this backup timestamp")
                            return@forEach
                        }
                        _processingMessage.value = if (isBackup) "Processing Call Logs..." else "Restoring Call Logs..."
                        val res = if (isBackup) {
                            systemDataRepo.backupCallLogs(timestamp = timestamp) { _processingMessage.value = it }
                        } else {
                            systemDataRepo.restoreCallLogs(timestamp = timestamp) { _processingMessage.value = it }
                        }
                        res.onSuccess { count ->
                            results.add("✓ Call Logs: $count ${if (isBackup) "backed up" else "restored"}")
                        }.onFailure {
                            overallSuccess = false
                            results.add("✗ Call Logs failed: ${it.localizedMessage ?: "Unknown error"}")
                        }
                    }

                    SystemDataType.SMS -> {
                        if (!isBackup && snapshot != null && !snapshot.hasType(type)) {
                            results.add("ℹ SMS: Not saved in this backup timestamp")
                            return@forEach
                        }
                        _processingMessage.value = if (isBackup) "Processing SMS Messages..." else "Restoring SMS Messages..."
                        val res = if (isBackup) {
                            systemDataRepo.backupSms(timestamp = timestamp) { _processingMessage.value = it }
                        } else {
                            systemDataRepo.restoreSms(timestamp = timestamp) { _processingMessage.value = it }
                        }
                        res.onSuccess { count ->
                            results.add("✓ SMS: $count ${if (isBackup) "backed up" else "restored"}")
                        }.onFailure {
                            overallSuccess = false
                            results.add("✗ SMS failed: ${it.localizedMessage ?: "Unknown error"}")
                        }
                    }

                    SystemDataType.CONTACTS -> {
                        if (!isBackup && snapshot != null && !snapshot.hasType(type)) {
                            results.add("ℹ Contacts: Not saved in this backup timestamp")
                            return@forEach
                        }
                        _processingMessage.value = if (isBackup) "Exporting Contacts (vCard & JSON)..." else "Restoring Contacts..."
                        val res = if (isBackup) {
                            systemDataRepo.backupContacts(timestamp = timestamp) { _processingMessage.value = it }
                        } else {
                            systemDataRepo.restoreContacts(timestamp = timestamp) { _processingMessage.value = it }
                        }
                        res.onSuccess { count ->
                            results.add("✓ Contacts: $count ${if (isBackup) "exported" else "restored"}")
                        }.onFailure {
                            overallSuccess = false
                            results.add("✗ Contacts failed: ${it.localizedMessage ?: "Unknown error"}")
                        }
                    }

                    SystemDataType.WIFI -> {
                        if (!isBackup && snapshot != null && !snapshot.hasType(type)) {
                            results.add("ℹ Wi-Fi: Not saved in this backup timestamp")
                            return@forEach
                        }
                        _processingMessage.value = if (isBackup) "Saving Wi-Fi Configurations..." else "Restoring Wi-Fi Configurations..."
                        val res = if (isBackup) {
                            systemDataRepo.backupWifi(timestamp = timestamp) { _processingMessage.value = it }
                        } else {
                            systemDataRepo.restoreWifi(timestamp = timestamp) { _processingMessage.value = it }
                        }
                        res.onSuccess { count ->
                            results.add("✓ Wi-Fi: $count networks ${if (isBackup) "saved" else "restored"}")
                        }.onFailure {
                            overallSuccess = false
                            results.add("✗ Wi-Fi failed: ${it.localizedMessage ?: "Unknown error"}")
                        }
                    }
                }
            }

            _processingSuccess.value = overallSuccess
            _processingTitle.value = if (overallSuccess) {
                if (isBackup) "Backup Completed" else "Restore Completed"
            } else {
                "Completed with Notices"
            }
            _processingMessage.value = results.joinToString("\n")
            _processingCompleted.value = true

            // Refresh categories info to reflect new counts/timestamps
            refresh()
        }
    }

    fun viewWifiPasswords() {
        viewModelScope.launch {
            _isLoading.value = true
            var list = systemDataRepo.getLiveWifiNetworks()
            if (list.isEmpty()) {
                list = systemDataRepo.getSavedWifiFromBackup()
            }
            _wifiNetworks.value = list
            _isLoading.value = false
        }
    }

    fun dismissWifiDialog() {
        _wifiNetworks.value = null
    }

    fun dismissProcessingDialog() {
        if (_processingCompleted.value) {
            _isProcessing.value = false
        }
    }
}

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
    ExperimentalLayoutApi::class,
    ExperimentalAnimationApi::class
)
@Composable
fun SystemDataScreen(
    viewModel: SystemDataViewModel = hiltViewModel(),
) {
    val navController = LocalNavController.current!!
    val context = LocalContext.current
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val selectedTypes by viewModel.selectedTypes.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isProcessing by viewModel.isProcessing.collectAsStateWithLifecycle()
    val processingTitle by viewModel.processingTitle.collectAsStateWithLifecycle()
    val processingMessage by viewModel.processingMessage.collectAsStateWithLifecycle()
    val processingCompleted by viewModel.processingCompleted.collectAsStateWithLifecycle()
    val processingSuccess by viewModel.processingSuccess.collectAsStateWithLifecycle()
    val wifiNetworks by viewModel.wifiNetworks.collectAsStateWithLifecycle()

    val restoreTargetType by viewModel.restoreTargetType.collectAsStateWithLifecycle()
    val isBatchRestore by viewModel.isBatchRestore.collectAsStateWithLifecycle()
    val availableSnapshots by viewModel.availableSnapshots.collectAsStateWithLifecycle()
    val selectedTimestamp by viewModel.selectedTimestamp.collectAsStateWithLifecycle()

    val allSelected = selectedTypes.size == 4

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = ThemedColorSchemeKeyTokens.Surface.value,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "System Records",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = ThemedColorSchemeKeyTokens.OnSurface.value
                        )
                        Text(
                            text = "Call Logs • SMS • Contacts • Wi-Fi",
                            style = MaterialTheme.typography.bodySmall,
                            color = ThemedColorSchemeKeyTokens.OnSurfaceVariant.value
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = ThemedColorSchemeKeyTokens.OnSurface.value
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.refresh() },
                        enabled = !isLoading && !isProcessing
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = ThemedColorSchemeKeyTokens.Primary.value
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = "Refresh",
                                tint = ThemedColorSchemeKeyTokens.OnSurface.value
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ThemedColorSchemeKeyTokens.Surface.value
                )
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                color = ThemedColorSchemeKeyTokens.SurfaceContainerLowest.value.copy(alpha = 0.95f),
                tonalElevation = SizeTokens.Level6,
                shadowElevation = SizeTokens.Level8,
                border = BorderStroke(Dp(1.2f), ThemedColorSchemeKeyTokens.OutlineVariant.value.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = SizeTokens.Level16, vertical = SizeTokens.Level12),
                    horizontalArrangement = Arrangement.spacedBy(SizeTokens.Level12),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.openRestoreBatch() },
                        enabled = selectedTypes.isNotEmpty() && !isProcessing,
                        shape = CircleShape,
                        border = BorderStroke(1.dp, ThemedColorSchemeKeyTokens.Primary.value.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "Restore (${selectedTypes.size})",
                            fontWeight = FontWeight.SemiBold,
                            color = ThemedColorSchemeKeyTokens.Primary.value
                        )
                    }

                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.backupSelected() },
                        enabled = selectedTypes.isNotEmpty() && !isProcessing,
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ThemedColorSchemeKeyTokens.Primary.value,
                            contentColor = ThemedColorSchemeKeyTokens.OnPrimary.value
                        )
                    ) {
                        Text(
                            text = "Backup (${selectedTypes.size})",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = SizeTokens.Level16)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Spacer(modifier = Modifier.height(SizeTokens.Level4))

            // Modern Compact Header Banner
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = ThemedColorSchemeKeyTokens.PrimaryContainer.value.copy(alpha = 0.25f),
                border = BorderStroke(1.dp, ThemedColorSchemeKeyTokens.Primary.value.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(36.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = ThemedColorSchemeKeyTokens.Primary.value.copy(alpha = 0.15f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Key,
                                contentDescription = null,
                                tint = ThemedColorSchemeKeyTokens.Primary.value,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "System Data Vault",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = ThemedColorSchemeKeyTokens.OnSurface.value
                        )
                        Text(
                            text = "Root-accelerated snapshots with timestamp restore.",
                            style = MaterialTheme.typography.bodySmall,
                            color = ThemedColorSchemeKeyTokens.OnSurfaceVariant.value
                        )
                    }
                }
            }

            // Quick Selection Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Categories",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ThemedColorSchemeKeyTokens.OnSurface.value
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { viewModel.selectAll(!allSelected) }
                ) {
                    Text(
                        text = if (allSelected) "Deselect All" else "Select All",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = ThemedColorSchemeKeyTokens.Primary.value
                    )
                }
            }

            // Category Cards
            categories.forEach { category ->
                val isSelected = selectedTypes.contains(category.type)
                val icon = when (category.type) {
                    SystemDataType.CALL_LOG -> Icons.Rounded.Call
                    SystemDataType.SMS -> Icons.Rounded.Message
                    SystemDataType.CONTACTS -> Icons.Rounded.Contacts
                    SystemDataType.WIFI -> Icons.Rounded.Wifi
                }
                val iconBg = when (category.type) {
                    SystemDataType.CALL_LOG -> Color(0xFF00796B).copy(alpha = 0.15f)
                    SystemDataType.SMS -> Color(0xFF1976D2).copy(alpha = 0.15f)
                    SystemDataType.CONTACTS -> Color(0xFFE65100).copy(alpha = 0.15f)
                    SystemDataType.WIFI -> Color(0xFF7B1FA2).copy(alpha = 0.15f)
                }
                val iconTint = when (category.type) {
                    SystemDataType.CALL_LOG -> Color(0xFF00796B)
                    SystemDataType.SMS -> Color(0xFF1976D2)
                    SystemDataType.CONTACTS -> Color(0xFFE65100)
                    SystemDataType.WIFI -> Color(0xFF7B1FA2)
                }

                SystemDataCategoryCard(
                    category = category,
                    isSelected = isSelected,
                    icon = icon,
                    iconBg = iconBg,
                    iconTint = iconTint,
                    onToggleSelect = { viewModel.toggleSelection(category.type) },
                    onBackup = { viewModel.backupSingle(category.type) },
                    onRestore = { viewModel.openRestoreSingle(category.type) },
                    onExtraAction = if (category.type == SystemDataType.WIFI) {
                        { viewModel.viewWifiPasswords() }
                    } else null
                )
            }

            Spacer(modifier = Modifier.height(SizeTokens.Level32))
        }
    }

    // Processing Dialog
    if (isProcessing) {
        Dialog(
            onDismissRequest = { viewModel.dismissProcessingDialog() },
            properties = DialogProperties(
                dismissOnBackPress = processingCompleted,
                dismissOnClickOutside = processingCompleted
            )
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = ThemedColorSchemeKeyTokens.SurfaceContainerLowest.value,
                tonalElevation = SizeTokens.Level6,
                shadowElevation = SizeTokens.Level8,
                border = BorderStroke(1.2.dp, ThemedColorSchemeKeyTokens.OutlineVariant.value.copy(alpha = 0.35f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = SizeTokens.Level8)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(SizeTokens.Level20),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (!processingCompleted) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp),
                            strokeWidth = 3.5.dp,
                            color = ThemedColorSchemeKeyTokens.Primary.value
                        )
                    } else {
                        Surface(
                            shape = CircleShape,
                            color = if (processingSuccess) Color(0xFF2E7D32).copy(alpha = 0.15f) else ThemedColorSchemeKeyTokens.ErrorContainer.value,
                            modifier = Modifier.size(52.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (processingSuccess) Icons.Rounded.CheckCircle else Icons.Rounded.ErrorOutline,
                                    contentDescription = null,
                                    tint = if (processingSuccess) Color(0xFF2E7D32) else ThemedColorSchemeKeyTokens.OnErrorContainer.value,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = processingTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ThemedColorSchemeKeyTokens.OnSurface.value
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = ThemedColorSchemeKeyTokens.SurfaceContainerHighest.value.copy(alpha = 0.4f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 220.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(SizeTokens.Level12)
                        ) {
                            Text(
                                text = processingMessage.ifEmpty { "Working..." },
                                style = MaterialTheme.typography.bodySmall,
                                color = ThemedColorSchemeKeyTokens.OnSurfaceVariant.value,
                                lineHeight = 18.sp
                            )
                        }
                    }

                    if (processingCompleted) {
                        Button(
                            onClick = { viewModel.dismissProcessingDialog() },
                            shape = CircleShape,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ThemedColorSchemeKeyTokens.Primary.value,
                                contentColor = ThemedColorSchemeKeyTokens.OnPrimary.value
                            )
                        ) {
                            Text(text = "Done", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Wi-Fi Password Viewer Dialog
    if (wifiNetworks != null) {
        WifiPasswordViewerDialog(
            networks = wifiNetworks!!,
            onDismiss = { viewModel.dismissWifiDialog() },
            onCopyPassword = { pass ->
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Wi-Fi Password", pass))
                Toast.makeText(context, "Password copied to clipboard!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Restore Timestamp Selection Dialog
    if (restoreTargetType != null || isBatchRestore) {
        RestoreTimestampDialog(
            targetType = restoreTargetType,
            isBatch = isBatchRestore,
            selectedTypes = selectedTypes,
            snapshots = availableSnapshots,
            selectedTimestamp = selectedTimestamp,
            onSelectTimestamp = { viewModel.selectTimestamp(it) },
            onDeleteSnapshot = { viewModel.deleteSnapshot(it) },
            onConfirm = { viewModel.confirmRestore() },
            onDismiss = { viewModel.dismissRestoreDialog() }
        )
    }
}

@Composable
private fun SystemDataCategoryCard(
    category: SystemDataCategoryInfo,
    isSelected: Boolean,
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    onToggleSelect: () -> Unit,
    onBackup: () -> Unit,
    onRestore: () -> Unit,
    onExtraAction: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val hasBackups = category.backedUpCount > 0 || category.backupCount > 0
    val lastBackupStr = if (category.lastBackupTimestamp > 0) {
        DateUtil.getShortRelativeTimeSpanString(context, category.lastBackupTimestamp, System.currentTimeMillis())
    } else {
        "Never"
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = ThemedColorSchemeKeyTokens.SurfaceContainerLowest.value,
        border = BorderStroke(
            if (isSelected) 1.5.dp else 1.dp,
            if (isSelected) ThemedColorSchemeKeyTokens.Primary.value.copy(alpha = 0.7f) else ThemedColorSchemeKeyTokens.OutlineVariant.value.copy(alpha = 0.3f)
        ),
        tonalElevation = SizeTokens.Level1,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header: Checkbox + Icon + Title/Subtitle + Backup Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelect() },
                    modifier = Modifier.size(24.dp),
                    colors = CheckboxDefaults.colors(
                        checkedColor = ThemedColorSchemeKeyTokens.Primary.value
                    )
                )

                Surface(
                    modifier = Modifier.size(36.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = iconBg
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = category.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = ThemedColorSchemeKeyTokens.OnSurface.value,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (hasBackups) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF2E7D32).copy(alpha = 0.12f),
                                border = BorderStroke(0.8.dp, Color(0xFF2E7D32).copy(alpha = 0.35f))
                            ) {
                                Text(
                                    text = if (category.backupCount > 1) "${category.backupCount} backups" else "${category.backedUpCount} saved",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2E7D32),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = "${category.countOnDevice} on device • $lastBackupStr",
                        style = MaterialTheme.typography.bodySmall,
                        color = ThemedColorSchemeKeyTokens.OnSurfaceVariant.value,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Action Row: Clean, compact buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onExtraAction != null) {
                    TextButton(
                        onClick = onExtraAction,
                        modifier = Modifier.height(30.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Visibility,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = ThemedColorSchemeKeyTokens.Primary.value
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Passwords",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = ThemedColorSchemeKeyTokens.Primary.value
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }

                OutlinedButton(
                    onClick = onRestore,
                    modifier = Modifier.height(30.dp),
                    shape = RoundedCornerShape(8.dp),
                    enabled = hasBackups,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    border = BorderStroke(1.dp, if (hasBackups) ThemedColorSchemeKeyTokens.Primary.value.copy(alpha = 0.4f) else ThemedColorSchemeKeyTokens.OutlineVariant.value.copy(alpha = 0.2f))
                ) {
                    Text(
                        text = "Restore",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (hasBackups) ThemedColorSchemeKeyTokens.Primary.value else ThemedColorSchemeKeyTokens.Outline.value
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Button(
                    onClick = onBackup,
                    modifier = Modifier.height(30.dp),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ThemedColorSchemeKeyTokens.Primary.value,
                        contentColor = ThemedColorSchemeKeyTokens.OnPrimary.value
                    )
                ) {
                    Text(
                        text = "Backup",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun WifiPasswordViewerDialog(
    networks: List<WifiNetworkItem>,
    onDismiss: () -> Unit,
    onCopyPassword: (String) -> Unit,
) {
    var searchQuery by remember { mutableStateOf("") }
    val filtered = remember(searchQuery, networks) {
        if (searchQuery.isBlank()) networks
        else networks.filter { it.ssid.contains(searchQuery, ignoreCase = true) }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SizeTokens.Level16, vertical = SizeTokens.Level24)
                .heightIn(max = 600.dp),
            shape = RoundedCornerShape(24.dp),
            color = ThemedColorSchemeKeyTokens.SurfaceContainerLowest.value,
            tonalElevation = SizeTokens.Level6,
            shadowElevation = SizeTokens.Level8,
            border = BorderStroke(1.2.dp, ThemedColorSchemeKeyTokens.OutlineVariant.value.copy(alpha = 0.35f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(SizeTokens.Level16),
                verticalArrangement = Arrangement.spacedBy(SizeTokens.Level12)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Saved Wi-Fi Passwords",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = ThemedColorSchemeKeyTokens.OnSurface.value
                        )
                        Text(
                            text = "${networks.size} networks recorded",
                            style = MaterialTheme.typography.bodySmall,
                            color = ThemedColorSchemeKeyTokens.OnSurfaceVariant.value
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = ThemedColorSchemeKeyTokens.OnSurfaceVariant.value
                        )
                    }
                }

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search Wi-Fi name…", fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = CircleShape,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ThemedColorSchemeKeyTokens.Primary.value,
                        unfocusedBorderColor = ThemedColorSchemeKeyTokens.OutlineVariant.value.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                )

                // List
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filtered) { net ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = ThemedColorSchemeKeyTokens.SurfaceContainerHighest.value.copy(alpha = 0.35f),
                            border = BorderStroke(1.dp, ThemedColorSchemeKeyTokens.OutlineVariant.value.copy(alpha = 0.2f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = SizeTokens.Level12, vertical = SizeTokens.Level10),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(SizeTokens.Level10)
                            ) {
                                Surface(
                                    modifier = Modifier.size(36.dp),
                                    shape = CircleShape,
                                    color = Color(0xFF7B1FA2).copy(alpha = 0.15f)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Rounded.Wifi,
                                            contentDescription = null,
                                            tint = Color(0xFF7B1FA2),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = net.ssid,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = ThemedColorSchemeKeyTokens.OnSurface.value,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = ThemedColorSchemeKeyTokens.SurfaceContainerHighest.value
                                        ) {
                                            Text(
                                                text = net.securityType,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = ThemedColorSchemeKeyTokens.OnSurfaceVariant.value,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }

                                    val password = net.password
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = password ?: "(Open / No Password)",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = if (password != null) FontWeight.Medium else FontWeight.Normal,
                                        color = if (password != null) ThemedColorSchemeKeyTokens.Primary.value else ThemedColorSchemeKeyTokens.Outline.value,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                val password = net.password
                                if (password != null) {
                                    IconButton(
                                        onClick = { onCopyPassword(password) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.ContentCopy,
                                            contentDescription = "Copy Password",
                                            tint = ThemedColorSchemeKeyTokens.Primary.value,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (filtered.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = SizeTokens.Level32),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No Wi-Fi networks found.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = ThemedColorSchemeKeyTokens.OnSurfaceVariant.value
                                )
                            }
                        }
                    }
                }

                // Done Button
                Button(
                    onClick = onDismiss,
                    shape = CircleShape,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ThemedColorSchemeKeyTokens.Primary.value,
                        contentColor = ThemedColorSchemeKeyTokens.OnPrimary.value
                    )
                ) {
                    Text(text = "Close", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun RestoreTimestampDialog(
    targetType: SystemDataType?,
    isBatch: Boolean,
    selectedTypes: Set<SystemDataType>,
    snapshots: List<SystemBackupSnapshot>,
    selectedTimestamp: Long?,
    onSelectTimestamp: (Long) -> Unit,
    onDeleteSnapshot: (Long) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var snapshotToDelete by remember { mutableStateOf<Long?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SizeTokens.Level16, vertical = SizeTokens.Level24)
                .heightIn(max = 580.dp),
            shape = RoundedCornerShape(24.dp),
            color = ThemedColorSchemeKeyTokens.SurfaceContainerLowest.value,
            tonalElevation = SizeTokens.Level6,
            shadowElevation = SizeTokens.Level8,
            border = BorderStroke(1.2.dp, ThemedColorSchemeKeyTokens.OutlineVariant.value.copy(alpha = 0.35f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(SizeTokens.Level20),
                verticalArrangement = Arrangement.spacedBy(SizeTokens.Level12)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(SizeTokens.Level8)
                    ) {
                        Surface(
                            modifier = Modifier.size(36.dp),
                            shape = CircleShape,
                            color = ThemedColorSchemeKeyTokens.Primary.value.copy(alpha = 0.12f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.History,
                                    contentDescription = null,
                                    tint = ThemedColorSchemeKeyTokens.Primary.value,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Column {
                            Text(
                                text = if (targetType != null) "Restore ${targetType.displayName}" else "Restore System Data",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = ThemedColorSchemeKeyTokens.OnSurface.value
                            )
                            Text(
                                text = "Choose a backup timestamp to restore",
                                style = MaterialTheme.typography.bodySmall,
                                color = ThemedColorSchemeKeyTokens.OnSurfaceVariant.value
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = ThemedColorSchemeKeyTokens.OnSurfaceVariant.value
                        )
                    }
                }

                if (snapshots.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .padding(vertical = SizeTokens.Level32),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No saved backups found for this category.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ThemedColorSchemeKeyTokens.OnSurfaceVariant.value
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(snapshots) { snap ->
                            val isSelected = snap.timestamp == selectedTimestamp
                            val relativeTime = DateUtil.getShortRelativeTimeSpanString(context, snap.timestamp, System.currentTimeMillis())
                            val formattedDate = snap.dateFormatted.ifEmpty {
                                DateUtil.formatTimestamp(snap.timestamp, DateUtil.PATTERN_YMD_HMS)
                            }

                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelected) {
                                    ThemedColorSchemeKeyTokens.PrimaryContainer.value.copy(alpha = 0.25f)
                                } else {
                                    ThemedColorSchemeKeyTokens.SurfaceContainerHighest.value.copy(alpha = 0.35f)
                                },
                                border = BorderStroke(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) {
                                        ThemedColorSchemeKeyTokens.Primary.value.copy(alpha = 0.7f)
                                    } else {
                                        ThemedColorSchemeKeyTokens.OutlineVariant.value.copy(alpha = 0.3f)
                                    }
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { onSelectTimestamp(snap.timestamp) }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = SizeTokens.Level12, vertical = SizeTokens.Level10),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(SizeTokens.Level8)
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { onSelectTimestamp(snap.timestamp) },
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = ThemedColorSchemeKeyTokens.Primary.value
                                        )
                                    )

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = formattedDate,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = ThemedColorSchemeKeyTokens.OnSurface.value
                                        )
                                        Text(
                                            text = relativeTime,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = ThemedColorSchemeKeyTokens.OnSurfaceVariant.value
                                        )

                                        Spacer(modifier = Modifier.height(4.dp))

                                        // Category badges
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (targetType != null) {
                                                val count = snap.countFor(targetType)
                                                Surface(
                                                    shape = CircleShape,
                                                    color = ThemedColorSchemeKeyTokens.Primary.value.copy(alpha = 0.12f)
                                                ) {
                                                    Text(
                                                        text = "$count saved",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = ThemedColorSchemeKeyTokens.Primary.value,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                    )
                                                }
                                            } else {
                                                if (snap.callLogsCount > 0) {
                                                    MiniBadge(text = "${snap.callLogsCount} Calls", color = Color(0xFF00796B))
                                                }
                                                if (snap.smsCount > 0) {
                                                    MiniBadge(text = "${snap.smsCount} SMS", color = Color(0xFF1976D2))
                                                }
                                                if (snap.contactsCount > 0) {
                                                    MiniBadge(text = "${snap.contactsCount} Contacts", color = Color(0xFFE65100))
                                                }
                                                if (snap.wifiCount > 0) {
                                                    MiniBadge(text = "${snap.wifiCount} Wi-Fi", color = Color(0xFF7B1FA2))
                                                }
                                            }
                                        }
                                    }

                                    IconButton(
                                        onClick = { snapshotToDelete = snap.timestamp },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Delete,
                                            contentDescription = "Delete",
                                            tint = ThemedColorSchemeKeyTokens.Error.value.copy(alpha = 0.7f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(SizeTokens.Level8, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = CircleShape
                    ) {
                        Text(text = "Cancel")
                    }

                    Button(
                        onClick = onConfirm,
                        enabled = selectedTimestamp != null,
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ThemedColorSchemeKeyTokens.Primary.value,
                            contentColor = ThemedColorSchemeKeyTokens.OnPrimary.value
                        )
                    ) {
                        Text(text = "Restore", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (snapshotToDelete != null) {
        Dialog(onDismissRequest = { snapshotToDelete = null }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = ThemedColorSchemeKeyTokens.SurfaceContainerLowest.value,
                tonalElevation = SizeTokens.Level6,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Delete Backup?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ThemedColorSchemeKeyTokens.OnSurface.value
                    )
                    Text(
                        text = "Are you sure you want to delete this backup snapshot? This cannot be undone.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ThemedColorSchemeKeyTokens.OnSurfaceVariant.value
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                    ) {
                        OutlinedButton(onClick = { snapshotToDelete = null }, shape = CircleShape) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                val toDel = snapshotToDelete
                                snapshotToDelete = null
                                if (toDel != null) onDeleteSnapshot(toDel)
                            },
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ThemedColorSchemeKeyTokens.Error.value,
                                contentColor = ThemedColorSchemeKeyTokens.OnError.value
                            )
                        ) {
                            Text("Delete", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniBadge(text: String, color: Color) {
    Surface(
        shape = CircleShape,
        color = color.copy(alpha = 0.12f)
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
