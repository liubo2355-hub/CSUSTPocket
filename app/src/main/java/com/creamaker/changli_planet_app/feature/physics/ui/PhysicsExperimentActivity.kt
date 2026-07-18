package com.creamaker.changli_planet_app.feature.physics.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creamaker.changli_planet_app.core.theme.AppSkinTheme
import com.creamaker.changli_planet_app.core.designsystem.PortalBackButton
import com.creamaker.changli_planet_app.core.theme.AppTheme
import com.creamaker.changli_planet_app.feature.physics.data.PhysicsCourse
import com.creamaker.changli_planet_app.feature.physics.data.PhysicsExperimentRepository
import com.creamaker.changli_planet_app.feature.physics.data.PhysicsGrade
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

class PhysicsExperimentActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val initialPage = intent.getStringExtra(EXTRA_PAGE) ?: PAGE_SCHEDULE
        setContent { AppSkinTheme { PhysicsExperimentScreen(initialPage) { finish() } } }
    }

    companion object {
        const val EXTRA_PAGE = "page"
        const val PAGE_SCHEDULE = "schedule"
        const val PAGE_GRADE = "grade"
    }
}

@Composable
private fun PhysicsExperimentScreen(initialPage: String, onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val repository = remember { PhysicsExperimentRepository(context.applicationContext) }
    val scope = rememberCoroutineScope()
    var page by remember { mutableStateOf(initialPage) }
    var courses by remember { mutableStateOf(repository.cachedCourses()) }
    var grades by remember { mutableStateOf(repository.cachedGrades()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var showLogin by remember { mutableStateOf(repository.savedUsername.isBlank()) }

    fun refresh() = scope.launch {
        loading = true
        error = null
        runCatching {
            if (page == PhysicsExperimentActivity.PAGE_SCHEDULE) courses = repository.getCourses()
            else grades = repository.getGrades()
        }.onFailure {
            error = it.message ?: "刷新失败"
            if (error?.contains("登录") == true) showLogin = true
        }
        loading = false
    }
    LaunchedEffect(page) { if ((page == PhysicsExperimentActivity.PAGE_SCHEDULE && courses.isEmpty()) || (page == PhysicsExperimentActivity.PAGE_GRADE && grades.isEmpty())) refresh() }

    Column(Modifier.fillMaxSize().background(AppTheme.colors.bgPrimaryColor).statusBarsPadding()) {
        FloatingHeader(
            title = if (page == PhysicsExperimentActivity.PAGE_SCHEDULE) "大物实验安排" else "大物实验成绩",
            subtitle = if (page == PhysicsExperimentActivity.PAGE_SCHEDULE) "共 ${courses.size} 个实验" else "共 ${grades.size} 项成绩",
            loading = loading,
            onBack = onBack,
            onLogin = { showLogin = true },
            onRefresh = ::refresh
        )
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Segment("实验安排", page == PhysicsExperimentActivity.PAGE_SCHEDULE, Modifier.weight(1f)) { page = PhysicsExperimentActivity.PAGE_SCHEDULE }
            Segment("实验成绩", page == PhysicsExperimentActivity.PAGE_GRADE, Modifier.weight(1f)) { page = PhysicsExperimentActivity.PAGE_GRADE }
        }
        error?.let { Text(it, color = Color(0xFFD74646), modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) }
        if (page == PhysicsExperimentActivity.PAGE_SCHEDULE) ScheduleList(courses) else GradeList(grades)
    }

    if (showLogin) LoginDialog(repository, onDismiss = { showLogin = false }, onSuccess = { showLogin = false; refresh() })
}

@Composable
private fun FloatingHeader(title: String, subtitle: String, loading: Boolean, onBack: () -> Unit, onLogin: () -> Unit, onRefresh: () -> Unit) {
    Surface(Modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(22.dp), color = AppTheme.colors.bgCardColor, shadowElevation = 6.dp) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            PortalBackButton(onClick = onBack, tint = Color(0xFF168FD0))
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, fontSize = 12.sp, color = AppTheme.colors.secondaryTextColor)
            }
            Text("登录", color = Color(0xFF168FD0), modifier = Modifier.clickable(onClick = onLogin).padding(8.dp))
            if (loading) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
            else Text("↻", fontSize = 30.sp, color = Color(0xFF168FD0), modifier = Modifier.clickable(onClick = onRefresh).padding(6.dp))
        }
    }
}

