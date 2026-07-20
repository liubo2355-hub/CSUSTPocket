package com.csust.pocket.overview.ui.compose

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.AssignmentTurnedIn
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.EventNote
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.CircularProgressIndicator
import com.csust.pocket.core.designsystem.HyperSurface
import com.csust.pocket.core.designsystem.HyperIconButton
import com.csust.pocket.core.designsystem.HyperSpacing
import com.csust.pocket.core.designsystem.hyperTap
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.csust.pocket.R
import com.csust.pocket.core.theme.AppSkinTheme
import com.csust.pocket.core.theme.AppTheme
import com.csust.pocket.overview.announcement.AnnouncementRepository
import com.csust.pocket.overview.announcement.AnnouncementState
import com.csust.pocket.overview.announcement.AnnouncementUiModel
import com.csust.pocket.overview.ui.model.ExamHighlightType
import com.csust.pocket.overview.ui.model.ExamHighlightUiModel
import com.csust.pocket.overview.ui.model.OverviewCourseUiModel
import com.csust.pocket.overview.ui.model.OverviewExamUiModel
import com.csust.pocket.overview.ui.model.OverviewHomeworkUiModel
import com.csust.pocket.overview.ui.model.OverviewMetricUiModel
import com.csust.pocket.overview.ui.model.OverviewTestUiModel
import com.csust.pocket.overview.ui.model.OverviewUiState
import com.csust.pocket.overview.viewmodel.OverviewViewModel
import kotlinx.coroutines.delay
import java.util.Calendar

private val HomeworkAccent = Color(0xFFEF9442)
private val TestAccent = Color(0xFF4F7FED)
private val IconContainerShape = RoundedCornerShape(14.dp)
// Deferred for a later release. Keep the implementation locally without exposing it in v2.0.29.

@Composable
fun OverviewScreen(
    viewModel: OverviewViewModel,
    onBindClick: () -> Unit,
    onQuickActionClick: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val announcementState by viewModel.announcementState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(viewModel) {
        viewModel.onHomeVisible()
        while (true) {
            delay(AnnouncementRepository.FOREGROUND_REFRESH_INTERVAL_MS)
            viewModel.refreshAnnouncements()
        }
    }

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.onResume()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    OverviewScreen(
        state = state,
        onBindClick = onBindClick,
        onMetricClick = onQuickActionClick,
        onQuickActionClick = onQuickActionClick,
        onRefreshMetric = viewModel::refreshMetric,
        onRefreshSection = viewModel::refreshSection,
        announcementState = announcementState,
        onRefreshAnnouncements = { viewModel.refreshAnnouncements(force = true) },
        onMarkAnnouncementRead = viewModel::markAnnouncementRead,
        onUrgentPresented = viewModel::markUrgentAnnouncementPresented
    )
}

