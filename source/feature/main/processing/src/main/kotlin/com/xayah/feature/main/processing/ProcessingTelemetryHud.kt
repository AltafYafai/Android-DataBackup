package com.xayah.feature.main.processing

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xayah.core.model.OperationState
import com.xayah.core.model.util.formatSize
import com.xayah.core.ui.component.PackageIconImage
import com.xayah.core.ui.model.ProcessingCardItem
import com.xayah.core.ui.theme.ThemedColorSchemeKeyTokens
import com.xayah.core.ui.theme.value
import com.xayah.core.ui.token.SizeTokens

@OptIn(ExperimentalAnimationGraphicsApi::class, ExperimentalFoundationApi::class)
@Composable
fun ProcessingTelemetryHud(
    modifier: Modifier = Modifier,
    isFinished: Boolean,
    title: String,
    subtitle: String,
    packageName: String?,
    statusChipText: String,
    statusChipColor: Color,
    // Track 1 (Overall Batch)
    batchCurrent: Int,
    batchTotal: Int,
    batchProgress: Float,
    // Track 2 (Active Stream)
    streamTitle: String,
    streamProgress: Float,
    // Style 3 Partition Pipeline Cells
    partitions: List<ProcessingCardItem>,
    activePartitionIndex: Int,
    // Telemetry Footer
    telemetryStats: String? = null,
) {
    val animatedBatchProgress = animateFloatAsState(
        targetValue = if (batchProgress.isNaN() || batchProgress < 0f) 0f else batchProgress.coerceIn(0f, 1f),
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "batch_progress"
    )

    val animatedStreamProgress = animateFloatAsState(
        targetValue = if (streamProgress.isNaN() || streamProgress < 0f) 0f else streamProgress.coerceIn(0f, 1f),
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "stream_progress"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pulse_transition")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    val animatedImageVector = AnimatedImageVector.animatedVectorResource(com.xayah.core.ui.R.drawable.ic_animated_tick)
    var atEnd by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = SizeTokens.Level16, vertical = SizeTokens.Level8),
        shape = RoundedCornerShape(SizeTokens.Level20),
        color = ThemedColorSchemeKeyTokens.SurfaceContainerLowest.value,
        tonalElevation = SizeTokens.Level2,
        shadowElevation = SizeTokens.Level4,
        border = BorderStroke(1.dp, ThemedColorSchemeKeyTokens.OutlineVariant.value.copy(alpha = 0.35f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SizeTokens.Level16),
            verticalArrangement = Arrangement.spacedBy(SizeTokens.Level12)
        ) {
            // 1. Header Row: Icon + Title/Subtitle + Status Chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(SizeTokens.Level12)
            ) {
                if (isFinished) {
                    LaunchedEffect(animatedImageVector) {
                        atEnd = true
                    }
                    Surface(
                        modifier = Modifier.size(SizeTokens.Level48),
                        shape = RoundedCornerShape(SizeTokens.Level12),
                        color = ThemedColorSchemeKeyTokens.PrimaryContainer.value.copy(alpha = 0.5f),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Image(
                                painter = rememberAnimatedVectorPainter(animatedImageVector, atEnd),
                                contentDescription = null,
                                modifier = Modifier.size(SizeTokens.Level32),
                            )
                        }
                    }
                } else {
                    Surface(
                        modifier = Modifier.size(SizeTokens.Level48),
                        shape = RoundedCornerShape(SizeTokens.Level12),
                        color = ThemedColorSchemeKeyTokens.SurfaceContainerHighest.value.copy(alpha = 0.5f),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            PackageIconImage(
                                icon = if (packageName.isNullOrEmpty()) ImageVector.vectorResource(id = com.xayah.core.ui.R.drawable.ic_rounded_hourglass_empty) else null,
                                packageName = packageName ?: "",
                                shape = RoundedCornerShape(SizeTokens.Level12),
                                size = SizeTokens.Level40
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ThemedColorSchemeKeyTokens.OnSurface.value,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (subtitle.isNotEmpty()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = ThemedColorSchemeKeyTokens.OnSurfaceVariant.value,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Status Chip
                Surface(
                    shape = CircleShape,
                    color = statusChipColor.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, statusChipColor.copy(alpha = 0.45f)),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = SizeTokens.Level8, vertical = SizeTokens.Level4),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(SizeTokens.Level4)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .alpha(if (!isFinished && statusChipText == "STREAMING") pulseAlpha else 1f)
                                .background(statusChipColor, CircleShape)
                        )
                        Text(
                            text = statusChipText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = statusChipColor,
                            fontSize = 9.sp,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            // 2. Track 1: Overall Batch Rail (Style 4)
            Column(modifier = Modifier.fillMaxWidth()) {
                val batchClamped = batchCurrent.coerceIn(0, batchTotal.coerceAtLeast(1))
                val batchPercent = if (batchTotal > 0) ((batchProgress.coerceIn(0f, 1f)) * 100).toInt() else 0
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "OVERALL BATCH",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = ThemedColorSchemeKeyTokens.OnSurfaceVariant.value,
                        fontSize = 9.sp,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text = "$batchClamped / $batchTotal ($batchPercent%)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = ThemedColorSchemeKeyTokens.Primary.value,
                        fontSize = 10.sp
                    )
                }
                Spacer(modifier = Modifier.height(SizeTokens.Level4))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(ThemedColorSchemeKeyTokens.SurfaceContainerHighest.value)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedBatchProgress.value.coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(3.dp))
                            .background(ThemedColorSchemeKeyTokens.Primary.value.copy(alpha = 0.85f))
                    )
                }
            }

            // 3. Track 2: Active Stream / Partition Rail (Style 4)
            Column(modifier = Modifier.fillMaxWidth()) {
                val streamPercent = (animatedStreamProgress.value * 100).toInt().coerceIn(0, 100)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ACTIVE STREAM",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = ThemedColorSchemeKeyTokens.OnSurfaceVariant.value,
                        fontSize = 9.sp,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text = if (streamTitle.isNotEmpty()) "$streamTitle • $streamPercent%" else "$streamPercent%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = ThemedColorSchemeKeyTokens.Primary.value,
                        fontSize = 10.sp
                    )
                }
                Spacer(modifier = Modifier.height(SizeTokens.Level4))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(ThemedColorSchemeKeyTokens.SurfaceContainerHighest.value)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedStreamProgress.value.coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(5.dp))
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

            // 4. Style 3 Partition Pipeline: 3x2 Segmented Grid Cells (APK, USER, USER_DE, DATA, OBB, MEDIA)
            if (partitions.isNotEmpty()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "PARTITION PIPELINE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = ThemedColorSchemeKeyTokens.OnSurfaceVariant.value,
                            fontSize = 9.sp,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = "${partitions.count { it.state == OperationState.DONE || it.state == OperationState.SKIP }}/${partitions.size}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = ThemedColorSchemeKeyTokens.Primary.value,
                            fontSize = 9.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(SizeTokens.Level6))

                    val columns = if (partitions.size <= 2) partitions.size else 3
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(SizeTokens.Level6)
                    ) {
                        partitions.chunked(columns).forEachIndexed { rowIndex, rowPartitions ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(SizeTokens.Level6)
                            ) {
                                rowPartitions.forEachIndexed { colIndex, part ->
                                    val idx = rowIndex * columns + colIndex
                                    val isCompleted = part.state == OperationState.DONE || (idx < activePartitionIndex)
                                    val isSkip = part.state == OperationState.SKIP
                                    val isError = part.state == OperationState.ERROR
                                    val isActive = part.state == OperationState.PROCESSING || (idx == activePartitionIndex && !isCompleted && !isSkip && !isError && !isFinished)

                                    val bgColor = when {
                                        isCompleted -> ThemedColorSchemeKeyTokens.PrimaryContainer.value.copy(alpha = 0.65f)
                                        isActive -> ThemedColorSchemeKeyTokens.Primary.value
                                        isError -> ThemedColorSchemeKeyTokens.ErrorContainer.value.copy(alpha = 0.7f)
                                        isSkip -> ThemedColorSchemeKeyTokens.SurfaceContainerHighest.value.copy(alpha = 0.35f)
                                        else -> ThemedColorSchemeKeyTokens.SurfaceContainerHighest.value.copy(alpha = 0.5f)
                                    }
                                    val contentColor = when {
                                        isCompleted -> ThemedColorSchemeKeyTokens.OnPrimaryContainer.value
                                        isActive -> ThemedColorSchemeKeyTokens.OnPrimary.value
                                        isError -> ThemedColorSchemeKeyTokens.OnErrorContainer.value
                                        isSkip -> ThemedColorSchemeKeyTokens.OnSurfaceVariant.value.copy(alpha = 0.5f)
                                        else -> ThemedColorSchemeKeyTokens.OnSurfaceVariant.value.copy(alpha = 0.7f)
                                    }

                                    val displayTitle = part.title
                                        .replace(Regex("^(Backup|Restore)\\s*", RegexOption.IGNORE_CASE), "")
                                        .trim()
                                        .uppercase()

                                    val sizeText = if (part.bytes > 0) part.bytes.toDouble().formatSize() else ""

                                    val pct = when {
                                        part.content.endsWith("%") -> part.content
                                        part.progress > 0f -> "${(part.progress * 100).toInt()}%"
                                        streamProgress > 0f -> "${(streamProgress * 100).toInt()}%"
                                        else -> "${(animatedStreamProgress.value * 100).toInt().coerceAtLeast(1)}%"
                                    }

                                    Surface(
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(SizeTokens.Level8),
                                        color = bgColor,
                                        border = if (isActive) BorderStroke(1.dp, ThemedColorSchemeKeyTokens.Primary.value) else null
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                                            ) {
                                                Text(
                                                    text = when {
                                                        isCompleted -> "✓"
                                                        isActive -> "⟳"
                                                        isSkip -> "⊘"
                                                        isError -> "✕"
                                                        else -> "○"
                                                    },
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = contentColor
                                                )
                                                Text(
                                                    text = displayTitle,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = contentColor,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(2.dp))

                                            Text(
                                                text = when {
                                                    isCompleted -> "100%"
                                                    isActive -> pct
                                                    isSkip -> "SKIP"
                                                    isError -> "ERROR"
                                                    else -> "WAIT"
                                                },
                                                fontSize = 8.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = contentColor.copy(alpha = 0.95f),
                                                maxLines = 1
                                            )

                                            if (sizeText.isNotEmpty()) {
                                                Text(
                                                    text = sizeText,
                                                    fontSize = 7.5.sp,
                                                    fontWeight = FontWeight.Normal,
                                                    color = contentColor.copy(alpha = 0.8f),
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }
                                }

                                if (rowPartitions.size < columns) {
                                    repeat(columns - rowPartitions.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 5. Telemetry Footer: Encryption, Root I/O, Sub-item stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(SizeTokens.Level6),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(SizeTokens.Level4),
                        color = ThemedColorSchemeKeyTokens.SurfaceContainerHighest.value.copy(alpha = 0.7f),
                    ) {
                        Text(
                            modifier = Modifier.padding(horizontal = SizeTokens.Level6, vertical = SizeTokens.Level2),
                            text = "🔒 AES-GCM",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium,
                            color = ThemedColorSchemeKeyTokens.OnSurfaceVariant.value
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(SizeTokens.Level4),
                        color = ThemedColorSchemeKeyTokens.SurfaceContainerHighest.value.copy(alpha = 0.7f),
                    ) {
                        Text(
                            modifier = Modifier.padding(horizontal = SizeTokens.Level6, vertical = SizeTokens.Level2),
                            text = "⚡ ROOT I/O",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium,
                            color = ThemedColorSchemeKeyTokens.OnSurfaceVariant.value
                        )
                    }
                }

                if (!telemetryStats.isNullOrEmpty()) {
                    Text(
                        text = telemetryStats,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Normal,
                        color = ThemedColorSchemeKeyTokens.OnSurfaceVariant.value,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
