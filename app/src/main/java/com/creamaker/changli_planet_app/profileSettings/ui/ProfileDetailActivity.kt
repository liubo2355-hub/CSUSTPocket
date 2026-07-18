package com.creamaker.changli_planet_app.profileSettings.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creamaker.changli_planet_app.R
import com.creamaker.changli_planet_app.common.data.local.mmkv.StudentInfoManager
import com.creamaker.changli_planet_app.common.data.local.mmkv.UserInfoManager
import com.creamaker.changli_planet_app.core.designsystem.HyperSpacing
import com.creamaker.changli_planet_app.core.designsystem.PortalBackButton
import com.creamaker.changli_planet_app.core.designsystem.HyperSurface
import com.creamaker.changli_planet_app.core.designsystem.hyperTap
import com.creamaker.changli_planet_app.core.theme.AppSkinTheme
import com.creamaker.changli_planet_app.core.theme.AppTheme
import com.dcelysia.csust_spider.core.Resource
import com.dcelysia.csust_spider.core.RetrofitUtils
import com.dcelysia.csust_spider.education.data.remote.services.AuthService
import com.dcelysia.csust_spider.mooc.data.remote.repository.MoocRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.jsoup.Jsoup

class ProfileDetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppSkinTheme {
                ProfileDetailScreen(onBack = { finish() })
            }
        }
    }
}

private data class SsoProfileUi(
    val categoryName: String,
    val account: String,
    val name: String,
    val phone: String,
    val email: String,
    val department: String
)

private data class EduProfileUi(
    val department: String,
    val major: String,
    val educationSystem: String,
    val className: String,
    val studentId: String,
    val name: String,
    val gender: String,
    val ethnicity: String
)

private data class MoocProfileUi(
    val name: String,
    val lastLoginTime: String,
    val totalOnlineTime: String,
    val loginCount: String
)

@Composable
private fun ProfileDetailScreen(onBack: () -> Unit) {
    val studentId = StudentInfoManager.studentId
    val fallbackName = UserInfoManager.account.ifBlank { studentId }
    val scope = rememberCoroutineScope()

    var ssoInfo by remember {
        mutableStateOf(
            SsoProfileUi(
                categoryName = "学生 / 本专科生",
                account = studentId,
                name = fallbackName,
                phone = "—",
                email = UserInfoManager.userEmail.ifBlank { "未设置" },
                department = "—"
            )
        )
    }
    var eduInfo by remember {
        mutableStateOf(EduProfileUi("—", "—", "—", "—", studentId, fallbackName, "—", "—"))
    }
    var moocInfo by remember {
        mutableStateOf(MoocProfileUi(fallbackName, "—", "—", "—"))
    }
    var ssoLoading by remember { mutableStateOf(false) }
    var eduLoading by remember { mutableStateOf(false) }
    var moocLoading by remember { mutableStateOf(false) }
    var ssoError by remember { mutableStateOf<String?>(null) }
    var eduError by remember { mutableStateOf<String?>(null) }
    var moocError by remember { mutableStateOf<String?>(null) }

    fun refreshSso() {
        if (ssoLoading) return
        scope.launch {
            ssoLoading = true
            ssoError = null
            runCatching { loadSsoProfile() }
                .onSuccess { ssoInfo = it }
                .onFailure { ssoError = "刷新失败，当前显示本地信息" }
            ssoLoading = false
        }
    }

    fun refreshEdu() {
        if (eduLoading) return
        scope.launch {
            eduLoading = true
            eduError = null
            runCatching { loadEduProfile() }
                .onSuccess { eduInfo = it }
                .onFailure { eduError = "教务个人信息暂时无法获取" }
            eduLoading = false
        }
    }

    fun refreshMooc() {
        if (moocLoading) return
        scope.launch {
            moocLoading = true
            moocError = null
            runCatching { loadMoocProfile() }
                .onSuccess { moocInfo = it }
                .onFailure { moocError = "网络课程中心个人信息暂时无法获取" }
            moocLoading = false
        }
    }

    LaunchedEffect(Unit) {
        refreshSso()
        refreshEdu()
        refreshMooc()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.bgPrimaryColor)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        CompactDetailTopBar("个人详情", onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = HyperSpacing.pageHorizontal, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(HyperSpacing.cardGap)
        ) {
            ProfileSection(
                title = "统一身份认证个人信息",
                loading = ssoLoading,
                error = ssoError,
                onRefresh = ::refreshSso,
                rows = listOf(
                    "学生类型" to ssoInfo.categoryName,
                    "账号" to ssoInfo.account,
                    "用户名" to ssoInfo.name,
                    "手机号" to ssoInfo.phone,
                    "邮箱" to ssoInfo.email,
                    "所属院系" to ssoInfo.department
                )
            )
            ProfileSection(
                title = "教务个人信息",
                loading = eduLoading,
                error = eduError,
                onRefresh = ::refreshEdu,
                rows = listOf(
                    "院系" to eduInfo.department,
                    "专业" to eduInfo.major,
                    "学制" to eduInfo.educationSystem,
                    "班级" to eduInfo.className,
                    "学号" to eduInfo.studentId,
                    "姓名" to eduInfo.name,
                    "性别" to eduInfo.gender,
                    "民族" to eduInfo.ethnicity
                )
            )
            ProfileSection(
                title = "网络课程中心个人信息",
                loading = moocLoading,
                error = moocError,
                onRefresh = ::refreshMooc,
                rows = listOf(
                    "姓名" to moocInfo.name,
                    "上次登录时间" to moocInfo.lastLoginTime,
                    "总在线时间" to moocInfo.totalOnlineTime,
                    "登录次数" to moocInfo.loginCount
                )
            )
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun CompactDetailTopBar(title: String, onBack: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = HyperSpacing.pageHorizontal, top = 6.dp, end = HyperSpacing.pageHorizontal, bottom = 10.dp),
        shape = RoundedCornerShape(HyperSpacing.topBarRadius),
        color = AppTheme.colors.bgCardColor,
        shadowElevation = 4.dp
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().height(HyperSpacing.topBarContentHeight).padding(horizontal = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            PortalBackButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.CenterStart),
                tint = Color(0xFF168FD0)
            )
            Text(title, color = AppTheme.colors.primaryTextColor, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ProfileSection(
    title: String,
    loading: Boolean,
    error: String?,
    onRefresh: () -> Unit,
    rows: List<Pair<String, String>>
) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 10.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, color = AppTheme.colors.secondaryTextColor, fontSize = 14.sp, modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier.size(32.dp).hyperTap(enabled = !loading, onClick = onRefresh),
                contentAlignment = Alignment.Center
            ) {
                if (loading) {
                    CircularProgressIndicator(Modifier.size(17.dp), strokeWidth = 2.dp, color = Color(0xFF168FD0))
                } else {
                    Icon(
                        painter = painterResource(R.drawable.coursetable_ic_refresh),
                        contentDescription = "刷新$title",
                        tint = Color(0xFF168FD0),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
        HyperSurface(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(horizontal = HyperSpacing.cardPaddingHorizontal)) {
                rows.forEachIndexed { index, row ->
                    ProfileValueRow(row.first, row.second)
                    if (index != rows.lastIndex) {
                        HorizontalDivider(color = AppTheme.colors.dividerColor.copy(alpha = .14f))
                    }
                }
            }
        }
        if (error != null) {
            Text(
                text = error,
                color = AppTheme.colors.secondaryTextColor,
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 10.dp)
            )
        }
    }
}

