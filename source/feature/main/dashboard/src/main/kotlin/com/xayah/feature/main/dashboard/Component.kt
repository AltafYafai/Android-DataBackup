package com.xayah.feature.main.dashboard

import android.annotation.SuppressLint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.util.Calendar
import com.xayah.core.ui.component.ActionButton
import com.xayah.core.ui.component.AutoLabelLargeText
import com.xayah.core.ui.component.BodyMediumText
import com.xayah.core.ui.component.OverviewCard
import com.xayah.core.ui.component.SegmentProgressIndicator
import com.xayah.core.ui.component.TitleLargeText
import com.xayah.core.ui.component.TitleMediumText
import com.xayah.core.ui.component.paddingBottom
import com.xayah.core.ui.component.paddingTop
import com.xayah.core.ui.model.SegmentProgress
import com.xayah.core.ui.theme.ThemedColorSchemeKeyTokens
import com.xayah.core.ui.theme.value
import com.xayah.core.ui.theme.withState
import com.xayah.core.ui.token.SizeTokens
import com.xayah.core.util.DateUtil

@SuppressLint("StringFormatInvalid")
@ExperimentalMaterial3Api
@Composable
fun OverviewStorageCard(
    title: String,
    used: SegmentProgress? = null,
    backupUsed: SegmentProgress? = null,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    OverviewCard(
        title = stringResource(id = R.string.storage),
        icon = ImageVector.vectorResource(id = R.drawable.ic_rounded_folder_open),
        colorContainer = ThemedColorSchemeKeyTokens.SecondaryContainer,
        onColorContainer = ThemedColorSchemeKeyTokens.OnSecondaryContainer,
        content = {
            TitleLargeText(text = title, color = ThemedColorSchemeKeyTokens.OnSurface.value)
            if (used != null && used.progress.isNaN().not()) {
                SegmentProgressIndicator(
                    modifier = Modifier
                        .paddingTop(SizeTokens.Level8)
                        .paddingBottom(SizeTokens.Level4),
                    enabled = true,
                    progress = used.progress,
                    color = ThemedColorSchemeKeyTokens.Secondary,
                    trackColor = ThemedColorSchemeKeyTokens.SecondaryL80D20,
                )

                BodyMediumText(
                    enabled = true,
                    text = "${context.getString(R.string.args_used, (used.progress * 100).toInt())} (${used.usedFormat} / ${used.totalFormat})",
                    color = ThemedColorSchemeKeyTokens.OnSurfaceVariant.value,
                )
            }
            if (backupUsed != null && backupUsed.progress.isNaN().not()) {
                SegmentProgressIndicator(
                    modifier = Modifier
                        .paddingTop(SizeTokens.Level8)
                        .paddingBottom(SizeTokens.Level4),
                    enabled = true,
                    progress = backupUsed.progress,
                    color = ThemedColorSchemeKeyTokens.Primary,
                    trackColor = ThemedColorSchemeKeyTokens.SecondaryL80D20,
                )
                BodyMediumText(
                    enabled = true,
                    text = "${context.getString(R.string.args_used_by_backups, (backupUsed.progress * 100).toInt())} (${backupUsed.usedFormat} / ${backupUsed.totalFormat})",
                    color = ThemedColorSchemeKeyTokens.OnSurfaceVariant.value,
                )
            }
        },
        actionIcon = Icons.Outlined.Settings,
        onClick = onClick
    )
}

