package com.csust.pocket.profileSettings.ui.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.SystemUpdateAlt
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material.icons.outlined.Widgets
import com.csust.pocket.core.designsystem.HyperSurface
import com.csust.pocket.core.designsystem.hyperTap
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.csust.pocket.R
import com.csust.pocket.common.data.local.mmkv.StudentInfoManager
import com.csust.pocket.common.data.local.mmkv.UserInfoManager
import com.csust.pocket.common.update.AppUpdateChecker
import com.csust.pocket.core.PlanetApplication
import com.csust.pocket.core.MainActivity
import com.csust.pocket.core.Route
import com.csust.pocket.core.theme.AppSkinTheme
import com.csust.pocket.core.theme.AppTheme
import com.csust.pocket.profileSettings.ui.model.SettingItemUiModel
import com.csust.pocket.profileSettings.ui.ParitySettingsActivity
import com.csust.pocket.feature.common.data.repository.ElectricityRepository
import com.csust.pocket.feature.mooc.data.MoocDataHelper
import com.csust.pocket.widget.view.CustomToast
import com.dcelysia.csust_spider.core.Resource
import com.dcelysia.csust_spider.education.data.remote.services.AuthService
import com.dcelysia.csust_spider.education.data.remote.EducationData
import com.dcelysia.csust_spider.mooc.data.remote.repository.MoocRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ProfileSettingsRoute(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isBound = remember { StudentInfoManager.studentId.isNotBlank() }
    val username = if (isBound) {
        UserInfoManager.account.takeIf { it.isNotBlank() } ?: StudentInfoManager.studentId
    } else {
        "长理学子~"
    }
    val avatarUrl = UserInfoManager.userAvatar
    var showCacheDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var refreshingKey by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun refreshLogin(key: String, block: suspend () -> Boolean) {
        if (!isBound) {
            Route.goBindingUser(context)
            return
        }
        if (refreshingKey != null) return
        scope.launch {
            refreshingKey = key
            val success = runCatching { withContext(Dispatchers.IO) { block() } }.getOrDefault(false)
            CustomToast.showMessage(context, if (success) "刷新成功" else "刷新失败，请检查账号或网络")
            refreshingKey = null
        }
    }

    ProfileSettingsScreen(
        modifier = modifier,
        items = createSettingItems(),
        username = username,
        avatarData = avatarUrl,
        isBound = isBound,
        refreshingKey = refreshingKey,
        onRefreshLogin = { key ->
            val studentId = StudentInfoManager.studentId
            val password = StudentInfoManager.studentPassword
            when (key) {
                "sso" -> refreshLogin(key) {
                    MoocRepository.instance.login(studentId, password)
                        .filter { it !is Resource.Loading }
                        .first() is Resource.Success
                }
                "edu" -> refreshLogin(key) { AuthService.login(studentId, password) }
                "mooc" -> refreshLogin(key) {
                    MoocDataHelper.fetchMoocCourses(studentId, password, forceRefresh = true)
                    true
                }
                "card" -> refreshLogin(key) { ElectricityRepository().query(force = true) != null }
            }
        },
        onItemClick = { item ->
            if (item.id == "4") {
                showCacheDialog = true
            } else if (item.id == "15") {
                AppUpdateChecker.check(context, manual = true)
            } else {
                handleSettingItemClick(context, item)
            }
        },
        onPrimaryClick = {
            if (isBound) {
                Route.goProfileDetail(context)
            } else {
                Route.goBindingUser(context)
            }
        },
        onLogout = { showLogoutDialog = true }
    )

    if (showCacheDialog) {
        ConfirmDialog(
            title = "将清除实用工具的所有缓存",
            content = "确定要清除缓存嘛₍ᐢ.ˬ.⑅ᐢ₎",
            onDismiss = { showCacheDialog = false },
            onConfirm = {
                PlanetApplication.clearContentCache()
                showCacheDialog = false
            }
        )
    }

    if (showLogoutDialog) {
        ConfirmDialog(
            title = "退出登录",
            content = "将退出统一身份认证、教务系统和网络课程中心，并清除本机保存的账号绑定。确定继续吗？",
            onDismiss = { showLogoutDialog = false },
            onConfirm = {
                showLogoutDialog = false
                if (refreshingKey == null) {
                    scope.launch {
                        refreshingKey = "logout"
                        withContext(Dispatchers.IO) {
                            runCatching {
                                MoocRepository.instance.logout()
                                    .filter { it !is Resource.Loading }
                                    .first()
                            }
                            runCatching { AuthService.LoginOut() }
                            EducationData.clear()
                            PlanetApplication.clearAccountDataNow()
                        }
                        (context.applicationContext as? PlanetApplication)
                            ?.moocViewModel
                            ?.clearForLogout()
                        refreshingKey = null
                        CustomToast.showMessage(context, "已退出登录")
                        context.startActivity(
                            Intent(context, MainActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                            }
                        )
                    }
                }
            }
        )
    }
}

