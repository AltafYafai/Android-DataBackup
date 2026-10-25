package com.xayah.feature.setup.page.one

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xayah.core.datastore.KeyCheckKeystore
import com.xayah.core.datastore.readBackupSavePath
import com.xayah.core.datastore.readBackupSavePathSaved
import com.xayah.core.datastore.readCustomSUFile
import com.xayah.core.datastore.saveCustomSUFile
import com.xayah.core.ui.component.AppIcon
import com.xayah.core.ui.component.Clickable
import com.xayah.core.ui.component.LocalSlotScope
import com.xayah.core.ui.component.Section
import com.xayah.core.ui.component.SetOnResume
import com.xayah.core.ui.component.Switchable
import com.xayah.core.ui.component.confirm
import com.xayah.core.ui.component.edit
import com.xayah.core.ui.component.paddingHorizontal
import com.xayah.core.ui.component.paddingTop
import com.xayah.core.ui.theme.ThemedColorSchemeKeyTokens
import com.xayah.core.ui.theme.value
import com.xayah.core.ui.token.SizeTokens
import com.xayah.core.ui.util.LocalNavController
import com.xayah.core.util.getActivity
import com.xayah.core.util.navigateSingle
import com.xayah.feature.setup.PermissionButton
import com.xayah.feature.setup.R
import com.xayah.feature.setup.SetupRoutes
import com.xayah.feature.setup.SetupScaffold
import kotlinx.coroutines.flow.first

