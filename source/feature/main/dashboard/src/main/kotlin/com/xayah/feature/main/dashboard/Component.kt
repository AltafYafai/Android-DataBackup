package com.xayah.feature.main.dashboard

import android.annotation.SuppressLint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
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