@Composable
private fun Segment(label: String, selected: Boolean, modifier: Modifier, action: () -> Unit) {
    Surface(modifier.clickable(onClick = action), shape = RoundedCornerShape(14.dp), color = if (selected) Color(0xFF169BD5) else AppTheme.colors.bgCardColor) {
        Text(label, color = if (selected) Color.White else AppTheme.colors.primaryTextColor, fontWeight = FontWeight.Bold, modifier = Modifier.padding(11.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@Composable
private fun ScheduleList(items: List<PhysicsCourse>) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (items.isEmpty()) EmptyCard("暂无实验安排", "没有找到任何大学物理实验安排信息")
        items.sortedBy { it.startTime }.forEach { course ->
            val finished = System.currentTimeMillis() > course.endTime
            val days = max(0, ((course.startTime - System.currentTimeMillis()) / 86_400_000L).toInt())
            Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = AppTheme.colors.bgCardColor, shadowElevation = 3.dp) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Row(verticalAlignment = Alignment.Top) {
                        Text(course.name, fontSize = 19.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        StatusPill(if (finished) "已结束" else if (days == 0) "今天" else "还有 $days 天", finished)
                    }
                    HorizontalDivider(color = Color(0x18000000))
                    Text("▣  ${formatCourseTime(course)}", color = Color(0xFF268EC5))
                    Text("●  ${course.location}", color = Color(0xFFDD5148))
                    Row { Text("♟  ${course.teacher}", modifier = Modifier.weight(1f)); Text("◷  ${course.classHours} 课时") }
                    Text("批次 ${course.batch}", fontSize = 12.sp, color = Color(0xFFE58A22))
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun GradeList(items: List<PhysicsGrade>) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (items.isEmpty()) EmptyCard("暂无成绩信息", "没有找到任何大学物理实验成绩信息")
        items.forEach { grade ->
            Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = AppTheme.colors.bgCardColor, shadowElevation = 3.dp) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(grade.itemName, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                        GradeValue("预习", grade.previewGrade, Color(0xFF8D5BC1)); GradeValue("操作", grade.operationGrade, Color(0xFF278FC5)); GradeValue("报告", grade.reportGrade, Color(0xFF31A762))
                    }
                    HorizontalDivider(color = Color(0x18000000))
                    Row(verticalAlignment = Alignment.CenterVertically) { Text("★  总成绩", modifier = Modifier.weight(1f)); Text("${grade.totalGrade}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = gradeColor(grade.totalGrade)) }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable private fun GradeValue(label: String, value: Int?, color: Color) = Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(label, fontSize = 12.sp, color = AppTheme.colors.secondaryTextColor); Text(value?.toString() ?: "-", fontWeight = FontWeight.Bold, color = color) }
@Composable private fun StatusPill(text: String, finished: Boolean) = Surface(shape = RoundedCornerShape(50), color = if (finished) Color(0xFFE9E9EC) else Color(0x1A168FD0)) { Text(text, fontSize = 12.sp, color = if (finished) Color.Gray else Color(0xFF168FD0), modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)) }
@Composable private fun EmptyCard(title: String, description: String) = Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = AppTheme.colors.bgCardColor) { Column(Modifier.padding(30.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text("⚗", fontSize = 38.sp); Text(title, fontWeight = FontWeight.Bold); Text(description, color = AppTheme.colors.secondaryTextColor, fontSize = 13.sp) } }

@Composable
private fun LoginDialog(repository: PhysicsExperimentRepository, onDismiss: () -> Unit, onSuccess: () -> Unit) {
    var username by remember { mutableStateOf(repository.savedUsername) }
    var password by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("登录大物实验") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("登录信息", color = Color(0xFF168FD0), fontWeight = FontWeight.Bold)
            OutlinedTextField(username, { username = it }, label = { Text("用户名") }, singleLine = true)
            OutlinedTextField(password, { password = it }, label = { Text("密码") }, singleLine = true, visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), trailingIcon = { Text(if (visible) "隐藏" else "显示", modifier = Modifier.clickable { visible = !visible }) })
            Text("需要连接校园网才能访问物理实验教学管理平台。登录信息仅保存在本机。", fontSize = 12.sp, color = AppTheme.colors.secondaryTextColor)
            error?.let { Text(it, color = Color(0xFFD74646), fontSize = 13.sp) }
        } },
        confirmButton = { TextButton(enabled = username.isNotBlank() && password.isNotBlank() && !loading, onClick = { scope.launch { loading = true; error = null; runCatching { repository.login(username, password) }.onSuccess { onSuccess() }.onFailure { error = it.message ?: "登录失败" }; loading = false } }) { Text(if (loading) "登录中…" else "登录") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("关闭") } }
    )
}

private fun formatCourseTime(course: PhysicsCourse): String {
    val date = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(Date(course.startTime))
    val time = SimpleDateFormat("HH:mm", Locale.CHINA)
    return "$date ${course.dayOfWeek} ${time.format(Date(course.startTime))}-${time.format(Date(course.endTime))}"
}
private fun gradeColor(score: Int) = when { score >= 90 -> Color(0xFF35A854); score >= 60 -> Color(0xFFDA991D); else -> Color(0xFFD64747) }
