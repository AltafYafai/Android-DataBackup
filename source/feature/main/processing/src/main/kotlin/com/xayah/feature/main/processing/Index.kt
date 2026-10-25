package com.xayah.feature.main.processing

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xayah.core.datastore.saveScreenOffCountDown
import com.xayah.core.model.OperationState
import com.xayah.core.ui.component.AnimatedTextContainer
import com.xayah.core.ui.component.BodyLargeText
import com.xayah.core.ui.component.HeadlineMediumText
import com.xayah.core.ui.component.LocalSlotScope
import com.xayah.core.ui.component.PackageIconImage
import com.xayah.core.ui.component.ProcessingCard
import com.xayah.core.ui.component.SegmentCircularProgressIndicator
import com.xayah.core.ui.component.Surface
import com.xayah.core.ui.component.confirm
import com.xayah.core.ui.component.paddingBottom
import com.xayah.core.ui.component.paddingHorizontal
import com.xayah.core.ui.component.paddingTop
import com.xayah.core.ui.component.paddingVertical
import com.xayah.core.ui.material3.SnackbarDuration
import com.xayah.core.ui.material3.SnackbarType
import com.xayah.core.ui.route.MainRoutes
import com.xayah.core.ui.theme.ThemedColorSchemeKeyTokens
import com.xayah.core.ui.theme.value
import com.xayah.core.ui.token.AnimationTokens
import com.xayah.core.ui.token.SizeTokens
import com.xayah.core.ui.util.LocalNavController
import com.xayah.core.ui.viewmodel.IndexUiEffect
import com.xayah.core.util.command.BaseUtil
import com.xayah.core.util.navigateSingle
import com.xayah.core.util.withMainContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay

