package com.creamaker.changli_planet_app.feature.mooc.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.creamaker.changli_planet_app.R
import com.creamaker.changli_planet_app.core.designsystem.HyperSpacing
import com.creamaker.changli_planet_app.core.designsystem.HyperSurface
import com.creamaker.changli_planet_app.core.designsystem.PortalBackButton
import com.creamaker.changli_planet_app.core.designsystem.hyperTap
import com.creamaker.changli_planet_app.core.network.ApiResponse
import com.creamaker.changli_planet_app.core.theme.AppSkinTheme
import com.creamaker.changli_planet_app.core.theme.AppTheme
import com.creamaker.changli_planet_app.feature.mooc.data.model.CourseItem
import com.creamaker.changli_planet_app.feature.mooc.data.model.HomeworkItem
import com.creamaker.changli_planet_app.feature.mooc.data.model.MoocUiState
import com.creamaker.changli_planet_app.feature.mooc.viewmodel.MoocViewModel
import com.dcelysia.csust_spider.mooc.data.remote.dto.MoocHomework
import com.dcelysia.csust_spider.mooc.data.remote.dto.MoocTest
import com.dcelysia.csust_spider.mooc.data.remote.dto.PendingAssignmentCourse

private val HomeworkAccent = Color(0xFFFF8A50)
private val TestAccent = Color(0xFF4F7FED)

enum class MoocPageMode {
    Courses,
    Homework
}