@ExperimentalFoundationApi
@ExperimentalMaterial3Api
@Composable
fun PageOne() {
    val navController = LocalNavController.current!!
    val context = LocalContext.current
    val activity = context.getActivity()
    val viewModel = hiltViewModel<IndexViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val rootState by viewModel.rootState.collectAsStateWithLifecycle()
    val abiState by viewModel.abiState.collectAsStateWithLifecycle()
    val notificationState by viewModel.notificationState.collectAsStateWithLifecycle()
    val allRequiredValidated by viewModel.allRequiredValidated.collectAsStateWithLifecycle()
    val allOptionalValidated by viewModel.allOptionalValidated.collectAsStateWithLifecycle()
    val backupSavePathSaved by context.readBackupSavePathSaved().collectAsStateWithLifecycle(initialValue = false)
    val backupSavePath by context.readBackupSavePath().collectAsStateWithLifecycle(initialValue = "")
    val dialogState = LocalSlotScope.current!!.dialogSlot

    SetOnResume {
        viewModel.emitIntentOnIO(IndexUiIntent.OnResume)
    }

    SetupScaffold(
        actions = {
            AnimatedVisibility(visible = allRequiredValidated.not() || allOptionalValidated.not()) {
                OutlinedButton(
                    onClick = {
                        viewModel.launchOnIO {
                            viewModel.emitIntent(IndexUiIntent.ValidateRoot)
                            viewModel.emitIntent(IndexUiIntent.ValidateAbi)
                            viewModel.emitIntent(IndexUiIntent.ValidateNotification(context = context))
                        }
                    }
                ) {
                    Text(text = stringResource(id = R.string.grant_all))
                }
            }

            AnimatedVisibility(visible = backupSavePathSaved.not()) {
                OutlinedButton(
                    onClick = {
                        viewModel.launchOnIO {
                            if (dialogState.confirm(title = context.getString(R.string.skip_setup), text = context.getString(R.string.skip_setup_alert))) {
                                viewModel.emitIntent(IndexUiIntent.ToMain(context = activity))
                            }
                        }
                    }
                ) {
                    Text(text = stringResource(id = R.string.skip_setup))
                }
            }

            Button(
                enabled = allRequiredValidated,
                onClick = {
                    viewModel.emitIntentOnIO(IndexUiIntent.ToMain(context = activity))
                }
            ) {
                Text(text = if (backupSavePathSaved) stringResource(id = R.string.finish) else stringResource(id = R.string.get_started))
            }
        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(SizeTokens.Level20)
        ) {
            // Expressive Hero Welcome Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .paddingHorizontal(SizeTokens.Level16)
                    .paddingTop(SizeTokens.Level16),
                shape = RoundedCornerShape(28.dp),
                color = ThemedColorSchemeKeyTokens.SurfaceContainerHigh.value,
                tonalElevation = SizeTokens.Level2,
                shadowElevation = SizeTokens.Level4,
                border = BorderStroke(Dp(1.2f), ThemedColorSchemeKeyTokens.OutlineVariant.value.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier.padding(SizeTokens.Level24),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(SizeTokens.Level12)
                ) {
                    Surface(
                        modifier = Modifier.size(72.dp),
                        shape = RoundedCornerShape(20.dp),
                        color = ThemedColorSchemeKeyTokens.PrimaryContainer.value,
                        border = BorderStroke(Dp(1.5f), ThemedColorSchemeKeyTokens.Primary.value.copy(alpha = 0.35f)),
                        tonalElevation = SizeTokens.Level3,
                        shadowElevation = SizeTokens.Level3
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            AppIcon(size = 48.dp)
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.welcome_to_use),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = ThemedColorSchemeKeyTokens.OnSurface.value,
                            textAlign = TextAlign.Center
                        )
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = ThemedColorSchemeKeyTokens.Primary.value.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, ThemedColorSchemeKeyTokens.Primary.value.copy(alpha = 0.25f))
                        ) {
                            Text(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                text = "Next-Gen Android Vault • FOSS",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = ThemedColorSchemeKeyTokens.Primary.value
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(id = R.string.app_short_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = ThemedColorSchemeKeyTokens.OnSurfaceVariant.value,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = SizeTokens.Level8)
                        )
                    }
                }
            }

            // Section 1: Required Environment & Root
            Section(title = stringResource(id = R.string.required)) {
                PermissionButton(
                    title = stringResource(id = R.string.root_permission),
                    desc = stringResource(id = R.string.root_permission_desc),
                    envState = rootState,
                    onSetting = {
                        viewModel.launchOnIO {
                            val (state, su) = dialogState.edit(
                                title = context.getString(R.string.custom_su_file),
                                defValue = context.readCustomSUFile().first(),
                                label = context.getString(R.string.name),
                                desc = context.getString(R.string.restart_to_take_effect)
                            )
                            if (state.isConfirm) {
                                context.saveCustomSUFile(su)
                            }
                        }
                    }
                ) {
                    viewModel.launchOnIO {
                        viewModel.emitIntent(IndexUiIntent.ValidateRoot)
                    }
                }
                PermissionButton(
                    title = stringResource(id = R.string.abi_validation),
                    desc = uiState.abiErr.ifEmpty { context.getString(R.string.abi_validation_desc) },
                    envState = abiState,
                ) {
                    viewModel.launchOnIO {
                        viewModel.emitIntent(IndexUiIntent.ValidateAbi)
                    }
                }
            }

            // Section 2: Storage Vault Setup (Merged from PageTwo)
            Section(title = stringResource(id = R.string.storage)) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = ThemedColorSchemeKeyTokens.SurfaceContainerLowest.value,
                    border = BorderStroke(Dp(1.2f), ThemedColorSchemeKeyTokens.OutlineVariant.value.copy(alpha = 0.45f)),
                    tonalElevation = SizeTokens.Level1,
                    shadowElevation = SizeTokens.Level1,
                    onClick = { navController.navigateSingle(SetupRoutes.Directory.route) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(SizeTokens.Level16),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(SizeTokens.Level14)
                    ) {
                        Surface(
                            modifier = Modifier.size(44.dp),
                            shape = CircleShape,
                            color = if (backupSavePathSaved) ThemedColorSchemeKeyTokens.PrimaryContainer.value else ThemedColorSchemeKeyTokens.SecondaryContainer.value
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    modifier = Modifier.size(22.dp),
                                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_rounded_folder_open),
                                    tint = if (backupSavePathSaved) ThemedColorSchemeKeyTokens.OnPrimaryContainer.value else ThemedColorSchemeKeyTokens.OnSecondaryContainer.value,
                                    contentDescription = null
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = stringResource(id = R.string.backup_dir),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = ThemedColorSchemeKeyTokens.OnSurface.value
                                )
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (backupSavePathSaved) Color(0xFF2E7D32).copy(alpha = 0.15f) else ThemedColorSchemeKeyTokens.SurfaceContainerHigh.value
                                ) {
                                    Text(
                                        text = if (backupSavePathSaved) "Configured" else "Recommended",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (backupSavePathSaved) Color(0xFF2E7D32) else ThemedColorSchemeKeyTokens.OnSurfaceVariant.value,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (backupSavePathSaved) backupSavePath else stringResource(id = R.string.setup_backup_dir_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = ThemedColorSchemeKeyTokens.OnSurfaceVariant.value
                            )
                        }

                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowRight,
                            tint = ThemedColorSchemeKeyTokens.Outline.value,
                            contentDescription = null
                        )
                    }
                }
            }

            // Section 3: Optional Settings & Security (Merged, system app toggle removed)
            Section(title = stringResource(id = R.string.optional)) {
                PermissionButton(
                    title = stringResource(id = R.string.notification_permission),
                    desc = stringResource(id = R.string.notification_permission_desc),
                    envState = notificationState,
                ) {
                    viewModel.launchOnIO {
                        viewModel.emitIntent(IndexUiIntent.ValidateNotification(context = context))
                    }
                }

                Switchable(
                    key = KeyCheckKeystore,
                    defValue = true,
                    title = stringResource(id = R.string.check_keystore),
                    checkedText = stringResource(id = R.string.enabled),
                    notCheckedText = stringResource(id = R.string.not_enabled),
                    desc = stringResource(id = R.string.set_them_later_in_settings)
                )

                AnimatedVisibility(visible = backupSavePathSaved) {
                    Clickable(
                        title = stringResource(id = R.string.configurations),
                        value = stringResource(id = R.string.configurations_desc),
                    ) {
                        navController.navigateSingle(SetupRoutes.Configurations.route)
                    }
                }
            }

            Spacer(modifier = Modifier.height(SizeTokens.Level32))
        }
    }
}
