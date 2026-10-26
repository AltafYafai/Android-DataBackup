package com.xayah.feature.main.history

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Circle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xayah.core.model.DataType
import com.xayah.core.model.OpType
import com.xayah.core.model.OperationState
import com.xayah.core.model.TaskType
import com.xayah.core.model.database.Info
import com.xayah.core.model.database.ProcessingInfoEntity
import com.xayah.core.model.database.TaskDetailMediaEntity
import com.xayah.core.model.database.TaskDetailPackageEntity
import com.xayah.core.model.database.TaskEntity
import com.xayah.core.model.util.formatSize
import com.xayah.core.ui.component.Divider
import com.xayah.core.ui.component.InnerBottomSpacer
import com.xayah.core.ui.component.InnerTopSpacer
import com.xayah.core.ui.component.PackageIconImage
import com.xayah.core.ui.component.SecondaryLargeTopBar
import com.xayah.core.ui.theme.ThemedColorSchemeKeyTokens
import com.xayah.core.ui.theme.value
import com.xayah.core.ui.token.AnimationTokens
import com.xayah.core.ui.token.SizeTokens
import com.xayah.core.ui.util.LocalNavController
import com.xayah.core.util.DateUtil
import com.xayah.core.util.maybePopBackStack

enum class DetailFilter {
    ALL,
    FAILED,
    SUCCEEDED
}

data class PartitionDetail(
    val title: String,
    val info: Info,
    val dataType: DataType
)

fun TaskDetailPackageEntity.getActivePartitions(): List<PartitionDetail> {
    val list = mutableListOf<PartitionDetail>()
    if (apkInfo.state != OperationState.IDLE || apkInfo.bytes > 0 || apkInfo.log.isNotEmpty()) {
        list.add(PartitionDetail("APK", apkInfo, DataType.PACKAGE_APK))
    }
    if (userInfo.state != OperationState.IDLE || userInfo.bytes > 0 || userInfo.log.isNotEmpty()) {
        list.add(PartitionDetail("User Data", userInfo, DataType.PACKAGE_USER))
    }
    if (userDeInfo.state != OperationState.IDLE || userDeInfo.bytes > 0 || userDeInfo.log.isNotEmpty()) {
        list.add(PartitionDetail("DE Data", userDeInfo, DataType.PACKAGE_USER_DE))
    }
    if (dataInfo.state != OperationState.IDLE || dataInfo.bytes > 0 || dataInfo.log.isNotEmpty()) {
        list.add(PartitionDetail("Ext Data", dataInfo, DataType.PACKAGE_DATA))
    }
    if (obbInfo.state != OperationState.IDLE || obbInfo.bytes > 0 || obbInfo.log.isNotEmpty()) {
        list.add(PartitionDetail("OBB", obbInfo, DataType.PACKAGE_OBB))
    }
    if (mediaInfo.state != OperationState.IDLE || mediaInfo.bytes > 0 || mediaInfo.log.isNotEmpty()) {
        list.add(PartitionDetail("Media", mediaInfo, DataType.PACKAGE_MEDIA))
    }
    return list
}

@Composable
fun TaskDetailsRoute(
    viewModel: TaskDetailsViewModel = hiltViewModel(),
) {
    val navController = LocalNavController.current!!
    val uiState: TaskDetailsUiState by viewModel.uiState.collectAsStateWithLifecycle()
    TaskDetailsScreen(uiState)
    LaunchedEffect(uiState) {
        if (uiState is TaskDetailsUiState.Error) {
            navController.maybePopBackStack()
        }
    }
}

