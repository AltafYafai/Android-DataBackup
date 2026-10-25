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
import androidx.compose.material.icons.rounded.ErrorOutline
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
        executeOperation(isBackup = true, types = targets)
    }

    fun restoreSelected() {
        val targets = _selectedTypes.value
        if (targets.isEmpty()) return
        executeOperation(isBackup = false, types = targets)
    }

    fun backupSingle(type: SystemDataType) {
        executeOperation(isBackup = true, types = setOf(type))
    }

    fun restoreSingle(type: SystemDataType) {
        executeOperation(isBackup = false, types = setOf(type))
    }

    private fun executeOperation(isBackup: Boolean, types: Set<SystemDataType>) {
        viewModelScope.launch {
            _isProcessing.value = true
            _processingCompleted.value = false
            _processingSuccess.value = true
            _processingTitle.value = if (isBackup) "Backing up System Data..." else "Restoring System Data..."

            val results = mutableListOf<String>()
            var overallSuccess = true

            types.forEach { type ->
                when (type) {
                    SystemDataType.CALL_LOG -> {
                        _processingMessage.value = if (isBackup) "Processing Call Logs..." else "Restoring Call Logs..."
                        val res = if (isBackup) {
                            systemDataRepo.backupCallLogs { _processingMessage.value = it }
                        } else {
                            systemDataRepo.restoreCallLogs { _processingMessage.value = it }
                        }
                        res.onSuccess { count ->
                            results.add("✓ Call Logs: $count ${if (isBackup) "backed up" else "restored"}")
                        }.onFailure {
                            overallSuccess = false
                            results.add("✗ Call Logs failed: ${it.localizedMessage ?: "Unknown error"}")
                        }
                    }

                    SystemDataType.SMS -> {
                        _processingMessage.value = if (isBackup) "Processing SMS Messages..." else "Restoring SMS Messages..."
                        val res = if (isBackup) {
                            systemDataRepo.backupSms { _processingMessage.value = it }
                        } else {
                            systemDataRepo.restoreSms { _processingMessage.value = it }
                        }
                        res.onSuccess { count ->
                            results.add("✓ SMS: $count ${if (isBackup) "backed up" else "restored"}")
                        }.onFailure {
                            overallSuccess = false
                            results.add("✗ SMS failed: ${it.localizedMessage ?: "Unknown error"}")
                        }
                    }

                    SystemDataType.CONTACTS -> {
                        _processingMessage.value = if (isBackup) "Exporting Contacts (vCard & JSON)..." else "Restoring Contacts..."
                        val res = if (isBackup) {
                            systemDataRepo.backupContacts { _processingMessage.value = it }
                        } else {
                            systemDataRepo.restoreContacts { _processingMessage.value = it }
                        }
                        res.onSuccess { count ->
                            results.add("✓ Contacts: $count ${if (isBackup) "exported" else "restored"}")
                        }.onFailure {
                            overallSuccess = false
                            results.add("✗ Contacts failed: ${it.localizedMessage ?: "Unknown error"}")
                        }
                    }

                    SystemDataType.WIFI -> {
                        _processingMessage.value = if (isBackup) "Saving Wi-Fi Configurations..." else "Restoring Wi-Fi Configurations..."
                        val res = if (isBackup) {
                            systemDataRepo.backupWifi { _processingMessage.value = it }
                        } else {
                            systemDataRepo.restoreWifi { _processingMessage.value = it }
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

    val allSelected = selectedTypes.size == 4

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
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
                        onClick = { viewModel.restoreSelected() },
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
            verticalArrangement = Arrangement.spacedBy(SizeTokens.Level16)
        ) {
            Spacer(modifier = Modifier.height(SizeTokens.Level4))

            // Modern Hero Banner
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = ThemedColorSchemeKeyTokens.PrimaryContainer.value.copy(alpha = 0.35f),
                border = BorderStroke(1.2.dp, ThemedColorSchemeKeyTokens.Primary.value.copy(alpha = 0.25f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(SizeTokens.Level16),
                    horizontalArrangement = Arrangement.spacedBy(SizeTokens.Level16),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(48.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = ThemedColorSchemeKeyTokens.Primary.value.copy(alpha = 0.15f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Key,
                                contentDescription = null,
                                tint = ThemedColorSchemeKeyTokens.Primary.value,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "System Data Vault",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = ThemedColorSchemeKeyTokens.OnSurface.value
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Direct, root-accelerated snapshots of telephony logs, messages, contacts and saved Wi-Fi networks.",
                            style = MaterialTheme.typography.bodySmall,
                            color = ThemedColorSchemeKeyTokens.OnSurfaceVariant.value,
                            lineHeight = 16.sp
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
                    onRestore = { viewModel.restoreSingle(category.type) },
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
                    verticalArrangement = Arrangement.spacedBy(SizeTokens.Level14),
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
    val lastBackupStr = if (category.lastBackupTimestamp > 0) {
        DateUtil.getShortRelativeTimeSpanString(context, category.lastBackupTimestamp, System.currentTimeMillis())
    } else {
        "Never backed up"
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = ThemedColorSchemeKeyTokens.SurfaceContainerLowest.value,
        border = BorderStroke(
            if (isSelected) 1.5.dp else 1.dp,
            if (isSelected) ThemedColorSchemeKeyTokens.Primary.value.copy(alpha = 0.6f) else ThemedColorSchemeKeyTokens.OutlineVariant.value.copy(alpha = 0.35f)
        ),
        tonalElevation = SizeTokens.Level1,
        shadowElevation = SizeTokens.Level1,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SizeTokens.Level16),
            verticalArrangement = Arrangement.spacedBy(SizeTokens.Level12)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(SizeTokens.Level12)
            ) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelect() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = ThemedColorSchemeKeyTokens.Primary.value
                    )
                )

                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = iconBg
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = category.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ThemedColorSchemeKeyTokens.OnSurface.value
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${category.countOnDevice} on device • $lastBackupStr",
                        style = MaterialTheme.typography.bodySmall,
                        color = ThemedColorSchemeKeyTokens.OnSurfaceVariant.value,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (category.backedUpCount > 0) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF2E7D32).copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, Color(0xFF2E7D32).copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = "${category.backedUpCount} saved",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Action row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(SizeTokens.Level8, Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onExtraAction != null) {
                    OutlinedButton(
                        onClick = onExtraAction,
                        shape = CircleShape,
                        border = BorderStroke(1.dp, ThemedColorSchemeKeyTokens.OutlineVariant.value.copy(alpha = 0.4f))
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Visibility,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = ThemedColorSchemeKeyTokens.Primary.value
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "View Passwords",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = ThemedColorSchemeKeyTokens.Primary.value
                        )
                    }
                }

                OutlinedButton(
                    onClick = onRestore,
                    shape = CircleShape,
                    enabled = category.backedUpCount > 0,
                    border = BorderStroke(1.dp, ThemedColorSchemeKeyTokens.OutlineVariant.value.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "Restore",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (category.backedUpCount > 0) ThemedColorSchemeKeyTokens.Primary.value else ThemedColorSchemeKeyTokens.Outline.value
                    )
                }

                Button(
                    onClick = onBackup,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ThemedColorSchemeKeyTokens.Primary.value,
                        contentColor = ThemedColorSchemeKeyTokens.OnPrimary.value
                    )
                ) {
                    Text(
                        text = "Backup",
                        style = MaterialTheme.typography.labelMedium,
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