@Composable
private fun ProfileValueRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = AppTheme.colors.primaryTextColor, fontSize = 14.sp, modifier = Modifier.weight(.38f))
        Text(
            text = value.ifBlank { "—" },
            color = AppTheme.colors.secondaryTextColor,
            fontSize = 14.sp,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(.62f)
        )
    }
}

private suspend fun loadSsoProfile(): SsoProfileUi = withContext(Dispatchers.IO) {
    val repository = MoocRepository.instance
    var result = repository.getLoginUser().filter { it !is Resource.Loading }.first()
    if (result !is Resource.Success) {
        repository.login(StudentInfoManager.studentId, StudentInfoManager.studentPassword)
            .filter { it !is Resource.Loading }.first()
        result = repository.getLoginUser().filter { it !is Resource.Loading }.first()
    }
    val profile = (result as? Resource.Success)?.data ?: error("未获取到统一身份认证信息")
    UserInfoManager.account = profile.userName
    if (profile.avatar.isNotBlank()) UserInfoManager.userAvatar = profile.avatar
    SsoProfileUi(
        categoryName = profile.categoryName,
        account = profile.userAccount,
        name = profile.userName,
        phone = profile.phone.ifBlank { "—" },
        email = profile.email?.ifBlank { "未设置" } ?: "未设置",
        department = profile.deptName
    )
}

private suspend fun loadEduProfile(): EduProfileUi = withContext(Dispatchers.IO) {
    val loggedIn = AuthService.CheckLoginStates() || AuthService.login(
        StudentInfoManager.studentId,
        StudentInfoManager.studentPassword
    )
    check(loggedIn) { "教务系统登录失败" }
    val request = Request.Builder().url("http://xk.csust.edu.cn/jsxsd/grxx/xsxx").get().build()
    val html = RetrofitUtils.EducationClientForService.newCall(request).execute().use { response ->
        check(response.isSuccessful) { "教务信息请求失败" }
        response.body?.string() ?: error("教务信息为空")
    }
    val rows = Jsoup.parse(html).select("#xjkpTable > tbody > tr")
    fun cell(row: Int, column: Int): String = rows.getOrNull(row)
        ?.select("td")
        ?.getOrNull(column)
        ?.text()
        ?.trim()
        .orEmpty()
    fun labelled(row: Int, column: Int): String = cell(row, column).substringAfter("：", cell(row, column)).trim()
    check(rows.size > 7) { "未找到教务个人信息表" }
    EduProfileUi(
        department = labelled(2, 0),
        major = labelled(2, 1),
        educationSystem = labelled(2, 2),
        className = labelled(2, 3),
        studentId = labelled(2, 4).ifBlank { StudentInfoManager.studentId },
        name = cell(3, 1).ifBlank { UserInfoManager.account },
        gender = cell(3, 3),
        ethnicity = cell(7, 3)
    )
}

private suspend fun loadMoocProfile(): MoocProfileUi = withContext(Dispatchers.IO) {
    val repository = MoocRepository.instance
    var result = repository.getProfile().filter { it !is Resource.Loading }.first()
    if (result !is Resource.Success) {
        repository.login(StudentInfoManager.studentId, StudentInfoManager.studentPassword)
            .filter { it !is Resource.Loading }.first()
        result = repository.getProfile().filter { it !is Resource.Loading }.first()
    }
    val profile = (result as? Resource.Success)?.data ?: error("未获取到网络课程中心信息")
    MoocProfileUi(
        name = profile.name,
        lastLoginTime = profile.lastLoginTime,
        totalOnlineTime = profile.totalOnlineTime,
        loginCount = profile.loginCount.toString()
    )
}
