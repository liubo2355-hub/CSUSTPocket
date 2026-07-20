package com.csust.pocket.feature.common.compose_ui

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.runtime.mutableIntStateOf
import android.Manifest
import android.os.Build
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.csust.pocket.feature.common.reminder.ElectricityReminder
import com.csust.pocket.feature.common.reminder.ElectricityReminderPrefs
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.csust.pocket.ElectronicAppWidget
import com.csust.pocket.R
import com.csust.pocket.core.PlanetApplication
import com.csust.pocket.core.designsystem.PortalBackButton
import com.csust.pocket.core.designsystem.hyperTap
import com.csust.pocket.core.theme.AppTheme
import com.csust.pocket.feature.common.contract.ElectronicContract
import com.csust.pocket.feature.common.viewModel.ElectronicViewModel
import com.csust.pocket.overview.data.local.OverviewLocalCache
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val ALPHANUMERIC_REGEX = Regex("^[a-zA-Z0-9]*$")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ElectronicScreen(
    viewModel: ElectronicViewModel,
    onBack: () -> Unit
) {
    val colors = AppTheme.colors
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val state by viewModel.state.collectAsStateWithLifecycle()

    val mmkv = remember { MMKV.defaultMMKV() }

    var schoolText by remember { mutableStateOf("选择校区") }
    var dormText by remember { mutableStateOf("选择宿舍楼") }
    var roomText by remember { mutableStateOf("") }
    var showBindingSheet by remember { mutableStateOf(false) }
    var showReminderDialog by remember { mutableStateOf(false) }
    val notifPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }
    val requestNotifIfNeeded: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // 强制刷新：清焦点 + 保存当前寝室 + 强制重查（ViewModel 会取消卡住的旧任务）
    val submitQuery: () -> Unit = {
        focusManager.clearFocus()
        val processedDoorNumber = processDormAndRoom(dormText, roomText)
        viewModel.processIntent(
            ElectronicContract.Intent.QueryElectricity(schoolText, dormText, processedDoorNumber)
        )
        mmkv.encode("school", schoolText)
        mmkv.encode("dor", dormText)
        mmkv.encode("door_number", roomText)
        refreshWidget()
    }

    val schoolList = remember {
        context.resources.getStringArray(R.array.school_location).toList()
    }
    val dormList = remember {
        context.resources.getStringArray(R.array.dormitory).toList()
    }

    // Restore state from MMKV on first composition
    LaunchedEffect(Unit) {
        val savedSchool = mmkv.decodeString("school", "选择校区") ?: "选择校区"
        val savedDor = mmkv.decodeString("dor", "选择宿舍楼") ?: "选择宿舍楼"
        val savedDoor = mmkv.decodeString("door_number", "") ?: ""

        schoolText = savedSchool
        dormText = savedDor
        roomText = savedDoor

        viewModel.processIntent(
            ElectronicContract.Intent.Init(savedSchool, savedDor, savedDoor)
        )
    }

    // Foreground automatic sampling. The first short delay lets Init restore the
    // saved binding; later samples arrive without requiring the query button.
    LaunchedEffect(Unit) {
        delay(1_500L)
        while (true) {
            viewModel.processIntent(ElectronicContract.Intent.AutoRefresh)
            delay(5 * 60 * 1_000L)
        }
    }

    // Save state to MMKV when leaving
    DisposableEffect(Unit) {
        onDispose {
            mmkv.encode("school", schoolText)
            mmkv.encode("dor", dormText)
            mmkv.encode("door_number", roomText)
        }
    }

    // Binding / reminder sheets
    if (showBindingSheet) {
        DormBindingSheet(
            state = state,
            viewModel = viewModel,
            initialSchool = schoolText,
            initialDorm = dormText,
            initialRoom = roomText,
            schoolList = schoolList,
            dormList = dormList,
            onDismiss = { showBindingSheet = false },
            onConfirm = { school, dorm, room ->
                schoolText = school
                dormText = dorm
                roomText = room
                viewModel.processIntent(ElectronicContract.Intent.SelectSchool(school))
                viewModel.processIntent(ElectronicContract.Intent.SelectDorm(dorm))
                submitQuery()
            }
        )
    }

    if (showReminderDialog) {
        ReminderDialog(
            onDismiss = { showReminderDialog = false },
            onEnableNeedsPermission = requestNotifIfNeeded
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.campusSnowColor)
    ) {
        // ── Top Bar ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .shadow(8.dp, RoundedCornerShape(20.dp))
                .clip(RoundedCornerShape(20.dp))
                .background(colors.bgCardColor)
                .padding(horizontal = 4.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PortalBackButton(onClick = onBack, tint = colors.campusInkColor)
            Text(
                text = "电费查询",
                fontSize = 18.sp,
                fontWeight = FontWeight.W700,
                color = colors.campusInkColor,
                letterSpacing = (-0.25).sp
            )
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = { if (!state.isLoading) submitQuery() }) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = colors.campusSkyBlueColor,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "强制刷新",
                        tint = colors.campusSkyBlueColor
                    )
                }
            }
            IconButton(onClick = { showReminderDialog = true }) {
                Icon(
                    imageVector = Icons.Filled.Notifications,
                    contentDescription = "电量提醒",
                    tint = colors.campusSkyBlueColor
                )
            }
        }

        // ── Scrollable Content ──
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            // Section label
            Text(
                text = "寝室绑定",
                fontSize = 13.sp,
                fontWeight = FontWeight.W500,
                color = colors.campusSlateColor,
                modifier = Modifier.padding(start = 4.dp, bottom = 10.dp)
            )

            // ── Binding summary button ──
            BindingSummaryButton(
                schoolText = schoolText,
                dormText = dormText,
                roomText = roomText,
                onClick = { showBindingSheet = true }
            )

            Spacer(modifier = Modifier.height(20.dp))

            CampusCard {
                ElectricityResult(state = state)
            }

            Spacer(modifier = Modifier.height(16.dp))

            ElectricityUsageCard(state = state)

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { submitQuery() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.campusSkyBlueColor,
                    contentColor = Color.White
                )
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "立即更新",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.W500
                    )
                }
            }
        }
    }
}

