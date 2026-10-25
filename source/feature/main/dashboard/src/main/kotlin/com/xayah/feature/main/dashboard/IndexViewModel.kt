package com.xayah.feature.main.dashboard

import android.content.Context
import androidx.compose.material3.ExperimentalMaterial3Api
import com.xayah.core.common.util.BuildConfigUtil
import com.xayah.core.data.repository.AppsRepo
import com.xayah.core.data.repository.DirectoryRepository
import com.xayah.core.data.repository.FilesRepo
import com.xayah.core.datastore.ConstantUtil
import com.xayah.core.datastore.readLastBackupTime
import com.xayah.core.model.OpType
import com.xayah.core.model.database.DirectoryEntity
import com.xayah.core.network.model.Release
import com.xayah.core.network.retrofit.GitHubRepository
import com.xayah.core.ui.viewmodel.BaseViewModel
import com.xayah.core.ui.viewmodel.IndexUiEffect
import com.xayah.core.ui.viewmodel.UiIntent
import com.xayah.core.ui.viewmodel.UiState
import com.xayah.core.util.toBrowser
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

data class IndexUiState(
    val latestRelease: Release? = null,
) : UiState

sealed class IndexUiIntent : UiIntent {
    data object Update : IndexUiIntent()
    data class ToBrowser(val context: Context, val url: String) : IndexUiIntent()
}

@ExperimentalMaterial3Api
@HiltViewModel
class IndexViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val directoryRepo: DirectoryRepository,
    private val githubRepo: GitHubRepository,
    private val appsRepo: AppsRepo,
    private val filesRepo: FilesRepo,
) : BaseViewModel<IndexUiState, IndexUiIntent, IndexUiEffect>(IndexUiState(latestRelease = null)) {
    override suspend fun onEvent(state: IndexUiState, intent: IndexUiIntent) {
        when (intent) {
            is IndexUiIntent.Update -> {
                directoryRepo.updateSelected()

                runCatching {
                    val release = githubRepo.getLatestRelease()
                    val relVer = release.tagName.ifEmpty { release.name }.trim().removePrefix("v").removePrefix("V")
                    val curVer = BuildConfigUtil.VERSION_NAME.trim().removePrefix("v").removePrefix("V")
                    if (relVer.isNotEmpty() && relVer != curVer) {
                        emitState(state.copy(latestRelease = release))
                    } else {
                        emitState(state.copy(latestRelease = null))
                    }
                }
            }

            is IndexUiIntent.ToBrowser -> {
                runCatching { intent.context.toBrowser(intent.url) }.onFailure { emitEffect(IndexUiEffect.ShowSnackbar(message = context.getString(R.string.no_browser))) }
            }
        }
    }

    private val _lastBackupTime: Flow<Long> = context.readLastBackupTime().flowOnIO()
    val lastBackupTimeState: StateFlow<Long> = _lastBackupTime.stateInScope(0)

    private val _directory: Flow<DirectoryEntity?> = directoryRepo.querySelectedByDirectoryTypeFlow().flowOnIO()
    val directoryState: StateFlow<DirectoryEntity?> = _directory.stateInScope(null)

    val appsCountState: StateFlow<Long> = appsRepo.countApps(OpType.BACKUP).flowOnIO().stateInScope(0L)
    val backedUpAppsCountState: StateFlow<Long> = appsRepo.countApps(OpType.RESTORE).flowOnIO().stateInScope(0L)
    val filesCountState: StateFlow<Long> = filesRepo.countFiles(OpType.BACKUP).flowOnIO().stateInScope(0L)
    val backedUpFilesCountState: StateFlow<Long> = filesRepo.countFiles(OpType.RESTORE).flowOnIO().stateInScope(0L)
}
