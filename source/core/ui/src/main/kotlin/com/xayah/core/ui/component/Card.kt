package com.xayah.core.ui.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Indication
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.rounded.Circle
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xayah.core.datastore.readRestoreUser
import com.xayah.core.model.OperationState
import com.xayah.core.model.util.formatSize
import com.xayah.core.ui.R
import com.xayah.core.ui.material3.CardColors
import com.xayah.core.ui.material3.CardDefaults
import com.xayah.core.ui.material3.CardElevation
import com.xayah.core.ui.material3.CircularProgressIndicator
import com.xayah.core.ui.material3.ShapeDefaults
import com.xayah.core.ui.material3.tokens.OutlinedCardTokens
import com.xayah.core.ui.model.ProcessingCardItem
import com.xayah.core.ui.model.ReportAppItemInfo
import com.xayah.core.ui.model.ReportFileItemInfo
import com.xayah.core.ui.theme.ThemedColorSchemeKeyTokens
import com.xayah.core.ui.theme.value
import com.xayah.core.ui.theme.withState
import com.xayah.core.ui.token.AnimationTokens
import com.xayah.core.ui.token.SizeTokens
import com.xayah.core.ui.util.StateView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun outlinedCardBorder(enabled: Boolean = true, borderColor: Color? = null): BorderStroke {
    val tint = borderColor ?: OutlinedCardTokens.OutlineColor.value
    val color = if (enabled) {
        tint
    } else {
        tint.copy(alpha = OutlinedCardTokens.DisabledOutlineOpacity)
            .compositeOver(
                MaterialTheme.colorScheme.surfaceColorAtElevation(
                    OutlinedCardTokens.DisabledContainerElevation
                )
            )
    }
    return remember(color) { BorderStroke(OutlinedCardTokens.OutlineWidth, color) }
}

@ExperimentalFoundationApi
@ExperimentalMaterial3Api
@Composable
fun Card(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    performHapticFeedback: Boolean = false,
    shape: Shape = CardDefaults.shape,
    colors: CardColors = CardDefaults.cardColors(),
    elevation: CardElevation = CardDefaults.cardElevation(),
    border: BorderStroke? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    indication: Indication? = ripple(),
    content: @Composable ColumnScope.() -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    Surface(
        onClick = onClick,
        onLongClick = {
            if (performHapticFeedback) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onLongClick?.invoke()
        },
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        color = colors.containerColor(enabled).value,
        contentColor = colors.contentColor(enabled).value,
        tonalElevation = elevation.tonalElevation(enabled, interactionSource).value,
        shadowElevation = elevation.shadowElevation(enabled, interactionSource).value,
        border = border,
        interactionSource = interactionSource,
        indication = indication,
    ) {
        Column(content = content)
    }
}

