package com.xayah.feature.main.restore

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.xayah.core.data.repository.FilesRepo
import com.xayah.core.model.OpType
import com.xayah.core.model.Target
import com.xayah.core.ui.component.BodyMediumText
import com.xayah.core.ui.component.TitleMediumText
import com.xayah.core.ui.route.MainRoutes
import com.xayah.core.ui.theme.ThemedColorSchemeKeyTokens
import com.xayah.core.ui.theme.value
import com.xayah.core.ui.token.SizeTokens
import com.xayah.core.ui.util.LocalNavController
import com.xayah.core.util.navigateSingle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class FilesHubViewModel @Inject constructor(
    filesRepo: FilesRepo,
) : ViewModel() {
    val backupFilesCount: StateFlow<Long> = filesRepo.countFiles(OpType.BACKUP)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val restoreFilesCount: StateFlow<Long> = filesRepo.countFiles(OpType.RESTORE)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)
}

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
    ExperimentalLayoutApi::class,
    ExperimentalAnimationApi::class
)
@Composable
fun FilesHubScreen(
    viewModel: FilesHubViewModel = hiltViewModel(),
) {
    val navController = LocalNavController.current!!
    val backupFilesCount by viewModel.backupFilesCount.collectAsStateWithLifecycle()
    val restoreFilesCount by viewModel.restoreFilesCount.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.files),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ThemedColorSchemeKeyTokens.Surface.value,
                    titleContentColor = ThemedColorSchemeKeyTokens.OnSurface.value,
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = SizeTokens.Level16)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(SizeTokens.Level16)
        ) {
            Spacer(modifier = Modifier.height(SizeTokens.Level4))

            // Hero Overview Card
            FilesHubHeroCard(
                backupFilesCount = backupFilesCount,
                restoreFilesCount = restoreFilesCount,
            )

            Text(
                text = stringResource(id = R.string.files),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = ThemedColorSchemeKeyTokens.OnSurfaceVariant.value,
                modifier = Modifier.padding(top = SizeTokens.Level8, start = SizeTokens.Level4)
            )

            // Option 1: Backup Files
            FilesOptionCard(
                title = stringResource(id = R.string.backup_files),
                subtitle = stringResource(id = R.string.backup_files_desc),
                icon = ImageVector.vectorResource(id = R.drawable.ic_rounded_arrow_circle_up),
                containerColor = ThemedColorSchemeKeyTokens.PrimaryContainer,
                onContainerColor = ThemedColorSchemeKeyTokens.OnPrimaryContainer,
                badgeText = if (backupFilesCount > 0) "$backupFilesCount files available" else "Select files",
                buttonText = "Select & Backup",
                onClick = {
                    navController.navigateSingle(MainRoutes.List.getRoute(Target.Files, OpType.BACKUP))
                }
            )

            // Option 2: Restore Files
            FilesOptionCard(
                title = stringResource(id = R.string.restore_files),
                subtitle = stringResource(id = R.string.restore_files_desc),
                icon = ImageVector.vectorResource(id = R.drawable.ic_rounded_arrow_circle_down),
                containerColor = ThemedColorSchemeKeyTokens.SecondaryContainer,
                onContainerColor = ThemedColorSchemeKeyTokens.OnSecondaryContainer,
                badgeText = if (restoreFilesCount > 0) "$restoreFilesCount archives saved" else "No archives yet",
                buttonText = "Browse & Restore",
                onClick = {
                    navController.navigateSingle(MainRoutes.List.getRoute(Target.Files, OpType.RESTORE))
                }
            )

            // System Data Shortcut Banner
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { navController.navigateSingle(MainRoutes.SystemData.route) },
                shape = RoundedCornerShape(20.dp),
                color = ThemedColorSchemeKeyTokens.SurfaceContainerHighest.value.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, ThemedColorSchemeKeyTokens.OutlineVariant.value.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = SizeTokens.Level16, vertical = SizeTokens.Level14),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(SizeTokens.Level12)
                ) {
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        color = ThemedColorSchemeKeyTokens.Primary.value.copy(alpha = 0.12f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Key,
                                contentDescription = null,
                                tint = ThemedColorSchemeKeyTokens.Primary.value,
                                modifier = Modifier.size(20.dp)
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
                            text = "Backup & restore Call Logs, SMS, Contacts & Wi-Fi",
                            style = MaterialTheme.typography.bodySmall,
                            color = ThemedColorSchemeKeyTokens.OnSurfaceVariant.value,
                            fontSize = 12.sp
                        )
                    }

                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        tint = ThemedColorSchemeKeyTokens.Outline.value,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(SizeTokens.Level64 + SizeTokens.Level32))
        }
    }
}

