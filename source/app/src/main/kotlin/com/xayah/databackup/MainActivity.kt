package com.xayah.databackup

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.xayah.core.ui.component.AnimatedNavHost
import com.xayah.core.ui.route.MainRoutes
import com.xayah.core.ui.theme.DataBackupTheme
import com.xayah.core.ui.util.LocalNavController
import com.xayah.core.util.command.BaseUtil
import com.xayah.databackup.component.BackupVaultBottomBar
import com.xayah.feature.main.configurations.PageConfigurations
import com.xayah.feature.main.dashboard.PageDashboard
import com.xayah.feature.main.details.DetailsRoute
import com.xayah.feature.main.directory.PageDirectory
import com.xayah.feature.main.history.HistoryRoute
import com.xayah.feature.main.history.TaskDetailsRoute
import com.xayah.feature.main.list.ListRoute
import com.xayah.feature.main.processing.medium.backup.MediumBackupProcessingGraph
import com.xayah.feature.main.processing.medium.restore.MediumRestoreProcessingGraph
import com.xayah.feature.main.processing.packages.backup.PackagesBackupProcessingGraph
import com.xayah.feature.main.processing.packages.restore.PackagesRestoreProcessingGraph
import com.xayah.feature.main.restore.PageRestore
import com.xayah.feature.main.restore.reload.PageReload
import com.xayah.feature.main.settings.PageSettings
import com.xayah.feature.main.settings.about.PageAboutSettings
import com.xayah.feature.main.settings.about.PageTranslatorsSettings
import com.xayah.feature.main.settings.backup.PageBackupSettings
import com.xayah.feature.main.settings.blacklist.PageBlackList
import com.xayah.feature.main.settings.language.PageLanguageSelector
import com.xayah.feature.main.settings.restore.PageRestoreSettings
import com.xayah.core.data.repository.UpdateRepo
import com.xayah.core.data.repository.UpdateState
import com.xayah.core.ui.component.AppUpdateDialog
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import javax.inject.Inject
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject
    lateinit var updateRepo: UpdateRepo

    @ExperimentalCoroutinesApi
    @ExperimentalAnimationApi
    @ExperimentalFoundationApi
    @ExperimentalLayoutApi
    @ExperimentalMaterial3Api
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        runBlocking {
            runCatching {
                BaseUtil.initializeEnvironment(context = this@MainActivity)
            }
        }

        setContent {
            DataBackupTheme {
                val navController = rememberNavController()
                val updateState by updateRepo.updateState.collectAsStateWithLifecycle()
                val scope = rememberCoroutineScope()

                LaunchedEffect(Unit) {
                    updateRepo.checkForUpdate(isManual = false)
                }

                when (val state = updateState) {
                    is UpdateState.Available -> {
                        AppUpdateDialog(
                            isOpen = true,
                            versionName = state.updateInfo.versionName,
                            changelog = state.updateInfo.changelog,
                            sizeBytes = state.updateInfo.sizeBytes,
                            sizeText = state.updateInfo.sizeText,
                            isDownloading = false,
                            onDownloadClick = {
                                scope.launch {
                                    updateRepo.downloadAndInstall(state.updateInfo)
                                }
                            },
                            onLaterClick = {
                                updateRepo.dismissUpdate()
                            }
                        )
                    }
                    is UpdateState.Downloading -> {
                        AppUpdateDialog(
                            isOpen = true,
                            versionName = state.updateInfo.versionName,
                            changelog = state.updateInfo.changelog,
                            sizeBytes = state.updateInfo.sizeBytes,
                            sizeText = state.updateInfo.sizeText,
                            isDownloading = true,
                            downloadProgress = state.progress,
                            downloadedBytes = state.downloadedBytes,
                            totalBytes = state.totalBytes,
                            onDownloadClick = {},
                            onLaterClick = {}
                        )
                    }
                    else -> {}
                }

                CompositionLocalProvider(
                    LocalNavController provides navController,
                    androidx.lifecycle.compose.LocalLifecycleOwner provides androidx.compose.ui.platform.LocalLifecycleOwner.current,
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AnimatedNavHost(
                            navController = navController,
                            startDestination = MainRoutes.Dashboard.route,
                        ) {
                            composable(MainRoutes.Dashboard.route) {
                                PageDashboard()
                            }
                            composable(MainRoutes.List.route) {
                                ListRoute()
                            }
                            composable(MainRoutes.Details.route) {
                                DetailsRoute()
                            }
                            composable(MainRoutes.History.route) {
                                HistoryRoute()
                            }
                            composable(MainRoutes.TaskDetails.route) {
                                TaskDetailsRoute()
                            }
                            composable(MainRoutes.PackagesBackupProcessingGraph.route) {
                                PackagesBackupProcessingGraph()
                            }
                            composable(MainRoutes.PackagesRestoreProcessingGraph.route) {
                                PackagesRestoreProcessingGraph()
                            }
                            composable(MainRoutes.MediumBackupProcessingGraph.route) {
                                MediumBackupProcessingGraph()
                            }
                            composable(MainRoutes.MediumRestoreProcessingGraph.route) {
                                MediumRestoreProcessingGraph()
                            }
                            composable(MainRoutes.Settings.route) {
                                PageSettings()
                            }
                            composable(MainRoutes.Restore.route) {
                                PageRestore()
                            }
                            composable(MainRoutes.Reload.route) {
                                PageReload()
                            }
                            composable(MainRoutes.BackupSettings.route) {
                                PageBackupSettings()
                            }
                            composable(MainRoutes.RestoreSettings.route) {
                                PageRestoreSettings()
                            }
                            composable(MainRoutes.LanguageSettings.route) {
                                PageLanguageSelector()
                            }
                            composable(MainRoutes.BlackList.route) {
                                PageBlackList()
                            }
                            composable(MainRoutes.Configurations.route) {
                                PageConfigurations()
                            }
                            composable(MainRoutes.About.route) {
                                PageAboutSettings()
                            }
                            composable(MainRoutes.Translators.route) {
                                PageTranslatorsSettings()
                            }
                            composable(route = MainRoutes.Directory.route) {
                                PageDirectory()
                            }
                        }

                        // Floating Frosted Glass Bottom Navigation Bar permanently anchored to bottom
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                        ) {
                            BackupVaultBottomBar(navController = navController)
                        }
                    }
                }
            }
        }
    }
}
