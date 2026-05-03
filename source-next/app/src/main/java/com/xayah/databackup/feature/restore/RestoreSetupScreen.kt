package com.xayah.databackup.feature.restore

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.xayah.databackup.R
import com.xayah.databackup.ui.component.SelectableCardButton
import com.xayah.databackup.ui.component.SmallCheckActionButton
import com.xayah.databackup.ui.component.defaultLargeTopAppBarColors
import com.xayah.databackup.ui.component.horizontalFadingEdges
import com.xayah.databackup.ui.component.selectableCardButtonTertiaryColors
import com.xayah.databackup.ui.component.verticalFadingEdges
import com.xayah.databackup.util.LaunchedEffect
import com.xayah.databackup.util.items
import com.xayah.databackup.util.popBackStackSafely
import kotlinx.coroutines.Dispatchers
import org.koin.androidx.compose.koinViewModel

import com.xayah.databackup.feature.RestoreProcessRoute
import com.xayah.databackup.util.navigateSafely

@Composable
fun RestoreSetupScreen(
    navController: NavHostController,
    viewModel: RestoreViewModel = koinViewModel(),
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(context = Dispatchers.IO, null) {
        viewModel.loadConfigs()
    }

    Scaffold(
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .fillMaxSize(),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.restore),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStackSafely() }) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_arrow_left),
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.defaultLargeTopAppBarColors(),
            )
        },
    ) { innerPadding ->
        Column(modifier = Modifier) {
            Spacer(modifier = Modifier.size(innerPadding.calculateTopPadding()))

            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .verticalFadingEdges(scrollState),
            ) {
                RestoreBackupRow(uiState = uiState, viewModel = viewModel)

                AnimatedVisibility(visible = uiState.selectedIndex != -1) {
                    RestoreTargetRow(navController = navController, viewModel = viewModel)
                }

                Spacer(modifier = Modifier.height(0.dp))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.weight(1f))

                Button(
                    modifier = Modifier.wrapContentSize(),
                    enabled = uiState.selectedIndex != -1,
                    onClick = {
                        viewModel.resetProcessRepo()
                        navController.navigateSafely(RestoreProcessRoute)
                    }
                ) {
                    Text(text = stringResource(R.string.next))
                }
            }

            Spacer(modifier = Modifier.size(innerPadding.calculateBottomPadding()))
        }
    }
}

@Composable
private fun RestoreBackupRow(
    uiState: RestoreUiState,
    viewModel: RestoreViewModel,
) {
    Text(
        modifier = Modifier.padding(16.dp),
        text = stringResource(R.string.backup),
        color = MaterialTheme.colorScheme.tertiary,
        style = MaterialTheme.typography.labelLarge
    )

    val lazyListState = rememberLazyListState()
    var showStartEdge by remember { mutableStateOf(false) }
    var showEndEdge by remember { mutableStateOf(false) }
    val startEdgeRange: Float by animateFloatAsState(if (showStartEdge) 1f else 0f, label = "alpha")
    val endEdgeRange: Float by animateFloatAsState(if (showEndEdge) 1f else 0f, label = "alpha")
    LaunchedEffect(context = Dispatchers.Default, lazyListState.canScrollBackward) {
        showStartEdge = lazyListState.canScrollBackward
    }
    LaunchedEffect(context = Dispatchers.Default, lazyListState.canScrollForward) {
        showEndEdge = lazyListState.canScrollForward
    }
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalFadingEdges(startEdgeRange, endEdgeRange),
        state = lazyListState,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.size(0.dp, 148.dp))
        }

        items(items = uiState.configs, key = { _, item -> item.uuid }) { index, item ->
            val title = remember(item.name, item.createdAt) { item.displayTitle }
            SelectableCardButton(
                modifier = Modifier
                    .size(148.dp)
                    .animateItem(),
                selected = uiState.selectedIndex == index,
                title = title,
                subtitle = item.displayCreatedAt,
                colors = selectableCardButtonTertiaryColors(),
                icon = ImageVector.vectorResource(R.drawable.ic_archive),
            ) {
                viewModel.selectBackup(index)
            }
        }

        item {
            Spacer(modifier = Modifier.size(0.dp, 148.dp))
        }
    }
}

@Composable
private fun RestoreTargetRow(
    navController: NavHostController,
    viewModel: RestoreViewModel,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        text = stringResource(R.string.target),
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.labelLarge
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SmallCheckActionButton(
                modifier = Modifier
                    .weight(1f)
                    .wrapContentSize(),
                checked = uiState.appsCount.first > 0,
                icon = ImageVector.vectorResource(R.drawable.ic_layout_grid),
                title = stringResource(R.string.apps),
                subtitle = stringResource(R.string.items_selected, uiState.appsCount.first, uiState.appsCount.second),
                onCheckedChange = {}
            ) {
                // TODO: Navigate to RestoreAppsScreen
            }

            SmallCheckActionButton(
                modifier = Modifier
                    .weight(1f)
                    .wrapContentSize(),
                checked = uiState.networksCount.first > 0,
                icon = ImageVector.vectorResource(R.drawable.ic_wifi),
                title = stringResource(R.string.networks),
                subtitle = stringResource(R.string.items_selected, uiState.networksCount.first, uiState.networksCount.second),
                onCheckedChange = {}
            ) {
                // TODO: Navigate to RestoreNetworksScreen
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SmallCheckActionButton(
                modifier = Modifier
                    .weight(1f)
                    .wrapContentSize(),
                checked = uiState.contactsCount.first > 0,
                icon = ImageVector.vectorResource(R.drawable.ic_user_round),
                title = stringResource(R.string.contacts),
                subtitle = stringResource(R.string.items_selected, uiState.contactsCount.first, uiState.contactsCount.second),
                onCheckedChange = {}
            ) {
                // TODO: Navigate to RestoreContactsScreen
            }

            SmallCheckActionButton(
                modifier = Modifier
                    .weight(1f)
                    .wrapContentSize(),
                checked = uiState.callLogsCount.first > 0,
                icon = ImageVector.vectorResource(R.drawable.ic_phone),
                title = stringResource(R.string.call_logs),
                subtitle = stringResource(R.string.items_selected, uiState.callLogsCount.first, uiState.callLogsCount.second),
                onCheckedChange = {}
            ) {
                // TODO: Navigate to RestoreCallLogsScreen
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SmallCheckActionButton(
                modifier = Modifier
                    .weight(1f)
                    .wrapContentSize(),
                checked = uiState.messagesCount.first > 0,
                icon = ImageVector.vectorResource(R.drawable.ic_message_circle),
                title = stringResource(R.string.messages),
                subtitle = stringResource(R.string.items_selected, uiState.messagesCount.first, uiState.messagesCount.second),
                onCheckedChange = {}
            ) {
                // TODO: Navigate to RestoreMessagesScreen
            }
        }
    }
}