@Composable
private fun OverviewScreen(
    state: OverviewUiState,
    onBindClick: () -> Unit,
    onMetricClick: (String) -> Unit,
    onQuickActionClick: (String) -> Unit,
    onRefreshMetric: (String) -> Unit,
    onRefreshSection: (String) -> Unit,
    announcementState: AnnouncementState,
    onRefreshAnnouncements: () -> Unit,
    onMarkAnnouncementRead: (String) -> Unit,
    onUrgentPresented: (String) -> Unit
) {
    val colors = AppTheme.colors
    val isLightPage = colors.overviewPageBackgroundColor.luminance() > 0.5f
    val pageBackground = remember(colors.overviewPageBackgroundColor) {
        if (isLightPage) {
            Brush.verticalGradient(
                colorStops = arrayOf(
                    0.00f to Color(0xFFE5F2FF),
                    0.24f to Color(0xFFEAF4FE),
                    0.58f to Color(0xFFEEF5FC),
                    1.00f to Color(0xFFF1F5FA)
                )
            )
        } else {
            Brush.verticalGradient(
                colorStops = arrayOf(
                    0.00f to Color(0xFF101A26),
                    0.28f to Color(0xFF131B24),
                    0.62f to Color(0xFF171C23),
                    1.00f to Color(0xFF1A1D22)
                )
            )
        }
    }
    var showAnnouncementCenter by remember { mutableStateOf(false) }
    var urgentAnnouncement by remember { mutableStateOf<AnnouncementUiModel?>(null) }
    LaunchedEffect(announcementState.urgentToPresent?.readKey) {
        announcementState.urgentToPresent?.let { announcement ->
            urgentAnnouncement = announcement
            onUrgentPresented(announcement.readKey)
        }
    }
    val greeting = remember {
        when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 5..10 -> "早上好"
            in 11..13 -> "中午好"
            in 14..17 -> "下午好"
            else -> "晚上好"
        }
    }
    val homeDateText = remember(state.currentWeek) {
        val today = Calendar.getInstance()
        val weekday = when (today.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "周一"
            Calendar.TUESDAY -> "周二"
            Calendar.WEDNESDAY -> "周三"
            Calendar.THURSDAY -> "周四"
            Calendar.FRIDAY -> "周五"
            Calendar.SATURDAY -> "周六"
            else -> "周日"
        }
        "${today.get(Calendar.MONTH) + 1}月${today.get(Calendar.DAY_OF_MONTH)}日  $weekday  第${state.currentWeek}周"
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(pageBackground),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            HomeGreetingHeader(
                greeting = greeting,
                dateText = homeDateText,
                unreadAnnouncementCount = announcementState.unreadCount,
                onAnnouncementClick = { showAnnouncementCenter = true }
            )
        }
        item {
            PortalDashboard(
                state,
                onMetricClick,
                onQuickActionClick,
                onRefreshMetric,
                onRefreshSection
            )
        }
    }

    if (showAnnouncementCenter) {
        AnnouncementCenterDialog(
            state = announcementState,
            onDismiss = { showAnnouncementCenter = false },
            onRefresh = onRefreshAnnouncements,
            onMarkRead = onMarkAnnouncementRead
        )
    }
    urgentAnnouncement?.let { announcement ->
        UrgentAnnouncementDialog(
            announcement = announcement,
            onDismiss = { urgentAnnouncement = null },
            onOpenCenter = {
                onMarkAnnouncementRead(announcement.readKey)
                urgentAnnouncement = null
                showAnnouncementCenter = true
            }
        )
    }
}

@Composable
private fun HomeGreetingHeader(
    greeting: String,
    dateText: String,
    unreadAnnouncementCount: Int,
    onAnnouncementClick: () -> Unit
) {
    val colors = AppTheme.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val building = Color(0xFF79B8ED).copy(alpha = 0.16f)
            val baseY = size.height * 0.96f
            drawRoundRect(building, Offset(size.width * 0.55f, baseY - size.height * 0.42f), androidx.compose.ui.geometry.Size(size.width * 0.39f, size.height * 0.42f), androidx.compose.ui.geometry.CornerRadius(12f))
            drawRoundRect(building.copy(alpha = 0.24f), Offset(size.width * 0.72f, baseY - size.height * 0.62f), androidx.compose.ui.geometry.Size(size.width * 0.12f, size.height * 0.62f), androidx.compose.ui.geometry.CornerRadius(12f))
            drawCircle(Color.White.copy(alpha = 0.58f), radius = size.height * 0.10f, center = Offset(size.width * 0.78f, baseY - size.height * 0.47f))
            repeat(5) { index ->
                val x = size.width * (0.59f + index * 0.065f)
                drawRoundRect(Color.White.copy(alpha = 0.46f), Offset(x, baseY - size.height * 0.28f), androidx.compose.ui.geometry.Size(size.width * 0.025f, size.height * 0.09f), androidx.compose.ui.geometry.CornerRadius(4f))
            }
        }
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 6.dp, bottom = 2.dp)
        ) {
            Text(
                "$greeting，同学 👋",
                color = colors.primaryTextColor,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(dateText, color = colors.secondaryTextColor, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 9.dp, end = 4.dp)
        ) {
            HyperIconButton(onClick = onAnnouncementClick, minSize = 48.dp) {
                Icon(
                    imageVector = Icons.Outlined.NotificationsNone,
                    contentDescription = "APP 公告",
                    tint = colors.primaryTextColor,
                    modifier = Modifier.size(25.dp)
                )
            }
            if (unreadAnnouncementCount > 0) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFF4D4F))
                )
            }
        }
    }
}