@Composable
private fun CampusCard(content: @Composable () -> Unit) {
    val colors = AppTheme.colors
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.bgCardColor),
        border = null,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color.Black.copy(alpha = 0.04f),
                spotColor = Color.Black.copy(alpha = 0.04f)
            )
    ) {
        content()
    }
}

@Composable
private fun CampusDivider() {
    HorizontalDivider(
        color = AppTheme.colors.campusDividerColor,
        modifier = Modifier.padding(horizontal = 18.dp)
    )
}

@Composable
private fun SelectorRow(label: String, value: String, onClick: () -> Unit) {
    val colors = AppTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = FontWeight.W500,
            color = colors.campusInkColor
        )
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(colors.campusSkyBlueLightColor)
                .border(1.dp, colors.campusSkyBlueColor.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = value,
                color = colors.campusSkyBlueColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.W500
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = colors.campusSkyBlueColor,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

private enum class EleState { LOW, NORMAL, HIGH, UNKNOWN, ERROR }

private enum class UsagePeriod(val title: String) {
    REALTIME("实时"),
    WEEK("近七日"),
    THREE_MONTHS("近三月")
}

@Composable
private fun ElectricityUsageCard(state: ElectronicContract.State) {
    val colors = AppTheme.colors
    var period by remember { mutableStateOf(UsagePeriod.REALTIME) }
    val entries = when (period) {
        UsagePeriod.REALTIME -> emptyList()
        UsagePeriod.WEEK -> state.usage7Days
        UsagePeriod.THREE_MONTHS -> state.usage3Months
    }
    val observed = entries.filter { it.usage != null }
    val total = observed.sumOf { it.usage?.toDouble() ?: 0.0 }
    val average = if (observed.isEmpty()) 0.0 else total / observed.size

    CampusCard {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "用电量趋势",
                        color = colors.campusInkColor,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.W700
                    )
                    Text(
                        if (period == UsagePeriod.REALTIME) "自动更新 · 最近 24 小时" else "根据真实电量查询记录计算",
                        color = colors.campusSlateColor,
                        fontSize = 11.sp
                    )
                }
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.campusCloudColor)
                        .padding(3.dp)
                ) {
                    UsagePeriod.entries.forEach { item ->
                        Text(
                            text = item.title,
                            color = if (period == item) colors.campusSkyBlueColor else colors.campusSlateColor,
                            fontSize = 12.sp,
                            fontWeight = if (period == item) FontWeight.W700 else FontWeight.W500,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (period == item) colors.bgCardColor else Color.Transparent)
                                .hyperTap { period = item }
                                .padding(horizontal = 10.dp, vertical = 7.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            if (period == UsagePeriod.REALTIME) {
                RealtimeElectricityContent(state.realtimeHistory)
            } else if (observed.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("暂无用电趋势", color = colors.campusInkColor, fontWeight = FontWeight.W600)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "至少需要两次不同时间的电量查询\n后续刷新会自动积累真实数据",
                        color = colors.campusSlateColor,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Row(Modifier.fillMaxWidth()) {
                    UsageMetric("累计用电", String.format(Locale.CHINA, "%.2f kWh", total), Modifier.weight(1f))
                    UsageMetric("已记录日均", String.format(Locale.CHINA, "%.2f kWh", average), Modifier.weight(1f))
                }
                Spacer(Modifier.height(14.dp))
                ElectricityLineChart(entries)
            }

            Spacer(Modifier.height(8.dp))
            Text(
                if (period == UsagePeriod.REALTIME) {
                    "每次手动刷新或后台查询都会记录真实采样点；余额下降表示用电，余额上升通常表示充值。"
                } else {
                    "余额上升视为充值，不计入用电量；数据覆盖范围取决于后台刷新记录。"
                },
                color = colors.campusSlateColor.copy(alpha = .82f),
                fontSize = 10.sp,
                lineHeight = 15.sp
            )
        }
    }
}

