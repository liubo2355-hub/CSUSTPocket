package com.csust.pocket.feature.timetable.ui.compose

import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.csust.pocket.R
import com.csust.pocket.core.designsystem.PortalBackButton
import com.csust.pocket.core.theme.AppSkinTheme
import com.csust.pocket.core.theme.AppTheme
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

internal val kCourseCardColors = listOf(
    Pair(Color(0xFF5B96F5), Color(0xFF4D88EB)),
    Pair(Color(0xFF51DEC0), Color(0xFF3FCFAF)),
    Pair(Color(0xFFA77BEA), Color(0xFF9669DC)),
    Pair(Color(0xFFFFAD3D), Color(0xFFFF9F27)),
    Pair(Color(0xFFFF4D70), Color(0xFFF43F64)),
    Pair(Color(0xFF55A9E8), Color(0xFF4598D7)),
    Pair(Color(0xFFFF8A65), Color(0xFFF57956)),
    Pair(Color(0xFF74C96A), Color(0xFF62B958)),
)
internal val kCourseCardTextColor = Color.White

internal fun courseCardColorPair(courseTitle: String): Pair<Color, Color> =
    kCourseCardColors[courseTitle.hashCode().absoluteValue % kCourseCardColors.size]

private fun generatedCourseColorPair(index: Int): Pair<Color, Color> {
    val hue = ((index * 37) % 360).toFloat()
    val top = Color.hsl(hue, saturation = 0.72f, lightness = 0.62f)
    val bottom = Color.hsl(hue, saturation = 0.76f, lightness = 0.55f)
    return top to bottom
}

private fun buildUniqueCourseColorMap(courses: List<TimeTableCourseUi>): Map<String, Pair<Color, Color>> {
    val titles = courses
        .map { it.title.trim() }
        .filter { it.isNotBlank() }
        .distinct()
        .sorted()

    return titles.mapIndexed { index, title ->
        val colors = if (index < kCourseCardColors.size) {
            kCourseCardColors[index]
        } else {
            generatedCourseColorPair(index)
        }
        title to colors
    }.toMap()
}

enum class WeekBadgeState {
    NotStarted,
    Current,
    NotCurrent,
}

private fun lerpFloat(start: Float, end: Float, fraction: Float): Float {
    return start + (end - start) * fraction
}

@Immutable
private data class TimetableSizeSpec(
    val timeColumnWidth: Dp = 38.dp,
    val cellHeight: Dp = 88.dp,
)

@Immutable
private data class EmptySlotSelection(
    val dayOfWeek: Int,
    val startSection: Int,
    val endSection: Int,
) {
    val sectionSpan: Int get() = endSection - startSection + 1
}

