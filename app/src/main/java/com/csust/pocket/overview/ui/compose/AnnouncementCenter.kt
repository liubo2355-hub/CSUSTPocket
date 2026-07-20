package com.csust.pocket.overview.ui.compose

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.csust.pocket.core.designsystem.HyperButton
import com.csust.pocket.core.designsystem.HyperIconButton
import com.csust.pocket.core.designsystem.HyperSurface
import com.csust.pocket.core.designsystem.hyperTap
import com.csust.pocket.core.theme.AppTheme
import com.csust.pocket.overview.announcement.AnnouncementLevel
import com.csust.pocket.overview.announcement.AnnouncementState
import com.csust.pocket.overview.announcement.AnnouncementUiModel

@Composable
internal fun AnnouncementCenterDialog(
    state: AnnouncementState,
    onDismiss: () -> Unit,
    onRefresh: () -> Unit,
    onMarkRead: (String) -> Unit
) {
    var selected by remember { mutableStateOf<AnnouncementUiModel?>(null) }
    Dialog(
        onDismissRequest = {
            if (selected != null) selected = null else onDismiss()
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(AppTheme.colors.overviewPageBackgroundColor)
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            if (selected == null) {
                AnnouncementList(
                    state = state,
                    onBack = onDismiss,
                    onRefresh = onRefresh,
                    onOpen = { announcement ->
                        onMarkRead(announcement.readKey)
                        selected = announcement.copy(isRead = true)
                    }
                )
            } else {
                AnnouncementDetail(
                    announcement = selected!!,
                    onBack = { selected = null }
                )
            }
        }
    }
}

@Composable
private fun AnnouncementList(
    state: AnnouncementState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onOpen: (AnnouncementUiModel) -> Unit
) {
    val colors = AppTheme.colors
    Column(Modifier.fillMaxSize()) {
        AnnouncementTopBar(
            title = "公告中心",
            subtitle = if (state.unreadCount > 0) "${state.unreadCount} 条未读" else "消息都已读",
            onBack = onBack,
            refreshing = state.isRefreshing,
            onRefresh = onRefresh
        )
        if (state.announcements.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(colors.bgCardColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.NotificationsNone,
                        contentDescription = null,
                        tint = colors.disabledTextColor,
                        modifier = Modifier.size(34.dp)
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text("暂无公告", color = colors.primaryTextColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text("新消息会在这里出现", color = colors.secondaryTextColor, fontSize = 13.sp)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.announcements, key = { it.readKey }) { announcement ->
                    AnnouncementListItem(announcement, onOpen)
                }
            }
        }
    }
}

@Composable
private fun AnnouncementTopBar(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    refreshing: Boolean,
    onRefresh: () -> Unit,
    showRefresh: Boolean = true
) {
    val colors = AppTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HyperIconButton(onClick = onBack, minSize = 46.dp) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, "返回", tint = colors.primaryTextColor)
        }
        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = colors.primaryTextColor, fontSize = 21.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = colors.secondaryTextColor, fontSize = 12.sp)
        }
        if (showRefresh) {
            HyperIconButton(onClick = onRefresh, minSize = 46.dp) {
                if (refreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = colors.functionalTextColor
                    )
                } else {
                    Icon(Icons.Outlined.Refresh, "刷新公告", tint = colors.functionalTextColor)
                }
            }
        }
    }
}