@ExperimentalMaterial3Api
@ExperimentalFoundationApi
@Composable
fun ProcessingCard(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    state: OperationState = OperationState.IDLE,
    title: String,
    packageName: String? = null,
    expanded: Boolean = false,
    items: List<ProcessingCardItem>,
    processingIndex: Int,
    progress: Float = -1f,
    onActionBarClick: () -> Unit = {},
) {
    val totalCount by remember(items.size) { mutableIntStateOf(items.size) }
    val successCount by remember(items) { mutableIntStateOf(items.count { it.state == OperationState.DONE || it.state == OperationState.SKIP }) }
    val failedCount by remember(items) { mutableIntStateOf(items.count { it.state == OperationState.ERROR }) }

    val activeSubItem = items.getOrNull(processingIndex)
    val subItemPercent = when {
        activeSubItem?.content?.endsWith("%") == true -> activeSubItem.content.trimEnd('%').toFloatOrNull()?.let { it / 100f }
        activeSubItem != null && activeSubItem.progress >= 0f -> activeSubItem.progress
        else -> null
    }

    val computedProgress: Float = when {
        state == OperationState.DONE -> 1f
        state == OperationState.PROCESSING -> {
            if (progress >= 0f) {
                progress
            } else if (totalCount > 0) {
                val subP = subItemPercent ?: 0f
                ((successCount.toFloat() + subP) / totalCount.toFloat()).coerceIn(0.02f, 0.99f)
            } else {
                0.05f
            }
        }
        else -> 0f
    }

    val animatedProgress = animateFloatAsState(
        targetValue = computedProgress.coerceIn(0f, 1f),
        animationSpec = tween(350, easing = FastOutSlowInEasing),
        label = "card_progress"
    )

    val percentText = when {
        state == OperationState.DONE -> "100%"
        state == OperationState.PROCESSING -> {
            val pct = (animatedProgress.value * 100).toInt().coerceAtLeast(1)
            "$pct%"
        }
        state == OperationState.ERROR -> "Failed"
        else -> "Queued"
    }

    val totalBytes = items.sumOf { it.bytes }
    val sizeText = if (totalBytes > 0) totalBytes.toDouble().formatSize() else ""

    val containerColor = when (state) {
        OperationState.PROCESSING -> ThemedColorSchemeKeyTokens.PrimaryContainer.value.copy(alpha = 0.18f)
        OperationState.DONE -> ThemedColorSchemeKeyTokens.SurfaceContainerLow.value
        OperationState.ERROR -> ThemedColorSchemeKeyTokens.ErrorContainer.value.copy(alpha = 0.15f)
        else -> ThemedColorSchemeKeyTokens.SurfaceContainerLowest.value
    }

    val borderColor = when (state) {
        OperationState.PROCESSING -> ThemedColorSchemeKeyTokens.Primary.value.copy(alpha = 0.7f)
        OperationState.DONE -> ThemedColorSchemeKeyTokens.OutlineVariant.value.copy(alpha = 0.35f)
        OperationState.ERROR -> ThemedColorSchemeKeyTokens.Error.value.copy(alpha = 0.6f)
        else -> ThemedColorSchemeKeyTokens.OutlineVariant.value.copy(alpha = 0.2f)
    }

    Surface(
        modifier = modifier,
        enabled = enabled,
        shape = RoundedCornerShape(SizeTokens.Level20),
        color = containerColor,
        border = BorderStroke(if (state == OperationState.PROCESSING) 1.5.dp else 1.dp, borderColor),
        shadowElevation = if (state == OperationState.PROCESSING) SizeTokens.Level4 else SizeTokens.Level1,
        onClick = onActionBarClick
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = SizeTokens.Level16, vertical = SizeTokens.Level12),
                horizontalArrangement = Arrangement.spacedBy(SizeTokens.Level12),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon Container
                Surface(
                    modifier = Modifier.size(38.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = ThemedColorSchemeKeyTokens.SurfaceContainerHighest.value.copy(alpha = 0.5f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (packageName != null) {
                            PackageIconImage(packageName = packageName, size = 30.dp)
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.FolderOpen,
                                contentDescription = null,
                                tint = ThemedColorSchemeKeyTokens.Primary.value,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Title & Subtitle block
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = ThemedColorSchemeKeyTokens.OnSurface.value,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (packageName != null) {
                            Text(
                                text = packageName,
                                style = MaterialTheme.typography.labelSmall,
                                color = ThemedColorSchemeKeyTokens.OnSurfaceVariant.value.copy(alpha = 0.75f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                        }
                        if (sizeText.isNotEmpty()) {
                            Text(
                                text = if (packageName != null) "• $sizeText" else sizeText,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = ThemedColorSchemeKeyTokens.Primary.value,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                // Status chip & counter
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(SizeTokens.Level6)
                ) {
                    if (state == OperationState.PROCESSING) {
                        Surface(
                            shape = RoundedCornerShape(SizeTokens.Level6),
                            color = ThemedColorSchemeKeyTokens.Primary.value.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, ThemedColorSchemeKeyTokens.Primary.value.copy(alpha = 0.45f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = "⟳",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ThemedColorSchemeKeyTokens.Primary.value
                                )
                                Text(
                                    text = percentText,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ThemedColorSchemeKeyTokens.Primary.value
                                )
                            }
                        }
                    } else if (state == OperationState.DONE) {
                        Surface(
                            shape = CircleShape,
                            color = ThemedColorSchemeKeyTokens.PrimaryContainer.value.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, ThemedColorSchemeKeyTokens.Primary.value.copy(alpha = 0.35f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = "✓",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ThemedColorSchemeKeyTokens.OnPrimaryContainer.value
                                )
                                Text(
                                    text = "Saved",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ThemedColorSchemeKeyTokens.OnPrimaryContainer.value
                                )
                            }
                        }
                    } else if (state == OperationState.ERROR) {
                        Surface(
                            shape = CircleShape,
                            color = ThemedColorSchemeKeyTokens.ErrorContainer.value.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, ThemedColorSchemeKeyTokens.Error.value.copy(alpha = 0.4f))
                        ) {
                            Text(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                text = "Failed",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = ThemedColorSchemeKeyTokens.OnErrorContainer.value
                            )
                        }
                    }

                    if (totalCount > 0) {
                        Text(
                            text = "${successCount + failedCount}/$totalCount",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = ThemedColorSchemeKeyTokens.OnSurfaceVariant.value.copy(alpha = 0.8f),
                            fontSize = 10.sp
                        )
                    }

                    Icon(
                        imageVector = if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                        contentDescription = null,
                        tint = ThemedColorSchemeKeyTokens.OnSurfaceVariant.value,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Live progress line under header
            if (state == OperationState.PROCESSING) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(ThemedColorSchemeKeyTokens.SurfaceContainerHighest.value)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedProgress.value.coerceIn(0.02f, 1f))
                            .fillMaxHeight()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        ThemedColorSchemeKeyTokens.Primary.value,
                                        ThemedColorSchemeKeyTokens.Secondary.value
                                    )
                                )
                            )
                    )
                }
            }

            // Expanded telemetry partition list
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = SizeTokens.Level12, vertical = SizeTokens.Level8),
                    verticalArrangement = Arrangement.spacedBy(SizeTokens.Level6)
                ) {
                    items.forEachIndexed { index, item ->
                        val isSubActive = (processingIndex == index) && (state == OperationState.PROCESSING)
                        val isSubDone = item.state == OperationState.DONE || (index < processingIndex && state == OperationState.PROCESSING)
                        val isSubSkip = item.state == OperationState.SKIP
                        val isSubError = item.state == OperationState.ERROR

                        val rawSubTitle = item.title
                            .replace(Regex("^(Backup|Restore)\\s*", RegexOption.IGNORE_CASE), "")
                            .trim()

                        val cleanSubTitle = when (rawSubTitle.uppercase()) {
                            "APK" -> "App"
                            "USER" -> "Data"
                            "USER_DE" -> "Encrypted"
                            "DATA" -> "Storage"
                            "OBB" -> "Assets"
                            "MEDIA" -> "Media"
                            else -> rawSubTitle
                        }

                        val subSizeText = if (item.bytes > 0) item.bytes.toDouble().formatSize() else ""

                        val subPctText = when {
                            isSubDone -> "Done"
                            isSubActive -> {
                                when {
                                    item.content.endsWith("%") -> item.content
                                    item.progress > 0f -> "${(item.progress * 100).toInt()}%"
                                    else -> "Saving..."
                                }
                            }
                            isSubSkip -> "Skipped"
                            isSubError -> "Failed"
                            else -> "Queued"
                        }

                        val subBgColor = when {
                            isSubActive -> ThemedColorSchemeKeyTokens.PrimaryContainer.value.copy(alpha = 0.28f)
                            isSubDone -> ThemedColorSchemeKeyTokens.SurfaceContainerHighest.value.copy(alpha = 0.35f)
                            isSubError -> ThemedColorSchemeKeyTokens.ErrorContainer.value.copy(alpha = 0.2f)
                            else -> ThemedColorSchemeKeyTokens.SurfaceContainerHighest.value.copy(alpha = 0.2f)
                        }

                        val subContentColor = when {
                            isSubActive -> ThemedColorSchemeKeyTokens.Primary.value
                            isSubDone -> ThemedColorSchemeKeyTokens.OnSurface.value
                            isSubError -> ThemedColorSchemeKeyTokens.Error.value
                            else -> ThemedColorSchemeKeyTokens.OnSurfaceVariant.value.copy(alpha = 0.65f)
                        }

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(SizeTokens.Level10),
                            color = subBgColor,
                            border = if (isSubActive) BorderStroke(1.dp, ThemedColorSchemeKeyTokens.Primary.value.copy(alpha = 0.5f)) else null
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = SizeTokens.Level12, vertical = SizeTokens.Level8),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(SizeTokens.Level8)
                                ) {
                                    Text(
                                        text = when {
                                            isSubDone -> "✓"
                                            isSubActive -> "⟳"
                                            isSubSkip -> "⊘"
                                            isSubError -> "✕"
                                            else -> "○"
                                        },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = subContentColor
                                    )

                                    Text(
                                        modifier = Modifier.weight(1f),
                                        text = cleanSubTitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = if (isSubActive) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSubActive) ThemedColorSchemeKeyTokens.Primary.value else ThemedColorSchemeKeyTokens.OnSurface.value,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    if (subSizeText.isNotEmpty()) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = ThemedColorSchemeKeyTokens.SurfaceContainerHighest.value.copy(alpha = 0.5f)
                                        ) {
                                            Text(
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                                text = subSizeText,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Normal,
                                                color = ThemedColorSchemeKeyTokens.OnSurfaceVariant.value
                                            )
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (isSubActive) ThemedColorSchemeKeyTokens.Primary.value.copy(alpha = 0.15f) else ThemedColorSchemeKeyTokens.SurfaceContainerHighest.value.copy(alpha = 0.5f),
                                        border = if (isSubActive) BorderStroke(0.5.dp, ThemedColorSchemeKeyTokens.Primary.value.copy(alpha = 0.35f)) else null
                                    ) {
                                        Text(
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            text = subPctText,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSubActive) ThemedColorSchemeKeyTokens.Primary.value else subContentColor
                                        )
                                    }
                                }

                                if (isSubActive) {
                                    val activeSubP = when {
                                        item.content.endsWith("%") -> item.content.trimEnd('%').toFloatOrNull()?.let { it / 100f } ?: 0.05f
                                        item.progress > 0f -> item.progress
                                        else -> 0.05f
                                    }
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(2.dp)
                                            .background(ThemedColorSchemeKeyTokens.SurfaceContainerHighest.value)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(activeSubP.coerceIn(0.02f, 1f))
                                                .fillMaxHeight()
                                                .background(ThemedColorSchemeKeyTokens.Primary.value)
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

@ExperimentalFoundationApi
@Composable
private fun ReportItem(
    enabled: Boolean = true,
    icon: ImageVector,
    iconTint: ThemedColorSchemeKeyTokens = ThemedColorSchemeKeyTokens.OnSurfaceVariant,
    title: String,
    titleTint: ThemedColorSchemeKeyTokens = ThemedColorSchemeKeyTokens.OnSurfaceVariant,
    content: String,
    expandedContent: (@Composable () -> Unit)? = null
) {
    val expandable by remember(expandedContent) { mutableStateOf(expandedContent != null) }
    var expanded by remember { mutableStateOf(true) }

    Column {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            enabled = true,
            color = ThemedColorSchemeKeyTokens.Transparent.value.withState(enabled),
            onClick = {
                if (expandable)
                    expanded = expanded.not()
            }
        ) {
            Row(
                modifier = Modifier.padding(SizeTokens.Level16),
                horizontalArrangement = Arrangement.spacedBy(SizeTokens.Level16),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    modifier = Modifier.size(SizeTokens.Level24),
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint.value.withState(enabled)
                )
                TitleSmallText(modifier = Modifier.weight(1f), text = title, color = titleTint.value.withState(enabled))
                LabelSmallText(text = content, color = ThemedColorSchemeKeyTokens.OnSurfaceVariant.value.withState(enabled))
                if (expandable)
                    Icon(
                        imageVector = if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                        contentDescription = null,
                        tint = ThemedColorSchemeKeyTokens.OnSurface.value.withState(enabled)
                    )
            }
        }
        AnimatedVisibility(expanded) {
            expandedContent?.invoke()
        }
    }
}

