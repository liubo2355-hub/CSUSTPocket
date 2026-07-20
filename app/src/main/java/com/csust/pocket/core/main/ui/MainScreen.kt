package com.csust.pocket.core.main.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.csust.pocket.core.Route
import com.csust.pocket.common.data.local.mmkv.StudentInfoManager
import com.csust.pocket.core.designsystem.HyperButton
import com.csust.pocket.core.designsystem.HyperSurface
import com.csust.pocket.core.designsystem.hyperConcave
import com.csust.pocket.core.designsystem.hyperTap
import com.csust.pocket.core.main.navigation.MainTabDestination
import com.csust.pocket.core.main.navigation.MainTabNavigator
import com.csust.pocket.core.theme.AppTheme
import com.csust.pocket.feature.common.compose_ui.FunctionDestination
import com.csust.pocket.feature.common.compose_ui.openFunctionShortcut
import com.csust.pocket.feature.common.compose_ui.primaryFunctionShortcuts
import com.csust.pocket.feature.common.compose_ui.portalServiceGroups
import com.csust.pocket.feature.common.compose_ui.functionIcon
import com.csust.pocket.feature.common.data.repository.ElectricityRepository
import com.csust.pocket.feature.common.ui.FeatureScreen
import com.csust.pocket.overview.ui.compose.OverviewScreen
import com.csust.pocket.overview.viewmodel.OverviewViewModel
import com.csust.pocket.profileSettings.ui.compose.ProfileSettingsRoute
import com.csust.pocket.feature.mooc.ui.MoocScreen
import com.csust.pocket.feature.mooc.viewmodel.MoocViewModel
import kotlinx.coroutines.launch
import com.csust.pocket.widget.view.rememberFloatingTabBarScrollConnection
import dev.chrisbanes.haze.rememberHazeState

