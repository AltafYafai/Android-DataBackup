package com.xayah.databackup.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.xayah.core.model.OpType
import com.xayah.core.model.Target
import com.xayah.core.ui.route.MainRoutes
import com.xayah.core.ui.theme.ThemedColorSchemeKeyTokens
import com.xayah.core.ui.theme.value
import com.xayah.core.ui.token.SizeTokens
import com.xayah.core.util.navigateSingle
import com.xayah.databackup.R

private data class NavTabItem(
    val id: String,
    val title: String,
    val iconRes: Int,
    val route: String,
)

@Composable
fun BackupVaultBottomBar(
    navController: NavController,
    modifier: Modifier = Modifier,
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: ""
    val currentArgs = navBackStackEntry?.arguments

    val currentTarget = currentArgs?.getString(MainRoutes.ARG_TARGET)
    val currentOpType = currentArgs?.getString(MainRoutes.ARG_OP_TYPE)

    val selectedId = when {
        currentRoute == MainRoutes.Dashboard.route || currentRoute.startsWith(MainRoutes.Dashboard.route) -> "dashboard"
        currentRoute.startsWith("main_list") && currentTarget == Target.Apps.name && currentOpType == OpType.BACKUP.name -> "apps"
        currentRoute.startsWith("main_list") && currentTarget == Target.Files.name && currentOpType == OpType.BACKUP.name -> "files"
        currentRoute == MainRoutes.Restore.route || currentRoute.startsWith(MainRoutes.Restore.route) -> "restore"
        currentRoute == MainRoutes.Settings.route || currentRoute.startsWith(MainRoutes.Settings.route) -> "settings"
        else -> null
    }

    val isVisible = selectedId != null

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.BottomCenter
    ) {
        AnimatedVisibility(
            visible = isVisible,
            enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(300, easing = FastOutSlowInEasing)) + fadeIn(animationSpec = tween(300)),
            exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(250, easing = FastOutSlowInEasing)) + fadeOut(animationSpec = tween(250)),
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = SizeTokens.Level16, vertical = SizeTokens.Level8)
                    .navigationBarsPadding(),
                shape = RoundedCornerShape(28.dp),
                color = ThemedColorSchemeKeyTokens.SurfaceContainerLowest.value.copy(alpha = 0.90f),
                tonalElevation = SizeTokens.Level6,
                shadowElevation = SizeTokens.Level8,
                border = BorderStroke(Dp(1.2f), ThemedColorSchemeKeyTokens.OutlineVariant.value.copy(alpha = 0.35f)),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .padding(horizontal = SizeTokens.Level6),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val tabs = remember {
                        listOf(
                            NavTabItem("dashboard", "Dashboard", R.drawable.ic_rounded_home, MainRoutes.Dashboard.route),
                            NavTabItem("apps", "Apps", R.drawable.ic_rounded_package_2, MainRoutes.List.getRoute(Target.Apps, OpType.BACKUP)),
                            NavTabItem("files", "Files", R.drawable.ic_rounded_folder_open, MainRoutes.List.getRoute(Target.Files, OpType.BACKUP)),
                            NavTabItem("restore", "Restore", R.drawable.ic_rounded_history, MainRoutes.Restore.route),
                            NavTabItem("settings", "Settings", 0, MainRoutes.Settings.route),
                        )
                    }

                    tabs.forEach { tab ->
                        val isSelected = tab.id == selectedId
                        val tint by animateColorAsState(
                            targetValue = if (isSelected) ThemedColorSchemeKeyTokens.Primary.value else ThemedColorSchemeKeyTokens.OnSurfaceVariant.value,
                            animationSpec = tween(220, easing = FastOutSlowInEasing),
                            label = "tab_tint"
                        )
                        val iconBg by animateColorAsState(
                            targetValue = if (isSelected) ThemedColorSchemeKeyTokens.PrimaryContainer.value else Color.Transparent,
                            animationSpec = tween(220, easing = FastOutSlowInEasing),
                            label = "tab_bg"
                        )

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) {
                                    if (!isSelected) {
                                        navController.navigateSingle(tab.route)
                                    }
                                },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Surface(
                                modifier = Modifier.size(width = 52.dp, height = 30.dp),
                                shape = CircleShape,
                                color = iconBg,
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (tab.id == "settings") {
                                        Icon(
                                            modifier = Modifier.size(20.dp),
                                            imageVector = Icons.Outlined.Settings,
                                            tint = if (isSelected) ThemedColorSchemeKeyTokens.OnPrimaryContainer.value else tint,
                                            contentDescription = tab.title,
                                        )
                                    } else {
                                        Icon(
                                            modifier = Modifier.size(20.dp),
                                            imageVector = ImageVector.vectorResource(id = tab.iconRes),
                                            tint = if (isSelected) ThemedColorSchemeKeyTokens.OnPrimaryContainer.value else tint,
                                            contentDescription = tab.title,
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = tab.title,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = tint,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}