@ExperimentalFoundationApi
@Composable
private fun ReportAppItem(enabled: Boolean, color: ThemedColorSchemeKeyTokens = ThemedColorSchemeKeyTokens.OnSurfaceVariant, item: ReportAppItemInfo, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        enabled = true,
        color = ThemedColorSchemeKeyTokens.Transparent.value.withState(enabled),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(SizeTokens.Level16),
            horizontalArrangement = Arrangement.spacedBy(SizeTokens.Level16),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.size(SizeTokens.Level24))
            LabelMediumText(modifier = Modifier.weight(1f), text = item.label, color = color.value.withState(enabled))
            LabelSmallText(text = item.user, color = ThemedColorSchemeKeyTokens.OnSurfaceVariant.value.withState(enabled))
            PackageIconImage(packageName = item.packageName, size = SizeTokens.Level24)
        }
    }
}

@ExperimentalFoundationApi
@Composable
private fun ReportFileItem(enabled: Boolean, color: ThemedColorSchemeKeyTokens = ThemedColorSchemeKeyTokens.OnSurfaceVariant, item: ReportFileItemInfo, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        enabled = true,
        color = ThemedColorSchemeKeyTokens.Transparent.value.withState(enabled),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(SizeTokens.Level16),
            horizontalArrangement = Arrangement.spacedBy(SizeTokens.Level16),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.size(SizeTokens.Level24))
            LabelMediumText(modifier = Modifier.weight(1f), text = item.name, color = color.value.withState(enabled))
        }
    }
}