@Composable
private fun PortalDashboard(
    state: OverviewUiState,
    onMetricClick: (String) -> Unit,
    onQuickActionClick: (String) -> Unit,
    onRefreshMetric: (String) -> Unit,
    onRefreshSection: (String) -> Unit
) {
    LegacyPortalDashboard(
        state = state,
        onMetricClick = onMetricClick,
        onQuickActionClick = onQuickActionClick,
        onRefreshMetric = onRefreshMetric,
        onRefreshSection = onRefreshSection
    )
}

@Composable
private fun LegacyPortalDashboard(
    state: OverviewUiState,
    onMetricClick: (String) -> Unit,
    onQuickActionClick: (String) -> Unit,
    onRefreshMetric: (String) -> Unit,
    onRefreshSection: (String) -> Unit
) {
    val score = state.metrics.firstOrNull {
        it.id.contains("score", true) || it.id.contains("Score", true)
    }
    val electric = state.metrics.firstOrNull {
        it.id.contains("electric", true) || it.id.contains("Electronic", true)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        HomeCourseHero(state) { onQuickActionClick("schedule") }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            HomeMetricCard(
                title = "成绩查询",
                metric = score,
                accent = Color(0xFF16B764),
                icon = Icons.Outlined.School,
                modifier = Modifier.weight(1f),
                onClick = { score?.let { onMetricClick(it.id) } }
            )
            HomeMetricCard(
                title = "宿舍电量",
                metric = electric,
                accent = Color(0xFF159CE4),
                icon = Icons.Outlined.Home,
                modifier = Modifier.weight(1f),
                onClick = { electric?.let { onMetricClick(it.id) } }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ExamHighlightCard(
                highlight = state.examHighlight,
                modifier = Modifier.weight(1f),
                onClick = { onQuickActionClick("exam") }
            )
            HomeStatusCard(
                title = "待提交作业",
                icon = Icons.Outlined.AssignmentTurnedIn,
                accent = Color(0xFF19B85A),
                primary = state.pendingHomeworks.firstOrNull()?.title ?: "暂无待提交作业",
                secondary = if (state.pendingHomeworks.isEmpty()) "太棒了，继续保持！" else state.pendingHomeworks.first().deadlineText,
                positive = state.pendingHomeworks.isEmpty(),
                modifier = Modifier.weight(1f),
                onClick = { onQuickActionClick("homework") }
            )
        }

    }
}

