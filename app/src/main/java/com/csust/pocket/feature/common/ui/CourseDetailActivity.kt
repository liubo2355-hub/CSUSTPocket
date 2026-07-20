package com.csust.pocket.feature.common.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.csust.pocket.R
import com.csust.pocket.core.designsystem.HyperIconButton
import com.csust.pocket.core.designsystem.PortalBackButton
import com.csust.pocket.core.designsystem.HyperSurface
import com.csust.pocket.core.designsystem.hyperConcave
import com.csust.pocket.core.designsystem.hyperTap
import com.csust.pocket.core.theme.AppSkinTheme
import com.csust.pocket.core.theme.AppTheme
import com.csust.pocket.feature.common.ui.adapter.model.CourseScore
import com.csust.pocket.feature.common.ui.compose.GradeGreen
import com.csust.pocket.feature.common.ui.compose.colorForGrade
import com.csust.pocket.feature.common.ui.compose.colorForGradeText
import com.csust.pocket.feature.common.ui.compose.colorForPoint
import com.dcelysia.csust_spider.education.data.remote.EducationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

private data class CourseDetailData(
    val id: String, val semester: String, val name: String, val score: String,
    val point: Double, val credit: Double, val hours: String, val studyType: String,
    val nature: String, val attribute: String, val method: String,
    val examNature: String, val detailUrl: String?
)

private data class ScoreComponentUi(val name: String, val score: Double, val ratio: Int)

class CourseDetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val data = CourseDetailData(
            intent.getStringExtra(EXTRA_ID).orEmpty(), intent.getStringExtra(EXTRA_SEMESTER).orEmpty(),
            intent.getStringExtra(EXTRA_NAME).orEmpty(), intent.getStringExtra(EXTRA_SCORE).orEmpty(),
            intent.getDoubleExtra(EXTRA_POINT, 0.0), intent.getDoubleExtra(EXTRA_CREDIT, 0.0),
            intent.getStringExtra(EXTRA_HOURS).orEmpty(), intent.getStringExtra(EXTRA_STUDY_TYPE).orEmpty(),
            intent.getStringExtra(EXTRA_NATURE).orEmpty(), intent.getStringExtra(EXTRA_ATTRIBUTE).orEmpty(),
            intent.getStringExtra(EXTRA_METHOD).orEmpty(), intent.getStringExtra(EXTRA_EXAM_NATURE).orEmpty(),
            intent.getStringExtra(EXTRA_DETAIL_URL)
        )
        setContent { AppSkinTheme { CourseDetailScreen(data) { finish() } } }
    }

    companion object {
        private const val EXTRA_ID = "course_id"; private const val EXTRA_SEMESTER = "semester"
        private const val EXTRA_NAME = "name"; private const val EXTRA_SCORE = "score"
        private const val EXTRA_POINT = "point"; private const val EXTRA_CREDIT = "credit"
        private const val EXTRA_HOURS = "hours"; private const val EXTRA_STUDY_TYPE = "study_type"
        private const val EXTRA_NATURE = "nature"; private const val EXTRA_ATTRIBUTE = "attribute"
        private const val EXTRA_METHOD = "method"; private const val EXTRA_EXAM_NATURE = "exam_nature"
        private const val EXTRA_DETAIL_URL = "detail_url"

        fun open(context: Context, course: CourseScore) {
            context.startActivity(Intent(context, CourseDetailActivity::class.java).apply {
                putExtra(EXTRA_ID, course.id); putExtra(EXTRA_SEMESTER, course.semester)
                putExtra(EXTRA_NAME, course.name); putExtra(EXTRA_SCORE, course.scoreText)
                putExtra(EXTRA_POINT, course.earnedCredit); putExtra(EXTRA_CREDIT, course.credit)
                putExtra(EXTRA_HOURS, course.totalHours); putExtra(EXTRA_STUDY_TYPE, course.studyType)
                putExtra(EXTRA_NATURE, course.courseNature); putExtra(EXTRA_ATTRIBUTE, course.courseType)
                putExtra(EXTRA_METHOD, course.assessmentMethod); putExtra(EXTRA_EXAM_NATURE, course.examNature)
                putExtra(EXTRA_DETAIL_URL, course.pscjUrl)
            })
        }
    }
}