@Composable
private fun ProfileSettingsScreen(
    items: List<SettingItemUiModel>,
    username: String,
    avatarData: Any?,
    isBound: Boolean,
    refreshingKey: String?,
    onRefreshLogin: (String) -> Unit,
    onItemClick: (SettingItemUiModel.Option) -> Unit,
    onPrimaryClick: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppTheme.colors.bgPrimaryColor)
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Text(
            text = "我的",
            color = AppTheme.colors.primaryTextColor,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp, bottom = 18.dp)
        )
        PortalGroupTitle("账号管理")
        HyperSurface(
            color = AppTheme.colors.bgCardColor,
            shape = RoundedCornerShape(15.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(horizontal = 16.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .hyperTap(onClick = onPrimaryClick)
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(avatarData)
                            .crossfade(true)
                            .placeholder(R.drawable.ic_fulilian)
                            .error(R.drawable.ic_error_vector)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(42.dp).clip(CircleShape)
                    )
                    Spacer(Modifier.size(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(username, color = AppTheme.colors.primaryTextColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text(if (isBound) "长沙理工大学 · 已绑定学号" else "未绑定学号", color = AppTheme.colors.secondaryTextColor, fontSize = 11.sp)
                    }
                    PortalArrow()
                }
                HorizontalDivider(color = AppTheme.colors.dividerColor.copy(alpha = 0.12f))
                PortalProfileRow("刷新统一身份认证登录", Icons.Outlined.Security, loading = refreshingKey == "sso") { onRefreshLogin("sso") }
                PortalProfileRow("刷新教务系统登录", Icons.Outlined.School, loading = refreshingKey == "edu") { onRefreshLogin("edu") }
                PortalProfileRow("刷新网络课程中心登录", Icons.Outlined.MenuBook, loading = refreshingKey == "mooc") { onRefreshLogin("mooc") }
                PortalProfileRow("刷新校园卡系统登录", Icons.Outlined.CreditCard, loading = refreshingKey == "card") { onRefreshLogin("card") }
                if (isBound) {
                    HorizontalDivider(color = AppTheme.colors.dividerColor.copy(alpha = 0.12f))
                    PortalLogoutRow(loading = refreshingKey == "logout", onClick = onLogout)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        PortalGroupTitle("设置")
        PortalSettingsGroup(
            listOf(
                SettingItemUiModel.Option("6", "外观主题", R.drawable.zhuti_tiaosepan),
                SettingItemUiModel.Option("7", "网络设置", R.drawable.ic_guanyuwomen),
                SettingItemUiModel.Option("14", "后台任务设置", R.drawable.qingchu),
                SettingItemUiModel.Option("8", "通知设置", R.drawable.yijianfankui)
                ,SettingItemUiModel.Option("13", "小组件设置", R.drawable.ic_document)
            ),
            onItemClick
        )
        Spacer(Modifier.height(16.dp))
        PortalGroupTitle("帮助与支持")
        PortalSettingsGroup(
            listOf(
                SettingItemUiModel.Option("15", "检查更新", R.drawable.qingchu),
                SettingItemUiModel.Option("9", "关于掌上长理", R.drawable.ic_guanyuwomen),
                SettingItemUiModel.Option("16", "软件官网", R.drawable.ic_document),
                SettingItemUiModel.Option("12", "用户协议", R.drawable.ic_document)
            ),
            onItemClick
        )
        Spacer(Modifier.height(30.dp))
    }
}

@Composable
private fun PortalGroupTitle(title: String) {
    Text(
        title,
        color = AppTheme.colors.secondaryTextColor,
        fontSize = 14.sp,
        modifier = Modifier.padding(start = 10.dp, bottom = 8.dp)
    )
}

@Composable
private fun PortalSettingsGroup(
    items: List<SettingItemUiModel.Option>,
    onItemClick: (SettingItemUiModel.Option) -> Unit
) {
    HyperSurface(
        color = AppTheme.colors.bgCardColor,
        shape = RoundedCornerShape(15.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(horizontal = 16.dp)) {
            items.forEachIndexed { index, item ->
                PortalProfileRow(item.title, profileIcon(item.id)) { onItemClick(item) }
                if (index != items.lastIndex) HorizontalDivider(color = AppTheme.colors.dividerColor.copy(alpha = 0.12f))
            }
        }
    }
}

@Composable
private fun PortalProfileRow(title: String, icon: ImageVector, loading: Boolean = false, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .hyperTap(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(AppTheme.colors.commonColor.copy(alpha = .10f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = AppTheme.colors.commonColor, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.size(12.dp))
        Text(title, color = AppTheme.colors.primaryTextColor, fontSize = 15.sp, modifier = Modifier.weight(1f))
        if (loading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Color(0xFF1697D5)) else PortalArrow()
    }
}

@Composable
private fun PortalLogoutRow(loading: Boolean, onClick: () -> Unit) {
    val dangerColor = Color(0xFFE94B55)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .hyperTap(enabled = !loading, onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(dangerColor.copy(alpha = .10f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Logout,
                contentDescription = null,
                tint = dangerColor,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(Modifier.size(12.dp))
        Text("退出登录", color = dangerColor, fontSize = 15.sp, modifier = Modifier.weight(1f))
        if (loading) {
            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = dangerColor)
        }
    }
}

@Composable
private fun PortalArrow() {
    Icon(
        imageVector = Icons.Outlined.ChevronRight,
        contentDescription = null,
        tint = AppTheme.colors.secondaryTextColor.copy(alpha = 0.55f),
        modifier = Modifier.size(18.dp)
    )
}

private fun profileIcon(id: String): ImageVector = when (id) {
    "6" -> Icons.Outlined.Palette
    "7" -> Icons.Outlined.Wifi
    "14" -> Icons.Outlined.CloudSync
    "8" -> Icons.Outlined.NotificationsNone
    "13" -> Icons.Outlined.Widgets
    "15" -> Icons.Outlined.SystemUpdateAlt
    "9" -> Icons.Outlined.Info
    "16" -> Icons.Outlined.Language
    "12" -> Icons.Outlined.Article
    else -> Icons.Outlined.Info
}

@Composable
private fun SettingsCard(
    items: List<SettingItemUiModel>,
    isBound: Boolean,
    onItemClick: (SettingItemUiModel.Option) -> Unit,
    onPrimaryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    HyperSurface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = AppTheme.colors.bgCardColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            items.forEach { item ->
                when (item) {
                    is SettingItemUiModel.Header -> SettingHeaderItem(item)
                    is SettingItemUiModel.Option -> {
                        SettingOptionItem(item = item, onClick = { onItemClick(item) })
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onPrimaryClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppTheme.colors.bgButtonLowlightColor,
                    contentColor = AppTheme.colors.functionalTextColor
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(
                    text = if (isBound) "切换学号" else "绑定学号",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun SettingHeaderItem(item: SettingItemUiModel.Header) {
    Text(
        text = item.title,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = AppTheme.colors.greyTextColor,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingOptionItem(
    item: SettingItemUiModel.Option,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .hyperTap(onClick = onClick)
            .padding(vertical = 14.dp)
    ) {
        item.iconResId?.takeIf { it != 0 }?.let { iconResId ->
            Icon(
                painter = painterResource(id = iconResId),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier
                    .size(24.dp)
                    .padding(end = 4.dp)
            )
            Spacer(modifier = Modifier.size(12.dp))
        }

        Text(
            text = item.title,
            fontSize = 16.sp,
            color = AppTheme.colors.primaryTextColor,
            modifier = Modifier.weight(1f)
        )
        Icon(
            painter = painterResource(id = R.drawable.ic_arrow_right),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun ConfirmDialog(
    title: String,
    content: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        containerColor = AppTheme.colors.bgCardColor,
        titleContentColor = AppTheme.colors.primaryTextColor,
        textContentColor = AppTheme.colors.greyTextColor,
        onDismissRequest = onDismiss,
        title = { Text(text = title, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
        text = { Text(text = content, fontSize = 16.sp) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("确定", color = AppTheme.colors.functionalTextColor, fontSize = 16.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = AppTheme.colors.greyTextColor, fontSize = 16.sp)
            }
        }
    )
}

private fun handleSettingItemClick(
    context: android.content.Context,
    item: SettingItemUiModel.Option
) {
    when (item.id) {
        "5" -> Route.goBindingUser(context)
        "6" -> Route.goSkinSecletion(context)
        "7" -> Route.goParitySettings(context, ParitySettingsActivity.MODE_NETWORK)
        "14" -> Route.goParitySettings(context, ParitySettingsActivity.MODE_BACKGROUND)
        "8" -> Route.goParitySettings(context, ParitySettingsActivity.MODE_NOTIFICATION)
        "13" -> Route.goParitySettings(context, ParitySettingsActivity.MODE_WIDGET)
        "9" -> Route.goAbout(context)
        "16" -> Route.goSoftwareWebsite(context)
        "12" -> Route.goContract(context)
        "11" -> Route.goMooc(context)
    }
}

private fun createSettingItems(): List<SettingItemUiModel> = listOf(
    SettingItemUiModel.Header("主要设置"),
    SettingItemUiModel.Option("4", "清除缓存", R.drawable.qingchu),
    SettingItemUiModel.Option("5", "绑定学号", R.drawable.ic_bianji),
    SettingItemUiModel.Option("6", "主题设置", R.drawable.zhuti_tiaosepan),
    SettingItemUiModel.Option("9", "关于掌上长理", R.drawable.ic_guanyuwomen)
)

@Preview
@Composable
private fun ProfileSettingsRoutePreview() {
    AppSkinTheme {
        ProfileSettingsScreen(
            items = createSettingItems(),
            username = "长理学子~",
            avatarData = null,
            isBound = false,
            refreshingKey = null,
            onRefreshLogin = {},
            onItemClick = {},
            onPrimaryClick = {},
            onLogout = {}
        )
    }
}