@Composable
fun MainScreen(
    navigator: MainTabNavigator,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isCompact = LocalConfiguration.current.screenWidthDp < 600
    val bottomBarScrollConnection = rememberFloatingTabBarScrollConnection()
    val bottomBarHazeState = rememberHazeState()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val preferences = remember { context.getSharedPreferences("pocket_csust_ui", Context.MODE_PRIVATE) }
    var showAgreement by remember { mutableStateOf(!preferences.getBoolean("agreement_accepted", false)) }
    var showOnboarding by remember {
        mutableStateOf(
            preferences.getBoolean("agreement_accepted", false) &&
                !preferences.getBoolean("onboarding_completed", false)
        )
    }
    var studentBindingSummary by remember { mutableStateOf<String?>(null) }
    var dormBindingSummary by remember { mutableStateOf<String?>(null) }
    val lifecycleOwner = LocalLifecycleOwner.current
    fun refreshOnboardingBindings() {
        studentBindingSummary = StudentInfoManager.studentId
            .takeIf { it.isNotBlank() && StudentInfoManager.studentPassword.isNotBlank() }
        dormBindingSummary = ElectricityRepository().getBinding()?.let {
            "${it.school} · ${it.dorm} ${it.room}"
        }
    }
    DisposableEffect(lifecycleOwner) {
        refreshOnboardingBindings()
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshOnboardingBindings()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    BackHandler(enabled = navigator.currentDestination != MainTabDestination.Overview) {
        navigator.select(MainTabDestination.Overview)
    }
    LaunchedEffect(Unit) { drawerState.close() }
    fun closeDrawer() = scope.launch { drawerState.close() }
    fun openServiceDestination(destination: FunctionDestination) {
        when (destination) {
            FunctionDestination.MoocCourses -> Route.goMoocCourses(context)
            FunctionDestination.MoocHomework -> navigator.select(MainTabDestination.Mooc)
            else -> openFunctionShortcut(context, destination)
        }
    }
    val drawerGroups = portalServiceGroups().map { group ->
        group.title to group.items.map { item ->
            PortalDrawerAction(item.title, functionIcon(item.destination)) {
                closeDrawer()
                openServiceDestination(item.destination)
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            PortalDrawer(
                onClose = { closeDrawer() },
                onOverview = { closeDrawer(); navigator.select(MainTabDestination.Overview) },
                onProfile = { closeDrawer(); navigator.select(MainTabDestination.Profile) },
                groups = drawerGroups
            )
        }
    ) {
        Surface(modifier = modifier.fillMaxSize(), color = AppTheme.colors.bgPrimaryColor) {
          Column(Modifier.fillMaxSize()) {
            if (!isCompact) {
                PortalTopBar(
                    selected = navigator.currentDestination,
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onSelect = navigator::select,
                    onMoocClick = { navigator.select(MainTabDestination.Mooc) }
                )
            }
            NavDisplay(
                backStack = navigator.displayBackStack,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .then(if (isCompact) Modifier.statusBarsPadding() else Modifier),
                onBack = {
                    if (navigator.currentDestination != MainTabDestination.Overview) {
                        navigator.select(MainTabDestination.Overview)
                    } else {
                        context.findActivity()?.finish()
                    }
                },
                transitionSpec = {
                    EnterTransition.None togetherWith ExitTransition.None
                },
                popTransitionSpec = {
                    EnterTransition.None togetherWith ExitTransition.None
                },
                entryProvider = entryProvider {
                    entry<MainTabDestination.Overview> {
                        OverviewTabRoute(navigator)
                    }
                    entry<MainTabDestination.Feature> {
                        FeatureScreen(
                            onDestinationSelected = ::openServiceDestination
                        )
                    }
                    entry<MainTabDestination.Profile> {
                        ProfileSettingsRoute()
                    }
                    entry<MainTabDestination.Mooc> {
                        val moocViewModel: MoocViewModel = viewModel()
                        MoocScreen(
                            moocViewModel = moocViewModel,
                            embedded = true,
                            onOpenCoursePage = { Route.goMoocCoursePage(context, it) }
                        )
                    }
                }
            )
            if (isCompact) {
                MainBottomBar(
                    selectedDestination = navigator.currentDestination,
                    onDestinationSelected = navigator::select,
                    scrollConnection = bottomBarScrollConnection,
                    hazeState = bottomBarHazeState
                )
            }
          }
        }
    }
    if (showAgreement) {
        FirstLaunchAgreement(
            onDecline = { context.findActivity()?.finish() },
            onAccept = {
                preferences.edit().putBoolean("agreement_accepted", true).apply()
                showAgreement = false
                showOnboarding = true
            }
        )
    }
    if (showOnboarding) {
        FirstLaunchOnboarding(
            initialPage = preferences.getInt("onboarding_page", 0),
            studentBindingSummary = studentBindingSummary,
            dormBindingSummary = dormBindingSummary,
            onPageChanged = { preferences.edit().putInt("onboarding_page", it).apply() },
            onOpenLogin = { Route.goBindingUser(context, returnToCaller = true) },
            onOpenDorm = { Route.goElectronic(context) },
            onComplete = {
                preferences.edit()
                    .putBoolean("onboarding_completed", true)
                    .remove("onboarding_page")
                    .apply()
                showOnboarding = false
            }
        )
    }
}

@Composable
private fun FirstLaunchAgreement(onDecline: () -> Unit, onAccept: () -> Unit) {
    Dialog(onDismissRequest = {}) {
        HyperSurface(
            modifier = Modifier.fillMaxWidth().heightIn(max = 570.dp),
            shape = RoundedCornerShape(28.dp),
            color = AppTheme.colors.bgPrimaryColor,
            shadowElevation = 12.dp
        ) {
            Column(Modifier.padding(20.dp)) {
                Text("用户协议", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = AppTheme.colors.primaryTextColor)
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.TopStart
                ) {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .hyperConcave(18.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("“掌上长理”用户协议", fontSize = 21.sp, fontWeight = FontWeight.Bold)
                        Text("1. 协议确认", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("在使用“掌上长理”前，请仔细阅读本用户协议。当您点击“同意”按钮时，即表示您已阅读、理解并同意接受本协议的全部内容。若您不同意本协议的任何条款，请立即退出并停止使用本应用。", fontSize = 14.sp, lineHeight = 21.sp)
                        Text("2. 免责声明", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("本应用为个人开发项目，非任何官方应用。使用过程中遇到的问题请直接联系开发者。开发者不对因使用本应用导致的间接损失承担责任，包括校园系统变更、网络异常与不可抗力造成的服务中断。", fontSize = 14.sp, lineHeight = 21.sp)
                        Text("3. 数据与隐私", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("课表、成绩与账号信息仅用于提供校园查询服务。敏感凭据采用本地保存，不会作为公开内容上传。", fontSize = 14.sp, lineHeight = 21.sp)
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    HyperButton("拒绝", onDecline, modifier = Modifier.weight(1f), primary = false)
                    HyperButton("同意", onAccept, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun FirstLaunchOnboarding(
    initialPage: Int,
    studentBindingSummary: String?,
    dormBindingSummary: String?,
    onPageChanged: (Int) -> Unit,
    onOpenLogin: () -> Unit,
    onOpenDorm: () -> Unit,
    onComplete: () -> Unit
) {
    val titles = listOf("掌上长理", "账号登录", "宿舍配置", "通知设置", "桌面小组件", "偏好设置", "设置完成")
    val headings = listOf(
        "欢迎使用掌上长理",
        "统一身份认证登录",
        "添加常用宿舍",
        "配置校园提醒",
        "把校园信息放到桌面",
        "选择适合你的使用方式",
        "一切准备就绪"
    )
    val descriptions = listOf(
        "课表、成绩、考试、作业与宿舍电量集中在一个简洁的校园入口。",
        "登录后可直接使用课表、成绩查询、考试安排和网络课程中心。",
        "绑定宿舍后可查询实时电量，并根据真实历史用电估算剩余天数。",
        "开启通知后，可以及时收到待提交作业、考试和低电量提醒。",
        "添加课表、成绩、作业或电量小组件，无需打开 App 也能快速查看。",
        "外观、后台刷新、通知与小组件都可以稍后在“我的”页面调整。",
        "现在可以开始使用掌上长理了。"
    )
    val pagerState = rememberPagerState(
        initialPage = initialPage.coerceIn(0, 6),
        pageCount = { 7 }
    )
    val scope = rememberCoroutineScope()
    val page = pagerState.currentPage
    LaunchedEffect(page) { onPageChanged(page) }

    Dialog(onDismissRequest = {}) {
        HyperSurface(
            modifier = Modifier.fillMaxWidth().heightIn(min = 520.dp, max = 620.dp),
            shape = RoundedCornerShape(28.dp),
            color = AppTheme.colors.bgPrimaryColor,
            shadowElevation = 12.dp
        ) {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onComplete) { Text("跳过") }
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(titles[page], fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("步骤 ${page + 1} / 7", color = AppTheme.colors.secondaryTextColor, fontSize = 10.sp)
                    }
                    TextButton(onClick = {
                        if (page == 6) onComplete()
                        else scope.launch { pagerState.animateScrollToPage(page + 1) }
                    }) {
                        Text(if (page == 6) "开始使用" else "下一步")
                    }
                }
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) { pageIndex ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Box(
                            Modifier.size(72.dp).clip(CircleShape).background(if (pageIndex == 6) Color(0xFF32C96B) else Color(0xFFE8F4FC)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(if (pageIndex == 6) "✓" else (pageIndex + 1).toString(), color = if (pageIndex == 6) Color.White else Color(0xFF1697D5), fontSize = 34.sp, fontWeight = FontWeight.Bold)
                        }
                        Text(headings[pageIndex], fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text(descriptions[pageIndex], color = AppTheme.colors.secondaryTextColor, fontSize = 14.sp, lineHeight = 21.sp, modifier = Modifier.padding(horizontal = 18.dp))
                        if (pageIndex == 1 || pageIndex == 2) {
                            val bindingSummary = if (pageIndex == 1) studentBindingSummary else dormBindingSummary
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .hyperConcave(16.dp)
                                    .hyperTap { if (pageIndex == 1) onOpenLogin() else onOpenDorm() }
                            ) {
                                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(if (pageIndex == 1) "统一身份认证" else "宿舍电量", fontWeight = FontWeight.Bold)
                                        if (bindingSummary != null) {
                                            Text(bindingSummary, color = AppTheme.colors.secondaryTextColor, fontSize = 12.sp)
                                        }
                                    }
                                    Text(
                                        if (bindingSummary != null) "已完成 ✓" else if (pageIndex == 1) "登录 ›" else "配置 ›",
                                        color = if (bindingSummary != null) Color(0xFF32C96B) else Color(0xFF1697D5)
                                    )
                                }
                            }
                        }
                    }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    repeat(7) { index ->
                        Box(
                            Modifier
                                .padding(horizontal = 3.dp)
                                .size(width = if (index == page) 24.dp else 8.dp, height = 8.dp)
                                .clip(CircleShape)
                                .background(if (index == page) Color(0xFF1697D5) else Color(0xFFD9DCE3))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OverviewTabRoute(navigator: MainTabNavigator) {
    val context = LocalContext.current
    val viewModel: OverviewViewModel = viewModel()

    OverviewScreen(
        viewModel = viewModel,
        onBindClick = { Route.goBindingUser(context) },
        onQuickActionClick = { actionId ->
            val shortcut = primaryFunctionShortcuts().firstOrNull { it.id == actionId }
            val serviceShortcut = actionId
                .takeIf { it.startsWith("service:") }
                ?.removePrefix("service:")
                ?.let { serviceId ->
                    portalServiceGroups().flatMap { it.items }.firstOrNull { it.id == serviceId }
                }
            when {
                actionId == "homework" -> navigator.select(MainTabDestination.Mooc)
                serviceShortcut != null -> openFunctionShortcut(context, serviceShortcut.destination)
                shortcut != null -> openFunctionShortcut(context, shortcut.destination)
                actionId == FunctionDestination.ScoreInquiry.name -> Route.goScoreInquiry(context)
                actionId == FunctionDestination.Electronic.name -> Route.goElectronic(context)
            }
        }
    )
}

private tailrec fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}