@Composable
private fun HomeCourseHero(state: OverviewUiState, onClick: () -> Unit) {
    val colors = AppTheme.colors
    val highlight = state.courseHighlight
    val statusText = if (state.isBoundStudent) highlight?.title.orEmpty().ifBlank { "默认学期" } else "未绑定"
    val headline = when {
        !state.isBoundStudent -> "请先绑定学号"
        highlight != null -> highlight.courseName
        else -> "今天没有课程，好好休息吧"
    }
    val supporting = when {
        !state.isBoundStudent -> "绑定后即可查看当前与下一节课程"
        highlight != null -> buildString {
            append(listOf(highlight.timeText, highlight.location).filter { it.isNotBlank() }.joinToString(" · "))
            if (highlight.moreCount > 0) append(" · 另有${highlight.moreCount}节")
        }
        else -> "暂无后续课程安排"
    }
    HyperSurface(
        shape = RoundedCornerShape(28.dp),
        color = Color(0xFF2584F4),
        shadowElevation = 7.dp,
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .hyperTap(onClick = onClick)
    ) {
        Box(Modifier.fillMaxSize()) {
            Canvas(Modifier.fillMaxSize()) {
                drawRect(
                    Brush.linearGradient(
                        listOf(Color(0xFF2F8EFF), Color(0xFF2E9FEA), Color(0xFF7BC5F7)),
                        start = Offset.Zero,
                        end = Offset(size.width, size.height)
                    )
                )
                drawCircle(Color.White.copy(alpha = 0.08f), size.width * 0.48f, Offset(size.width * 0.95f, size.height * 0.22f))
                drawCircle(Color.White.copy(alpha = 0.07f), size.width * 0.34f, Offset(size.width * 0.72f, size.height * 0.92f))
            }
            Icon(
                imageVector = Icons.Outlined.EventNote,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.22f),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 14.dp, bottom = 4.dp)
                    .size(86.dp)
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(15.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.94f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.EventNote,
                            contentDescription = null,
                            tint = Color(0xFF1879E8),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Text("我的课表", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(7.dp))
                    Text(
                        statusText,
                        color = Color.White.copy(alpha = 0.92f),
                        fontSize = 10.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.16f))
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                    )
                    Spacer(Modifier.weight(1f))
                    Box(
                        Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.ChevronRight, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    headline,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(0.72f)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    supporting,
                    color = Color.White.copy(alpha = 0.82f),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(0.72f)
                )
            }
        }
    }
}

@Composable
private fun HomeMetricCard(
    title: String,
    metric: OverviewMetricUiModel?,
    accent: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = AppTheme.colors
    HyperSurface(
        modifier = modifier
            .height(180.dp)
            .hyperTap(enabled = metric != null, onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        color = colors.bgCardColor,
        shadowElevation = 4.dp
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = accent, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(7.dp))
                Text(title, color = colors.primaryTextColor, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Icon(Icons.Outlined.ChevronRight, null, tint = colors.primaryTextColor, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(metric?.value ?: "--", color = accent, fontSize = 31.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                if (!metric?.unit.isNullOrBlank()) {
                    Text(" ${metric?.unit}", color = colors.secondaryTextColor, fontSize = 10.sp, modifier = Modifier.padding(bottom = 5.dp))
                }
            }
             Text(
                 metric?.subtitle ?: "数据同步后显示",
                 color = colors.secondaryTextColor,
                 fontSize = 10.sp,
                 maxLines = 1,
                 overflow = TextOverflow.Ellipsis
             )
             if (!metric?.badgeText.isNullOrBlank()) {
                 Text(
                     metric!!.badgeText,
                     color = accent.copy(alpha = 0.7f),
                     fontSize = 9.sp,
                     fontWeight = FontWeight.Medium,
                     modifier = Modifier.padding(start = 4.dp)
                 )
             }
             Spacer(Modifier.weight(1f))
            HomeMiniTrend(values = metric?.trendValues.orEmpty(), accent = accent)
        }
    }
}

@Composable
private fun HomeMiniTrend(values: List<Float>, accent: Color) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
    ) {
        if (values.size < 2) {
            drawLine(
                color = accent.copy(alpha = 0.18f),
                start = Offset(0f, size.height * 0.62f),
                end = Offset(size.width, size.height * 0.62f),
                strokeWidth = 3f
            )
            return@Canvas
        }
        val source = values
        val min = source.minOrNull() ?: 0f
        val range = ((source.maxOrNull() ?: min) - min).coerceAtLeast(0.01f)
        val points = source.mapIndexed { index, value ->
            Offset(
                x = index * size.width / (source.lastIndex.coerceAtLeast(1)),
                y = 5f + (1f - (value - min) / range) * (size.height - 10f)
            )
        }
        val area = Path().apply {
            moveTo(points.first().x, size.height)
            points.forEach { lineTo(it.x, it.y) }
            lineTo(points.last().x, size.height)
            close()
        }
        drawPath(area, Brush.verticalGradient(listOf(accent.copy(alpha = 0.22f), Color.Transparent)))
        val line = Path().apply {
            moveTo(points.first().x, points.first().y)
            points.drop(1).forEach { lineTo(it.x, it.y) }
        }
        drawPath(line, accent, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.5f))
        if (values.size >= 2 && values.size <= 8) points.forEach { drawCircle(accent, 4.5f, it) }
    }
}