@SuppressLint("StringFormatInvalid")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TaskDetailsScreen(uiState: TaskDetailsUiState) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    var selectedFilter by remember { mutableStateOf(DetailFilter.ALL) }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            SecondaryLargeTopBar(
                scrollBehavior = scrollBehavior,
                title = stringResource(R.string.task_details),
            )
        }
    ) { innerPadding ->
        AnimatedContent(uiState, label = AnimationTokens.AnimatedContentLabel) { state ->
            when (state) {
                is TaskDetailsUiState.Success -> {
                    val filteredAppList = remember(state.appInfoList, selectedFilter) {
                        when (selectedFilter) {
                            DetailFilter.ALL -> state.appInfoList
                            DetailFilter.FAILED -> state.appInfoList.filter { !it.isSuccess }
                            DetailFilter.SUCCEEDED -> state.appInfoList.filter { it.isSuccess }
                        }
                    }

                    val filteredFileList = remember(state.fileInfoList, selectedFilter) {
                        when (selectedFilter) {
                            DetailFilter.ALL -> state.fileInfoList
                            DetailFilter.FAILED -> state.fileInfoList.filter { !it.isSuccess }
                            DetailFilter.SUCCEEDED -> state.fileInfoList.filter { it.isSuccess }
                        }
                    }

                    val totalItemCount = state.appInfoList.size + state.fileInfoList.size
                    val failedItemCount = state.appInfoList.count { !it.isSuccess } + state.fileInfoList.count { !it.isSuccess }
                    val succeededItemCount = state.appInfoList.count { it.isSuccess } + state.fileInfoList.count { it.isSuccess }

                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        item {
                            InnerTopSpacer(innerPadding = innerPadding)
                        }

                        // Hero Summary Card
                        item {
                            TaskSummaryCard(
                                task = state.task,
                                totalCount = totalItemCount,
                                successCount = state.task.successCount.takeIf { it > 0 } ?: succeededItemCount,
                                failureCount = state.task.failureCount.takeIf { it > 0 } ?: failedItemCount
                            )
                        }

                        // Filter Chips Bar (if multiple items exist)
                        if (totalItemCount > 0) {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = SizeTokens.Level16, vertical = SizeTokens.Level4),
                                    horizontalArrangement = Arrangement.spacedBy(SizeTokens.Level8),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    FilterChip(
                                        selected = selectedFilter == DetailFilter.ALL,
                                        onClick = { selectedFilter = DetailFilter.ALL },
                                        label = { Text("All ($totalItemCount)") },
                                        shape = RoundedCornerShape(10.dp)
                                    )

                                    if (failedItemCount > 0) {
                                        FilterChip(
                                            selected = selectedFilter == DetailFilter.FAILED,
                                            onClick = { selectedFilter = DetailFilter.FAILED },
                                            label = {
                                                Text(
                                                    text = "Failed ($failedItemCount)",
                                                    color = if (selectedFilter == DetailFilter.FAILED) ThemedColorSchemeKeyTokens.OnErrorContainer.value else ThemedColorSchemeKeyTokens.Error.value
                                                )
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = ThemedColorSchemeKeyTokens.ErrorContainer.value,
                                                selectedLabelColor = ThemedColorSchemeKeyTokens.OnErrorContainer.value
                                            )
                                        )
                                    }

                                    FilterChip(
                                        selected = selectedFilter == DetailFilter.SUCCEEDED,
                                        onClick = { selectedFilter = DetailFilter.SUCCEEDED },
                                        label = { Text("Success ($succeededItemCount)") },
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                }
                            }
                        }

                        // Preprocessing (only in ALL filter or if error)
                        if (state.preprocessingInfoList.isNotEmpty() && (selectedFilter == DetailFilter.ALL || (selectedFilter == DetailFilter.FAILED && state.preprocessingInfoList.any { it.state == OperationState.ERROR }))) {
                            item {
                                ProcessingGroupCard(
                                    title = stringResource(R.string.preprocessing),
                                    items = state.preprocessingInfoList
                                )
                            }
                        }

                        // App Items
                        if (filteredAppList.isNotEmpty()) {
                            item {
                                Text(
                                    text = "${stringResource(R.string.apps)} (${filteredAppList.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = ThemedColorSchemeKeyTokens.OnSurface.value,
                                    modifier = Modifier.padding(horizontal = SizeTokens.Level20, vertical = SizeTokens.Level8)
                                )
                            }
                            items(items = filteredAppList, key = { it.id }) { appItem ->
                                AppDetailCard(item = appItem)
                            }
                        }

                        // File Items
                        if (filteredFileList.isNotEmpty()) {
                            item {
                                Text(
                                    text = "${stringResource(R.string.files)} (${filteredFileList.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = ThemedColorSchemeKeyTokens.OnSurface.value,
                                    modifier = Modifier.padding(horizontal = SizeTokens.Level20, vertical = SizeTokens.Level8)
                                )
                            }
                            items(items = filteredFileList, key = { it.id }) { fileItem ->
                                FileDetailCard(item = fileItem)
                            }
                        }

                        // Post-processing
                        if (state.postProcessingInfoList.isNotEmpty() && (selectedFilter == DetailFilter.ALL || (selectedFilter == DetailFilter.FAILED && state.postProcessingInfoList.any { it.state == OperationState.ERROR }))) {
                            item {
                                ProcessingGroupCard(
                                    title = stringResource(R.string.post_processing),
                                    items = state.postProcessingInfoList
                                )
                            }
                        }

                        // Empty filter message
                        if (filteredAppList.isEmpty() && filteredFileList.isEmpty()) {
                            item {
                                EmptyFilterView(filter = selectedFilter)
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(SizeTokens.Level64))
                            InnerBottomSpacer(innerPadding = innerPadding)
                        }
                    }
                }

                else -> {}
            }
        }
    }
}