@OptIn(ExperimentalAnimationGraphicsApi::class)
@SuppressLint("StringFormatInvalid")
@ExperimentalCoroutinesApi
@ExperimentalFoundationApi
@ExperimentalLayoutApi
@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@Composable
fun PageProcessing(
    topBarTitleId: (state: OperationState) -> Int,
    finishedTitleId: Int,
    finishedSubtitleId: Int,
    finishedWithErrorsSubtitleId: Int,
    viewModel: AbstractProcessingViewModel
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val task by viewModel.task.collectAsStateWithLifecycle()
    val preItemsProgress by viewModel.preItemsProgress.collectAsStateWithLifecycle()
    val preItems by viewModel.preItems.collectAsStateWithLifecycle()
    val postItemsProgress by viewModel.postItemsProgress.collectAsStateWithLifecycle()
    val postItems by viewModel.postItems.collectAsStateWithLifecycle()
    val dataItems by viewModel.dataItems.collectAsStateWithLifecycle()
    val navController = LocalNavController.current!!
    val dialogState = LocalSlotScope.current!!.dialogSlot
    val progress: Float by remember(task?.processingIndex, dataItems.size) {
        mutableFloatStateOf(
            if (task != null)
                task!!.processingIndex.toFloat() / (dataItems.size + 1)
            else
                -1f
        )
    }
    val screenOffCountDown by viewModel.screenOffCountDown.collectAsStateWithLifecycle()

    LaunchedEffect(null) {
        viewModel.emitIntentOnIO(ProcessingUiIntent.Initialize)
    }

    LaunchedEffect(screenOffCountDown, uiState.state) {
        viewModel.launchOnIO {
            if (screenOffCountDown != 0) {
                if (uiState.state != OperationState.PROCESSING) {
                    context.saveScreenOffCountDown(0)
                } else {
                    viewModel.launchOnIO {
                        viewModel.emitEffect(IndexUiEffect.DismissSnackbar)
                        viewModel.emitEffect(
                            IndexUiEffect.ShowSnackbar(
                                type = SnackbarType.Success,
                                message = context.getString(R.string.args_screen_off_in_seconds, screenOffCountDown),
                                duration = SnackbarDuration.Indefinite
                            )
                        )
                    }
                    var count = screenOffCountDown
                    while (count != 0) {
                        delay(1000)
                        count--
                    }
                    viewModel.emitEffectOnIO(IndexUiEffect.DismissSnackbar)
                    context.saveScreenOffCountDown(count)
                    viewModel.emitIntent(ProcessingUiIntent.TurnOffScreen)
                }
            }
        }
    }

    val onBack: () -> Unit = remember {
        {
            if (uiState.state == OperationState.PROCESSING) {
                viewModel.launchOnIO {
                    if (dialogState.confirm(title = context.getString(R.string.prompt), text = context.getString(R.string.processing_exit_confirmation))) {
                        BaseUtil.kill(context, "tar", "root")
                        viewModel.emitIntent(ProcessingUiIntent.DestroyService)
                        withMainContext {
                            navController.popBackStack()
                        }
                    }
                }
            } else {
                viewModel.launchOnIO {
                    viewModel.emitIntent(ProcessingUiIntent.DestroyService)
                    withMainContext {
                        navController.popBackStack()
                    }
                }
            }
        }
    }

    BackHandler {
        onBack()
    }

    ProcessingSetupScaffold(
        scrollBehavior = null,
        snackbarHostState = viewModel.snackbarHostState,
        title = stringResource(id = topBarTitleId.invoke(uiState.state)),
        progress = progress,
        actions = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .paddingHorizontal(SizeTokens.Level24)
                    .paddingVertical(SizeTokens.Level8),
            ) {
                AnimatedVisibility(uiState.state == OperationState.DONE) {
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            task?.apply {
                                navController.popBackStack()
                                navController.navigateSingle(MainRoutes.TaskDetails.getRoute(id))
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ThemedColorSchemeKeyTokens.SecondaryContainer.value, contentColor = ThemedColorSchemeKeyTokens.OnSecondaryContainer.value)
                    ) {
                        Text(text = stringResource(R.string.visit_details))
                    }
                }

                Button(modifier = Modifier.fillMaxWidth(), enabled = uiState.state == OperationState.IDLE || uiState.state == OperationState.DONE, onClick = {
                    if (uiState.state == OperationState.IDLE) viewModel.emitIntentOnIO(ProcessingUiIntent.Process)
                    else navController.popBackStack()
                }) {
                    AnimatedTextContainer(targetState = if (uiState.state == OperationState.DONE) stringResource(id = R.string.finish) else stringResource(id = R.string._continue)) { text ->
                        Text(text = text)
                    }
                }
            }

        },
        onBackClick = {
            onBack()
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(ThemedColorSchemeKeyTokens.Surface.value)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                var title: String by remember { mutableStateOf("") }
                var subtitle: String by remember { mutableStateOf("") }
                var activePackageName: String? by remember { mutableStateOf(null) }
                var activeStreamTitle: String by remember { mutableStateOf("") }
                var partitions: List<com.xayah.core.ui.model.ProcessingCardItem> by remember { mutableStateOf(emptyList()) }
                var activePartitionIndex: Int by remember { mutableIntStateOf(0) }
                var telemetryStats: String? by remember { mutableStateOf(null) }
                var isFinished: Boolean by remember { mutableStateOf(false) }
                var statusChipText: String by remember { mutableStateOf("IDLE") }
                var segmentProgress: Float by remember { mutableFloatStateOf(0f) }

                val currentIdx = task?.processingIndex
                if (currentIdx != null) {
                    when (currentIdx) {
                        0 -> {
                            // Preprocessing
                            title = stringResource(id = R.string.preprocessing)
                            val currentPre = preItems.getOrNull(task?.preprocessingIndex ?: -1)
                            subtitle = currentPre?.title ?: ""
                            activePackageName = null
                            activeStreamTitle = currentPre?.title ?: ""
                            segmentProgress = preItemsProgress
                            partitions = preItems
                            activePartitionIndex = task?.preprocessingIndex ?: 0
                            telemetryStats = currentPre?.content?.ifEmpty { null }
                            isFinished = false
                            statusChipText = "PREPARING"
                        }

                        dataItems.size + 1 -> {
                            // Post-processing
                            title = stringResource(id = R.string.post_processing)
                            val currentPost = postItems.getOrNull(task?.postProcessingIndex ?: -1)
                            subtitle = currentPost?.title ?: ""
                            activePackageName = null
                            activeStreamTitle = currentPost?.title ?: ""
                            segmentProgress = postItemsProgress
                            partitions = postItems
                            activePartitionIndex = task?.postProcessingIndex ?: 0
                            telemetryStats = currentPost?.content?.ifEmpty { null }
                            isFinished = false
                            statusChipText = "CLEANING"
                        }

                        dataItems.size + 2 -> {
                            // Finished
                            title = stringResource(id = finishedTitleId)
                            subtitle = if (task != null) {
                                if (task!!.totalCount == task!!.successCount) {
                                    context.getString(finishedSubtitleId, task!!.totalCount)
                                } else {
                                    context.getString(finishedWithErrorsSubtitleId, task!!.successCount, task!!.failureCount)
                                }
                            } else {
                                context.getString(finishedSubtitleId, dataItems.size)
                            }
                            activePackageName = null
                            activeStreamTitle = "COMPLETE"
                            segmentProgress = 1.0f
                            partitions = emptyList()
                            activePartitionIndex = 0
                            telemetryStats = "All operations finalized"
                            isFinished = true
                            statusChipText = "SEALED"
                        }

                        else -> {
                            // Processing
                            val item = dataItems.getOrNull(currentIdx - 1)
                            if (item != null) {
                                val index = item.processingIndex
                                val activeSub = item.items.getOrNull(index)
                                title = item.title
                                activePackageName = item.key
                                activeStreamTitle = activeSub?.title ?: ""
                                subtitle = activeSub?.let { it.title + if (it.content.isEmpty()) "" else " (${it.content})" }
                                    ?: stringResource(id = R.string.necessary_remaining_data_processing)
                                val subP = activeSub?.progress ?: -1f
                                val parsedP = activeSub?.content?.trimEnd('%')?.toFloatOrNull()?.let { it / 100f } ?: -1f
                                val computedStreamP = if (subP > 0f) subP else if (parsedP > 0f) parsedP else item.progress
                                segmentProgress = if (computedStreamP > 0f) computedStreamP else 0.05f
                                partitions = item.items
                                activePartitionIndex = index
                                telemetryStats = activeSub?.content?.ifEmpty { null } ?: item.key
                                isFinished = false
                                statusChipText = "STREAMING"
                            }
                        }
                    }
                }

                val statusChipColor = when {
                    isFinished || statusChipText == "STREAMING" -> ThemedColorSchemeKeyTokens.Primary.value
                    statusChipText == "PREPARING" || statusChipText == "CLEANING" -> ThemedColorSchemeKeyTokens.Secondary.value
                    else -> ThemedColorSchemeKeyTokens.Outline.value
                }

                ProcessingTelemetryHud(
                    isFinished = isFinished,
                    title = title,
                    subtitle = subtitle,
                    packageName = activePackageName,
                    statusChipText = statusChipText,
                    statusChipColor = statusChipColor,
                    batchCurrent = task?.processingIndex ?: 0,
                    batchTotal = dataItems.size,
                    batchProgress = progress,
                    streamTitle = activeStreamTitle,
                    streamProgress = segmentProgress,
                    partitions = partitions,
                    activePartitionIndex = activePartitionIndex,
                    telemetryStats = telemetryStats,
                )
            }

            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(topStart = SizeTokens.Level24, topEnd = SizeTokens.Level24),
                color = ThemedColorSchemeKeyTokens.SurfaceContainerLowest.value,
                shadowElevation = SizeTokens.Level2
            ) {
                val lazyListState = rememberLazyListState()

                LaunchedEffect(task) {
                    if (task != null) {
                        runCatching {
                            lazyListState.animateScrollToItem((task!!.processingIndex - 1))
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = lazyListState,
                    verticalArrangement = Arrangement.spacedBy(SizeTokens.Level12)
                ) {
                    item {
                        Spacer(modifier = Modifier.size(SizeTokens.Level12))
                    }
                    items(count = dataItems.size) {
                        var expanded by rememberSaveable(task, it) { mutableStateOf((task?.processingIndex?.minus(1) ?: -1) == it) }
                        val item = dataItems.getOrNull(it)
                        if (item != null) {
                            ProcessingCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .paddingHorizontal(SizeTokens.Level24),
                                title = item.title,
                                state = item.state,
                                packageName = item.key,
                                expanded = expanded,
                                items = item.items,
                                processingIndex = item.processingIndex,
                                progress = item.progress,
                                onActionBarClick = {
                                    expanded = expanded.not()
                                }
                            )
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.size(SizeTokens.Level12 + it))
                    }
                }
            }
        }
    }
}