@Composable
fun TimeTableComposeScreen(
    modifier: Modifier = Modifier,
    termText: String,
    weekText: String,
    displayWeek: Int,
    currentWeek: Int,
    termStarted: Boolean,
    courses: List<TimeTableCourseUi>,
    remark: List<String> = emptyList(),
    dateHeaderProvider: (Int) -> Pair<String, List<TimeTableDayHeaderUi>>,
    isRefreshing: Boolean,
    onBackClick: () -> Unit = {},
    onRefreshClick: () -> Unit,
    onTermClick: () -> Unit,
    onWeekClick: () -> Unit,
    onWeekChange: (Int) -> Unit,
    onEmptySlotClick: (week: Int, dayOfWeek: Int, startSection: Int, sectionSpan: Int) -> Unit,
    onCourseClick: (TimeTableCourseUi) -> Unit,
    onOverlapCoursesClick: (List<TimeTableCourseUi>) -> Unit,
    onCourseLongClick: (TimeTableCourseUi) -> Unit,
    onCourseMove: (TimeTableCourseUi, dayOfWeek: Int, startSection: Int) -> Unit,
) {
    val colors = AppTheme.colors
    val sizeSpec = remember { TimetableSizeSpec() }
    val pagerState = rememberPagerState(
        initialPage = (displayWeek - 1).coerceIn(0, TIMETABLE_TOTAL_WEEKS - 1),
        pageCount = { TIMETABLE_TOTAL_WEEKS },
    )

    LaunchedEffect(displayWeek) {
        val targetPage = (displayWeek - 1).coerceIn(0, TIMETABLE_TOTAL_WEEKS - 1)
        if (!pagerState.isScrollInProgress && pagerState.currentPage != targetPage) {
            if (abs(pagerState.currentPage - targetPage) > 2) {
                pagerState.scrollToPage(targetPage)
            } else {
                pagerState.animateScrollToPage(targetPage)
            }
        }
    }

    val coursesByWeek = remember(courses) {
        (1..TIMETABLE_TOTAL_WEEKS).associateWith { week ->
            courses.filter { it.isVisibleInWeek(week) }
        }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }
            .map { it + 1 }
            .distinctUntilChanged()
            .collect { week ->
                if (week != displayWeek) onWeekChange(week)
            }
    }

    val settledWeek = remember { derivedStateOf { pagerState.settledPage + 1 } }
    val effectiveWeek = settledWeek.value
    val weekBadgeState = remember(termStarted, currentWeek, effectiveWeek) {
        when {
            !termStarted -> WeekBadgeState.NotStarted
            effectiveWeek == currentWeek -> WeekBadgeState.Current
            else -> WeekBadgeState.NotCurrent
        }
    }
    val (monthText, dayHeaders) = dateHeaderProvider(settledWeek.value)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        colors.bgTopBarColor.copy(alpha = 0.95f),
                        colors.bgPrimaryColor,
                        colors.bgPrimaryColor,
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        TimeTableTopBar(
            week = effectiveWeek,
            dateText = dayHeaders.firstOrNull()?.fullDateLabel.orEmpty(),
            termText = termText,
            isRefreshing = isRefreshing,
            onBackClick = onBackClick,
            onRefreshClick = onRefreshClick,
            onAddClick = { onEmptySlotClick(effectiveWeek, 1, 1, 1) },
            onTermClick = onTermClick,
            onWeekClick = onWeekClick,
        )
        DateHeaderRow(
            monthText = monthText,
            dayHeaders = dayHeaders,
            timeColumnWidth = sizeSpec.timeColumnWidth,
        )

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(0.dp),
            pageSpacing = 0.dp,
        ) { pageIndex ->
            val week = pageIndex + 1
            val pageCourses = coursesByWeek[week].orEmpty()
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors.bgPrimaryColor)
            ) {
                val scrollState = rememberScrollState()
                TimeAxisColumn(
                    modifier = Modifier
                        .width(sizeSpec.timeColumnWidth)
                        .verticalScroll(scrollState),
                    cellHeight = sizeSpec.cellHeight,
                )

                TimeTableGrid(
                    modifier = Modifier.weight(1f),
                    cellHeight = sizeSpec.cellHeight,
                    scrollState = scrollState,
                    visibleCourses = pageCourses,
                    onEmptySlotClick = { dayOfWeek, startSection, sectionSpan ->
                        val slot = timetableGridSlot(week, dayOfWeek, startSection)
                        onEmptySlotClick(slot.week, slot.dayOfWeek, slot.startSection, sectionSpan)
                    },
                    onCourseClick = onCourseClick,
                    onOverlapCoursesClick = onOverlapCoursesClick,
                    onCourseLongClick = onCourseLongClick,
                    onCourseMove = onCourseMove,
                )
            }
        }
    }
}

