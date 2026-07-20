package com.csust.pocket.core.main.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import com.csust.pocket.core.designsystem.HyperSurface
import com.csust.pocket.core.designsystem.hyperConcave
import com.csust.pocket.core.designsystem.hyperTap
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.csust.pocket.core.main.navigation.MainTabDestination
import com.csust.pocket.core.theme.AppTheme

@Composable
fun PortalTopBar(
    selected: MainTabDestination,
    onMenuClick: () -> Unit,
    onSelect: (MainTabDestination) -> Unit,
    onMoocClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        HyperSurface(
            shape = RoundedCornerShape(24.dp),
            color = AppTheme.colors.bgCardColor,
            shadowElevation = 8.dp,
            tonalElevation = 1.dp
        ) {
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PortalNavIcon(Icons.Outlined.Menu, "打开功能目录", onMenuClick)
                PortalNavItem("概览", selected is MainTabDestination.Overview) {
                    onSelect(MainTabDestination.Overview)
                }
                PortalNavItem("我的", selected is MainTabDestination.Profile) {
                    onSelect(MainTabDestination.Profile)
                }
                PortalNavItem("教务系统", selected is MainTabDestination.Feature) {
                    onSelect(MainTabDestination.Feature)
                }
                PortalNavItem("网络课程中心", selected is MainTabDestination.Mooc, onMoocClick)
                PortalNavIcon(Icons.Outlined.ChevronRight, "更多功能", onMenuClick)
            }
        }
    }
}

@Composable
private fun PortalNavItem(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .then(if (selected) Modifier.hyperConcave(18.dp) else Modifier)
            .hyperTap(onClick = onClick)
    ) {
        Text(
            text = label,
            color = if (selected) Color(0xFF1697D5) else AppTheme.colors.primaryTextColor,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun PortalNavIcon(icon: ImageVector, description: String, onClick: () -> Unit) {
    Icon(
        imageVector = icon,
        contentDescription = description,
        tint = AppTheme.colors.primaryTextColor,
        modifier = Modifier
            .hyperTap(onClick = onClick)
            .padding(8.dp)
            .size(20.dp)
    )
}

data class PortalDrawerAction(
    val title: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

@Composable
fun PortalDrawer(
    onClose: () -> Unit,
    onOverview: () -> Unit,
    onProfile: () -> Unit,
    groups: List<Pair<String, List<PortalDrawerAction>>>
) {
    val expandedGroups = remember { mutableStateMapOf<String, Boolean>() }
    HyperSurface(
        modifier = Modifier
            .width(268.dp)
            .fillMaxHeight(),
        shape = RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp),
        color = AppTheme.colors.bgCardColor,
        shadowElevation = 14.dp
    ) {
        Column(
            modifier = Modifier
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                PortalNavIcon(Icons.Outlined.Close, "关闭", onClose)
            }
            Spacer(Modifier.height(16.dp))
            DrawerRootRow("概览", Icons.Outlined.Dashboard, onOverview)
            DrawerRootRow("我的", Icons.Outlined.AccountCircle, onProfile, accent = true)
            Spacer(Modifier.height(14.dp))
            groups.forEach { (title, actions) ->
                val expanded = expandedGroups[title] ?: true
                val arrowRotation = animateFloatAsState(
                    targetValue = if (expanded) 180f else 0f,
                    label = "drawer_group_arrow"
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .hyperTap {
                            expandedGroups[title] = !(expandedGroups[title] ?: true)
                        }
                        .padding(start = 10.dp, end = 8.dp, top = 12.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        title,
                        color = AppTheme.colors.secondaryTextColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Outlined.ExpandMore,
                        contentDescription = if (expanded) "收起$title" else "展开$title",
                        tint = AppTheme.colors.secondaryTextColor,
                        modifier = Modifier
                            .size(20.dp)
                            .graphicsLayer { rotationZ = arrowRotation.value }
                    )
                }
                AnimatedVisibility(
                    visible = expanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column {
                        actions.forEach { action ->
                            DrawerActionRow(action.title, action.icon, action.onClick)
                        }
                    }
                }
                HorizontalDivider(color = AppTheme.colors.dividerColor.copy(alpha = 0.12f))
            }
        }
    }
}

@Composable
private fun DrawerActionRow(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    accent: Boolean = false
) {
    val tint = if (accent) Color(0xFF1697D5) else AppTheme.colors.primaryTextColor
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .hyperTap(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Text(title, color = tint, fontSize = 16.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun DrawerRootRow(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    accent: Boolean = false
) {
    val tint = if (accent) Color(0xFF1697D5) else AppTheme.colors.primaryTextColor
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .hyperTap(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(23.dp))
        Spacer(Modifier.width(14.dp))
        Text(title, color = tint, fontSize = 16.sp, fontWeight = FontWeight.Medium)
    }
}