@Composable
private fun TaskSummaryCard(
    task: TaskEntity,
    totalCount: Int,
    successCount: Int,
    failureCount: Int
) {
    val isBackup = task.opType == OpType.BACKUP
    val isPackages = task.taskType == TaskType.PACKAGE
    val opTitle = if (isBackup) stringResource(R.string.backup) else stringResource(R.string.restore)
    val typeTitle = if (isPackages) stringResource(R.string.apps) else stringResource(R.string.files)

    val durationMs = (task.endTimestamp - task.startTimestamp).coerceAtLeast(0)
    val durationSec = durationMs / 1000
    val durationStr = if (durationSec >= 60) {
        "${durationSec / 60}m ${durationSec % 60}s"
    } else {
        "${durationSec}s"
    }

    val formattedDate = DateUtil.formatTimestamp(task.startTimestamp, DateUtil.PATTERN_YMD_HMS)
    val totalSizeStr = if (task.totalBytes > 0) task.totalBytes.formatSize() else if (task.rawBytes > 0) task.rawBytes.formatSize() else "0 B"

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SizeTokens.Level16, vertical = SizeTokens.Level8),
        shape = RoundedCornerShape(20.dp),
        color = ThemedColorSchemeKeyTokens.SurfaceContainer.value,
        tonalElevation = 2.dp,
        shadowElevation = 1.dp,
        border = BorderStroke(1.dp, ThemedColorSchemeKeyTokens.OutlineVariant.value.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row: Op Badge + Success/Failure Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = ThemedColorSchemeKeyTokens.PrimaryContainer.value,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(
                                if (isBackup) R.drawable.ic_rounded_acute else R.drawable.ic_rounded_history
                            ),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = ThemedColorSchemeKeyTokens.OnPrimaryContainer.value
                        )
                        Text(
                            text = "$opTitle • $typeTitle",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = ThemedColorSchemeKeyTokens.OnPrimaryContainer.value
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (failureCount == 0) {
                        Color(0xFF4CAF50).copy(alpha = 0.15f)
                    } else {
                        ThemedColorSchemeKeyTokens.ErrorContainer.value.copy(alpha = 0.6f)
                    },
                    border = BorderStroke(
                        1.dp,
                        if (failureCount == 0) Color(0xFF4CAF50).copy(alpha = 0.4f)
                        else ThemedColorSchemeKeyTokens.Error.value.copy(alpha = 0.4f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(
                                if (failureCount == 0) R.drawable.ic_rounded_check_circle else R.drawable.ic_rounded_cancel_circle
                            ),
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = if (failureCount == 0) Color(0xFF2E7D32) else ThemedColorSchemeKeyTokens.Error.value
                        )
                        Text(
                            text = if (failureCount == 0) stringResource(R.string.succeed) else "$failureCount " + stringResource(R.string.failed),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (failureCount == 0) Color(0xFF2E7D32) else ThemedColorSchemeKeyTokens.Error.value
                        )
                    }
                }
            }

            // Timestamp & Duration line
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Schedule,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = ThemedColorSchemeKeyTokens.OnSurfaceVariant.value
                )
                Text(
                    text = "$formattedDate • $durationStr",
                    style = MaterialTheme.typography.bodySmall,
                    color = ThemedColorSchemeKeyTokens.OnSurfaceVariant.value
                )
            }

            // Stats Metric Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatMetricPill(
                    modifier = Modifier.weight(1f),
                    label = "Total Size",
                    value = totalSizeStr,
                    icon = Icons.Rounded.Storage,
                    color = ThemedColorSchemeKeyTokens.Primary.value
                )
                StatMetricPill(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.succeed),
                    value = "$successCount",
                    icon = Icons.Rounded.Check,
                    color = Color(0xFF2E7D32)
                )
                if (failureCount > 0) {
                    StatMetricPill(
                        modifier = Modifier.weight(1f),
                        label = stringResource(R.string.failed),
                        value = "$failureCount",
                        icon = Icons.Rounded.Close,
                        color = ThemedColorSchemeKeyTokens.Error.value
                    )
                }
                StatMetricPill(
                    modifier = Modifier.weight(1f),
                    label = "Total",
                    value = "${task.totalCount.takeIf { it > 0 } ?: totalCount}",
                    icon = ImageVector.vectorResource(R.drawable.ic_rounded_apps),
                    color = ThemedColorSchemeKeyTokens.OnSurfaceVariant.value
                )
            }

            // Backup directory path (if available)
            if (task.backupDir.isNotBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_rounded_folder_open),
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = ThemedColorSchemeKeyTokens.OnSurfaceVariant.value
                    )
                    Text(
                        text = task.backupDir,
                        style = MaterialTheme.typography.labelSmall,
                        color = ThemedColorSchemeKeyTokens.OnSurfaceVariant.value,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun StatMetricPill(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    icon: ImageVector,
    color: Color
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = ThemedColorSchemeKeyTokens.SurfaceContainerHigh.value,
        border = BorderStroke(0.8.dp, ThemedColorSchemeKeyTokens.OutlineVariant.value.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = color
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = color,
                    maxLines = 1
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = ThemedColorSchemeKeyTokens.OnSurfaceVariant.value,
                maxLines = 1
            )
        }
    }
}