@SuppressLint("StringFormatInvalid")
@ExperimentalMaterial3Api
@Composable
fun OverviewLastBackupCard(nullBackupDir: Boolean, lastBackupTime: Long, onClick: () -> Unit) {
    val context = LocalContext.current
    val relativeTime by remember(lastBackupTime) {
        mutableStateOf(DateUtil.getShortRelativeTimeSpanString(context = context, time1 = lastBackupTime, time2 = DateUtil.getTimestamp()))
    }
    val finishTime by remember(lastBackupTime) {
        mutableStateOf(context.getString(R.string.args_finished_at, DateUtil.formatTimestamp(lastBackupTime, DateUtil.PATTERN_FINISH)))
    }
    OverviewCard(
        title = stringResource(id = R.string.last_backup),
        icon = ImageVector.vectorResource(id = R.drawable.ic_rounded_package_2),
        colorContainer = if (nullBackupDir) ThemedColorSchemeKeyTokens.SurfaceContainerHighBaselineFixed else ThemedColorSchemeKeyTokens.PrimaryContainer,
        onColorContainer = if (nullBackupDir) ThemedColorSchemeKeyTokens.OnSurface else ThemedColorSchemeKeyTokens.OnPrimaryContainer,
        content = {
            TitleLargeText(
                text = (if (nullBackupDir || lastBackupTime == 0L) stringResource(id = R.string.never) else relativeTime),
                color = (if (nullBackupDir) ThemedColorSchemeKeyTokens.OnSurfaceVariant else ThemedColorSchemeKeyTokens.OnSurface).value
            )
            if (nullBackupDir || lastBackupTime != 0L)
                BodyMediumText(
                    text = if (nullBackupDir) stringResource(id = R.string.setup_required) else finishTime,
                    color = ThemedColorSchemeKeyTokens.OnSurfaceVariant.value,
                )
        },
        actionIcon = if (nullBackupDir) Icons.Rounded.KeyboardArrowRight else null,
        onClick = onClick,
    )
}

@ExperimentalFoundationApi
@ExperimentalMaterial3Api
@Composable
fun QuickActionsButton(
    modifier: Modifier = Modifier,
    enabled: Boolean,
    title: String,
    icon: ImageVector,
    colorContainer: ThemedColorSchemeKeyTokens,
    colorL80D20: ThemedColorSchemeKeyTokens,
    onColorContainer: ThemedColorSchemeKeyTokens,
    actionIcon: ImageVector? = null,
    onClick: () -> Unit = {},
) {
    ActionButton(
        modifier = modifier,
        enabled = enabled,
        icon = icon,
        colorContainer = colorContainer,
        colorL80D20 = colorL80D20,
        onColorContainer = onColorContainer,
        trailingIcon = {
            if (actionIcon != null)
                Icon(
                    imageVector = actionIcon,
                    tint = onColorContainer.value.withState(enabled),
                    contentDescription = null
                )
        },
        onClick = onClick
    ) {
        AutoLabelLargeText(modifier = Modifier.weight(1f), text = title, color = onColorContainer.value.withState(enabled), enabled = enabled)
    }
}

@ExperimentalFoundationApi
@ExperimentalMaterial3Api
@Composable
fun DashboardActionTile(
    modifier: Modifier = Modifier,
    enabled: Boolean,
    title: String,
    subtitle: String,
    icon: ImageVector,
    colorContainer: ThemedColorSchemeKeyTokens,
    onColorContainer: ThemedColorSchemeKeyTokens,
    onClick: () -> Unit = {},
) {
    val interactionSource = remember { MutableInteractionSource() }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(SizeTokens.Level20),
        color = ThemedColorSchemeKeyTokens.SurfaceContainerLowest.value.withState(enabled),
        tonalElevation = SizeTokens.Level1,
        shadowElevation = SizeTokens.Level1,
        border = BorderStroke(Dp(1f), ThemedColorSchemeKeyTokens.OutlineVariant.value.copy(alpha = 0.5f)),
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
    ) {
        Row(
            modifier = Modifier
                .padding(SizeTokens.Level16)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SizeTokens.Level16)
        ) {
            Surface(
                modifier = Modifier.size(SizeTokens.Level48),
                shape = CircleShape,
                color = colorContainer.value.withState(enabled),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        modifier = Modifier.size(SizeTokens.Level24),
                        imageVector = icon,
                        tint = onColorContainer.value.withState(enabled),
                        contentDescription = null,
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                TitleMediumText(
                    text = title,
                    color = ThemedColorSchemeKeyTokens.OnSurface.value.withState(enabled),
                    fontWeight = FontWeight.Bold
                )
                BodyMediumText(
                    modifier = Modifier.paddingTop(SizeTokens.Level2),
                    text = subtitle,
                    color = ThemedColorSchemeKeyTokens.OnSurfaceVariant.value.withState(enabled)
                )
            }

            Icon(
                imageVector = Icons.Rounded.KeyboardArrowRight,
                tint = ThemedColorSchemeKeyTokens.Outline.value.withState(enabled),
                contentDescription = null
            )
        }
    }
}

