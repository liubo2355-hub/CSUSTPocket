package com.creamaker.changli_planet_app.feature.common.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.ExpandMore
import com.creamaker.changli_planet_app.core.designsystem.HyperSpacing
import com.creamaker.changli_planet_app.core.designsystem.HyperSurface
import com.creamaker.changli_planet_app.core.designsystem.hyperTap
import com.creamaker.changli_planet_app.core.theme.AppSkinTheme
import com.creamaker.changli_planet_app.core.theme.AppTheme
import com.creamaker.changli_planet_app.feature.common.compose_ui.FunctionDestination
import com.creamaker.changli_planet_app.feature.common.compose_ui.FunctionGroup
import com.creamaker.changli_planet_app.feature.common.compose_ui.FunctionShortcut
import com.creamaker.changli_planet_app.feature.common.compose_ui.openFunctionShortcut
import com.creamaker.changli_planet_app.feature.common.compose_ui.portalServiceGroups
import com.creamaker.changli_planet_app.feature.common.compose_ui.functionIcon

@Composable
fun FeatureScreen(
    modifier: Modifier = Modifier,
    onDestinationSelected: ((FunctionDestination) -> Unit)? = null
) {
    val context = LocalContext.current
    val groups = remember { portalServiceGroups() }
    val expandedGroups = remember { mutableStateMapOf<String, Boolean>() }
    val openDestination: (FunctionDestination) -> Unit = { destination ->
        onDestinationSelected?.invoke(destination) ?: openFunctionShortcut(context, destination)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppTheme.colors.bgPrimaryColor)
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = HyperSpacing.pageHorizontal, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(HyperSpacing.cardGap)
    ) {
        Text(
            text = "服务",
            color = AppTheme.colors.primaryTextColor,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "校园服务按系统分级整理",
            color = AppTheme.colors.secondaryTextColor,
            fontSize = 14.sp
        )

        groups.forEachIndexed { index, group ->
            val expanded = expandedGroups[group.title] ?: true
            ServiceHierarchyGroup(
                number = index + 1,
                group = group,
                expanded = expanded,
                onToggle = {
                    expandedGroups[group.title] = !(expandedGroups[group.title] ?: true)
                },
                onDestinationSelected = openDestination
            )
        }
        Spacer(Modifier.height(14.dp))
    }
}

@Composable
private fun ServiceHierarchyGroup(
    number: Int,
    group: FunctionGroup,
    expanded: Boolean,
    onToggle: () -> Unit,
    onDestinationSelected: (FunctionDestination) -> Unit
) {
    val arrowRotation = animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "service_group_arrow"
    )
    HyperSurface(modifier = Modifier.fillMaxWidth(), color = AppTheme.colors.bgCardColor) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .hyperTap(onClick = onToggle)
                    .padding(
                        horizontal = HyperSpacing.cardPaddingHorizontal,
                        vertical = HyperSpacing.cardPaddingVertical
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NumberBadge(number.toString())
                Spacer(Modifier.size(12.dp))
                Text(
                    text = group.title,
                    color = AppTheme.colors.primaryTextColor,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${group.items.size} 项",
                    color = AppTheme.colors.secondaryTextColor,
                    fontSize = 12.sp
                )
                Spacer(Modifier.size(8.dp))
                Icon(
                    imageVector = Icons.Outlined.ExpandMore,
                    contentDescription = if (expanded) "收起${group.title}" else "展开${group.title}",
                    tint = AppTheme.colors.secondaryTextColor,
                    modifier = Modifier
                        .size(22.dp)
                        .graphicsLayer { rotationZ = arrowRotation.value }
                )
            }
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = HyperSpacing.cardPaddingHorizontal),
                        color = AppTheme.colors.dividerColor.copy(alpha = .14f)
                    )
                    group.items.forEachIndexed { index, item ->
                        ServiceChildRow(item) { onDestinationSelected(item.destination) }
                        if (index != group.items.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 70.dp, end = 18.dp),
                                color = AppTheme.colors.dividerColor.copy(alpha = .10f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ServiceChildRow(item: FunctionShortcut, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .hyperTap(onClick = onClick)
            .padding(
                horizontal = HyperSpacing.cardPaddingHorizontal,
                vertical = 10.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(AppTheme.colors.commonColor.copy(alpha = .10f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = functionIcon(item.destination),
                contentDescription = null,
                tint = AppTheme.colors.commonColor,
                modifier = Modifier.size(21.dp)
            )
        }
        Spacer(Modifier.size(14.dp))
        Text(
            text = item.title,
            color = AppTheme.colors.primaryTextColor,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = AppTheme.colors.secondaryTextColor.copy(alpha = .50f),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun NumberBadge(number: String) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(AppTheme.colors.commonColor.copy(alpha = .12f)),
        contentAlignment = Alignment.Center
    ) {
        Text(number, color = AppTheme.colors.commonColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Preview(showBackground = true)
@Composable
private fun FeatureScreenPreview() {
    AppSkinTheme { FeatureScreen() }
}