@ExperimentalMaterial3Api
@ExperimentalFoundationApi
@Composable
fun AppsReportCard(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    scope: CoroutineScope,
    pagerState: PagerState,
    title: String,
    timer: String,
    showRestoreUser: Boolean = false,
    packageSize: String,
    succeed: List<ReportAppItemInfo>,
    failed: List<ReportAppItemInfo>,
) {
    Card(
        modifier = modifier,
        enabled = enabled,
        colors = CardDefaults.cardColors(containerColor = ThemedColorSchemeKeyTokens.SurfaceVariantDim.value.withState(enabled)),
        indication = null,
    ) {
        Column {
            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = enabled,
                    color = ThemedColorSchemeKeyTokens.SurfaceVariant.value.withState(enabled),
                    shape = ShapeDefaults.Medium,
                    onClick = {}
                ) {
                    Row(modifier = Modifier.padding(SizeTokens.Level16), horizontalArrangement = Arrangement.spacedBy(SizeTokens.Level16), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            modifier = Modifier.size(SizeTokens.Level24),
                            imageVector = Icons.Outlined.Description,
                            contentDescription = null,
                            tint = ThemedColorSchemeKeyTokens.OnSurfaceVariant.value.withState(enabled)
                        )

                        TitleMediumText(
                            modifier = Modifier.weight(1f),
                            text = title,
                            color = ThemedColorSchemeKeyTokens.OnSurface.value.withState(enabled)
                        )

                        LabelSmallText(text = packageSize, color = ThemedColorSchemeKeyTokens.OnSurfaceVariant.value.withState(enabled))
                    }
                }
                Column(modifier = Modifier.fillMaxWidth()) {
                    ReportItem(
                        icon = Icons.Filled.Timer,
                        iconTint = ThemedColorSchemeKeyTokens.Primary,
                        title = stringResource(id = R.string.time),
                        titleTint = ThemedColorSchemeKeyTokens.Primary,
                        content = timer
                    )
                    if (showRestoreUser) {
                        val context = LocalContext.current
                        val restoreUser by context.readRestoreUser().collectAsStateWithLifecycle(initialValue = -1)
                        ReportItem(
                            icon = Icons.Filled.AccountCircle,
                            iconTint = ThemedColorSchemeKeyTokens.YellowPrimary,
                            title = stringResource(id = R.string.restore_user),
                            titleTint = ThemedColorSchemeKeyTokens.YellowPrimary,
                            content = if (restoreUser == -1) stringResource(id = R.string.backup_user) else restoreUser.toString()
                        )
                    }

                    ReportItem(
                        icon = ImageVector.vectorResource(id = R.drawable.ic_rounded_cancel_circle),
                        iconTint = ThemedColorSchemeKeyTokens.Error,
                        title = stringResource(id = R.string.failed),
                        titleTint = ThemedColorSchemeKeyTokens.Error,
                        content = failed.size.toString(),
                        expandedContent = if (failed.isEmpty()) null else {
                            {
                                Column {
                                    failed.forEach {
                                        ReportAppItem(enabled = enabled, color = ThemedColorSchemeKeyTokens.Error, item = it) {
                                            scope.launch {
                                                pagerState.animateScrollToPage(it.index)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    )
                    ReportItem(
                        icon = ImageVector.vectorResource(id = R.drawable.ic_rounded_check_circle),
                        iconTint = ThemedColorSchemeKeyTokens.GreenPrimary,
                        title = stringResource(id = R.string.succeed),
                        titleTint = ThemedColorSchemeKeyTokens.GreenPrimary,
                        content = succeed.size.toString(),
                        expandedContent = if (succeed.isEmpty()) null else {
                            {
                                Column {
                                    succeed.forEach {
                                        ReportAppItem(enabled = enabled, color = ThemedColorSchemeKeyTokens.GreenPrimary, item = it) {
                                            scope.launch {
                                                pagerState.animateScrollToPage(it.index)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@ExperimentalMaterial3Api
@ExperimentalFoundationApi
@Composable
fun FilesReportCard(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    scope: CoroutineScope,
    pagerState: PagerState,
    title: String,
    timer: String,
    showRestoreUser: Boolean = false,
    mediaSize: String,
    succeed: List<ReportFileItemInfo>,
    failed: List<ReportFileItemInfo>,
) {
    Card(
        modifier = modifier,
        enabled = enabled,
        colors = CardDefaults.cardColors(containerColor = ThemedColorSchemeKeyTokens.SurfaceVariantDim.value.withState(enabled)),
        indication = null,
    ) {
        Column {
            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = enabled,
                    color = ThemedColorSchemeKeyTokens.SurfaceVariant.value.withState(enabled),
                    shape = ShapeDefaults.Medium,
                    onClick = {}
                ) {
                    Row(modifier = Modifier.padding(SizeTokens.Level16), horizontalArrangement = Arrangement.spacedBy(SizeTokens.Level16), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            modifier = Modifier.size(SizeTokens.Level24),
                            imageVector = Icons.Outlined.Description,
                            contentDescription = null,
                            tint = ThemedColorSchemeKeyTokens.OnSurfaceVariant.value.withState(enabled)
                        )

                        TitleMediumText(
                            modifier = Modifier.weight(1f),
                            text = title,
                            color = ThemedColorSchemeKeyTokens.OnSurface.value.withState(enabled)
                        )

                        LabelSmallText(text = mediaSize, color = ThemedColorSchemeKeyTokens.OnSurfaceVariant.value.withState(enabled))
                    }
                }
                Column(modifier = Modifier.fillMaxWidth()) {
                    ReportItem(
                        icon = Icons.Filled.Timer,
                        iconTint = ThemedColorSchemeKeyTokens.Primary,
                        title = stringResource(id = R.string.time),
                        titleTint = ThemedColorSchemeKeyTokens.Primary,
                        content = timer
                    )
                    if (showRestoreUser) {
                        val context = LocalContext.current
                        val restoreUser by context.readRestoreUser().collectAsStateWithLifecycle(initialValue = -1)
                        ReportItem(
                            icon = Icons.Filled.AccountCircle,
                            iconTint = ThemedColorSchemeKeyTokens.YellowPrimary,
                            title = stringResource(id = R.string.restore_user),
                            titleTint = ThemedColorSchemeKeyTokens.YellowPrimary,
                            content = if (restoreUser == -1) stringResource(id = R.string.backup_user) else restoreUser.toString()
                        )
                    }

                    ReportItem(
                        icon = ImageVector.vectorResource(id = R.drawable.ic_rounded_cancel_circle),
                        iconTint = ThemedColorSchemeKeyTokens.Error,
                        title = stringResource(id = R.string.failed),
                        titleTint = ThemedColorSchemeKeyTokens.Error,
                        content = failed.size.toString(),
                        expandedContent = if (failed.isEmpty()) null else {
                            {
                                Column {
                                    failed.forEach {
                                        ReportFileItem(enabled = enabled, color = ThemedColorSchemeKeyTokens.Error, item = it) {
                                            scope.launch {
                                                pagerState.animateScrollToPage(it.index)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    )
                    ReportItem(
                        icon = ImageVector.vectorResource(id = R.drawable.ic_rounded_check_circle),
                        iconTint = ThemedColorSchemeKeyTokens.GreenPrimary,
                        title = stringResource(id = R.string.succeed),
                        titleTint = ThemedColorSchemeKeyTokens.GreenPrimary,
                        content = succeed.size.toString(),
                        expandedContent = if (succeed.isEmpty()) null else {
                            {
                                Column {
                                    succeed.forEach {
                                        ReportFileItem(enabled = enabled, color = ThemedColorSchemeKeyTokens.GreenPrimary, item = it) {
                                            scope.launch {
                                                pagerState.animateScrollToPage(it.index)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@ExperimentalMaterial3Api
@Composable
fun OverviewCard(
    modifier: Modifier = Modifier,
    title: String,
    icon: ImageVector,
    colorContainer: ThemedColorSchemeKeyTokens,
    onColorContainer: ThemedColorSchemeKeyTokens,
    content: @Composable ColumnScope.() -> Unit,
    actionIcon: ImageVector?,
    onClick: () -> Unit = {},
) {
    androidx.compose.material3.Card(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(SizeTokens.Level20),
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = colorContainer.value),
        elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = SizeTokens.Level1),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .padding(SizeTokens.Level20)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SizeTokens.Level16)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier
                        .paddingBottom(SizeTokens.Level8)
                        .height(IntrinsicSize.Min),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(SizeTokens.Level6)
                ) {
                    Icon(
                        modifier = Modifier.intrinsicIcon(),
                        imageVector = icon,
                        tint = onColorContainer.value,
                        contentDescription = null,
                    )
                    LabelLargeText(
                        text = title,
                        color = onColorContainer.value,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                content()
            }

            if (actionIcon != null)
                Icon(
                    imageVector = actionIcon,
                    tint = onColorContainer.value,
                    contentDescription = null
                )
        }
    }
}