@Composable
private fun HomeStatusCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    primary: String,
    secondary: String,
    positive: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = AppTheme.colors
    HyperSurface(
        modifier = modifier
            .height(150.dp)
            .hyperTap(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        color = colors.bgCardColor,
        shadowElevation = 4.dp
    ) {
        Column(Modifier.fillMaxSize().padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = accent, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(7.dp))
                Text(title, color = colors.primaryTextColor, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Icon(Icons.Outlined.ChevronRight, null, tint = colors.primaryTextColor, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(17.dp))
                    .background(accent.copy(alpha = 0.09f))
                    .padding(9.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.align(Alignment.CenterStart)) {
                    if (positive) {
                        Icon(Icons.Outlined.CheckCircle, null, tint = accent, modifier = Modifier.size(30.dp))
                        Spacer(Modifier.width(8.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(primary, color = if (positive) accent else colors.primaryTextColor, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (secondary.isNotBlank()) {
                            Spacer(Modifier.height(2.dp))
                            Text(secondary, color = colors.secondaryTextColor, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExamHighlightCard(
    highlight: ExamHighlightUiModel?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = AppTheme.colors
    val accent = Color(0xFF278CF3)
    val model = highlight ?: ExamHighlightUiModel(
        type = ExamHighlightType.EMPTY,
        title = "考试安排",
        courseName = "今天没有考试哦",
        timeText = "继续保持！"
    )
    val isEmpty = model.type == ExamHighlightType.EMPTY

    HyperSurface(
        modifier = modifier
            .height(150.dp)
            .hyperTap(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        color = colors.bgCardColor,
        shadowElevation = 4.dp
    ) {
        Column(Modifier.fillMaxSize().padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.EventNote, null, tint = accent, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(7.dp))
                Text("考试安排", color = colors.primaryTextColor, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Icon(Icons.Outlined.ChevronRight, null, tint = colors.primaryTextColor, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(17.dp))
                    .background(accent.copy(alpha = 0.09f))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Column(modifier = Modifier.align(Alignment.TopStart)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isEmpty) {
                            Icon(Icons.Outlined.CheckCircle, null, tint = accent, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(8.dp))
                        } else {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(accent.copy(alpha = 0.18f))
                                    .padding(horizontal = 10.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    model.title,
                                    color = accent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        model.courseName,
                        color = if (isEmpty) accent else colors.primaryTextColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (model.timeText.isNotBlank()) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            model.timeText,
                            color = colors.secondaryTextColor,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (model.moreCount > 0) {
                    Text(
                        text = "还有 ${model.moreCount} 场考试 >",
                        color = accent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.BottomEnd)
                    )
                }
            }
        }
    }
}

@Composable
private fun WeeklyStudyOverview(state: OverviewUiState, onClick: () -> Unit) {
    val colors = AppTheme.colors
    val courseCount = state.todayCourses.size
    val homeworkCount = state.pendingHomeworks.size
    HyperSurface(
        modifier = Modifier
            .fillMaxWidth()
            .height(112.dp)
            .hyperTap(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        color = colors.bgCardColor,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(17.dp))
                    .background(Color(0xFF2B8EF4).copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.School, null, tint = Color(0xFF2B8EF4), modifier = Modifier.size(30.dp))
            }
            Spacer(Modifier.width(15.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("本周学习概览", color = colors.primaryTextColor, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text("今日课程 $courseCount 节 · 待提交作业 $homeworkCount 项", color = colors.secondaryTextColor, fontSize = 12.sp)
                Spacer(Modifier.height(9.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFDCE4EE))
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(if (homeworkCount == 0) 1f else 0.68f)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(Color(0xFF2B9CF0))
                    )
                }
            }
            Spacer(Modifier.width(14.dp))
            Icon(Icons.Outlined.ChevronRight, contentDescription = "查看课表", tint = Color(0xFF2589EF), modifier = Modifier.size(25.dp))
        }
    }
}

@Composable
private fun HeaderBlock(
    state: OverviewUiState,
    onBindClick: () -> Unit
) {
    val colors = AppTheme.colors
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "概览",
            color = colors.primaryTextColor,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = state.dateText.substringBefore("·").trim(),
            color = colors.secondaryTextColor,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )
        if (!state.isBoundStudent) {
            Spacer(modifier = Modifier.height(14.dp))
            BindPrompt("绑定学号，开启完整校园服务", onBindClick)
        } else if (state.isSilentSyncing && state.todayCourses.isEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text("正在同步校园数据…", color = colors.secondaryTextColor, fontSize = 14.sp)
        }
    }
}

@Composable
private fun BindPrompt(text: String, onClick: () -> Unit) {
    val colors = AppTheme.colors
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(colors.overviewPromptBackgroundColor)
            .hyperTap(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(text = text, color = colors.overviewPromptTextColor, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SectionTitle(
    title: String,
    actionText: String,
    onClick: () -> Unit
) {
    val colors = AppTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = colors.primaryTextColor,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        Row(
            modifier = Modifier.hyperTap(onClick = onClick),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = actionText,
                color = colors.secondaryTextColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Icon(
                painter = painterResource(R.drawable.ic_arrow_right),
                contentDescription = actionText,
                tint = colors.secondaryTextColor,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun TodayCourseSection(
    courses: List<OverviewCourseUiModel>,
    emptyMessage: String,
    onClick: () -> Unit
) {
    val colors = AppTheme.colors
    HyperSurface(
        shape = RoundedCornerShape(HyperSpacing.cardRadius),
        color = colors.bgCardColor,
        modifier = Modifier.hyperTap(onClick = onClick)
    ) {
        if (courses.isEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = HyperSpacing.cardPaddingHorizontal,
                        vertical = 16.dp
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_confirm),
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    alpha = 0.95f
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = emptyMessage.ifBlank { "没有数据" },
                    color = colors.primaryTextColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = HyperSpacing.cardPaddingHorizontal,
                        vertical = HyperSpacing.cardPaddingVertical
                    ),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                courses.forEach { course ->
                    Row(
                        modifier = Modifier.height(IntrinsicSize.Min),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .width(6.dp)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(999.dp))
                                .background(course.accentColor)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = course.courseName,
                                color = colors.primaryTextColor,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                lineHeight = 24.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${course.timeText}  ·  ${course.classroom}",
                                color = course.accentColor,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = course.teacher,
                                color = colors.secondaryTextColor,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricRow(
    metrics: List<OverviewMetricUiModel>,
    onMetricClick: (String) -> Unit
) {
    val colors = AppTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        metrics.forEach { metric ->
            HyperSurface(
                shape = RoundedCornerShape(HyperSpacing.cardRadius),
                color = colors.bgCardColor,
                modifier = Modifier
                    .weight(1f)
                    .height(148.dp)
                    .hyperTap { onMetricClick(metric.id) }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(HyperSpacing.cardPadding),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(IconContainerShape)
                                .background(
                                    when (metric.id) {
                                        "ScoreInquiry", "score" -> colors.overviewScoreIconBackgroundColor
                                        "Electronic", "electric" -> colors.overviewElectricIconBackgroundColor
                                        else -> metric.accentColor.copy(alpha = 0.14f)
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (metric.id == "Electronic" || metric.id == "electric") {
                                    Icons.Outlined.Bolt
                                } else {
                                    Icons.Outlined.Insights
                                },
                                contentDescription = metric.title,
                                tint = metric.accentColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = metric.title,
                            color = colors.secondaryTextColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                text = metric.value,
                                color = metric.accentColor,
                                fontSize = metricValueFontSize(metric.value),
                                fontWeight = FontWeight.Black,
                                maxLines = 1
                            )
                            if (metric.unit.isNotBlank()) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = metric.unit,
                                    color = colors.secondaryTextColor,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 5.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        MetricSubtitle(metric)
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricSubtitle(metric: OverviewMetricUiModel) {
    val colors = AppTheme.colors
    val subtitlePages = remember(metric.subtitle, metric.secondarySubtitle) {
        listOf(metric.subtitle, metric.secondarySubtitle)
            .filter { it.isNotBlank() }
            .distinct()
    }
    val currentIndex = remember(metric.id, subtitlePages) { mutableIntStateOf(0) }

    LaunchedEffect(metric.id, subtitlePages) {
        if (subtitlePages.size <= 1) return@LaunchedEffect
        while (true) {
            delay(2800)
            currentIndex.intValue = (currentIndex.intValue + 1) % subtitlePages.size
        }
    }

    val currentSubtitle = subtitlePages.getOrElse(currentIndex.intValue) { metric.subtitle }
    AnimatedContent(
        targetState = currentSubtitle,
        transitionSpec = {
            (slideInVertically { it / 2 } + fadeIn()).togetherWith(
                slideOutVertically { -it / 2 } + fadeOut()
            )
        },
        label = "overview_metric_subtitle"
    ) { subtitle ->
        Text(
            text = subtitle,
            color = colors.secondaryTextColor,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun HomeworkCard(homework: OverviewHomeworkUiModel, onClick: () -> Unit) {
    val colors = AppTheme.colors
    val courseName = runCatching { homework.courseName }.getOrDefault("")
    HyperSurface(
        shape = RoundedCornerShape(HyperSpacing.cardRadius),
        color = if (homework.isUrgent) colors.overviewUrgentBackgroundColor else colors.bgCardColor,
        border = if (homework.isUrgent) BorderStroke(1.5.dp, colors.overviewUrgentBorderColor) else null,
        modifier = Modifier.hyperTap(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .height(IntrinsicSize.Min)
                .fillMaxWidth()
                .padding(
                    horizontal = HyperSpacing.cardPaddingHorizontal,
                    vertical = HyperSpacing.cardPaddingVertical
                ),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(999.dp))
                    .background(HomeworkAccent)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                Text(
                    text = homework.title,
                    color = colors.primaryTextColor,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 23.sp
                )
                if (courseName.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = courseName,
                        color = colors.secondaryTextColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (homework.deadlineText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = homework.deadlineText,
                        color = if (homework.isUrgent) colors.overviewUrgentBorderColor else colors.secondaryTextColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                if (homework.urgencyText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = homework.urgencyText,
                        color = if (homework.isUrgent) colors.overviewUrgentBorderColor else colors.secondaryTextColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(HomeworkAccent.copy(alpha = 0.12f))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = homework.statusText,
                    color = HomeworkAccent,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ExamCard(exam: OverviewExamUiModel) {
    val colors = AppTheme.colors
    HyperSurface(
        shape = RoundedCornerShape(HyperSpacing.cardRadius),
        color = colors.bgCardColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = HyperSpacing.cardPaddingHorizontal,
                    vertical = HyperSpacing.cardPaddingVertical
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = exam.courseName,
                    color = colors.primaryTextColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(5.dp))
                Text(text = exam.examTime, color = colors.secondaryTextColor, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(3.dp))
                Text(text = exam.location, color = colors.secondaryTextColor, fontSize = 13.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(colors.overviewExamBadgeBackgroundColor)
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = exam.badge,
                    color = colors.overviewExamBadgeTextColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun TestCard(test: OverviewTestUiModel, onClick: () -> Unit) {
    val colors = AppTheme.colors
    val courseName = runCatching { test.courseName }.getOrDefault("")
    HyperSurface(
        shape = RoundedCornerShape(HyperSpacing.cardRadius),
        color = if (test.isUrgent) colors.overviewUrgentBackgroundColor else colors.bgCardColor,
        border = if (test.isUrgent) BorderStroke(1.5.dp, colors.overviewUrgentBorderColor) else null,
        modifier = Modifier.hyperTap(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .height(IntrinsicSize.Min)
                .fillMaxWidth()
                .padding(
                    horizontal = HyperSpacing.cardPaddingHorizontal,
                    vertical = HyperSpacing.cardPaddingVertical
                ),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(999.dp))
                    .background(TestAccent)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                Text(
                    text = test.title,
                    color = colors.primaryTextColor,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 23.sp
                )
                if (courseName.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = courseName,
                        color = colors.secondaryTextColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (test.timeText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = test.timeText,
                        color = if (test.isUrgent) colors.overviewUrgentBorderColor else colors.secondaryTextColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                if (test.urgencyText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = test.urgencyText,
                        color = if (test.isUrgent) colors.overviewUrgentBorderColor else colors.secondaryTextColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(TestAccent.copy(alpha = 0.12f))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = test.statusText,
                    color = TestAccent,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun NeutralCard(text: String) {
    val colors = AppTheme.colors
    HyperSurface(
        shape = RoundedCornerShape(HyperSpacing.cardRadius),
        color = colors.bgCardColor
    ) {
        Text(
            text = if (text.isBlank()) "没有数据" else text,
            color = colors.secondaryTextColor,
            fontSize = 15.sp,
            modifier = Modifier.padding(
                horizontal = HyperSpacing.cardPaddingHorizontal,
                vertical = HyperSpacing.cardPaddingVertical
            )
        )
    }
}

private fun metricValueFontSize(value: String) = when {
    value.length >= 6 -> 28.sp
    value.length >= 5 -> 32.sp
    else -> 38.sp
}

@Preview(showBackground = true, backgroundColor = 0xFFF3F3F6)
@Composable
private fun OverviewScreenPreview() {
    AppSkinTheme {
        OverviewScreen(
            state = OverviewUiState(
                isBoundStudent = true,
                isElectricityBound = true,
                dateText = "3月13日 周五  ·  2025-2026-2 第1周",
                metrics = listOf(
                    OverviewMetricUiModel("score", "GPA", "3.02", "", "平均分: 82.0", "", R.drawable.ic_rank, Color(0xFFE3B92C)),
                    OverviewMetricUiModel("electric", "电费", "207.61", "kWh", "预计163天后电量耗尽", "更新于 03-15 21:30", R.drawable.ic_bill, Color(0xFF62C466))
                ),
                todayCourses = listOf(
                    OverviewCourseUiModel("1", "高等数学", "云塘 A201", "陈老师", "1-2节", "校园课表", Color(0xFF5E87F6)),
                    OverviewCourseUiModel("2", "大学英语", "云塘 B305", "李老师", "3-4节", "校园课表", Color(0xFFF08D3C))
                ),
                pendingHomeworks = listOf(
                    OverviewHomeworkUiModel("1", "大数据存储与管理实验A", "计算机网络", "2026-03-13 23:59", "8小时内截止", true),
                    OverviewHomeworkUiModel("2", "数据库原理与技术", "数据库系统概论", "2026-03-15 20:00", "2天内截止", false)
                ),
                pendingTests = listOf(
                    OverviewTestUiModel("1", "第3章随堂测试", "数据库原理与技术", "2026-03-14 09:00 - 2026-03-14 22:00", "今天内截止", true)
                ),
                examHighlight = ExamHighlightUiModel(
                    type = ExamHighlightType.TOMORROW,
                    title = "明日考试",
                    courseName = "大学物理实验",
                    timeText = "09:00 - 11:00",
                    location = "云塘校区 · 综合楼",
                    moreCount = 1
                )
            ),
            onBindClick = {},
            onMetricClick = {},
            onQuickActionClick = {},
            onRefreshMetric = {},
            onRefreshSection = {},
            announcementState = AnnouncementState(),
            onRefreshAnnouncements = {},
            onMarkAnnouncementRead = {},
            onUrgentPresented = {}
        )
    }
}
