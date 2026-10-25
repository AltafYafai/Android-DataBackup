package com.xayah.feature.main.processing

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.xayah.core.model.OperationState
import com.xayah.core.model.util.formatSize
import com.xayah.core.ui.component.PackageIconImage
import com.xayah.core.ui.model.ProcessingDataCardItem
import com.xayah.core.ui.theme.ThemedColorSchemeKeyTokens
import com.xayah.core.ui.theme.value
import com.xayah.core.ui.token.SizeTokens

import androidx.compose.foundation.ExperimentalFoundationApi

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BackedUpAppsDialog(
    isOpen: Boolean,
    dataItems: List<ProcessingDataCardItem>,
    onDismiss: () -> Unit
) {
    if (!isOpen) return

    val doneCount = dataItems.count { it.state == OperationState.DONE }
    val activeCount = dataItems.count { it.state == OperationState.PROCESSING }
    val queuedCount = dataItems.count { it.state == OperationState.IDLE }
    val totalBytes = dataItems.sumOf { item -> item.items.sumOf { it.bytes } }
    val totalSizeStr = if (totalBytes > 0) totalBytes.toDouble().formatSize() else ""

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .statusBarsPadding()
                .padding(horizontal = SizeTokens.Level16, vertical = SizeTokens.Level20),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 560.dp),
                shape = RoundedCornerShape(28.dp),
                color = ThemedColorSchemeKeyTokens.SurfaceContainerLowest.value,
                tonalElevation = SizeTokens.Level6,
                shadowElevation = SizeTokens.Level8,
                border = BorderStroke(1.2.dp, ThemedColorSchemeKeyTokens.OutlineVariant.value.copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(SizeTokens.Level20)
                ) {
                    // Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Backed Up Apps",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = ThemedColorSchemeKeyTokens.OnSurface.value
                                )
                                if (totalSizeStr.isNotEmpty()) {
                                    Surface(
                                        shape = CircleShape,
                                        color = ThemedColorSchemeKeyTokens.Primary.value.copy(alpha = 0.12f)
                                    ) {
                                        Text(
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                            text = totalSizeStr,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ThemedColorSchemeKeyTokens.Primary.value
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "$doneCount of ${dataItems.size} completed",
                                style = MaterialTheme.typography.bodySmall,
                                color = ThemedColorSchemeKeyTokens.OnSurfaceVariant.value
                            )
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Close",
                                tint = ThemedColorSchemeKeyTokens.OnSurfaceVariant.value
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(SizeTokens.Level12))

                    // Summary Status Badges
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF4CAF50).copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, Color(0xFF4CAF50).copy(alpha = 0.45f))
                        ) {
                            Text(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                text = "✓ $doneCount Done",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )
                        }

                        if (activeCount > 0) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF38BDF8).copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f))
                            ) {
                                Text(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    text = "⟳ $activeCount Active",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0288D1)
                                )
                            }
                        }

                        Surface(
                            shape = CircleShape,
                            color = ThemedColorSchemeKeyTokens.SurfaceContainerHighest.value.copy(alpha = 0.7f)
                        ) {
                            Text(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                text = "○ $queuedCount Queued",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = ThemedColorSchemeKeyTokens.OnSurfaceVariant.value
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(SizeTokens.Level12))

                    // App Items List
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(count = dataItems.size) { idx ->
                            val app = dataItems[idx]
                            val appBytes = app.items.sumOf { it.bytes }
                            val appSizeStr = if (appBytes > 0) appBytes.toDouble().formatSize() else ""

                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                color = when (app.state) {
                                    OperationState.DONE -> Color(0xFF4CAF50).copy(alpha = 0.08f)
                                    OperationState.PROCESSING -> Color(0xFF38BDF8).copy(alpha = 0.1f)
                                    OperationState.ERROR -> ThemedColorSchemeKeyTokens.ErrorContainer.value.copy(alpha = 0.2f)
                                    else -> ThemedColorSchemeKeyTokens.SurfaceContainerHighest.value.copy(alpha = 0.35f)
                                },
                                border = BorderStroke(
                                    1.dp,
                                    when (app.state) {
                                        OperationState.DONE -> Color(0xFF4CAF50).copy(alpha = 0.35f)
                                        OperationState.PROCESSING -> Color(0xFF38BDF8).copy(alpha = 0.55f)
                                        OperationState.ERROR -> ThemedColorSchemeKeyTokens.Error.value.copy(alpha = 0.45f)
                                        else -> ThemedColorSchemeKeyTokens.OutlineVariant.value.copy(alpha = 0.2f)
                                    }
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Surface(
                                        modifier = Modifier.size(38.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        color = ThemedColorSchemeKeyTokens.SurfaceContainerHighest.value.copy(alpha = 0.6f)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            if (app.key.isNotEmpty()) {
                                                PackageIconImage(packageName = app.key, size = 28.dp)
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

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = app.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = ThemedColorSchemeKeyTokens.OnSurface.value,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = app.key,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = ThemedColorSchemeKeyTokens.OnSurfaceVariant.value,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            if (appSizeStr.isNotEmpty()) {
                                                Text(
                                                    text = "• $appSizeStr",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = ThemedColorSchemeKeyTokens.Primary.value
                                                )
                                            }
                                        }
                                    }

                                    // Status Badge Pill
                                    Surface(
                                        shape = CircleShape,
                                        color = when (app.state) {
                                            OperationState.DONE -> Color(0xFF4CAF50).copy(alpha = 0.15f)
                                            OperationState.PROCESSING -> Color(0xFF38BDF8).copy(alpha = 0.22f)
                                            OperationState.ERROR -> ThemedColorSchemeKeyTokens.ErrorContainer.value.copy(alpha = 0.5f)
                                            else -> ThemedColorSchemeKeyTokens.SurfaceContainerHighest.value.copy(alpha = 0.5f)
                                        },
                                        border = BorderStroke(
                                            1.dp,
                                            when (app.state) {
                                                OperationState.DONE -> Color(0xFF4CAF50).copy(alpha = 0.5f)
                                                OperationState.PROCESSING -> Color(0xFF38BDF8).copy(alpha = 0.65f)
                                                OperationState.ERROR -> ThemedColorSchemeKeyTokens.Error.value.copy(alpha = 0.45f)
                                                else -> ThemedColorSchemeKeyTokens.OutlineVariant.value.copy(alpha = 0.25f)
                                            }
                                        )
                                    ) {
                                        Text(
                                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                                            text = when (app.state) {
                                                OperationState.DONE -> "Done ✓"
                                                OperationState.PROCESSING -> "Active"
                                                OperationState.ERROR -> "Failed ✕"
                                                OperationState.SKIP -> "Skipped"
                                                else -> "Queued"
                                            },
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = when (app.state) {
                                                OperationState.DONE -> Color(0xFF2E7D32)
                                                OperationState.PROCESSING -> Color(0xFF0288D1)
                                                OperationState.ERROR -> ThemedColorSchemeKeyTokens.Error.value
                                                else -> ThemedColorSchemeKeyTokens.OnSurfaceVariant.value
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(SizeTokens.Level16))

                    // Close Button
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onDismiss,
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ThemedColorSchemeKeyTokens.Primary.value,
                            contentColor = ThemedColorSchemeKeyTokens.OnPrimary.value
                        )
                    ) {
                        Text(text = "Close", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