@Composable
private fun CourseDetailScreen(data: CourseDetailData, onBack: () -> Unit) {
    val colors = AppTheme.colors
    var refreshKey by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var components by remember { mutableStateOf<List<ScoreComponentUi>>(emptyList()) }
    LaunchedEffect(data.detailUrl, refreshKey) {
        val url = data.detailUrl
        if (url.isNullOrBlank()) { message = "该课程暂无平时成绩明细"; return@LaunchedEffect }
        loading = true; message = null
        val result = withContext(Dispatchers.IO) { runCatching { EducationHelper.getGradeDetail(url) }.getOrNull() }
        if (result?.code == "200") {
            components = result.data?.components.orEmpty().map { ScoreComponentUi(it.type, it.grade, it.ratio) }
            message = if (components.isEmpty()) "该课程暂无成绩构成数据" else null
        } else message = "成绩构成暂时无法加载，可稍后刷新"
        loading = false
    }

    Column(Modifier.fillMaxSize().background(colors.bgPrimaryColor).statusBarsPadding()) {
        HyperSurface(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
            Row(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                PortalBackButton(onClick = onBack)
                Text("课程成绩详情", Modifier.weight(1f).padding(start = 12.dp), fontSize = 19.sp, fontWeight = FontWeight.Bold)
                HyperIconButton(onClick = { refreshKey++ }, modifier = Modifier.size(44.dp)) {
                    Icon(painterResource(R.drawable.coursetable_ic_refresh), "刷新成绩明细", Modifier.size(21.dp), tint = colors.functionalTextColor)
                }
            }
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(data.name, fontSize = 27.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            SummaryMetrics(data)
            ScoreCompositionCard(data.score, components, loading, message)
            CourseInformationCard(data)
            Spacer(Modifier.height(22.dp))
        }
    }
}

@Composable
private fun SummaryMetrics(data: CourseDetailData) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        SummaryMetric("总成绩", data.score.ifBlank { "--" }, colorForGradeText(data.score, GradeGreen), Modifier.weight(1f))
        SummaryMetric("绩点", format(data.point), colorForPoint(data.point), Modifier.weight(1f))
        SummaryMetric("学分", format(data.credit), AppTheme.colors.primaryTextColor, Modifier.weight(1f))
        SummaryMetric("学时", data.hours.ifBlank { "--" }, AppTheme.colors.primaryTextColor, Modifier.weight(1f))
    }
}

@Composable
private fun SummaryMetric(label: String, value: String, color: Color, modifier: Modifier) {
    Column(modifier.hyperConcave(16.dp).padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = color, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(label, color = AppTheme.colors.secondaryTextColor, fontSize = 11.sp)
    }
}

