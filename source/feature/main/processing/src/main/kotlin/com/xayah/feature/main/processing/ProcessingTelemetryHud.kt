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
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.painterResource
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
        shape = RoundedCornerShape(SizeTokens.Level24),
        color = ThemedColorSchemeKeyTokens.SurfaceContainerLowest.value,
        tonalElevation = SizeTokens.Level3,
        shadowElevation = SizeTokens.Level4,
        border = BorderStroke(1.2.dp, ThemedColorSchemeKeyTokens.OutlineVariant.value.copy(alpha = 0.35f)),
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
                        shape = RoundedCornerShape(SizeTokens.Level16),
                        color = ThemedColorSchemeKeyTokens.PrimaryContainer.value.copy(alpha = 0.6f),
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
                        shape = RoundedCornerShape(SizeTokens.Level16),
                        color = ThemedColorSchemeKeyTokens.SurfaceContainerHighest.value.copy(alpha = 0.6f),
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

                // Status Chip (Humatic & Expressive Pill)
                Surface(
                    shape = CircleShape,
                    color = statusChipColor.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, statusChipColor.copy(alpha = 0.45f)),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = SizeTokens.Level10, vertical = SizeTokens.Level6),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(SizeTokens.Level6)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .alpha(if (!isFinished && (statusChipText.contains("Progress") || statusChipText.contains("..."))) pulseAlpha else 1f)
                                .background(statusChipColor, CircleShape)
                        )
                        Text(
                            text = statusChipText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = statusChipColor,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // 2. Track 1: Overall Progress (Thick & Green)
            Column(modifier = Modifier.fillMaxWidth()) {
                val batchClamped = batchCurrent.coerceIn(0, batchTotal.coerceAtLeast(1))
                val batchPercent = if (batchTotal > 0) ((batchProgress.coerceIn(0f, 1f)) * 100).toInt() else 0
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Total Progress",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32),
                        fontSize = 12.sp
                    )
                    Text(
                        text = if (isFinished) "All $batchTotal completed" else "$batchClamped of $batchTotal ($batchPercent%)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32),
                        fontSize = 12.sp
                    )
                }
                Spacer(modifier = Modifier.height(SizeTokens.Level6))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(16.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4CAF50).copy(alpha = 0.15f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedBatchProgress.value.coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color(0xFF2E7D32),
                                        Color(0xFF4CAF50)
                                    )
                                )
                            )
                    )
                }
            }

            // 3. Track 2: Current Task (Thin & Light Blue)
            Column(modifier = Modifier.fillMaxWidth()) {
                val streamPercent = (animatedStreamProgress.value * 100).toInt().coerceIn(0, 100)
                val cleanStreamTitle = streamTitle
                    .replace(Regex("^(Backup|Restore)\\s*", RegexOption.IGNORE_CASE), "")
                    .trim()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Current Step",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF0288D1),
                        fontSize = 11.sp
                    )
                    Text(
                        text = if (cleanStreamTitle.isNotEmpty()) "$cleanStreamTitle • $streamPercent%" else "$streamPercent%",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0288D1),
                        fontSize = 11.sp
                    )
                }
                Spacer(modifier = Modifier.height(SizeTokens.Level6))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF38BDF8).copy(alpha = 0.18f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedStreamProgress.value.coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color(0xFF0288D1),
                                        Color(0xFF38BDF8)
                                    )
                                )
                            )
                    )
                }
            }

            // 4. Data Breakdown Cards (divided into 3 parts: icon+name, size, done/processing)
            if (partitions.isNotEmpty()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Data Breakdown",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = ThemedColorSchemeKeyTokens.OnSurfaceVariant.value,
                            fontSize = 11.sp
                        )
                        val completedCount = partitions.count { it.state == OperationState.DONE || it.state == OperationState.SKIP }
                        Text(
                            text = "$completedCount of ${partitions.size} completed",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = ThemedColorSchemeKeyTokens.Primary.value,
                            fontSize = 10.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(SizeTokens.Level8))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(SizeTokens.Level6)
                    ) {
                        partitions.forEachIndexed { idx, part ->
                            val isCompleted = part.state == OperationState.DONE || (idx < activePartitionIndex)
                            val isSkip = part.state == OperationState.SKIP
                            val isError = part.state == OperationState.ERROR
                            val isActive = part.state == OperationState.PROCESSING || (idx == activePartitionIndex && !isCompleted && !isSkip && !isError && !isFinished)

                            val rawTitle = part.title
                                .replace(Regex("^(Backup|Restore)\\s*", RegexOption.IGNORE_CASE), "")
                                .trim()

                            val iconRes = when (rawTitle.uppercase()) {
                                "APK" -> com.xayah.core.ui.R.drawable.ic_rounded_android
                                "USER" -> com.xayah.core.ui.R.drawable.ic_rounded_person
                                "USER_DE" -> com.xayah.core.ui.R.drawable.ic_rounded_manage_accounts
                                "DATA" -> com.xayah.core.ui.R.drawable.ic_rounded_database
                                "OBB" -> com.xayah.core.ui.R.drawable.ic_rounded_stadia_controller
                                "MEDIA" -> com.xayah.core.ui.R.drawable.ic_rounded_image
                                else -> com.xayah.core.ui.R.drawable.ic_rounded_package_2
                            }

                            val displayTitle = when (rawTitle.uppercase()) {
                                "APK" -> "App"
                                "USER" -> "Data"
                                "USER_DE" -> "Encrypted"
                                "DATA" -> "Storage"
                                "OBB" -> "Assets"
                                "MEDIA" -> "Media"
                                else -> rawTitle
                            }

                            val sizeText = if (part.bytes > 0) part.bytes.toDouble().formatSize() else "--"

                            val pct = when {
                                part.content.endsWith("%") -> part.content
                                part.progress > 0f -> "${(part.progress * 100).toInt()}%"
                                streamProgress > 0f -> "${(streamProgress * 100).toInt()}%"
                                else -> "${(animatedStreamProgress.value * 100).toInt().coerceAtLeast(1)}%"
                            }

                            val cardBgColor = when {
                                isActive -> Color(0xFF38BDF8).copy(alpha = 0.12f)
                                isCompleted -> ThemedColorSchemeKeyTokens.PrimaryContainer.value.copy(alpha = 0.35f)
                                isError -> ThemedColorSchemeKeyTokens.ErrorContainer.value.copy(alpha = 0.3f)
                                isSkip -> ThemedColorSchemeKeyTokens.SurfaceContainerHighest.value.copy(alpha = 0.25f)
                                else -> ThemedColorSchemeKeyTokens.SurfaceContainerHighest.value.copy(alpha = 0.4f)
                            }

                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = cardBgColor,
                                border = if (isActive) BorderStroke(1.2.dp, Color(0xFF38BDF8)) else BorderStroke(0.8.dp, ThemedColorSchemeKeyTokens.OutlineVariant.value.copy(alpha = 0.2f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Part 1: Icon + Name
                                    Row(
                                        modifier = Modifier.weight(1.3f),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(id = iconRes),
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = when {
                                                isCompleted -> Color(0xFF2E7D32)
                                                isActive -> Color(0xFF0288D1)
                                                isError -> ThemedColorSchemeKeyTokens.Error.value
                                                else -> ThemedColorSchemeKeyTokens.OnSurfaceVariant.value
                                            }
                                        )
                                        Text(
                                            text = displayTitle,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = ThemedColorSchemeKeyTokens.OnSurface.value,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    // Part 2: Size
                                    Box(
                                        modifier = Modifier.weight(1.0f),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = sizeText,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (sizeText != "--") ThemedColorSchemeKeyTokens.OnSurface.value else ThemedColorSchemeKeyTokens.OnSurfaceVariant.value.copy(alpha = 0.6f)
                                        )
                                    }

                                    // Part 3: Done / Processing (Status Chip)
                                    Box(
                                        modifier = Modifier.weight(1.3f),
                                        contentAlignment = Alignment.CenterEnd
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = when {
                                                isCompleted -> Color(0xFF4CAF50).copy(alpha = 0.15f)
                                                isActive -> Color(0xFF38BDF8).copy(alpha = 0.22f)
                                                isError -> ThemedColorSchemeKeyTokens.ErrorContainer.value.copy(alpha = 0.6f)
                                                isSkip -> ThemedColorSchemeKeyTokens.SurfaceContainerHighest.value.copy(alpha = 0.4f)
                                                else -> ThemedColorSchemeKeyTokens.SurfaceContainerHighest.value.copy(alpha = 0.5f)
                                            },
                                            border = BorderStroke(
                                                1.dp,
                                                when {
                                                    isCompleted -> Color(0xFF4CAF50).copy(alpha = 0.5f)
                                                    isActive -> Color(0xFF38BDF8).copy(alpha = 0.7f)
                                                    isError -> ThemedColorSchemeKeyTokens.Error.value.copy(alpha = 0.5f)
                                                    else -> ThemedColorSchemeKeyTokens.OutlineVariant.value.copy(alpha = 0.3f)
                                                }
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    text = when {
                                                        isCompleted -> "✓"
                                                        isActive -> "⟳"
                                                        isSkip -> "⊘"
                                                        isError -> "✕"
                                                        else -> "○"
                                                    },
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = when {
                                                        isCompleted -> Color(0xFF2E7D32)
                                                        isActive -> Color(0xFF0288D1)
                                                        isError -> ThemedColorSchemeKeyTokens.Error.value
                                                        else -> ThemedColorSchemeKeyTokens.OnSurfaceVariant.value
                                                    }
                                                )
                                                Text(
                                                    text = when {
                                                        isCompleted -> "Done"
                                                        isActive -> if (pct.isNotEmpty() && pct != "0%") pct else "Processing"
                                                        isSkip -> "Skipped"
                                                        isError -> "Failed"
                                                        else -> "Queued"
                                                    },
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = when {
                                                        isCompleted -> Color(0xFF2E7D32)
                                                        isActive -> Color(0xFF0288D1)
                                                        isError -> ThemedColorSchemeKeyTokens.Error.value
                                                        else -> ThemedColorSchemeKeyTokens.OnSurfaceVariant.value
                                                    }
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

            // 5. Reassurance Footer (Humatic & Expressive)
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
                        shape = CircleShape,
                        color = ThemedColorSchemeKeyTokens.SurfaceContainerHighest.value.copy(alpha = 0.7f),
                    ) {
                        Text(
                            modifier = Modifier.padding(horizontal = SizeTokens.Level8, vertical = SizeTokens.Level3),
                            text = "🔒 Encrypted",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = ThemedColorSchemeKeyTokens.OnSurfaceVariant.value
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = ThemedColorSchemeKeyTokens.SurfaceContainerHighest.value.copy(alpha = 0.7f),
                    ) {
                        Text(
                            modifier = Modifier.padding(horizontal = SizeTokens.Level8, vertical = SizeTokens.Level3),
                            text = "⚡ Direct I/O",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = ThemedColorSchemeKeyTokens.OnSurfaceVariant.value
                        )
                    }
                }

                if (!telemetryStats.isNullOrEmpty()) {
                    Text(
                        text = telemetryStats,
                        fontSize = 10.sp,
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