@Composable
fun AppDetailCard(
    item: TaskDetailPackageEntity,
) {
    var expanded by remember { mutableStateOf(!item.isSuccess) }
    val context = LocalContext.current

    val activePartitions = remember(item) {
        item.getActivePartitions()
    }
    val appTotalBytes = remember(item) {
        item.apkInfo.bytes + item.userInfo.bytes + item.userDeInfo.bytes + item.dataInfo.bytes + item.obbInfo.bytes + item.mediaInfo.bytes
    }
    val appSizeStr = remember(appTotalBytes) {
        if (appTotalBytes > 0) appTotalBytes.toDouble().formatSize() else ""
    }

    val isSuccess = item.isSuccess

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SizeTokens.Level16, vertical = SizeTokens.Level4)
            .animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        color = ThemedColorSchemeKeyTokens.SurfaceContainerLowest.value,
        tonalElevation = 1.dp,
        shadowElevation = 1.dp,
        border = BorderStroke(
            1.dp,
            if (isSuccess) ThemedColorSchemeKeyTokens.OutlineVariant.value.copy(alpha = 0.35f)
            else ThemedColorSchemeKeyTokens.Error.value.copy(alpha = 0.45f)
        ),
        onClick = { expanded = !expanded }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header: App icon, Title, Subtitle, Status Badge, Expand Arrow
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PackageIconImage(
                    packageName = item.packageEntity.packageName,
                    size = 38.dp
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.packageEntity.packageInfo.label.ifEmpty { item.packageEntity.packageName },
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = ThemedColorSchemeKeyTokens.OnSurface.value,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = item.packageEntity.packageName,
                            style = MaterialTheme.typography.labelSmall,
                            color = ThemedColorSchemeKeyTokens.OnSurfaceVariant.value,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (appSizeStr.isNotEmpty()) {
                            Text(
                                text = "• $appSizeStr",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = ThemedColorSchemeKeyTokens.Primary.value
                            )
                        }
                    }
                }

                // Status Badge Pill
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSuccess) Color(0xFF4CAF50).copy(alpha = 0.12f) else ThemedColorSchemeKeyTokens.ErrorContainer.value.copy(alpha = 0.5f),
                    border = BorderStroke(
                        0.8.dp,
                        if (isSuccess) Color(0xFF4CAF50).copy(alpha = 0.4f) else ThemedColorSchemeKeyTokens.Error.value.copy(alpha = 0.4f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = if (isSuccess) Icons.Rounded.Check else Icons.Rounded.Close,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = if (isSuccess) Color(0xFF2E7D32) else ThemedColorSchemeKeyTokens.Error.value
                        )
                        Text(
                            text = if (isSuccess) stringResource(R.string.succeed) else stringResource(R.string.failed),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            fontWeight = FontWeight.Bold,
                            color = if (isSuccess) Color(0xFF2E7D32) else ThemedColorSchemeKeyTokens.Error.value
                        )
                    }
                }

                // Expand Chevron Icon
                Icon(
                    imageVector = if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = ThemedColorSchemeKeyTokens.OnSurfaceVariant.value
                )
            }

            // Compact Active Partition Pills
            if (activePartitions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    activePartitions.take(4).forEach { part ->
                        PartitionMiniPill(part)
                    }
                    if (activePartitions.size > 4) {
                        Text(
                            text = "+${activePartitions.size - 4}",
                            style = MaterialTheme.typography.labelSmall,
                            color = ThemedColorSchemeKeyTokens.OnSurfaceVariant.value
                        )
                    }
                }
            }

            // Expanded Section: Partition details and logs
            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                Divider()
                Spacer(modifier = Modifier.height(10.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    activePartitions.forEach { part ->
                        PartitionDetailRow(part = part, onCopyLog = { log ->
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Log", log))
                            Toast.makeText(context, "Log copied", Toast.LENGTH_SHORT).show()
                        })
                    }
                }
            }
        }
    }
}