@Composable
private fun ScoreCompositionCard(total: String, parts: List<ScoreComponentUi>, loading: Boolean, message: String?) {
    val colors = AppTheme.colors
    var mode by rememberSaveable { mutableIntStateOf(0) }
    HyperSurface(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("成绩构成", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("所有分数均来自教务系统明细", fontSize = 11.sp, color = colors.secondaryTextColor)
                }
                Row(Modifier.hyperConcave(12.dp).padding(3.dp)) {
                    listOf("饼图", "进度条").forEachIndexed { index, label ->
                        Text(label, Modifier.then(if (mode == index) Modifier.background(colors.bgCardColor, RoundedCornerShape(9.dp)) else Modifier)
                            .hyperTap { mode = index }.padding(horizontal = 10.dp, vertical = 7.dp),
                            color = if (mode == index) colors.functionalTextColor else colors.secondaryTextColor,
                            fontSize = 11.sp, fontWeight = if (mode == index) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            when {
                loading -> Box(Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(30.dp), color = colors.functionalTextColor, strokeWidth = 3.dp)
                }
                parts.isEmpty() -> Box(Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                    Text(message ?: "暂无成绩构成", color = colors.secondaryTextColor, fontSize = 13.sp)
                }
                mode == 0 -> ScoreDonut(total, parts)
                else -> ScoreProgress(parts, total)
            }
        }
    }
}

@Composable
private fun ScoreDonut(total: String, parts: List<ScoreComponentUi>) {
    val colors = AppTheme.colors
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Box(Modifier.size(168.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize().padding(14.dp)) {
                var start = -90f; val ratioTotal = parts.sumOf { it.ratio }.coerceAtLeast(1)
                parts.forEach { part ->
                    val sweep = part.ratio * 360f / ratioTotal
                    drawArc(colorForGrade(part.score), start, sweep - 1.2f, false,
                        style = Stroke(width = 34.dp.toPx(), cap = StrokeCap.Butt)); start += sweep
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(total, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = colorForGradeText(total, colors.primaryTextColor))
                Text("总成绩", fontSize = 11.sp, color = colors.secondaryTextColor)
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            parts.forEach { part ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).background(colorForGrade(part.score), CircleShape))
                    Column(Modifier.weight(1f).padding(start = 8.dp)) {
                        Text(part.name, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        Text("占比 ${part.ratio}%", fontSize = 10.sp, color = colors.secondaryTextColor)
                    }
                    Text(scoreText(part.score), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = colorForGrade(part.score))
                }
            }
        }
    }
}

@Composable
private fun ScoreProgress(parts: List<ScoreComponentUi>, total: String) {
    val colors = AppTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(15.dp)) {
        parts.forEach { part ->
            Column {
                Row {
                    Text(part.name, Modifier.weight(1f), fontSize = 12.sp, color = colors.secondaryTextColor)
                    Text("${scoreText(part.score)}/100 · ${part.ratio}%", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = colorForGrade(part.score))
                }
                Spacer(Modifier.height(6.dp))
                ProgressTrack((part.score / 100.0).toFloat(), colorForGrade(part.score))
            }
        }
        Column {
            Row {
                Text("总成绩", Modifier.weight(1f), fontSize = 12.sp, color = colors.secondaryTextColor)
                Text("$total/100", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = colorForGradeText(total, colors.primaryTextColor))
            }
            Spacer(Modifier.height(6.dp)); ProgressTrack(total.toFloatOrNull()?.div(100f) ?: 0f, colorForGradeText(total, GradeGreen))
        }
    }
}

@Composable
private fun ProgressTrack(progress: Float, color: Color) {
    Box(Modifier.fillMaxWidth().height(9.dp).hyperConcave(999.dp)) {
        Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).height(9.dp).background(color, RoundedCornerShape(999.dp)))
    }
}

@Composable
private fun CourseInformationCard(data: CourseDetailData) {
    val rows = listOf("课程编号" to data.id, "开课学期" to data.semester, "修读方式" to data.studyType,
        "课程性质" to data.nature, "课程属性" to data.attribute, "考核方式" to data.method, "考试性质" to data.examNature)
    HyperSurface(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            rows.forEachIndexed { index, (label, value) ->
                Row(Modifier.fillMaxWidth().padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(label, Modifier.weight(1f), color = AppTheme.colors.secondaryTextColor, fontSize = 13.sp)
                    Text(value.ifBlank { "—" }, color = AppTheme.colors.primaryTextColor, fontSize = 13.sp,
                        fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                if (index < rows.lastIndex) Box(Modifier.fillMaxWidth().height(1.dp).background(AppTheme.colors.outlineLowContrastColor.copy(alpha = .25f)))
            }
        }
    }
}

private fun format(value: Double) = String.format(Locale.CHINA, "%.1f", value)
private fun scoreText(value: Double) = if (value % 1.0 == 0.0) value.toInt().toString() else String.format(Locale.CHINA, "%.1f", value)
