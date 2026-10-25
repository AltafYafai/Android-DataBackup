package com.xayah.core.ui.component

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.xayah.core.ui.theme.ThemedColorSchemeKeyTokens
import com.xayah.core.ui.theme.value
import com.xayah.core.ui.token.SizeTokens
import com.xayah.core.model.util.formatSize

@Composable
fun AppUpdateDialog(
    isOpen: Boolean,
    versionName: String,
    changelog: String,
    sizeBytes: Long = 0L,
    sizeText: String = "",
    isDownloading: Boolean = false,
    downloadProgress: Float = 0f,
    downloadedBytes: Long = 0L,
    totalBytes: Long = 0L,
    isReadyToInstall: Boolean = false,
    onDownloadClick: () -> Unit,
    onInstallClick: () -> Unit = {},
    onLaterClick: () -> Unit,
) {
    if (!isOpen) return

    val animatedDownloadProgress = animateFloatAsState(
        targetValue = if (isReadyToInstall) 1f else downloadProgress.coerceIn(0f, 1f),
        animationSpec = tween(200, easing = FastOutSlowInEasing),
        label = "update_download_progress"
    )

    Dialog(
        onDismissRequest = {
            if (!isDownloading) onLaterClick()
        },
        properties = DialogProperties(
            dismissOnBackPress = !isDownloading,
            dismissOnClickOutside = !isDownloading,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .statusBarsPadding()
                .padding(horizontal = SizeTokens.Level20, vertical = SizeTokens.Level24),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                shape = RoundedCornerShape(28.dp),
                color = ThemedColorSchemeKeyTokens.SurfaceContainerLowest.value,
                tonalElevation = SizeTokens.Level6,
                shadowElevation = SizeTokens.Level8,
                border = BorderStroke(1.2.dp, ThemedColorSchemeKeyTokens.OutlineVariant.value.copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(SizeTokens.Level24),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(SizeTokens.Level16)
                ) {
                    // Header Row with Icon & Close Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = if (isReadyToInstall) Color(0xFFE8F5E9) else ThemedColorSchemeKeyTokens.PrimaryContainer.value.copy(alpha = 0.6f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(text = if (isReadyToInstall) "📦" else "🚀", fontSize = 26.sp)
                            }
                        }

                        if (!isDownloading) {
                            IconButton(
                                onClick = onLaterClick,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Close",
                                    tint = ThemedColorSchemeKeyTokens.OnSurfaceVariant.value
                                )
                            }
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (isReadyToInstall) "Update Ready to Install!" else "New Update Available!",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = ThemedColorSchemeKeyTokens.OnSurface.value,
                            textAlign = TextAlign.Center
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = ThemedColorSchemeKeyTokens.Primary.value.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, ThemedColorSchemeKeyTokens.Primary.value.copy(alpha = 0.45f))
                            ) {
                                Text(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    text = versionName,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ThemedColorSchemeKeyTokens.Primary.value
                                )
                            }

                            val displaySize = if (sizeText.isNotEmpty()) sizeText else if (sizeBytes > 0) sizeBytes.toDouble().formatSize() else ""
                            if (displaySize.isNotEmpty()) {
                                Surface(
                                    shape = CircleShape,
                                    color = ThemedColorSchemeKeyTokens.SecondaryContainer.value.copy(alpha = 0.7f)
                                ) {
                                    Text(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        text = "📦 $displaySize",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = ThemedColorSchemeKeyTokens.OnSecondaryContainer.value
                                    )
                                }
                            }
                        }
                    }

                    // Changelog Card
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 160.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = ThemedColorSchemeKeyTokens.SurfaceContainerHighest.value.copy(alpha = 0.35f),
                        border = BorderStroke(1.dp, ThemedColorSchemeKeyTokens.OutlineVariant.value.copy(alpha = 0.2f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(SizeTokens.Level16),
                            verticalArrangement = Arrangement.spacedBy(SizeTokens.Level6)
                        ) {
                            Text(
                                text = "What's New:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = ThemedColorSchemeKeyTokens.Primary.value
                            )
                            Text(
                                text = changelog.ifEmpty { "Bug fixes and performance improvements." },
                                style = MaterialTheme.typography.bodySmall,
                                color = ThemedColorSchemeKeyTokens.OnSurfaceVariant.value,
                                lineHeight = 18.sp
                            )
                        }
                    }

                    // Progress Bar or Actions
                    if (isDownloading) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val percent = (animatedDownloadProgress.value * 100).toInt().coerceIn(0, 100)
                            val downloadedStr = if (downloadedBytes > 0) downloadedBytes.toDouble().formatSize() else ""
                            val totalStr = if (totalBytes > 0) totalBytes.toDouble().formatSize() else ""
                            val statusText = if (downloadedStr.isNotEmpty() && totalStr.isNotEmpty()) {
                                "$downloadedStr of $totalStr ($percent%)"
                            } else {
                                "$percent%"
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Downloading update...",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF0288D1),
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = statusText,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0288D1),
                                    fontSize = 11.sp
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF38BDF8).copy(alpha = 0.2f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(animatedDownloadProgress.value.coerceIn(0.02f, 1f))
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
                    } else if (isReadyToInstall) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Download complete! Ready to install.",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF2E7D32),
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = "100%",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2E7D32),
                                    fontSize = 12.sp
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF4CAF50).copy(alpha = 0.25f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
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

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(SizeTokens.Level12, Alignment.End),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = onLaterClick,
                                shape = CircleShape,
                                border = BorderStroke(1.dp, ThemedColorSchemeKeyTokens.OutlineVariant.value.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = "Later",
                                    color = ThemedColorSchemeKeyTokens.OnSurfaceVariant.value,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Button(
                                onClick = onInstallClick,
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF2E7D32),
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Install Now",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(SizeTokens.Level12, Alignment.End),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = onLaterClick,
                                shape = CircleShape,
                                border = BorderStroke(1.dp, ThemedColorSchemeKeyTokens.OutlineVariant.value.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = "Later",
                                    color = ThemedColorSchemeKeyTokens.OnSurfaceVariant.value,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Button(
                                onClick = onDownloadClick,
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ThemedColorSchemeKeyTokens.Primary.value,
                                    contentColor = ThemedColorSchemeKeyTokens.OnPrimary.value
                                )
                            ) {
                                Text(
                                    text = "Download & Install",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NoUpdateDialog(
    isOpen: Boolean,
    versionName: String,
    changelog: String,
    onDismiss: () -> Unit,
) {
    if (!isOpen) return

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
                .padding(horizontal = SizeTokens.Level20, vertical = SizeTokens.Level24),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                shape = RoundedCornerShape(28.dp),
                color = ThemedColorSchemeKeyTokens.SurfaceContainerLowest.value,
                tonalElevation = SizeTokens.Level6,
                shadowElevation = SizeTokens.Level8,
                border = BorderStroke(1.2.dp, ThemedColorSchemeKeyTokens.OutlineVariant.value.copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(SizeTokens.Level24),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(SizeTokens.Level16)
                ) {
                    // Header Row with Icon & Close Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF2E7D32).copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, Color(0xFF2E7D32).copy(alpha = 0.3f))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = Color(0xFF2E7D32),
                                    modifier = Modifier.size(26.dp)
                                )
                            }
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

                    // Title & Version Badges
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "No Update Available",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = ThemedColorSchemeKeyTokens.OnSurface.value
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = ThemedColorSchemeKeyTokens.Primary.value.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, ThemedColorSchemeKeyTokens.Primary.value.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    text = "v$versionName",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ThemedColorSchemeKeyTokens.Primary.value
                                )
                            }

                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF2E7D32).copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, Color(0xFF2E7D32).copy(alpha = 0.3f))
                            ) {
                                Text(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    text = "✓ Up to Date",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF2E7D32)
                                )
                            }
                        }
                    }

                    // Current Build Changelog Card
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = ThemedColorSchemeKeyTokens.SurfaceContainerHighest.value.copy(alpha = 0.35f),
                        border = BorderStroke(1.dp, ThemedColorSchemeKeyTokens.OutlineVariant.value.copy(alpha = 0.2f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(SizeTokens.Level16),
                            verticalArrangement = Arrangement.spacedBy(SizeTokens.Level6)
                        ) {
                            Text(
                                text = "Current Build Changelog:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = ThemedColorSchemeKeyTokens.Primary.value
                            )
                            Text(
                                text = changelog.ifEmpty { "Performance optimizations and stability improvements." },
                                style = MaterialTheme.typography.bodySmall,
                                color = ThemedColorSchemeKeyTokens.OnSurfaceVariant.value,
                                lineHeight = 18.sp
                            )
                        }
                    }

                    // Done Button
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ThemedColorSchemeKeyTokens.Primary.value,
                            contentColor = ThemedColorSchemeKeyTokens.OnPrimary.value
                        )
                    ) {
                        Text(
                            text = "Done",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CheckingUpdateDialog(
    isOpen: Boolean,
    onDismiss: () -> Unit = {}
) {
    if (!isOpen) return

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
                .padding(horizontal = SizeTokens.Level20, vertical = SizeTokens.Level24),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .wrapContentHeight(),
                shape = RoundedCornerShape(24.dp),
                color = ThemedColorSchemeKeyTokens.SurfaceContainerLowest.value,
                tonalElevation = SizeTokens.Level6,
                shadowElevation = SizeTokens.Level8,
                border = BorderStroke(1.2.dp, ThemedColorSchemeKeyTokens.OutlineVariant.value.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        strokeWidth = 3.dp,
                        color = ThemedColorSchemeKeyTokens.Primary.value
                    )
                    Text(
                        text = "Checking for updates...",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = ThemedColorSchemeKeyTokens.OnSurface.value
                    )
                }
            }
        }
    }
}