@Composable
private fun RealtimeElectricityContent(entries: List<OverviewLocalCache.ElectricityHistoryEntry>) {
    val colors = AppTheme.colors
    val latest = entries.lastOrNull()
    val change = if (entries.size >= 2) latest!!.value - entries[entries.lastIndex - 1].value else null

    if (entries.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxWidth().height(190.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("暂无实时采样", color = colors.campusInkColor, fontWeight = FontWeight.W600)
            Spacer(Modifier.height(6.dp))
            Text("点击下方查询按钮后会记录第一个真实电量点",
                color = colors.campusSlateColor, fontSize = 12.sp, textAlign = TextAlign.Center)
        }
        return
    }

    Row(Modifier.fillMaxWidth()) {
        UsageMetric("当前剩余", String.format(Locale.CHINA, "%.2f kWh", latest!!.value), Modifier.weight(1f))
        UsageMetric(
            "较上次查询",
            change?.let { String.format(Locale.CHINA, "%+.2f kWh", it) } ?: "等待下一次采样",
            Modifier.weight(1f)
        )
    }
    Spacer(Modifier.height(14.dp))
    RealtimeElectricityChart(entries)
    if (entries.size == 1) {
        Text("已记录 1 个采样点，再次刷新后将形成趋势线",
            color = colors.campusSlateColor, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun RealtimeElectricityChart(entries: List<OverviewLocalCache.ElectricityHistoryEntry>) {
    val colors = AppTheme.colors
    val values = entries.map { it.value }
    val minValue = values.minOrNull() ?: 0f
    val maxValue = values.maxOrNull() ?: 0f
    val spread = (maxValue - minValue).coerceAtLeast(.5f)
    val lowerBound = minValue - spread * .18f
    val upperBound = maxValue + spread * .18f
    val formatter = remember { SimpleDateFormat("HH:mm", Locale.CHINA) }
    val lineColor = colors.campusSkyBlueColor
    val gridColor = colors.campusDividerColor

    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(String.format(Locale.CHINA, "%.2f kWh", maxValue), color = colors.campusSlateColor, fontSize = 10.sp)
            Text("剩余电量 · ${entries.size} 个采样点", color = colors.campusSlateColor, fontSize = 10.sp)
        }
        Canvas(Modifier.fillMaxWidth().height(150.dp).padding(vertical = 8.dp)) {
            repeat(4) { index ->
                val y = size.height * index / 3f
                drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
            }
            val points = entries.mapIndexed { index, entry ->
                val x = if (entries.size == 1) size.width / 2f else size.width * index / (entries.size - 1f)
                val ratio = ((entry.value - lowerBound) / (upperBound - lowerBound)).coerceIn(0f, 1f)
                Offset(x, size.height - ratio * size.height)
            }
            if (points.size > 1) {
                val path = Path().apply {
                    moveTo(points.first().x, points.first().y)
                    points.drop(1).forEach { lineTo(it.x, it.y) }
                }
                drawPath(path, lineColor, style = Stroke(width = 5f, cap = StrokeCap.Round))
            }
            points.forEach { point ->
                drawCircle(Color.White, radius = 7f, center = point)
                drawCircle(lineColor, radius = 4.5f, center = point)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatter.format(Date(entries.first().timestamp)), color = colors.campusSlateColor, fontSize = 10.sp)
            if (entries.size > 2) Text(formatter.format(Date(entries[entries.lastIndex / 2].timestamp)), color = colors.campusSlateColor, fontSize = 10.sp)
            Text(formatter.format(Date(entries.last().timestamp)), color = colors.campusSlateColor, fontSize = 10.sp)
        }
    }
}

@Composable
private fun UsageMetric(label: String, value: String, modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    Column(modifier) {
        Text(label, color = colors.campusSlateColor, fontSize = 11.sp)
        Spacer(Modifier.height(2.dp))
        Text(value, color = colors.campusInkColor, fontSize = 16.sp, fontWeight = FontWeight.W700)
    }
}

@Composable
private fun ElectricityLineChart(entries: List<OverviewLocalCache.ElectricityUsageEntry>) {
    val colors = AppTheme.colors
    val values = entries.mapNotNull { it.usage }
    val maxValue = (values.maxOrNull() ?: 1f).coerceAtLeast(.1f)
    val formatter = remember { SimpleDateFormat("M/d", Locale.CHINA) }
    val lineColor = colors.campusSkyBlueColor
    val gridColor = colors.campusDividerColor

    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(String.format(Locale.CHINA, "%.2f kWh", maxValue), color = colors.campusSlateColor, fontSize = 10.sp)
            Text("每日用电量", color = colors.campusSlateColor, fontSize = 10.sp)
        }
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .padding(vertical = 8.dp)
        ) {
            val chartWidth = size.width
            val chartHeight = size.height
            repeat(4) { index ->
                val y = chartHeight * index / 3f
                drawLine(
                    gridColor,
                    start = androidx.compose.ui.geometry.Offset(0f, y),
                    end = androidx.compose.ui.geometry.Offset(chartWidth, y),
                    strokeWidth = 1f
                )
            }

            val path = Path()
            var pathStarted = false
            entries.forEachIndexed { index, entry ->
                val value = entry.usage
                if (value == null) {
                    pathStarted = false
                } else {
                    val x = if (entries.size <= 1) chartWidth / 2f else chartWidth * index / (entries.size - 1f)
                    val y = chartHeight - (value / maxValue * chartHeight)
                    if (pathStarted) path.lineTo(x, y) else path.moveTo(x, y)
                    pathStarted = true
                }
            }
            drawPath(
                path,
                color = lineColor,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 5f, cap = StrokeCap.Round)
            )
            entries.forEachIndexed { index, entry ->
                entry.usage?.let { value ->
                    val x = if (entries.size <= 1) chartWidth / 2f else chartWidth * index / (entries.size - 1f)
                    val y = chartHeight - (value / maxValue * chartHeight)
                    val center = androidx.compose.ui.geometry.Offset(x, y)
                    drawCircle(color = lineColor, radius = 5f, center = center)
                    drawCircle(color = Color.White, radius = 2.2f, center = center)
                }
            }
        }
        if (entries.isNotEmpty()) {
            val middle = entries[entries.lastIndex / 2]
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatter.format(Date(entries.first().timestamp)), color = colors.campusSlateColor, fontSize = 10.sp)
                Text(formatter.format(Date(middle.timestamp)), color = colors.campusSlateColor, fontSize = 10.sp)
                Text(formatter.format(Date(entries.last().timestamp)), color = colors.campusSlateColor, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun ElectricityResult(state: ElectronicContract.State) {
    val colors = AppTheme.colors

    val eleState = remember(state.elec, state.isElec) {
        if (!state.isElec) EleState.UNKNOWN
        else {
            val regex = Regex("(\\d*\\.?\\d+)")
            val match = regex.find(state.elec)
            val value = match?.value?.toFloatOrNull()
            when {
                value == null -> if (state.elec == "无数据" || state.elec == "查询失败") EleState.ERROR else EleState.UNKNOWN
                value in 0.0f..20f -> EleState.LOW
                value in 20.1f..100f -> EleState.NORMAL
                value > 100f -> EleState.HIGH
                else -> EleState.UNKNOWN
            }
        }
    }

    val displayValue = remember(state.elec, state.isElec) {
        if (!state.isElec) "--"
        else {
            val regex = Regex("(\\d*\\.?\\d+)")
            val match = regex.find(state.elec)
            match?.value ?: when (state.elec) {
                "无数据" -> "无数据"
                "查询失败" -> "查询失败"
                else -> "--"
            }
        }
    }

    val stateText = when (eleState) {
        EleState.LOW -> "电量过低"
        EleState.NORMAL -> "电量正常"
        EleState.HIGH -> "电量充足"
        EleState.UNKNOWN -> "状态未知"
        EleState.ERROR -> state.elec
    }

    Column(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        Text(
            text = "当前电量",
            fontSize = 13.sp,
            fontWeight = FontWeight.W500,
            color = colors.campusSlateColor
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(
                    when (eleState) {
                        EleState.LOW -> R.drawable.ic_electricity_none
                        EleState.NORMAL -> R.drawable.ic_electricity_low
                        EleState.HIGH -> R.drawable.ic_electricity_high
                        EleState.UNKNOWN -> R.drawable.ic_electricity_default
                        EleState.ERROR -> R.drawable.ic_electricity_default
                    }
                ),
                contentDescription = stateText,
                modifier = Modifier.size(50.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.width(40.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = displayValue,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.W600,
                    color = colors.campusInkColor,
                    letterSpacing = (-0.5).sp,
                    style = TextStyle(fontFeatureSettings = "\"tnum\"")
                )
                if (eleState != EleState.ERROR && eleState != EleState.UNKNOWN) {
                    Text(
                        text = " kWh",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.W500,
                        color = colors.campusSlateColor,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Surface(
                shape = RoundedCornerShape(999.dp),
                color = when (eleState) {
                    EleState.LOW -> colors.campusCoralColor.copy(alpha = 0.12f)
                    EleState.NORMAL -> colors.campusAmberColor.copy(alpha = 0.12f)
                    EleState.HIGH -> colors.campusMintColor.copy(alpha = 0.12f)
                    EleState.UNKNOWN -> colors.campusCloudColor
                    EleState.ERROR -> colors.campusCloudColor
                }
            ) {
                Text(
                    text = stateText,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.W600,
                    color = when (eleState) {
                        EleState.LOW -> colors.campusCoralColor
                        EleState.NORMAL -> colors.campusAmberColor
                        EleState.HIGH -> colors.campusMintColor
                        EleState.UNKNOWN -> colors.campusSlateColor
                        EleState.ERROR -> colors.campusSlateColor
                    }
                )
            }
        }
    }
}

private fun processDormAndRoom(dor: String, doorNumber: String): String {
    val containsA = dor.contains('A')
    val containsB = dor.contains('B')
    val doorContainsLetter = doorNumber.any { it.isLetter() }
    return when {
        containsA && !doorContainsLetter -> "A$doorNumber"
        containsB && !doorContainsLetter -> "B$doorNumber"
        else -> doorNumber
    }
}

private fun refreshWidget() {
    val appWidgetManager = AppWidgetManager.getInstance(PlanetApplication.appContext)
    val componentName = ComponentName(PlanetApplication.appContext, ElectronicAppWidget::class.java)
    val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

    if (appWidgetIds.isNotEmpty()) {
        val intent = Intent(PlanetApplication.appContext, ElectronicAppWidget::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
        }
        PlanetApplication.appContext.sendBroadcast(intent)
    }
}

@Composable
private fun ReminderDialog(
    onDismiss: () -> Unit,
    onEnableNeedsPermission: () -> Unit
) {
    val context = LocalContext.current
    val colors = AppTheme.colors
    var daily by remember { mutableStateOf(ElectricityReminderPrefs.dailyEnabled) }
    var hour by remember { mutableIntStateOf(ElectricityReminderPrefs.hour) }
    var minute by remember { mutableIntStateOf(ElectricityReminderPrefs.minute) }
    var low by remember { mutableStateOf(ElectricityReminderPrefs.lowEnabled) }
    var thresholdText by remember {
        mutableStateOf(
            ElectricityReminderPrefs.lowThreshold.let { if (it % 1f == 0f) it.toInt().toString() else it.toString() }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.bgCardColor,
        title = { Text("电量提醒", fontWeight = FontWeight.W700, color = colors.campusInkColor) },
        confirmButton = {
            TextButton(onClick = {
                ElectricityReminderPrefs.dailyEnabled = daily
                ElectricityReminderPrefs.hour = hour
                ElectricityReminderPrefs.minute = minute
                ElectricityReminderPrefs.lowEnabled = low
                thresholdText.toFloatOrNull()?.let { ElectricityReminderPrefs.lowThreshold = it }
                ElectricityReminder.ensureChannel(context)
                ElectricityReminder.reschedule(context)
                onDismiss()
            }) { Text("完成", color = colors.campusSkyBlueColor, fontWeight = FontWeight.W700) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = colors.campusSlateColor) }
        },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("每日提醒", color = colors.campusInkColor, fontSize = 15.sp, fontWeight = FontWeight.W600)
                        Text("每天到点推送当前剩余电量", color = colors.campusSlateColor, fontSize = 12.sp)
                    }
                    Switch(checked = daily, onCheckedChange = { c -> daily = c; if (c) onEnableNeedsPermission() })
                }
                if (daily) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                android.app.TimePickerDialog(
                                    context,
                                    { _, h, m -> hour = h; minute = m },
                                    hour, minute, true
                                ).show()
                            }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("提醒时间", color = colors.campusInkColor, fontSize = 14.sp)
                        Text(
                            String.format(Locale.CHINA, "%02d:%02d", hour, minute),
                            color = colors.campusSkyBlueColor, fontSize = 15.sp, fontWeight = FontWeight.W700
                        )
                    }
                }

                Spacer(Modifier.height(6.dp))
                HorizontalDivider(color = colors.campusDividerColor)
                Spacer(Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("低电量预警", color = colors.campusInkColor, fontSize = 15.sp, fontWeight = FontWeight.W600)
                        Text("低于阈值时额外提醒", color = colors.campusSlateColor, fontSize = 12.sp)
                    }
                    Switch(checked = low, onCheckedChange = { c -> low = c; if (c) onEnableNeedsPermission() })
                }
                if (low) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("预警阈值（度）", color = colors.campusInkColor, fontSize = 14.sp)
                        OutlinedTextField(
                            value = thresholdText,
                            onValueChange = { s -> if (s.matches(Regex("^\\d{0,3}(\\.\\d?)?$"))) thresholdText = s },
                            singleLine = true,
                            modifier = Modifier.width(110.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun BindingSummaryButton(
    schoolText: String,
    dormText: String,
    roomText: String,
    onClick: () -> Unit
) {
    val colors = AppTheme.colors
    val isBound = schoolText != "选择校区" && dormText != "选择宿舍楼" && roomText.isNotEmpty()
    val summary = if (isBound) {
        "$schoolText · $dormText · $roomText"
    } else {
        "点击绑定寝室，查询更方便"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color.Black.copy(alpha = 0.04f),
                spotColor = Color.Black.copy(alpha = 0.04f)
            )
            .clip(RoundedCornerShape(16.dp))
            .background(colors.bgCardColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (isBound) "已绑定寝室" else "未绑定寝室",
                fontSize = 12.sp,
                fontWeight = FontWeight.W500,
                color = colors.campusSlateColor
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = summary,
                fontSize = 15.sp,
                fontWeight = FontWeight.W600,
                color = if (isBound) colors.campusInkColor else colors.campusMistColor,
                maxLines = 1
            )
        }
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(colors.campusSkyBlueLightColor)
                .border(1.dp, colors.campusSkyBlueColor.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isBound) "修改" else "去绑定",
                color = colors.campusSkyBlueColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.W600
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DormBindingSheet(
    state: ElectronicContract.State,
    viewModel: ElectronicViewModel,
    initialSchool: String,
    initialDorm: String,
    initialRoom: String,
    schoolList: List<String>,
    dormList: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (school: String, dorm: String, room: String) -> Unit
) {
    val colors = AppTheme.colors
    val focusManager = LocalFocusManager.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var schoolText by remember { mutableStateOf(initialSchool) }
    var dormText by remember { mutableStateOf(initialDorm) }
    var roomText by remember { mutableStateOf(initialRoom) }
    var lastRoomSelectionContext by remember(initialSchool, initialDorm) {
        mutableStateOf(initialSchool to initialDorm)
    }
    var showSchoolSheet by remember { mutableStateOf(false) }
    var showDormSheet by remember { mutableStateOf(false) }
    var showRoomSheet by remember { mutableStateOf(false) }

    // 校区或楼栋变化后自动拉取房间号列表
    LaunchedEffect(schoolText, dormText) {
        val nextContext = schoolText to dormText
        if (shouldClearRoomForBindingChange(lastRoomSelectionContext, nextContext)) {
            roomText = ""
        }
        lastRoomSelectionContext = nextContext
        if (schoolText != "选择校区" && dormText != "选择宿舍楼") {
            viewModel.processIntent(ElectronicContract.Intent.LoadRooms(schoolText, dormText))
        }
    }

    if (showSchoolSheet) {
        SelectionBottomSheet(
            items = schoolList,
            onDismiss = { showSchoolSheet = false },
            onSelect = { selected ->
                schoolText = selected
                dormText = "选择宿舍楼"
            }
        )
    }

    if (showDormSheet) {
        val filteredDorms = remember(schoolText) {
            when (schoolText) {
                "云塘校区" -> dormList.subList(0, minOf(45, dormList.size))
                "金盆岭校区" -> dormList.subList(minOf(45, dormList.size), dormList.size)
                else -> dormList
            }
        }
        SelectionBottomSheet(
            items = filteredDorms,
            onDismiss = { showDormSheet = false },
            onSelect = { selected ->
                dormText = selected
            }
        )
    }

    if (showRoomSheet) {
        SelectionBottomSheet(
            items = state.availableRooms,
            onDismiss = { showRoomSheet = false },
            onSelect = { selected ->
                roomText = selected
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.campusSnowColor,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        dragHandle = { BottomSheetDefaults.DragHandle(color = colors.campusMistColor) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = "绑定寝室",
                fontSize = 18.sp,
                fontWeight = FontWeight.W700,
                color = colors.campusInkColor,
                modifier = Modifier.padding(top = 8.dp, bottom = 18.dp)
            )

            CampusCard {
                SelectorRow(
                    label = "校区",
                    value = schoolText,
                    onClick = {
                        focusManager.clearFocus()
                        showSchoolSheet = true
                    }
                )

                CampusDivider()

                SelectorRow(
                    label = "宿舍楼",
                    value = dormText,
                    onClick = {
                        focusManager.clearFocus()
                        showDormSheet = true
                    }
                )

                CampusDivider()

                RoomSelectorRow(
                    label = "房间号",
                    value = roomText,
                    placeholder = when {
                        schoolText == "选择校区" || dormText == "选择宿舍楼" -> "请先选择校区和楼栋"
                        state.isLoadingRooms -> "正在加载房间号..."
                        state.availableRooms.isEmpty() -> "暂无房间列表，请手动输入"
                        else -> "选择房间号"
                    },
                    isLoading = state.isLoadingRooms,
                    enabled = schoolText != "选择校区" && dormText != "选择宿舍楼" && !state.isLoadingRooms,
                    onClick = {
                        focusManager.clearFocus()
                        if (state.availableRooms.isNotEmpty()) {
                            showRoomSheet = true
                        }
                    },
                    onManualInput = { roomText = it },
                    allowManualInput = state.availableRooms.isEmpty()
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "取消",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.W600,
                        color = colors.campusSlateColor
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Button(
                    onClick = {
                        if (schoolText != "选择校区" && dormText != "选择宿舍楼" && roomText.isNotEmpty()) {
                            onConfirm(schoolText, dormText, roomText)
                            onDismiss()
                        }
                    },
                    modifier = Modifier
                        .weight(1.5f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.campusSkyBlueColor,
                        contentColor = Color.White,
                        disabledContainerColor = colors.campusSkyBlueColor.copy(alpha = 0.4f),
                        disabledContentColor = Color.White.copy(alpha = 0.7f)
                    ),
                    enabled = schoolText != "选择校区" && dormText != "选择宿舍楼" && roomText.isNotEmpty()
                ) {
                    Text(
                        text = "确认绑定",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.W600
                    )
                }
            }

            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}

internal fun shouldClearRoomForBindingChange(
    previousContext: Pair<String, String>,
    nextContext: Pair<String, String>
): Boolean = previousContext != nextContext

@Composable
private fun RoomSelectorRow(
    label: String,
    value: String,
    placeholder: String,
    isLoading: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    onManualInput: (String) -> Unit,
    allowManualInput: Boolean
) {
    val colors = AppTheme.colors

    if (allowManualInput) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 15.sp,
                fontWeight = FontWeight.W500,
                color = colors.campusInkColor
            )
            val interactionSource = remember { MutableInteractionSource() }
            val textFieldColors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = colors.campusDividerColor,
                focusedBorderColor = colors.campusSkyBlueColor,
                unfocusedContainerColor = colors.campusSkyBlueGhostColor,
                focusedContainerColor = colors.campusSkyBlueGhostColor,
                cursorColor = colors.campusSkyBlueColor
            )
            BasicTextField(
                value = value,
                onValueChange = { newText: String ->
                    if (ALPHANUMERIC_REGEX.matches(newText)) {
                        onManualInput(newText)
                    }
                },
                modifier = Modifier
                    .width(160.dp)
                    .height(48.dp),
                textStyle = TextStyle(
                    fontSize = 14.sp,
                    color = colors.campusInkColor,
                    fontWeight = FontWeight.W500,
                    fontFeatureSettings = "\"tnum\""
                ),
                cursorBrush = SolidColor(colors.campusSkyBlueColor),
                singleLine = true,
                interactionSource = interactionSource,
                decorationBox = { innerTextField ->
                    OutlinedTextFieldDefaults.DecorationBox(
                        value = value,
                        innerTextField = innerTextField,
                        enabled = true,
                        singleLine = true,
                        visualTransformation = VisualTransformation.None,
                        interactionSource = interactionSource,
                        placeholder = {
                            Text(
                                placeholder,
                                color = colors.campusMistColor,
                                fontSize = 14.sp
                            )
                        },
                        colors = textFieldColors,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        container = {
                            OutlinedTextFieldDefaults.Container(
                                enabled = true,
                                isError = false,
                                interactionSource = interactionSource,
                                colors = textFieldColors,
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    )
                }
            )
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled, onClick = onClick)
                .padding(horizontal = 18.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 15.sp,
                fontWeight = FontWeight.W500,
                color = colors.campusInkColor
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = colors.campusSkyBlueColor,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = value.ifEmpty { placeholder },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.W500,
                    color = if (value.isEmpty()) colors.campusMistColor else colors.campusInkColor,
                    modifier = Modifier.padding(end = 6.dp)
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint = colors.campusMistColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