@Composable
private fun PartitionMiniPill(part: PartitionDetail) {
    val isPartDone = part.info.state == OperationState.DONE
    val isPartError = part.info.state == OperationState.ERROR
    val sizeText = if (part.info.bytes > 0) part.info.bytes.toDouble().formatSize() else ""

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = when {
            isPartError -> ThemedColorSchemeKeyTokens.ErrorContainer.value.copy(alpha = 0.4f)
            isPartDone -> ThemedColorSchemeKeyTokens.SurfaceContainerHighest.value.copy(alpha = 0.6f)
            else -> ThemedColorSchemeKeyTokens.SurfaceContainerHighest.value.copy(alpha = 0.3f)
        },
        border = BorderStroke(
            0.6.dp,
            when {
                isPartError -> ThemedColorSchemeKeyTokens.Error.value.copy(alpha = 0.4f)
                else -> ThemedColorSchemeKeyTokens.OutlineVariant.value.copy(alpha = 0.2f)
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .background(
                        color = when {
                            isPartError -> ThemedColorSchemeKeyTokens.Error.value
                            isPartDone -> Color(0xFF4CAF50)
                            else -> ThemedColorSchemeKeyTokens.OnSurfaceVariant.value.copy(alpha = 0.5f)
                        },
                        shape = CircleShape
                    )
            )
            Text(
                text = if (sizeText.isNotEmpty()) "${part.title} $sizeText" else part.title,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = if (isPartError) ThemedColorSchemeKeyTokens.Error.value else ThemedColorSchemeKeyTokens.OnSurface.value
            )
        }
    }
}