@Composable
private fun AnnouncementListItem(
    announcement: AnnouncementUiModel,
    onOpen: (AnnouncementUiModel) -> Unit
) {
    val colors = AppTheme.colors
    val accent = announcement.level.accentColor
    HyperSurface(
        modifier = Modifier
            .fillMaxWidth()
            .hyperTap { onOpen(announcement) },
        shape = RoundedCornerShape(24.dp),
        color = colors.bgCardColor,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.NotificationsNone, null, tint = accent, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!announcement.isRead) {
                        Box(Modifier.size(7.dp).clip(CircleShape).background(accent))
                        Spacer(Modifier.size(7.dp))
                    }
                    Text(
                        announcement.title,
                        color = colors.primaryTextColor,
                        fontSize = 16.sp,
                        fontWeight = if (announcement.isRead) FontWeight.Medium else FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (announcement.level != AnnouncementLevel.NORMAL) {
                        LevelBadge(announcement.level)
                    }
                }
                if (announcement.summary.isNotBlank()) {
                    Spacer(Modifier.height(5.dp))
                    Text(
                        announcement.summary,
                        color = colors.secondaryTextColor,
                        fontSize = 12.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (announcement.displayTime.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(announcement.displayTime, color = colors.disabledTextColor, fontSize = 11.sp)
                }
            }
            Spacer(Modifier.size(6.dp))
            Icon(Icons.Outlined.ChevronRight, null, tint = colors.disabledTextColor, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun AnnouncementDetail(
    announcement: AnnouncementUiModel,
    onBack: () -> Unit
) {
    val colors = AppTheme.colors
    val context = LocalContext.current
    Column(Modifier.fillMaxSize()) {
        AnnouncementTopBar(
            title = "公告详情",
            subtitle = announcement.displayTime,
            onBack = onBack,
            refreshing = false,
            onRefresh = {},
            showRefresh = false
        )
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
        ) {
            item {
                HyperSurface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    color = colors.bgCardColor,
                    shadowElevation = 5.dp
                ) {
                    Column(Modifier.padding(20.dp)) {
                        LevelBadge(announcement.level)
                        Spacer(Modifier.height(14.dp))
                        Text(
                            announcement.title,
                            color = colors.primaryTextColor,
                            fontSize = 22.sp,
                            lineHeight = 30.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (announcement.summary.isNotBlank()) {
                            Spacer(Modifier.height(10.dp))
                            Text(
                                announcement.summary,
                                color = colors.secondaryTextColor,
                                fontSize = 14.sp,
                                lineHeight = 22.sp
                            )
                        }
                        if (announcement.content.isNotEmpty()) {
                            Spacer(Modifier.height(18.dp))
                            announcement.content.forEach { paragraph ->
                                Row(Modifier.padding(vertical = 5.dp)) {
                                    Text("•", color = announcement.level.accentColor, fontSize = 16.sp)
                                    Spacer(Modifier.size(9.dp))
                                    Text(
                                        paragraph,
                                        color = colors.primaryTextColor,
                                        fontSize = 15.sp,
                                        lineHeight = 23.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                        if (announcement.actionText.isNotBlank() && announcement.actionUrl.isHttpUrl()) {
                            Spacer(Modifier.height(20.dp))
                            HyperButton(
                                text = announcement.actionText,
                                onClick = {
                                    runCatching {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(announcement.actionUrl)))
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun UrgentAnnouncementDialog(
    announcement: AnnouncementUiModel,
    onDismiss: () -> Unit,
    onOpenCenter: () -> Unit
) {
    val colors = AppTheme.colors
    Dialog(onDismissRequest = onDismiss) {
        HyperSurface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = colors.bgCardColor,
            shadowElevation = 8.dp
        ) {
            Column(Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .size(58.dp)
                        .clip(CircleShape)
                        .background(AnnouncementLevel.URGENT.accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.NotificationsNone,
                        null,
                        tint = AnnouncementLevel.URGENT.accentColor,
                        modifier = Modifier.size(30.dp)
                    )
                }
                Spacer(Modifier.height(15.dp))
                Text("重要公告", color = colors.primaryTextColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(9.dp))
                Text(
                    announcement.title,
                    color = colors.primaryTextColor,
                    fontSize = 16.sp,
                    lineHeight = 23.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (announcement.summary.isNotBlank()) {
                    Spacer(Modifier.height(7.dp))
                    Text(
                        announcement.summary,
                        color = colors.secondaryTextColor,
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    )
                }
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    HyperButton("稍后查看", onDismiss, modifier = Modifier.weight(1f), primary = false)
                    HyperButton("查看详情", onOpenCenter, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun LevelBadge(level: AnnouncementLevel) {
    val label = when (level) {
        AnnouncementLevel.NORMAL -> "通知"
        AnnouncementLevel.IMPORTANT -> "重要"
        AnnouncementLevel.URGENT -> "紧急"
    }
    val color = level.accentColor
    Text(
        label,
        color = color,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}

private val AnnouncementLevel.accentColor: Color
    get() = when (this) {
        AnnouncementLevel.NORMAL -> Color(0xFF2589EF)
        AnnouncementLevel.IMPORTANT -> Color(0xFFF39A2E)
        AnnouncementLevel.URGENT -> Color(0xFFFF4D4F)
    }

private fun String.isHttpUrl(): Boolean =
    startsWith("https://", ignoreCase = true) || startsWith("http://", ignoreCase = true)