@Composable
private fun FilesHubHeroCard(
    backupFilesCount: Long,
    restoreFilesCount: Long,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = ThemedColorSchemeKeyTokens.SurfaceContainerHigh.value,
        tonalElevation = SizeTokens.Level2,
        border = BorderStroke(Dp(1f), ThemedColorSchemeKeyTokens.OutlineVariant.value.copy(alpha = 0.4f)),
    ) {
        Column(
            modifier = Modifier.padding(SizeTokens.Level20),
            verticalArrangement = Arrangement.spacedBy(SizeTokens.Level12)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(SizeTokens.Level12)
            ) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = CircleShape,
                    color = ThemedColorSchemeKeyTokens.Primary.value.copy(alpha = 0.15f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = R.drawable.ic_rounded_folder_open),
                            contentDescription = null,
                            tint = ThemedColorSchemeKeyTokens.Primary.value,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Column {
                    Text(
                        text = "Files & Media Vault",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ThemedColorSchemeKeyTokens.OnSurface.value
                    )
                    Text(
                        text = "Preserve files, pictures, downloads and folders",
                        style = MaterialTheme.typography.bodySmall,
                        color = ThemedColorSchemeKeyTokens.OnSurfaceVariant.value
                    )
                }
            }

            // Stat pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(SizeTokens.Level12)
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    color = ThemedColorSchemeKeyTokens.SurfaceContainerLowest.value
                ) {
                    Column(modifier = Modifier.padding(SizeTokens.Level12)) {
                        Text(
                            text = backupFilesCount.toString(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = ThemedColorSchemeKeyTokens.Primary.value
                        )
                        Text(
                            text = "Configured",
                            style = MaterialTheme.typography.labelSmall,
                            color = ThemedColorSchemeKeyTokens.OnSurfaceVariant.value
                        )
                    }
                }

                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    color = ThemedColorSchemeKeyTokens.SurfaceContainerLowest.value
                ) {
                    Column(modifier = Modifier.padding(SizeTokens.Level12)) {
                        Text(
                            text = restoreFilesCount.toString(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = ThemedColorSchemeKeyTokens.Secondary.value
                        )
                        Text(
                            text = "Backed Up",
                            style = MaterialTheme.typography.labelSmall,
                            color = ThemedColorSchemeKeyTokens.OnSurfaceVariant.value
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilesOptionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    containerColor: ThemedColorSchemeKeyTokens,
    onContainerColor: ThemedColorSchemeKeyTokens,
    badgeText: String,
    buttonText: String,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = ThemedColorSchemeKeyTokens.SurfaceContainerLowest.value,
        tonalElevation = SizeTokens.Level1,
        shadowElevation = SizeTokens.Level2,
        border = BorderStroke(Dp(1.2f), ThemedColorSchemeKeyTokens.OutlineVariant.value.copy(alpha = 0.5f)),
        onClick = onClick,
        interactionSource = interactionSource,
    ) {
        Column(
            modifier = Modifier.padding(SizeTokens.Level20),
            verticalArrangement = Arrangement.spacedBy(SizeTokens.Level16)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(SizeTokens.Level16)
            ) {
                Surface(
                    modifier = Modifier.size(52.dp),
                    shape = CircleShape,
                    color = containerColor.value,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            modifier = Modifier.size(28.dp),
                            imageVector = icon,
                            tint = onContainerColor.value,
                            contentDescription = null,
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = ThemedColorSchemeKeyTokens.OnSurface.value
                    )
                    Spacer(modifier = Modifier.height(SizeTokens.Level4))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = ThemedColorSchemeKeyTokens.OnSurfaceVariant.value
                    )
                }
            }

            // Bottom action row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ThemedColorSchemeKeyTokens.SurfaceContainerHigh.value,
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = ThemedColorSchemeKeyTokens.OnSurfaceVariant.value,
                        modifier = Modifier.padding(horizontal = SizeTokens.Level10, vertical = SizeTokens.Level4)
                    )
                }

                FilledTonalButton(
                    onClick = onClick,
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                ) {
                    Text(
                        text = buttonText,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(SizeTokens.Level6))
                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