@Composable
private fun PartitionDetailRow(
    part: PartitionDetail,
    onCopyLog: (String) -> Unit
) {
    val isPartDone = part.info.state == OperationState.DONE
    val isPartError = part.info.state == OperationState.ERROR
    val sizeText = if (part.info.bytes > 0) part.info.bytes.toDouble().formatSize() else "--"

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = when {
                        isPartError -> Icons.Rounded.Close
                        isPartDone -> Icons.Rounded.Check
                        else -> Icons.Rounded.Circle
                    },
                    contentDescription = null,
                    modifier = Modifier.size(13.dp),
                    tint = when {
                        isPartError -> ThemedColorSchemeKeyTokens.Error.value
                        isPartDone -> Color(0xFF2E7D32)
                        else -> ThemedColorSchemeKeyTokens.OnSurfaceVariant.value
                    }
                )
                Text(
                    text = part.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = ThemedColorSchemeKeyTokens.OnSurface.value
                )
            }

            Text(
                text = sizeText,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = ThemedColorSchemeKeyTokens.OnSurfaceVariant.value
            )
        }

        // If partition has a log
        if (part.info.log.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = if (isPartError) ThemedColorSchemeKeyTokens.ErrorContainer.value.copy(alpha = 0.25f)
                else ThemedColorSchemeKeyTokens.SurfaceContainerHighest.value.copy(alpha = 0.5f),
                border = BorderStroke(
                    0.6.dp,
                    if (isPartError) ThemedColorSchemeKeyTokens.Error.value.copy(alpha = 0.35f)
                    else ThemedColorSchemeKeyTokens.OutlineVariant.value.copy(alpha = 0.2f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = part.info.log.trim(),
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = if (isPartError) ThemedColorSchemeKeyTokens.Error.value
                        else ThemedColorSchemeKeyTokens.OnSurfaceVariant.value,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { onCopyLog(part.info.log) },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ContentCopy,
                            contentDescription = "Copy log",
                            modifier = Modifier.size(12.dp),
                            tint = ThemedColorSchemeKeyTokens.OnSurfaceVariant.value
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FileDetailCard(
    item: TaskDetailMediaEntity,
) {
    var expanded by remember { mutableStateOf(!item.isSuccess) }
    val context = LocalContext.current
    val sizeText = if (item.mediaInfo.bytes > 0) item.mediaInfo.bytes.toDouble().formatSize() else ""
    val isSuccess = item.isSuccess

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SizeTokens.Level16, vertical = SizeTokens.Level4)
            .animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        color = ThemedColorSchemeKeyTokens.SurfaceContainerLowest.value,
        tonalElevation = 1.dp,
        shadowElevation = 1.dp,
        border = BorderStroke(
            1.dp,
            if (isSuccess) ThemedColorSchemeKeyTokens.OutlineVariant.value.copy(alpha = 0.35f)
            else ThemedColorSchemeKeyTokens.Error.value.copy(alpha = 0.45f)
        ),
        onClick = { expanded = !expanded }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    modifier = Modifier.size(38.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = ThemedColorSchemeKeyTokens.SurfaceContainerHighest.value.copy(alpha = 0.6f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_rounded_folder_open),
                            contentDescription = null,
                            tint = ThemedColorSchemeKeyTokens.Primary.value,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.mediaEntity.name.ifEmpty { item.mediaEntity.path },
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = ThemedColorSchemeKeyTokens.OnSurface.value,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = item.mediaEntity.path,
                            style = MaterialTheme.typography.labelSmall,
                            color = ThemedColorSchemeKeyTokens.OnSurfaceVariant.value,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (sizeText.isNotEmpty()) {
                            Text(
                                text = "• $sizeText",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = ThemedColorSchemeKeyTokens.Primary.value
                            )
                        }
                    }
                }

                // Status Badge Pill
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSuccess) Color(0xFF4CAF50).copy(alpha = 0.12f) else ThemedColorSchemeKeyTokens.ErrorContainer.value.copy(alpha = 0.5f),
                    border = BorderStroke(
                        0.8.dp,
                        if (isSuccess) Color(0xFF4CAF50).copy(alpha = 0.4f) else ThemedColorSchemeKeyTokens.Error.value.copy(alpha = 0.4f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = if (isSuccess) Icons.Rounded.Check else Icons.Rounded.Close,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = if (isSuccess) Color(0xFF2E7D32) else ThemedColorSchemeKeyTokens.Error.value
                        )
                        Text(
                            text = if (isSuccess) stringResource(R.string.succeed) else stringResource(R.string.failed),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            fontWeight = FontWeight.Bold,
                            color = if (isSuccess) Color(0xFF2E7D32) else ThemedColorSchemeKeyTokens.Error.value
                        )
                    }
                }
            }

            if (expanded && item.mediaInfo.log.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = if (!isSuccess) ThemedColorSchemeKeyTokens.ErrorContainer.value.copy(alpha = 0.25f)
                    else ThemedColorSchemeKeyTokens.SurfaceContainerHighest.value.copy(alpha = 0.5f),
                    border = BorderStroke(
                        0.6.dp,
                        if (!isSuccess) ThemedColorSchemeKeyTokens.Error.value.copy(alpha = 0.35f)
                        else ThemedColorSchemeKeyTokens.OutlineVariant.value.copy(alpha = 0.2f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = item.mediaInfo.log.trim(),
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = if (!isSuccess) ThemedColorSchemeKeyTokens.Error.value
                            else ThemedColorSchemeKeyTokens.OnSurfaceVariant.value,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Log", item.mediaInfo.log))
                                Toast.makeText(context, "Log copied", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ContentCopy,
                                contentDescription = "Copy log",
                                modifier = Modifier.size(12.dp),
                                tint = ThemedColorSchemeKeyTokens.OnSurfaceVariant.value
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProcessingGroupCard(
    title: String,
    items: List<ProcessingInfoEntity>
) {
    if (items.isEmpty()) return
    val context = LocalContext.current
    val hasError = items.any { it.state == OperationState.ERROR }
    var expanded by remember { mutableStateOf(hasError) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SizeTokens.Level16, vertical = SizeTokens.Level4)
            .animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        color = ThemedColorSchemeKeyTokens.SurfaceContainerLowest.value,
        tonalElevation = 1.dp,
        shadowElevation = 1.dp,
        border = BorderStroke(
            1.dp,
            if (!hasError) ThemedColorSchemeKeyTokens.OutlineVariant.value.copy(alpha = 0.35f)
            else ThemedColorSchemeKeyTokens.Error.value.copy(alpha = 0.45f)
        ),
        onClick = { expanded = !expanded }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(com.xayah.core.ui.R.drawable.ic_rounded_hourglass_empty),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = if (hasError) ThemedColorSchemeKeyTokens.Error.value else ThemedColorSchemeKeyTokens.Primary.value
                    )
                    Text(
                        text = "$title (${items.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = ThemedColorSchemeKeyTokens.OnSurface.value
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (!hasError) Color(0xFF4CAF50).copy(alpha = 0.12f) else ThemedColorSchemeKeyTokens.ErrorContainer.value.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = if (!hasError) stringResource(R.string.succeed) else stringResource(R.string.failed),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            fontWeight = FontWeight.Bold,
                            color = if (!hasError) Color(0xFF2E7D32) else ThemedColorSchemeKeyTokens.Error.value
                        )
                    }

                    Icon(
                        imageVector = if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = ThemedColorSchemeKeyTokens.OnSurfaceVariant.value
                    )
                }
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(10.dp))
                Divider()
                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items.forEach { proc ->
                        val isProcError = proc.state == OperationState.ERROR
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = proc.title,
                                style = MaterialTheme.typography.bodySmall,
                                color = ThemedColorSchemeKeyTokens.OnSurface.value
                            )
                            Icon(
                                imageVector = if (isProcError) Icons.Rounded.Close else Icons.Rounded.Check,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = if (isProcError) ThemedColorSchemeKeyTokens.Error.value else Color(0xFF2E7D32)
                            )
                        }
                        if (proc.log.isNotBlank()) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(6.dp),
                                color = ThemedColorSchemeKeyTokens.SurfaceContainerHighest.value.copy(alpha = 0.4f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = proc.log.trim(),
                                        fontFamily = FontFamily.Monospace,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = ThemedColorSchemeKeyTokens.OnSurfaceVariant.value,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            clipboard.setPrimaryClip(ClipData.newPlainText("Log", proc.log))
                                            Toast.makeText(context, "Log copied", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(18.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.ContentCopy,
                                            contentDescription = "Copy log",
                                            modifier = Modifier.size(10.dp),
                                            tint = ThemedColorSchemeKeyTokens.OnSurfaceVariant.value
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyFilterView(filter: DetailFilter) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SizeTokens.Level16, vertical = SizeTokens.Level24),
        shape = RoundedCornerShape(16.dp),
        color = ThemedColorSchemeKeyTokens.SurfaceContainerLowest.value,
        border = BorderStroke(1.dp, ThemedColorSchemeKeyTokens.OutlineVariant.value.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = SizeTokens.Level32, horizontal = SizeTokens.Level16),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (filter == DetailFilter.FAILED) ImageVector.vectorResource(R.drawable.ic_rounded_check_circle) else ImageVector.vectorResource(R.drawable.ic_rounded_apps),
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = if (filter == DetailFilter.FAILED) Color(0xFF4CAF50) else ThemedColorSchemeKeyTokens.OnSurfaceVariant.value
            )
            Spacer(modifier = Modifier.height(SizeTokens.Level12))
            Text(
                text = if (filter == DetailFilter.FAILED) "No failed items!" else "No items found",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = ThemedColorSchemeKeyTokens.OnSurface.value
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (filter == DetailFilter.FAILED) "All operations completed successfully without errors." else "No records matched your selection.",
                style = MaterialTheme.typography.bodySmall,
                color = ThemedColorSchemeKeyTokens.OnSurfaceVariant.value,
                textAlign = TextAlign.Center
            )
        }
    }
}
