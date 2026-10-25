package com.xayah.feature.main.dashboard

import android.annotation.SuppressLint
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.ListAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xayah.core.common.util.BuildConfigUtil
import com.xayah.core.model.OpType
import com.xayah.core.model.Target
import com.xayah.core.ui.component.DismissState
import com.xayah.core.ui.component.IconButton
import com.xayah.core.ui.component.LocalSlotScope
import com.xayah.core.ui.component.MainIndexSubScaffold
import com.xayah.core.ui.component.Section
import com.xayah.core.ui.component.paddingTop
import com.xayah.core.ui.model.SegmentProgress
import com.xayah.core.ui.route.MainRoutes
import com.xayah.core.ui.theme.ThemedColorSchemeKeyTokens
import com.xayah.core.ui.token.SizeTokens
import com.xayah.core.ui.util.LocalNavController
import com.xayah.core.util.navigateSingle
import kotlinx.coroutines.launch

@SuppressLint("StringFormatInvalid")
@ExperimentalFoundationApi
@ExperimentalLayoutApi
@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@Composable
fun PageDashboard() {
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val viewModel = hiltViewModel<IndexViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = LocalNavController.current!!
    val lastBackupTime by viewModel.lastBackupTimeState.collectAsStateWithLifecycle()
    val directoryState by viewModel.directoryState.collectAsStateWithLifecycle()
    val nullBackupDir by remember(directoryState) { mutableStateOf(directoryState == null) }
    val appsCount by viewModel.appsCountState.collectAsStateWithLifecycle()
    val backedUpAppsCount by viewModel.backedUpAppsCountState.collectAsStateWithLifecycle()
    val filesCount by viewModel.filesCountState.collectAsStateWithLifecycle()
    val backedUpFilesCount by viewModel.backedUpFilesCountState.collectAsStateWithLifecycle()
    val rootInfo by viewModel.rootInfoState.collectAsStateWithLifecycle()
    val dialogState = LocalSlotScope.current!!.dialogSlot
    val scope = rememberCoroutineScope()

    LaunchedEffect(null) {
        viewModel.emitIntentOnIO(IndexUiIntent.Update)
    }

    MainIndexSubScaffold(
        scrollBehavior = scrollBehavior,
        snackbarHostState = viewModel.snackbarHostState,
        title = stringResource(id = R.string.app_name),
        updateAvailable = uiState.latestRelease != null,
        onVersionChipClick = {
            scope.launch {
                val state = dialogState.open(
                    initialState = false,
                    title = context.getString(R.string.update_available),
                    icon = null,
                    dismissText = context.getString(R.string.changelog),
                    confirmText = context.getString(R.string.download),
                    block = { _ -> Text(text = context.getString(R.string.args_update_from, BuildConfigUtil.VERSION_NAME, uiState.latestRelease?.name)) }
                ).first
                when (state) {
                    DismissState.CONFIRM -> {
                        uiState.latestRelease?.assets?.firstOrNull { it.url.contains(BuildConfigUtil.FLAVOR_feature) && it.url.contains(BuildConfigUtil.FLAVOR_abi) }?.apply {
                            viewModel.emitIntent(IndexUiIntent.ToBrowser(context = context, url = this.url))
                        }
                    }

                    DismissState.CANCEL -> {
                        uiState.latestRelease?.url?.apply {
                            viewModel.emitIntent(IndexUiIntent.ToBrowser(context = context, url = this))
                        }
                    }

                    DismissState.DISMISS -> {}
                }
            }
        },
        actions = {
            IconButton(
                enabled = nullBackupDir.not(),
                icon = Icons.Outlined.Settings,
                onClick = {
                    navController.navigateSingle(MainRoutes.Settings.route)
                }
            )
        }
    ) {
        Column(
            modifier = Modifier
                .paddingTop(SizeTokens.Level8)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(SizeTokens.Level20)
        ) {
            // Modern Hero Status Card
            DashboardHeroStatusCard(
                nullBackupDir = nullBackupDir,
                lastBackupTime = lastBackupTime,
                appsCount = appsCount,
                backedUpAppsCount = backedUpAppsCount,
                filesCount = filesCount,
                backedUpFilesCount = backedUpFilesCount,
                rootInfo = rootInfo,
            ) {
                if (nullBackupDir) {
                    navController.navigateSingle(MainRoutes.Directory.route)
                }
            }

            // Storage Overview Telemetry Card
            if (directoryState != null) {
                DashboardStorageCardExpressive(
                    title = stringResource(id = directoryState!!.titleResId),
                    used = SegmentProgress(used = directoryState!!.usedBytes, total = directoryState!!.totalBytes),
                    backupUsed = SegmentProgress(used = directoryState!!.childUsedBytes, total = directoryState!!.totalBytes),
                ) {
                    navController.navigateSingle(MainRoutes.Directory.route)
                }
            }

            // Quick Actions 2x2 Grid
            Section(title = stringResource(id = R.string.quick_actions)) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(SizeTokens.Level12)
                ) {
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(SizeTokens.Level12)
                    ) {
                        DashboardExpressiveActionCard(
                            modifier = Modifier.weight(1f),
                            title = stringResource(id = R.string.backup_apps),
                            subtitle = "Full apps & data",
                            badgeText = if (appsCount > 0) "$appsCount apps" else "Ready",
                            icon = ImageVector.vectorResource(id = R.drawable.ic_rounded_package_2),
                            containerColor = ThemedColorSchemeKeyTokens.PrimaryContainer,
                            onContainerColor = ThemedColorSchemeKeyTokens.OnPrimaryContainer,
                            onClick = {
                                navController.navigateSingle(MainRoutes.List.getRoute(target = Target.Apps, opType = OpType.BACKUP))
                            }
                        )

                        DashboardExpressiveActionCard(
                            modifier = Modifier.weight(1f),
                            title = stringResource(id = R.string.restore),
                            subtitle = "Restore snapshots",
                            badgeText = if (backedUpAppsCount > 0) "$backedUpAppsCount saved" else "Empty",
                            icon = ImageVector.vectorResource(id = R.drawable.ic_rounded_history),
                            containerColor = ThemedColorSchemeKeyTokens.TertiaryContainer,
                            onContainerColor = ThemedColorSchemeKeyTokens.OnTertiaryContainer,
                            onClick = {
                                navController.navigateSingle(MainRoutes.List.getRoute(target = Target.Apps, opType = OpType.RESTORE))
                            }
                        )
                    }

                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(SizeTokens.Level12)
                    ) {
                        DashboardExpressiveActionCard(
                            modifier = Modifier.weight(1f),
                            title = stringResource(id = R.string.backup_files),
                            subtitle = "Backup & restore",
                            badgeText = if (filesCount > 0) "$filesCount files" else "Configure",
                            icon = ImageVector.vectorResource(id = R.drawable.ic_rounded_folder_open),
                            containerColor = ThemedColorSchemeKeyTokens.SecondaryContainer,
                            onContainerColor = ThemedColorSchemeKeyTokens.OnSecondaryContainer,
                            onClick = {
                                navController.navigateSingle(MainRoutes.FilesHub.route)
                            }
                        )

                        DashboardExpressiveActionCard(
                            modifier = Modifier.weight(1f),
                            title = stringResource(id = R.string.history),
                            subtitle = "Logs & speeds",
                            badgeText = "Records",
                            icon = Icons.Rounded.ListAlt,
                            containerColor = ThemedColorSchemeKeyTokens.SurfaceContainerHigh,
                            onContainerColor = ThemedColorSchemeKeyTokens.OnSurface,
                            onClick = {
                                navController.navigateSingle(MainRoutes.History.route)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(SizeTokens.Level64 + SizeTokens.Level32))
        }
    }
}