@Composable
private fun TimeTableTopBar(
    week: Int,
    dateText: String,
    termText: String,
    isRefreshing: Boolean,
    onBackClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onAddClick: () -> Unit,
    onTermClick: () -> Unit,
    onWeekClick: () -> Unit,
) {
    val colors = AppTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBackClick, modifier = Modifier.size(36.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = colors.primaryTextColor)
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp),
        ) {
            Text(
                "第${week}周  周一",
                modifier = Modifier.clickable(onClick = onWeekClick),
                fontSize = 19.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.primaryTextColor,
            )
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onTermClick)
                    .padding(end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (dateText.isNotBlank()) {
                    Text(
                        text = "$dateText  ",
                        fontSize = 10.sp,
                        color = colors.secondaryTextColor,
                        maxLines = 1,
                    )
                }
                Text(
                    text = "$termText  ▾",
                    modifier = Modifier
                        .clip(RoundedCornerShape(7.dp))
                        .background(colors.titleTopColor.copy(alpha = 0.10f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.titleTopColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        IconButton(onClick = onAddClick, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Filled.Add, contentDescription = "添加课程", tint = colors.primaryTextColor, modifier = Modifier.size(28.dp))
        }
        if (isRefreshing) {
            CircularProgressIndicator(
                modifier = Modifier
                    .width(24.dp)
                    .height(24.dp),
                strokeWidth = 2.dp,
                color = colors.titleTopColor,
            )
        } else {
            IconButton(onClick = onRefreshClick) {
                Icon(
                    painter = painterResource(id = R.drawable.coursetable_ic_refresh),
                    contentDescription = "刷新",
                    tint = colors.titleTopColor,
                )
            }
        }
    }
}

@Composable
private fun TermWeekBar(
    termText: String,
    weekText: String,
    weekBadgeState: WeekBadgeState,
    remark: List<String> = emptyList(),
    onTermClick: () -> Unit,
    onWeekClick: () -> Unit,
) {
    val colors = AppTheme.colors
    var showRemarkDialog = remember { mutableStateOf(false) }
    val context = LocalContext.current
    val badgeContainerColor = when (weekBadgeState) {
        WeekBadgeState.NotStarted -> colors.textHeighLightColor.copy(alpha = 0.14f)
        WeekBadgeState.Current -> colors.successGreenColor.copy(alpha = 0.16f)
        WeekBadgeState.NotCurrent -> colors.secondaryTextColor.copy(alpha = 0.12f)
    }
    val badgeTextColor = when (weekBadgeState) {
        WeekBadgeState.NotStarted -> colors.textHeighLightColor
        WeekBadgeState.Current -> colors.successGreenColor
        WeekBadgeState.NotCurrent -> colors.secondaryTextColor
    }
    val badgeText = when (weekBadgeState) {
        WeekBadgeState.NotStarted -> "未开学"
        WeekBadgeState.Current -> "本周"
        WeekBadgeState.NotCurrent -> "非本周"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 14.dp, end = 14.dp, top = 1.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        TimetableCapsule(
            text = termText,
            leadingIcon = R.drawable.coursetable_ic_calendar,
            onClick = onTermClick,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            TimetableCapsule(
                text = weekText,
                trailingIcon = R.drawable.coursetable_ic_extend,
                onClick = onWeekClick,
            )
            IconButton(
                onClick = {
                    if (remark.isNotEmpty()) {
                        showRemarkDialog.value = true
                    } else {
                        Toast.makeText(context, "暂无备注", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = "备注",
                    tint = colors.secondaryTextColor,
                    modifier = Modifier.size(20.dp),
                )
            }
            Surface(
                shape = CircleShape,
                color = badgeContainerColor,
            ) {
                Text(
                    text = badgeText,
                    color = badgeTextColor,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    if (showRemarkDialog.value) {
        RemarkDialog(
            remark = remark,
            onDismiss = { showRemarkDialog.value = false },
        )
    }

    }

}
@Composable
private fun TimetableCapsule(
    text: String,
    onClick: () -> Unit,
    leadingIcon: Int? = null,
    trailingIcon: Int? = null,
) {
    val colors = AppTheme.colors
    Surface(
        modifier = Modifier
            .clip(CircleShape)
            .border(1.dp, colors.outlineLowContrastColor.copy(alpha = 0.5f), CircleShape)
            .combinedClickable(onClick = onClick),
        color = colors.bgCardColor.copy(alpha = 0.92f),
        shape = CircleShape,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (leadingIcon != null) {
                Icon(
                    modifier = Modifier.size(24.dp, 24.dp),
                    painter = painterResource(id = leadingIcon),
                    contentDescription = null,
                    tint = colors.iconSecondaryColor,
                )
            }
            Text(
                text = text,
                color = colors.primaryTextColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )
            if (trailingIcon != null) {
                Icon(
                    painter = painterResource(id = trailingIcon),
                    contentDescription = null,
                    tint = colors.iconSecondaryColor,
                )
            }
        }
    }
}

@Composable
private fun DateHeaderRow(
    monthText: String,
    dayHeaders: List<TimeTableDayHeaderUi>,
    timeColumnWidth: Dp,
) {
    val colors = AppTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = monthText.replace("月", "\n月"),
            color = colors.primaryTextColor,
            fontSize = 12.sp,
            lineHeight = 15.sp,
            modifier = Modifier.width(timeColumnWidth),
            textAlign = TextAlign.Center,
        )
        dayHeaders.forEach { header ->
            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = header.weekdayLabel.removePrefix("周"),
                    color = if (header.isToday) colors.primaryTextColor else colors.greyTextColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                )
                Surface(
                    color = if (header.isToday) colors.primaryTextColor else Color.Transparent,
                    shape = RoundedCornerShape(9.dp),
                ) {
                    Text(
                        text = header.dayOfMonthLabel,
                        color = if (header.isToday) colors.bgPrimaryColor else colors.greyTextColor,
                        fontSize = 12.sp,
                        fontWeight = if (header.isToday) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun TimeAxisColumn(
    modifier: Modifier,
    cellHeight: Dp,
) {
    val colors = AppTheme.colors
    val times = remember {
        listOf(
            "8:00\n8:45", "8:55\n9:40", "10:10\n10:55", "11:05\n11:50",
            "14:00\n14:45", "14:55\n15:40", "16:10\n16:55", "17:05\n17:50",
            "19:30\n20:15", "20:25\n21:10"
        )
    }

    Column(modifier = modifier) {
        repeat(TIMETABLE_TOTAL_SECTIONS) { index ->
            Column(
                modifier = Modifier
                    .height(cellHeight)
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "${index + 1}",
                    fontSize = 16.sp,
                    color = colors.primaryTextColor,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = times[index],
                    fontSize = 9.sp,
                    lineHeight = 11.sp,
                    color = colors.greyTextColor,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun TimeTableGrid(
    modifier: Modifier,
    cellHeight: Dp,
    scrollState: androidx.compose.foundation.ScrollState,
    visibleCourses: List<TimeTableCourseUi>,
    onEmptySlotClick: (dayOfWeek: Int, startSection: Int, sectionSpan: Int) -> Unit,
    onCourseClick: (TimeTableCourseUi) -> Unit,
    onOverlapCoursesClick: (List<TimeTableCourseUi>) -> Unit,
    onCourseLongClick: (TimeTableCourseUi) -> Unit,
    onCourseMove: (TimeTableCourseUi, dayOfWeek: Int, startSection: Int) -> Unit,
) {
    val colors = AppTheme.colors
    val totalHeight = cellHeight * TIMETABLE_TOTAL_SECTIONS
    val courseColorMap = remember(visibleCourses) {
        buildUniqueCourseColorMap(visibleCourses)
    }
    val cellCoursesMap = remember(visibleCourses) {
        val result = mutableMapOf<Pair<Int, Int>, MutableList<TimeTableCourseUi>>()
        visibleCourses.forEach { course ->
            for (section in course.occupiedSections()) {
                val key = course.dayOfWeek to section
                result.getOrPut(key) { mutableListOf() }.add(course)
            }
        }
        result
    }
    val courseOverlapMap = remember(visibleCourses, cellCoursesMap) {
        val result = mutableMapOf<Int, List<TimeTableCourseUi>>()
        visibleCourses.forEach { course ->
            val overlaps = linkedSetOf<TimeTableCourseUi>()
            for (section in course.occupiedSections()) {
                cellCoursesMap[course.dayOfWeek to section].orEmpty().forEach { overlaps.add(it) }
            }
            result[course.id] = overlaps.toList()
        }
        result
    }
    val emptySelection = remember { mutableStateOf<EmptySlotSelection?>(null) }

    LaunchedEffect(visibleCourses) {
        emptySelection.value = null
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(scrollState),
    ) {
        val boardWidth = maxWidth
        val columnWidth = boardWidth / TIMETABLE_TOTAL_DAYS

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(totalHeight)
                .padding(end = 2.dp),
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val lineColor = colors.secondaryTextColor.copy(alpha = 0.34f)
                val dash = PathEffect.dashPathEffect(floatArrayOf(7.dp.toPx(), 6.dp.toPx()))
                val h = size.height
                val w = size.width
                val rowHeightPx = cellHeight.toPx()
                val colWidthPx = w / TIMETABLE_TOTAL_DAYS

                repeat(TIMETABLE_TOTAL_SECTIONS + 1) { row ->
                    val y = row * rowHeightPx
                    drawLine(
                        lineColor,
                        start = androidx.compose.ui.geometry.Offset(0f, y),
                        end = androidx.compose.ui.geometry.Offset(w, y),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = dash,
                    )
                }

                repeat(TIMETABLE_TOTAL_DAYS + 1) { col ->
                    val x = col * colWidthPx
                    drawLine(
                        lineColor,
                        start = androidx.compose.ui.geometry.Offset(x, 0f),
                        end = androidx.compose.ui.geometry.Offset(x, h),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = dash,
                    )
                }
            }

            Column(modifier = Modifier.matchParentSize()) {
                repeat(TIMETABLE_TOTAL_SECTIONS) { section ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        repeat(TIMETABLE_TOTAL_DAYS) { day ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(cellHeight)
                                    .combinedClickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = {
                                            val hitCourses =
                                                cellCoursesMap[day + 1 to section + 1].orEmpty()
                                            when (hitCourses.size) {
                                                0 -> {
                                                    if (emptySelection.value == null) {
                                                        emptySelection.value = EmptySlotSelection(
                                                            dayOfWeek = day + 1,
                                                            startSection = section + 1,
                                                            endSection = section + 1,
                                                        )
                                                    } else {
                                                        emptySelection.value = null
                                                    }
                                                }
                                                1 -> {
                                                    emptySelection.value = null
                                                    onCourseClick(hitCourses.first())
                                                }
                                                else -> {
                                                    emptySelection.value = null
                                                    onOverlapCoursesClick(hitCourses)
                                                }
                                            }
                                        },
                                        onLongClick = {
                                            val hitCourses =
                                                cellCoursesMap[day + 1 to section + 1].orEmpty()
                                            if (hitCourses.isNotEmpty()) {
                                                onCourseLongClick(hitCourses.first())
                                            }
                                        },
                                    ),
                            )
                        }
                    }
                }
            }

            val courseGestureScope = rememberCoroutineScope()
            visibleCourses.forEach { course ->
                val x = columnWidth * (course.dayOfWeek - 1)
                val y = cellHeight * (course.startSection - 1)
                val cardHeight = cellHeight * course.occupiedSections().count()
                val columnWidthPx = with(androidx.compose.ui.platform.LocalDensity.current) {
                    columnWidth.toPx()
                }
                val cellHeightPx = with(androidx.compose.ui.platform.LocalDensity.current) {
                    cellHeight.toPx()
                }
                val courseDragOffset = remember(
                    course.id,
                    course.dayOfWeek,
                    course.startSection,
                ) {
                    mutableStateOf(androidx.compose.ui.geometry.Offset.Zero)
                }
                val courseDragging = remember(course.id) { mutableStateOf(false) }
                val pendingCourseDragOffset = remember(course.id) {
                    mutableStateOf(androidx.compose.ui.geometry.Offset.Zero)
                }
                val courseDragArmed = remember(course.id) { mutableStateOf(false) }
                val courseArmJob = remember(course.id) { mutableStateOf<Job?>(null) }
                val courseScale = animateFloatAsState(
                    targetValue = if (courseDragArmed.value || courseDragging.value) 0.97f else 1f,
                    animationSpec = tween(durationMillis = 140),
                    label = "courseDragScale",
                )
                val moveModifier = Modifier
                        .zIndex(if (courseDragging.value) 5f else 1f)
                        .graphicsLayer {
                            translationX = courseDragOffset.value.x
                            translationY = courseDragOffset.value.y
                            scaleX = courseScale.value
                            scaleY = courseScale.value
                            if (courseDragging.value) {
                                shadowElevation = 10.dp.toPx()
                            } else if (courseDragArmed.value) {
                                shadowElevation = 4.dp.toPx()
                            }
                        }
                        .pointerInput(
                            course.id,
                            course.dayOfWeek,
                            course.startSection,
                            columnWidthPx,
                            cellHeightPx,
                        ) {
                            val dragActivationDistance = 12.dp.toPx()
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    courseDragArmed.value = false
                                    pendingCourseDragOffset.value =
                                        androidx.compose.ui.geometry.Offset.Zero
                                    courseArmJob.value?.cancel()
                                    courseArmJob.value = courseGestureScope.launch {
                                        delay(300L)
                                        courseDragArmed.value = true
                                    }
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    if (courseDragging.value) {
                                        courseDragOffset.value += dragAmount
                                    } else {
                                        val pending = pendingCourseDragOffset.value + dragAmount
                                        pendingCourseDragOffset.value = pending
                                        if (
                                            courseDragArmed.value &&
                                            pending.getDistance() >= dragActivationDistance
                                        ) {
                                            courseDragging.value = true
                                            courseDragOffset.value = pending
                                        }
                                    }
                                },
                                onDragCancel = {
                                    courseArmJob.value?.cancel()
                                    courseArmJob.value = null
                                    courseDragOffset.value = androidx.compose.ui.geometry.Offset.Zero
                                    pendingCourseDragOffset.value =
                                        androidx.compose.ui.geometry.Offset.Zero
                                    courseDragArmed.value = false
                                    courseDragging.value = false
                                },
                                onDragEnd = {
                                    courseArmJob.value?.cancel()
                                    courseArmJob.value = null
                                    val dayDelta =
                                        (courseDragOffset.value.x / columnWidthPx).roundToInt()
                                    val sectionDelta =
                                        (courseDragOffset.value.y / cellHeightPx).roundToInt()
                                    val targetDay = (course.dayOfWeek + dayDelta)
                                        .coerceIn(1, TIMETABLE_TOTAL_DAYS)
                                    val targetStart = (course.startSection + sectionDelta)
                                        .coerceIn(
                                            1,
                                            TIMETABLE_TOTAL_SECTIONS - course.sectionSpan + 1,
                                        )
                                    val targetEnd = targetStart + course.sectionSpan - 1
                                    val targetIsEmpty = (targetStart..targetEnd).all { section ->
                                        cellCoursesMap[targetDay to section]
                                            .orEmpty()
                                            .all { it.id == course.id }
                                    }
                                    if (
                                        targetIsEmpty &&
                                        (targetDay != course.dayOfWeek || targetStart != course.startSection)
                                    ) {
                                        onCourseMove(course, targetDay, targetStart)
                                    }
                                    courseDragOffset.value = androidx.compose.ui.geometry.Offset.Zero
                                    pendingCourseDragOffset.value =
                                        androidx.compose.ui.geometry.Offset.Zero
                                    courseDragArmed.value = false
                                    courseDragging.value = false
                                },
                            )
                        }

                TimeTableCourseCard(
                    course = course,
                    courseColors = course.customColor?.let {
                        val selected = Color(it.toULong())
                        selected.copy(alpha = 0.72f) to selected.copy(alpha = 0.92f)
                    } ?: courseColorMap[course.title] ?: courseCardColorPair(course.title),
                    modifier = Modifier
                        .offset { IntOffset(x.roundToPx(), y.roundToPx()) }
                        .then(moveModifier)
                        .width(columnWidth)
                        .height(cardHeight)
                        .padding(horizontal = 2.dp, vertical = 3.dp),
                    onClick = {
                        val overlaps = courseOverlapMap[course.id].orEmpty()
                        when (overlaps.size) {
                            0, 1 -> onCourseClick(course)
                            else -> onOverlapCoursesClick(overlaps)
                        }
                    },
                )
            }

            emptySelection.value?.let { selection ->
                val x = columnWidth * (selection.dayOfWeek - 1)
                val y = cellHeight * (selection.startSection - 1)
                val selectionHeight = cellHeight * selection.sectionSpan
                val cellHeightPx = with(androidx.compose.ui.platform.LocalDensity.current) {
                    cellHeight.toPx()
                }
                val columnWidthPx = with(androidx.compose.ui.platform.LocalDensity.current) {
                    columnWidth.toPx()
                }
                val dragDistance = remember(selection.dayOfWeek, selection.startSection) {
                    mutableFloatStateOf((selection.sectionSpan - 1) * cellHeightPx)
                }
                val dragState = rememberDraggableState { delta ->
                    dragDistance.floatValue =
                        (dragDistance.floatValue + delta).coerceAtLeast(0f)
                    val desiredEnd = (
                        selection.startSection +
                            (dragDistance.floatValue / cellHeightPx).roundToInt()
                        ).coerceIn(selection.startSection, TIMETABLE_TOTAL_SECTIONS)
                    val allowedEnd = (selection.startSection..desiredEnd)
                        .takeWhile { section ->
                            cellCoursesMap[selection.dayOfWeek to section].isNullOrEmpty()
                        }
                        .lastOrNull()
                        ?: selection.startSection
                    emptySelection.value = selection.copy(endSection = allowedEnd)
                }

                Box(
                    modifier = Modifier
                        .offset { IntOffset(x.roundToPx(), y.roundToPx()) }
                        .width(columnWidth)
                        .height(selectionHeight)
                        .padding(horizontal = 2.dp, vertical = 2.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(7.dp))
                            .background(colors.titleTopColor.copy(alpha = 0.14f))
                            .border(
                                width = 1.5.dp,
                                color = colors.titleTopColor.copy(alpha = 0.72f),
                                shape = RoundedCornerShape(7.dp),
                            )
                            .pointerInput(columnWidthPx, cellHeightPx) {
                                var origin: EmptySlotSelection? = null
                                var totalX = 0f
                                var totalY = 0f
                                detectDragGestures(
                                    onDragStart = {
                                        origin = emptySelection.value
                                        totalX = 0f
                                        totalY = 0f
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        totalX += dragAmount.x
                                        totalY += dragAmount.y
                                        val base = origin ?: return@detectDragGestures
                                        val targetDay = (
                                            base.dayOfWeek + (totalX / columnWidthPx).roundToInt()
                                            ).coerceIn(1, TIMETABLE_TOTAL_DAYS)
                                        val targetStart = (
                                            base.startSection + (totalY / cellHeightPx).roundToInt()
                                            ).coerceIn(
                                                1,
                                                TIMETABLE_TOTAL_SECTIONS - base.sectionSpan + 1,
                                            )
                                        val targetEnd = targetStart + base.sectionSpan - 1
                                        val targetIsEmpty = (targetStart..targetEnd).all { section ->
                                            cellCoursesMap[targetDay to section].isNullOrEmpty()
                                        }
                                        if (targetIsEmpty) {
                                            emptySelection.value = base.copy(
                                                dayOfWeek = targetDay,
                                                startSection = targetStart,
                                                endSection = targetEnd,
                                            )
                                        }
                                    },
                                )
                            },
                    ) {
                        Text(
                            text = if (selection.sectionSpan == 1) {
                                "第${selection.startSection}节"
                            } else {
                                "${selection.startSection}-${selection.endSection}节"
                            },
                            modifier = Modifier.align(Alignment.Center),
                            color = colors.titleTopColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .offset(y = 13.dp)
                            .size(28.dp)
                            .shadow(4.dp, CircleShape)
                            .clip(CircleShape)
                            .background(colors.titleTopColor)
                            .draggable(
                                state = dragState,
                                orientation = Orientation.Vertical,
                            )
                            .clickable {
                                emptySelection.value = null
                                onEmptySlotClick(
                                    selection.dayOfWeek,
                                    selection.startSection,
                                    selection.sectionSpan,
                                )
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "确认添加课程",
                            tint = colors.bgPrimaryColor,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeTableCourseCard(
    course: TimeTableCourseUi,
    courseColors: Pair<Color, Color>,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val (topColor, bottomColor) = courseColors
    val cardBrush = remember(topColor, bottomColor) {
        Brush.verticalGradient(
            listOf(
                topColor.copy(alpha = 0.86f),
                bottomColor.copy(alpha = 0.90f),
            ),
        )
    }
    val shape = RoundedCornerShape(6.dp)

    Surface(
        modifier = modifier
            .clip(shape)
            .border(1.dp, Color.White.copy(alpha = 0.72f), shape)
            .clickable(onClick = onClick),
        color = Color.Transparent,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(cardBrush)
                .padding(horizontal = 4.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = course.title,
                color = kCourseCardTextColor,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                maxLines = 3,
                lineHeight = 12.sp,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            if (course.room.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = course.room,
                    color = kCourseCardTextColor.copy(alpha = 0.9f),
                    fontSize = 10.sp,
                    lineHeight = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (course.teacher.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = course.teacher,
                    color = kCourseCardTextColor.copy(alpha = 0.72f),
                    fontSize = 9.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}


@Composable
private fun RemarkDialog(
    remark: List<String>,
    onDismiss: () -> Unit,
) {
    val colors = AppTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "我知道了", color = colors.commonColor)
            }
        },
        title = {
            Text(text = "备注", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                remark.forEachIndexed { index, item ->
                    Text(
                        text = "${index + 1}. $item",
                        color = colors.primaryTextColor,
                        fontSize = 14.sp,
                    )
                }
            }
        },
        containerColor = colors.bgCardColor,
        titleContentColor = colors.primaryTextColor,
        textContentColor = colors.secondaryTextColor,
    )
}

@Preview(showBackground = true)
@Composable
private fun TimeTableScreenPreview() {
    AppSkinTheme {
        TimeTableComposeScreen(
            termText = "2025-2026-2",
            weekText = "第5周",
            displayWeek = 5,
            currentWeek = 5,
            termStarted = true,
            isRefreshing = false,
            courses = listOf(
                TimeTableCourseUi(1, "高等数学", "张老师", "明理楼A201", 1, 1, 2, setOf(1, 2, 3, 4, 5), false),
                TimeTableCourseUi(2, "数据结构", "李老师", "综合楼C305", 3, 5, 2, setOf(5, 6, 7), true),
                TimeTableCourseUi(3, "英语", "王老师", "外语楼202", 5, 9, 2, setOf(5), false),
            ),
            dateHeaderProvider = {
                "3月" to listOf(
                    TimeTableDayHeaderUi("周一", "03", "2026/3/3", false),
                    TimeTableDayHeaderUi("周二", "04", "2026/3/4", false),
                    TimeTableDayHeaderUi("周三", "05", "2026/3/5", true),
                    TimeTableDayHeaderUi("周四", "06", "2026/3/6", false),
                    TimeTableDayHeaderUi("周五", "07", "2026/3/7", false),
                    TimeTableDayHeaderUi("周六", "08", "2026/3/8", false),
                    TimeTableDayHeaderUi("周日", "09", "2026/3/9", false),
                )
            },
            onRefreshClick = {},
            onTermClick = {},
            onWeekClick = {},
            onWeekChange = {},
            onEmptySlotClick = { _, _, _, _ -> },
            onCourseClick = {},
            onOverlapCoursesClick = {},
            onCourseLongClick = {},
            onCourseMove = { _, _, _ -> },
        )
    }
}
