package com.creamaker.changli_planet_app.profileSettings.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.creamaker.changli_planet_app.R
import com.creamaker.changli_planet_app.core.designsystem.HyperSpacing
import com.creamaker.changli_planet_app.core.designsystem.PortalBackButton
import com.creamaker.changli_planet_app.core.theme.AppSkinTheme
import com.creamaker.changli_planet_app.core.theme.AppTheme
import com.creamaker.changli_planet_app.profileSettings.data.CampusBackgroundWorker
import java.util.concurrent.TimeUnit

class ParitySettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val mode = intent.getStringExtra(EXTRA_MODE) ?: MODE_NETWORK
        setContent { AppSkinTheme { SettingsScreen(mode, { finish() }) } }
    }
    companion object {
        const val EXTRA_MODE = "mode"; const val MODE_NETWORK = "network"; const val MODE_BACKGROUND = "background"; const val MODE_NOTIFICATION = "notification"; const val MODE_WIDGET = "widget"
    }
}

@Composable
private fun SettingsScreen(mode: String, onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences(CampusBackgroundWorker.PREFS, Context.MODE_PRIVATE) }
    val title = when (mode) { ParitySettingsActivity.MODE_BACKGROUND -> "后台任务设置"; ParitySettingsActivity.MODE_NOTIFICATION -> "通知设置"; ParitySettingsActivity.MODE_WIDGET -> "小组件设置"; else -> "网络设置" }
    Column(Modifier.fillMaxSize().background(AppTheme.colors.bgPrimaryColor).statusBarsPadding()) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = HyperSpacing.pageHorizontal,
                    top = 6.dp,
                    end = HyperSpacing.pageHorizontal,
                    bottom = 10.dp
                ),
            shape = RoundedCornerShape(HyperSpacing.topBarRadius),
            color = AppTheme.colors.bgCardColor,
            shadowElevation = 4.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(HyperSpacing.topBarContentHeight)
                    .padding(horizontal = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                PortalBackButton(
                    onClick = onBack,
                    modifier = Modifier
                        .align(Alignment.CenterStart),
                    tint = Color(0xFF168FD0)
                )
                Text(
                    text = title,
                    color = AppTheme.colors.primaryTextColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            when (mode) {
                ParitySettingsActivity.MODE_BACKGROUND -> BackgroundSettings(context, prefs)
                ParitySettingsActivity.MODE_NOTIFICATION -> NotificationSettings(context, prefs)
                ParitySettingsActivity.MODE_WIDGET -> WidgetSettings(prefs)
                else -> NetworkSettings(prefs)
            }
            Spacer(Modifier.height(30.dp))
        }
    }
}

@Composable private fun NetworkSettings(prefs: android.content.SharedPreferences) {
    var enabled by remember { mutableStateOf(prefs.getBoolean("webvpn_enabled", false)) }
    SettingsCard("WebVPN", "开启后应用优先通过学校官方 WebVPN 访问校园网资源；切换后重新进入功能页生效。") { SettingSwitch("开启 WebVPN 模式", enabled) { enabled = it; prefs.edit().putBoolean("webvpn_enabled", it).apply() } }
}

@Composable private fun BackgroundSettings(context: Context, prefs: android.content.SharedPreferences) {
    var enabled by remember { mutableStateOf(prefs.getBoolean("background_enabled", false)) }
    var electricity by remember { mutableStateOf(prefs.getBoolean("task_electricity", true)) }
    var hours by remember { mutableIntStateOf(prefs.getInt("background_hours", 3)) }
    fun applySchedule() {
        prefs.edit().putBoolean("background_enabled", enabled).putBoolean("task_electricity", electricity).putInt("background_hours", hours).apply()
        val manager = WorkManager.getInstance(context)
        if (!enabled) manager.cancelUniqueWork(CampusBackgroundWorker.UNIQUE_WORK) else {
            val request = PeriodicWorkRequestBuilder<CampusBackgroundWorker>(hours.toLong().coerceAtLeast(1), TimeUnit.HOURS).setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).build()
            manager.enqueueUniquePeriodicWork(CampusBackgroundWorker.UNIQUE_WORK, ExistingPeriodicWorkPolicy.UPDATE, request)
        }
    }
    SettingsCard("后台任务", "系统会在满足网络和电量条件时定期刷新，实际执行时间由 Android 调度。") {
        SettingSwitch("后台任务总开关", enabled) { enabled = it; applySchedule() }
        if (enabled) { HorizontalDivider(); FrequencyRow(hours) { hours = it; applySchedule() } }
    }
    SettingsCard("宿舍电量定时查询", "自动刷新已绑定宿舍的剩余电量，并同步概览卡片和小组件。") { SettingSwitch("开启", electricity) { electricity = it; applySchedule() } }
}

@Composable private fun NotificationSettings(context: Context, prefs: android.content.SharedPreferences) {
    var liveStatus by remember { mutableStateOf(prefs.getBoolean("course_live_status", true)) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    SettingsCard("推送通知", "开启后可收到低电量、课程和考试等重要提醒。") {
        TextButton(onClick = { if (Build.VERSION.SDK_INT >= 33) launcher.launch(Manifest.permission.POST_NOTIFICATIONS) else openNotificationSettings(context) }) { Text("点击开启通知") }
        TextButton(onClick = { openNotificationSettings(context) }) { Text("前往系统设置管理通知") }
    }
    SettingsCard("实时课程状态", "Android 将用常驻通知显示上课前、上课中和下课后的课程状态。") { SettingSwitch("允许实时课程状态", liveStatus) { liveStatus = it; prefs.edit().putBoolean("course_live_status", it).apply() } }
}

@Composable private fun WidgetSettings(prefs: android.content.SharedPreferences) {
    WidgetSection("宿舍电量小组件", "widget_electricity", prefs)
    WidgetSection("成绩分析小组件", "widget_grade", prefs)
    WidgetSection("待提交作业小组件", "widget_assignment", prefs)
}

@Composable private fun WidgetSection(title: String, key: String, prefs: android.content.SharedPreferences) {
    var enabled by remember { mutableStateOf(prefs.getBoolean("${key}_enabled", true)) }
    var hours by remember { mutableIntStateOf(prefs.getInt("${key}_hours", 3)) }
    SettingsCard(title, "开启后小组件会按所选频率请求最新数据。") {
        SettingSwitch("自动刷新", enabled) { enabled = it; prefs.edit().putBoolean("${key}_enabled", it).apply() }
        if (enabled) { HorizontalDivider(); FrequencyRow(hours) { hours = it; prefs.edit().putInt("${key}_hours", it).apply() } }
    }
}

@Composable private fun SettingsCard(title: String, footer: String, content: @Composable ColumnScope.() -> Unit) { Column(verticalArrangement = Arrangement.spacedBy(7.dp)) { Text(title, color = AppTheme.colors.secondaryTextColor, fontSize = 13.sp, modifier = Modifier.padding(start = 10.dp)); Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = AppTheme.colors.bgCardColor) { Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), content = content) }; Text(footer, color = AppTheme.colors.secondaryTextColor, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 10.dp)) } }
@Composable private fun SettingSwitch(label: String, checked: Boolean, action: (Boolean) -> Unit) = Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) { Text(label, modifier = Modifier.weight(1f)); Switch(checked, action) }
@Composable private fun FrequencyRow(value: Int, action: (Int) -> Unit) = Column(Modifier.padding(vertical = 8.dp)) { Text("刷新频率：$value 小时"); Slider(value.toFloat(), { action(it.toInt().coerceIn(1, 6)) }, valueRange = 1f..6f, steps = 4) }
private fun openNotificationSettings(context: Context) { context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