@Composable
fun DashboardHeroStatusCard(
    nullBackupDir: Boolean,
    lastBackupTime: Long,
    appsCount: Long,
    backedUpAppsCount: Long,
    filesCount: Long,
    backedUpFilesCount: Long,
    onStatusClick: () -> Unit,
) {
    val context = LocalContext.current
    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 4..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            else -> "Good evening"
        }
    }
    val relativeTime by remember(lastBackupTime) {
        mutableStateOf(DateUtil.getShortRelativeTimeSpanString(context = context, time1 = lastBackupTime, time2 = DateUtil.getTimestamp()))
    }
    val isProtected = !nullBackupDir && lastBackupTime > 0L

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = ThemedColorSchemeKeyTokens.SurfaceContainerHigh.value,
        tonalElevation = SizeTokens.Level2,
        shadowElevation = SizeTokens.Level4,
        border = BorderStroke(Dp(1.2f), ThemedColorSchemeKeyTokens.OutlineVariant.value.copy(alpha = 0.4f)),
        onClick = onStatusClick,
    ) {
        Column(
            modifier = Modifier.padding(SizeTokens.Level20),
            verticalArrangement = Arrangement.spacedBy(SizeTokens.Level16)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = greeting,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = ThemedColorSchemeKeyTokens.OnSurface.value
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (nullBackupDir) stringResource(id = R.string.setup_required)
                               else if (lastBackupTime == 0L) "No backups created yet"
                               else "Last backup: $relativeTime",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ThemedColorSchemeKeyTokens.OnSurfaceVariant.value
                    )
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isProtected) ThemedColorSchemeKeyTokens.PrimaryContainer.value else ThemedColorSchemeKeyTokens.ErrorContainer.value,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(8.dp),
                            shape = CircleShape,
                            color = if (isProtected) ThemedColorSchemeKeyTokens.Primary.value else ThemedColorSchemeKeyTokens.Error.value
                        ) {}
                        Text(
                            text = if (nullBackupDir) "Setup" else if (isProtected) "Protected" else "At Risk",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isProtected) ThemedColorSchemeKeyTokens.OnPrimaryContainer.value else ThemedColorSchemeKeyTokens.OnErrorContainer.value
                        )
                    }
                }
            }

            // Quick Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(SizeTokens.Level12)
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    color = ThemedColorSchemeKeyTokens.SurfaceContainerLowest.value,
                    border = BorderStroke(Dp(1f), ThemedColorSchemeKeyTokens.OutlineVariant.value.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(SizeTokens.Level12),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(SizeTokens.Level10)
                    ) {
                        Surface(
                            modifier = Modifier.size(36.dp),
                            shape = CircleShape,
                            color = ThemedColorSchemeKeyTokens.PrimaryContainer.value
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    modifier = Modifier.size(20.dp),
                                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_rounded_package_2),
                                    tint = ThemedColorSchemeKeyTokens.OnPrimaryContainer.value,
                                    contentDescription = null
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "$backedUpAppsCount / $appsCount",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = ThemedColorSchemeKeyTokens.OnSurface.value
                            )
                            Text(
                                text = "Apps Saved",
                                style = MaterialTheme.typography.labelSmall,
                                color = ThemedColorSchemeKeyTokens.OnSurfaceVariant.value
                            )
                        }
                    }
                }

                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    color = ThemedColorSchemeKeyTokens.SurfaceContainerLowest.value,
                    border = BorderStroke(Dp(1f), ThemedColorSchemeKeyTokens.OutlineVariant.value.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(SizeTokens.Level12),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(SizeTokens.Level10)
                    ) {
                        Surface(
                            modifier = Modifier.size(36.dp),
                            shape = CircleShape,
                            color = ThemedColorSchemeKeyTokens.SecondaryContainer.value
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    modifier = Modifier.size(20.dp),
                                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_rounded_folder_open),
                                    tint = ThemedColorSchemeKeyTokens.OnSecondaryContainer.value,
                                    contentDescription = null
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "$backedUpFilesCount / $filesCount",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = ThemedColorSchemeKeyTokens.OnSurface.value
                            )
                            Text(
                                text = "Files Saved",
                                style = MaterialTheme.typography.labelSmall,
                                color = ThemedColorSchemeKeyTokens.OnSurfaceVariant.value
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardExpressiveActionCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    badgeText: String,
    icon: ImageVector,
    containerColor: ThemedColorSchemeKeyTokens,
    onContainerColor: ThemedColorSchemeKeyTokens,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = ThemedColorSchemeKeyTokens.SurfaceContainerLowest.value,
        tonalElevation = SizeTokens.Level2,
        shadowElevation = SizeTokens.Level2,
        border = BorderStroke(Dp(1.2f), ThemedColorSchemeKeyTokens.OutlineVariant.value.copy(alpha = 0.45f)),
        onClick = onClick,
        interactionSource = interactionSource,
    ) {
        Column(
            modifier = Modifier.padding(SizeTokens.Level16),
            verticalArrangement = Arrangement.spacedBy(SizeTokens.Level12)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    color = containerColor.value,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            modifier = Modifier.size(24.dp),
                            imageVector = icon,
                            tint = onContainerColor.value,
                            contentDescription = null,
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = ThemedColorSchemeKeyTokens.SurfaceContainerHigh.value,
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = ThemedColorSchemeKeyTokens.OnSurfaceVariant.value,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ThemedColorSchemeKeyTokens.OnSurface.value
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = ThemedColorSchemeKeyTokens.OnSurfaceVariant.value,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun DashboardStorageCardExpressive(
    title: String,
    used: SegmentProgress? = null,
    backupUsed: SegmentProgress? = null,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = ThemedColorSchemeKeyTokens.SurfaceContainerLowest.value,
        tonalElevation = SizeTokens.Level2,
        shadowElevation = SizeTokens.Level2,
        border = BorderStroke(Dp(1.2f), ThemedColorSchemeKeyTokens.OutlineVariant.value.copy(alpha = 0.45f)),
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier.padding(SizeTokens.Level20),
            verticalArrangement = Arrangement.spacedBy(SizeTokens.Level12)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(SizeTokens.Level10)
                ) {
                    Surface(
                        modifier = Modifier.size(36.dp),
                        shape = CircleShape,
                        color = ThemedColorSchemeKeyTokens.SecondaryContainer.value
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                modifier = Modifier.size(20.dp),
                                imageVector = ImageVector.vectorResource(id = R.drawable.ic_rounded_folder_open),
                                tint = ThemedColorSchemeKeyTokens.OnSecondaryContainer.value,
                                contentDescription = null
                            )
                        }
                    }

                    Column {
                        Text(
                            text = stringResource(id = R.string.storage),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = ThemedColorSchemeKeyTokens.OnSurface.value
                        )
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodySmall,
                            color = ThemedColorSchemeKeyTokens.OnSurfaceVariant.value
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = ThemedColorSchemeKeyTokens.SurfaceContainerHigh.value,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            tint = ThemedColorSchemeKeyTokens.OnSurfaceVariant.value,
                            contentDescription = "Configure storage",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            if (used != null && !used.progress.isNaN()) {
                Column(verticalArrangement = Arrangement.spacedBy(SizeTokens.Level6)) {
                    SegmentProgressIndicator(
                        modifier = Modifier.paddingTop(SizeTokens.Level4),
                        enabled = true,
                        progress = used.progress,
                        color = ThemedColorSchemeKeyTokens.Secondary,
                        trackColor = ThemedColorSchemeKeyTokens.SecondaryL80D20,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${context.getString(R.string.args_used, (used.progress * 100).toInt())} (${used.usedFormat} / ${used.totalFormat})",
                            style = MaterialTheme.typography.bodySmall,
                            color = ThemedColorSchemeKeyTokens.OnSurfaceVariant.value
                        )
                    }
                }
            }

            if (backupUsed != null && !backupUsed.progress.isNaN()) {
                Column(verticalArrangement = Arrangement.spacedBy(SizeTokens.Level6)) {
                    SegmentProgressIndicator(
                        enabled = true,
                        progress = backupUsed.progress,
                        color = ThemedColorSchemeKeyTokens.Primary,
                        trackColor = ThemedColorSchemeKeyTokens.SecondaryL80D20,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${context.getString(R.string.args_used_by_backups, (backupUsed.progress * 100).toInt())} (${backupUsed.usedFormat})",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = ThemedColorSchemeKeyTokens.Primary.value
                        )
                    }
                }
            }
        }
    }
}