@Composable
fun MoocScreen(
    moocViewModel: MoocViewModel,
    pageMode: MoocPageMode = MoocPageMode.Homework,
    onBack: () -> Unit = {},
    onOpenCoursePage: (String) -> Unit = {},
    embedded: Boolean = false
) {
    val uiState by moocViewModel.uiState.collectAsStateWithLifecycle()

    MoocScreenContent(
        uiState = uiState,
        onToggleExpand = moocViewModel::handleCourseClick,
        onDismissDialog = moocViewModel::dismissDialog,
        onRequestForceRefresh = moocViewModel::requestForceRefresh,
        onDismissForceRefreshPrompt = moocViewModel::dismissForceRefreshPrompt,
        onConfirmForceRefresh = moocViewModel::confirmForceRefresh,
        onRefreshCourse = moocViewModel::refreshCourseHomeworks,
        onOpenCoursePage = onOpenCoursePage,
        onBack = onBack,
        embedded = embedded,
        pageMode = pageMode
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MoocScreenContent(
    uiState: MoocUiState,
    onToggleExpand: (String) -> Unit,
    onDismissDialog: () -> Unit,
    onRequestForceRefresh: () -> Unit,
    onDismissForceRefreshPrompt: () -> Unit,
    onConfirmForceRefresh: () -> Unit,
    onRefreshCourse: (String) -> Unit,
    onOpenCoursePage: (String) -> Unit,
    onBack: () -> Unit,
    embedded: Boolean,
    pageMode: MoocPageMode
) {
    val colors = AppTheme.colors
    var selectedCourseId by rememberSaveable { mutableStateOf<String?>(null) }
    val allCourses = (uiState.courses as? ApiResponse.Success)?.data.orEmpty()
    val selectedCourse = allCourses.firstOrNull { it.course.id == selectedCourseId }
    LaunchedEffect(selectedCourseId) {
        selectedCourseId?.let(onRefreshCourse)
    }
    BackHandler(
        enabled = !embedded && pageMode == MoocPageMode.Courses && selectedCourseId != null
    ) {
        selectedCourseId = null
    }
    if (!uiState.dialogMessage.isNullOrBlank()) {
        AlertDialog(
            onDismissRequest = onDismissDialog,
            confirmButton = {
                TextButton(onClick = onDismissDialog) {
                    Text(text = "我知道了")
                }
            },
            title = {
                Text(
                    text = "加载失败",
                    color = colors.primaryTextColor,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = uiState.dialogMessage,
                    color = colors.secondaryTextColor
                )
            },
            containerColor = colors.bgCardColor
        )
    }
    if (uiState.showForceRefreshPrompt) {
        AlertDialog(
            onDismissRequest = onDismissForceRefreshPrompt,
            confirmButton = {
                TextButton(onClick = onConfirmForceRefresh) {
                    Text(text = "开始刷新")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissForceRefreshPrompt) {
                    Text(text = "取消")
                }
            },
            title = {
                Text(
                    text = "强制刷新提醒",
                    color = colors.primaryTextColor,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "将进行网络强制刷新，再重新拉取作业和测试数据。不适合太频繁使用。",
                    color = colors.secondaryTextColor
                )
            },
            containerColor = colors.bgCardColor
        )
    }
    if (embedded) {
        IosAssignmentsEmbedded(
            uiState = uiState,
            onToggleExpand = onToggleExpand,
            onOpenCoursePage = onOpenCoursePage,
            onRefresh = onRequestForceRefresh
        )
        return
    }
    Scaffold(
        topBar = {
            if (!embedded) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.overviewPageBackgroundColor)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                TopAppBar(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .shadow(3.dp, RoundedCornerShape(18.dp))
                        .clip(RoundedCornerShape(18.dp)),
                    title = {
                        Text(
                            text = when {
                                selectedCourse != null -> "课程详情"
                                pageMode == MoocPageMode.Courses -> "所有课程"
                                else -> stringResource(R.string.pending_homework)
                            },
                            color = colors.primaryTextColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        PortalBackButton(onClick = {
                            if (selectedCourseId != null) selectedCourseId = null else onBack()
                        })
                    },
                    actions = {
                        TextButton(
                            onClick = {
                                selectedCourse?.let { onRefreshCourse(it.course.id) }
                                    ?: onRequestForceRefresh()
                            },
                            enabled = selectedCourse?.course?.id !in uiState.loadingCourseIds && !uiState.isRefreshing
                        ) {
                            Text(
                                text = if (
                                    uiState.isRefreshing || selectedCourse?.course?.id in uiState.loadingCourseIds
                                ) "刷新中" else "刷新",
                                color = colors.primaryTextColor,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    },
                    windowInsets = WindowInsets(0, 0, 0, 0),
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = colors.bgCardColor,
                        titleContentColor = colors.primaryTextColor,
                        navigationIconContentColor = colors.primaryTextColor
                    )
                )
            }
            }
        },
        containerColor = colors.overviewPageBackgroundColor
    ) { paddingValues ->
        if (pageMode == MoocPageMode.Courses) {
            if (selectedCourse != null) {
                CourseDetailContent(
                    courseItem = selectedCourse,
                    contentPadding = paddingValues,
                    loading = selectedCourse.course.id in uiState.loadingCourseIds,
                    onOpenCoursePage = { onOpenCoursePage(selectedCourse.course.id) },
                    onRefresh = { onRefreshCourse(selectedCourse.course.id) }
                )
            } else {
                AllCoursesContent(
                    courses = uiState.courses,
                    contentPadding = paddingValues,
                    onCourseClick = { selectedCourseId = it }
                )
            }
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.overviewPageBackgroundColor)
                .padding(paddingValues),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SummaryCard(courses = uiState.courses, pageMode = pageMode)
            }

            if (uiState.isRefreshing) {
                item {
                    MessageCard(
                        title = "正在刷新",
                        message = "正在拉取最新作业和测试数据，请稍候~",
                        accent = colors.loadingColor
                    )
                }
            }

            when (val courses = uiState.courses) {
                is ApiResponse.Loading -> {
                    item { LoadingCard() }
                }

                is ApiResponse.Error -> {
                    item {
                        MessageCard(
                            title = "加载失败",
                            message = courses.msg.ifBlank { "当前无法获取慕课数据，请稍后再试" },
                            accent = colors.errorRedColor
                        )
                    }
                }

                is ApiResponse.Success -> {
                    val courseList = if (pageMode == MoocPageMode.Courses) {
                        courses.data
                    } else {
                        courses.data.filter(CourseItem::hasPendingWork)
                    }
                    if (courseList.isEmpty()) {
                        item {
                            MessageCard(
                                title = if (pageMode == MoocPageMode.Courses) "暂无课程" else "当前没有待处理事项",
                                message = if (pageMode == MoocPageMode.Courses) "暂时没有获取到网络课程" else stringResource(R.string.no_pending_assignments),
                                accent = colors.successGreenColor
                            )
                        }
                    } else {
                        items(courseList, key = { it.course.id }) { courseItem ->
                            CourseCard(
                                courseItem = courseItem,
                                onToggleExpand = onToggleExpand,
                                onOpenCoursePage = onOpenCoursePage
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AllCoursesContent(
    courses: ApiResponse<List<CourseItem>>,
    contentPadding: PaddingValues,
    onCourseClick: (String) -> Unit
) {
    val colors = AppTheme.colors
    var query by rememberSaveable { mutableStateOf("") }
    val courseList = (courses as? ApiResponse.Success)?.data.orEmpty()
    val filteredCourses = remember(courseList, query) {
        val keyword = query.trim()
        if (keyword.isEmpty()) courseList else courseList.filter { item ->
            item.course.name.contains(keyword, ignoreCase = true) ||
                item.teacher.orEmpty().contains(keyword, ignoreCase = true) ||
                item.department.orEmpty().contains(keyword, ignoreCase = true) ||
                item.courseNumber.orEmpty().contains(keyword, ignoreCase = true)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.overviewPageBackgroundColor)
            .padding(contentPadding),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            HyperSurface(
                shape = RoundedCornerShape(18.dp),
                color = colors.bgSecondaryColor,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.Search, "搜索课程", tint = colors.secondaryTextColor, modifier = Modifier.size(21.dp))
                    Spacer(Modifier.size(10.dp))
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = colors.primaryTextColor,
                            fontSize = 16.sp
                        ),
                        decorationBox = { innerTextField ->
                            if (query.isBlank()) {
                                Text("搜索课程、教师或院系", color = colors.secondaryTextColor, fontSize = 16.sp)
                            }
                            innerTextField()
                        }
                    )
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }, modifier = Modifier.size(44.dp)) {
                            Icon(Icons.Outlined.Close, "清除搜索", tint = colors.secondaryTextColor, modifier = Modifier.size(19.dp))
                        }
                    }
                }
            }
        }

        when (courses) {
            is ApiResponse.Loading -> item { LoadingCard() }
            is ApiResponse.Error -> item {
                MessageCard("加载失败", courses.msg.ifBlank { "暂时无法获取课程" }, colors.errorRedColor)
            }
            is ApiResponse.Success -> {
                if (filteredCourses.isEmpty()) {
                    item {
                        MessageCard(
                            if (query.isBlank()) "暂无课程" else "没有搜索结果",
                            if (query.isBlank()) "暂时没有获取到网络课程" else "换个课程名、教师或院系试试",
                            colors.secondaryTextColor
                        )
                    }
                } else {
                    items(filteredCourses, key = { it.course.id }) { course ->
                        AllCourseListItem(course = course, onClick = { onCourseClick(course.course.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun AllCourseListItem(course: CourseItem, onClick: () -> Unit) {
    val colors = AppTheme.colors
    HyperSurface(
        shape = RoundedCornerShape(22.dp),
        color = colors.bgCardColor,
        shadowElevation = 3.dp,
        modifier = Modifier
            .fillMaxWidth()
            .hyperTap(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    course.course.name,
                    color = colors.primaryTextColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Person, null, tint = Color(0xFFE45AC8), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(4.dp))
                    Text(
                        course.teacher.orEmpty().ifBlank { "教师信息暂无" },
                        color = colors.secondaryTextColor,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.size(12.dp))
                    Icon(Icons.Outlined.AccountBalance, null, tint = Color(0xFF28B866), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(4.dp))
                    Text(
                        course.department.orEmpty().ifBlank { "院系信息暂无" },
                        color = colors.secondaryTextColor,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(Modifier.size(10.dp))
            Icon(Icons.Outlined.ChevronRight, "查看课程详情", tint = colors.iconSecondaryColor, modifier = Modifier.size(25.dp))
        }
    }
}

@Composable
private fun CourseDetailContent(
    courseItem: CourseItem,
    contentPadding: PaddingValues,
    loading: Boolean,
    onOpenCoursePage: () -> Unit,
    onRefresh: () -> Unit
) {
    val colors = AppTheme.colors
    val submitCount = courseItem.homeworks.count { it.homework.canSubmit && !it.homework.submitStatus }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.overviewPageBackgroundColor)
            .padding(contentPadding),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            HyperSurface(
                shape = RoundedCornerShape(24.dp),
                color = colors.bgCardColor,
                shadowElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(HyperSpacing.cardPadding),
                    verticalArrangement = Arrangement.spacedBy(13.dp)
                ) {
                    CourseInfoRow("课程名称", courseItem.course.name)
                    CourseInfoRow("课程编号", courseItem.courseNumber.orEmpty().ifBlank { courseItem.course.id })
                    CourseInfoRow("开课院系", courseItem.department.orEmpty().ifBlank { "暂无" })
                    CourseInfoRow("授课教师", courseItem.teacher.orEmpty().ifBlank { "暂无" })
                    HyperSurface(
                        shape = RoundedCornerShape(18.dp),
                        color = colors.bgSecondaryColor,
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .align(Alignment.End)
                            .hyperTap(onClick = onOpenCoursePage)
                    ) {
                        Text(
                            "前往课程网页",
                            color = Color(0xFF159BD3),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                        )
                    }
                }
            }
        }
        item {
            HyperSurface(
                shape = RoundedCornerShape(24.dp),
                color = colors.bgCardColor,
                shadowElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(HyperSpacing.cardPadding)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("作业列表", color = colors.primaryTextColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        IconButton(onClick = onRefresh, enabled = !loading, modifier = Modifier.size(44.dp)) {
                            if (loading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                            else Icon(painterResource(R.drawable.coursetable_ic_refresh), "刷新作业", tint = Color(0xFF159BD3), modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.weight(1f))
                        Column(horizontalAlignment = Alignment.End) {
                            Text("${courseItem.homeworks.size} 个作业", color = colors.secondaryTextColor, fontSize = 12.sp)
                            Text("$submitCount 个可提交", color = colors.secondaryTextColor, fontSize = 12.sp)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(color = colors.dividerColor.copy(alpha = 0.35f))
                    Spacer(Modifier.height(12.dp))
                    if (courseItem.homeworks.isEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 34.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("▣", color = colors.secondaryTextColor, fontSize = 40.sp)
                            Spacer(Modifier.height(8.dp))
                            Text("暂无作业", color = colors.primaryTextColor, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            courseItem.homeworks.forEach { homework ->
                                HomeworkItemRow(homeworkItem = homework, onClick = onOpenCoursePage)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CourseInfoRow(label: String, value: String) {
    val colors = AppTheme.colors
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Text(label, color = colors.secondaryTextColor, fontSize = 14.sp, modifier = Modifier.weight(0.36f))
        Text(
            value,
            color = colors.primaryTextColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(0.64f),
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
    }
}

@Composable
private fun IosAssignmentsEmbedded(
    uiState: MoocUiState,
    onToggleExpand: (String) -> Unit,
    onOpenCoursePage: (String) -> Unit,
    onRefresh: () -> Unit
) {
    val colors = AppTheme.colors
    val courseList = (uiState.courses as? ApiResponse.Success)?.data.orEmpty().filter(CourseItem::hasPendingWork)
    Box(Modifier.fillMaxSize().background(colors.overviewPageBackgroundColor)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("待提交作业", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = colors.primaryTextColor)
                        Text("共 ${courseList.sumOf { it.homeworks.count { homework -> !homework.homework.submitStatus } }} 个可提交作业", fontSize = 12.sp, color = colors.secondaryTextColor)
                    }
                    IconButton(onClick = onRefresh, enabled = !uiState.isRefreshing) {
                        if (uiState.isRefreshing) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        else Icon(painterResource(R.drawable.coursetable_ic_refresh), "刷新", tint = Color(0xFF1596CF), modifier = Modifier.size(22.dp))
                    }
                }
            }
            if (courseList.isNotEmpty()) {
                items(courseList, key = { it.course.id }) { course ->
                    CourseCard(courseItem = course, onToggleExpand = onToggleExpand, onOpenCoursePage = onOpenCoursePage)
                }
            }
        }
        if (courseList.isEmpty() && uiState.courses !is ApiResponse.Loading) {
            Column(Modifier.align(Alignment.Center).padding(bottom = 90.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("▣", fontSize = 38.sp, color = colors.secondaryTextColor)
                Spacer(Modifier.height(10.dp))
                Text("暂无待提交作业", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = colors.primaryTextColor)
                Text("当前没有需要提交的作业", fontSize = 13.sp, color = colors.secondaryTextColor)
            }
        }
    }
}

@Composable
private fun SummaryCard(
    courses: ApiResponse<List<CourseItem>>,
    pageMode: MoocPageMode = MoocPageMode.Homework
) {
    val colors = AppTheme.colors
    val count = (courses as? ApiResponse.Success)?.data?.size ?: 0
    Surface(
        shape = RoundedCornerShape(HyperSpacing.cardRadius),
        color = colors.bgCardColor,
        tonalElevation = 0.dp,
        shadowElevation = if (isSystemInDarkTheme()) 0.dp else 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = HyperSpacing.cardPaddingHorizontal,
                    vertical = HyperSpacing.cardPaddingVertical
                ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = if (pageMode == MoocPageMode.Courses) "课程总览" else "待办总览",
                color = colors.secondaryTextColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = if (pageMode == MoocPageMode.Courses) {
                    if (count > 0) "共 $count 门课程" else "正在获取课程"
                } else {
                    val pendingCount = (courses as? ApiResponse.Success)?.data.orEmpty().count(CourseItem::hasPendingWork)
                    if (pendingCount > 0) "当前有 $pendingCount 门课程需要处理" else "当前没有待处理事项"
                },
                color = colors.primaryTextColor,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

private fun CourseItem.hasPendingWork(): Boolean =
    homeworks.any { it.homework.canSubmit && !it.homework.submitStatus } ||
        tests.any { !it.isSubmitted }

@Composable
private fun LoadingCard() {
    val colors = AppTheme.colors
    Surface(
        shape = RoundedCornerShape(HyperSpacing.cardRadius),
        color = colors.bgCardColor
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 28.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = colors.loadingColor)
        }
    }
}

@Composable
private fun MessageCard(
    title: String,
    message: String,
    accent: Color
) {
    val colors = AppTheme.colors
    Surface(
        shape = RoundedCornerShape(HyperSpacing.cardRadius),
        color = colors.bgCardColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = HyperSpacing.cardPaddingHorizontal,
                    vertical = HyperSpacing.cardPaddingVertical
                ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                color = accent,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = message,
                color = colors.secondaryTextColor,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
private fun CourseCard(
    courseItem: CourseItem,
    onToggleExpand: (String) -> Unit,
    onOpenCoursePage: (String) -> Unit
) {
    val colors = AppTheme.colors
    val expanded = courseItem.isExpanded
    val rotationAngle by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "courseExpansionAngle"
    )
    val courseId = courseItem.course.id
    // 固定 lambda 引用，避免 LazyColumn item 在相邻重组时因 lambda 新实例导致下游重组
    val openThisCourse = remember(courseId, onOpenCoursePage) {
        { onOpenCoursePage(courseId) }
    }
    val toggleThisCourse = remember(courseId, onToggleExpand) {
        { onToggleExpand(courseId) }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(HyperSpacing.cardRadius),
        color = colors.bgCardColor,
        tonalElevation = 0.dp,
        shadowElevation = if (isSystemInDarkTheme()) 0.dp else 4.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = toggleThisCourse)
                    .padding(
                        horizontal = HyperSpacing.cardPaddingHorizontal,
                        vertical = HyperSpacing.cardPaddingVertical
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StatusPill(text = "课程待办", accent = HomeworkAccent, filled = false)
                    Text(
                        text = courseItem.course.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = colors.primaryTextColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 24.sp
                    )
                    Text(
                        text = "课程 ID: $courseId",
                        fontSize = 12.sp,
                        color = colors.secondaryTextColor,
                        fontWeight = FontWeight.Medium
                    )
                }

                TextButton(
                    onClick = openThisCourse,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "前往课程",
                        color = HomeworkAccent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Icon(
                    painter = painterResource(id = R.drawable.ic_expand),
                    contentDescription = if (expanded) "收起" else "展开",
                    tint = colors.iconSecondaryColor,
                    modifier = Modifier
                        .rotate(rotationAngle)
                        .size(22.dp)
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(animationSpec = tween(260)) + expandVertically(animationSpec = tween(260)),
                exit = fadeOut(animationSpec = tween(220)) + shrinkVertically(animationSpec = tween(220))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    HorizontalDivider(color = colors.dividerColor.copy(alpha = 0.3f))
                    TaskSection(
                        title = "待提交作业",
                        subtitle = "点击条目进入课程提交",
                        accent = HomeworkAccent
                    ) {
                        HomeworkContent(
                            homeworkItems = courseItem.homeworks,
                            onClickItem = openThisCourse
                        )
                    }
                    TaskSection(
                        title = "待测试",
                        subtitle = "点击条目进入课程测试",
                        accent = TestAccent
                    ) {
                        TestContent(
                            tests = courseItem.tests,
                            onClickItem = openThisCourse
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskSection(
    title: String,
    subtitle: String,
    accent: Color,
    content: @Composable () -> Unit
) {
    val colors = AppTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusPill(text = title, accent = accent, filled = true)
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = subtitle,
                color = colors.secondaryTextColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
        content()
    }
}

@Composable
private fun HomeworkContent(
    homeworkItems: List<HomeworkItem>,
    onClickItem: () -> Unit
) {
    val colors = AppTheme.colors
    if (homeworkItems.isEmpty()) {
        SectionMessage(text = "当前没有待提交作业", accent = colors.secondaryTextColor)
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            homeworkItems.forEach { item ->
                HomeworkItemRow(homeworkItem = item, onClick = onClickItem)
            }
        }
    }
}

@Composable
private fun TestContent(
    tests: List<MoocTest>,
    onClickItem: () -> Unit
) {
    val colors = AppTheme.colors
    val pendingTests = tests.filterNot { it.isSubmitted }
    if (pendingTests.isEmpty()) {
        SectionMessage(text = "当前没有待完成测试", accent = colors.secondaryTextColor)
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            pendingTests.forEach { test ->
                TestItem(test = test, onClick = onClickItem)
            }
        }
    }
}

@Composable
private fun SectionMessage(
    text: String,
    accent: Color
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = AppTheme.colors.bgSecondaryColor
    ) {
        Text(
            text = text,
            color = accent,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
        )
    }
}

@Composable
private fun HomeworkItemRow(
    homeworkItem: HomeworkItem,
    onClick: () -> Unit
) {
    val colors = AppTheme.colors
    val homework = homeworkItem.homework
    val titleColor = if (homeworkItem.isDueSoon) colorResource(R.color.color_base_red) else colors.primaryTextColor

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.bgSecondaryColor,
        tonalElevation = 0.dp,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = homework.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = titleColor,
                lineHeight = 21.sp
            )
            Text(
                text = "截止时间: ${homework.deadline}",
                fontSize = 12.sp,
                color = colors.secondaryTextColor,
                fontWeight = FontWeight.Medium
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusPill(
                    text = if (homework.submitStatus) "已提交" else "未提交",
                    accent = if (homework.submitStatus) colorResource(R.color.color_base_green) else colorResource(R.color.color_base_red)
                )
                StatusPill(
                    text = if (homework.canSubmit) "可提交" else "不可提交",
                    accent = if (homework.canSubmit) colorResource(R.color.color_base_green) else colorResource(R.color.color_base_red)
                )
            }
            Text(
                text = "发布人: ${homework.publisher}",
                fontSize = 12.sp,
                color = colors.secondaryTextColor
            )
        }
    }
}

@Composable
private fun TestItem(
    test: MoocTest,
    onClick: () -> Unit
) {
    val colors = AppTheme.colors
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.bgSecondaryColor,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = test.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = colors.primaryTextColor,
                lineHeight = 21.sp
            )
            Text(
                text = "测试时间: ${test.startTime} - ${test.endTime}",
                fontSize = 12.sp,
                color = colors.secondaryTextColor,
                lineHeight = 18.sp
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusPill(text = "时长 ${test.timeLimit} 分钟", accent = TestAccent)
                StatusPill(
                    text = test.allowRetake?.let { "可重考 $it 次" } ?: "不限重考",
                    accent = HomeworkAccent,
                    filled = false
                )
            }
        }
    }
}

@Composable
private fun StatusPill(
    text: String,
    accent: Color,
    filled: Boolean = true
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = if (filled) accent.copy(alpha = 0.14f) else Color.Transparent,
        border = if (filled) null else BorderStroke(1.dp, accent.copy(alpha = 0.35f))
    ) {
        Text(
            text = text,
            color = accent,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF3F3F6)
@Composable
private fun MoocScreenPreview() {
    AppSkinTheme {
        Column(
            modifier = Modifier
                .background(AppTheme.colors.overviewPageBackgroundColor)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SummaryCard(
                courses = ApiResponse.Success(
                    listOf(
                        CourseItem(course = PendingAssignmentCourse("10842", "数据库原理与技术")),
                        CourseItem(course = PendingAssignmentCourse("20491", "大数据平台开发"))
                    )
                )
            )
            MessageCard(
                title = "待测试 UI",
                message = "课程展开后会显示待提交作业和待测试双区块。",
                accent = TestAccent
            )
        }
    }
}
