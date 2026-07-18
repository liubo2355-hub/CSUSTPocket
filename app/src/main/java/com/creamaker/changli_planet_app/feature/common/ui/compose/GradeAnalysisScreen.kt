package com.creamaker.changli_planet_app.feature.common.ui.compose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creamaker.changli_planet_app.core.designsystem.HyperSurface
import com.creamaker.changli_planet_app.core.designsystem.hyperConcave
import com.creamaker.changli_planet_app.core.designsystem.hyperTap
import com.creamaker.changli_planet_app.core.theme.AppTheme
import com.creamaker.changli_planet_app.feature.common.data.local.entity.Grade
import com.creamaker.changli_planet_app.feature.common.data.local.mmkv.ScoreCache
import java.util.Locale
import kotlin.math.ceil

// 成绩 / 绩点动态配色见 GradeColor.kt（colorForGrade / colorForPoint / gradePointToRange）

// ---------------------------------------------------------------------------
// 数据模型（移植自 GradeAnalysisData.fromCourseGrades）
// ---------------------------------------------------------------------------

private data class GradeAnalytics(
    val totalCourses: Int,
    val totalHours: Double,
    val totalCredits: Double,
    val overallAverageGrade: Double,   // 平均成绩：简单平均
    val overallGPA: Double,            // 平均绩点：学分加权
    val weightedAverageGrade: Double,  // 加权平均成绩：学分加权
    val gradePointDistribution: List<Pair<Double, Int>>, // (绩点, 门数) 按绩点降序
    val semesterAverageGrades: List<Pair<String, Double>>, // (学期, 平均成绩) 按学期升序
    val semesterGPAs: List<Pair<String, Double>>           // (学期, GPA) 按学期升序
)

@Composable
fun GradeAnalysisScreen() {
    val rawGrades = remember { ScoreCache.getGrades().orEmpty() }
    val grades = remember(rawGrades) { rawGrades.filter { it.name.isNotBlank() } }

    if (grades.isEmpty()) {
        EmptyGrades()
        return
    }

    val data = remember(grades) { buildAnalytics(grades) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item { SummaryCard(data) }
        item { SemesterChartSection(data) }
        item { DistributionSection(data) }
        item { Spacer(Modifier.height(18.dp)) }
    }
}

// ---------------------------------------------------------------------------
// 学习总览卡
// ---------------------------------------------------------------------------

@Composable
private fun SummaryCard(data: GradeAnalytics) {
    val colors = AppTheme.colors
    HyperSurface(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("学习总览", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = colors.primaryTextColor)
            Row(Modifier.fillMaxWidth()) {
                StatItem("课程总数", data.totalCourses.toString(), GradePurple, Modifier.weight(1f))
                StatItem("总学分", fmt(data.totalCredits, 1), GradeBlue, Modifier.weight(1f))
                StatItem("总学时", fmt(data.totalHours, 1), GradeRed, Modifier.weight(1f))
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(colors.dividerColor))
            Row(Modifier.fillMaxWidth()) {
                StatItem("平均成绩", fmt(data.overallAverageGrade, 2), colorForGrade(data.overallAverageGrade), Modifier.weight(1f))
                StatItem("加权平均成绩", fmt(data.weightedAverageGrade, 2), colorForGrade(data.weightedAverageGrade), Modifier.weight(1f))
                StatItem("平均绩点", fmt(data.overallGPA, 2), colorForPoint(data.overallGPA), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StatItem(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, fontSize = 12.sp, color = AppTheme.colors.secondaryTextColor)
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

// ---------------------------------------------------------------------------
// 学期平均成绩 / GPA 折线图
// ---------------------------------------------------------------------------

@Composable
private fun SemesterChartSection(data: GradeAnalytics) {
    val colors = AppTheme.colors
    var metric by rememberSaveable { mutableIntStateOf(0) } // 0 平均成绩, 1 GPA
    HyperSurface(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("学期平均成绩/GPA", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = colors.primaryTextColor, modifier = Modifier.weight(1f))
                Segmented(listOf("平均成绩", "GPA"), metric) { metric = it }
            }
            Spacer(Modifier.height(16.dp))
            val series = if (metric == 0) data.semesterAverageGrades else data.semesterGPAs
            LineChart(
                labels = series.map { shortTerm(it.first) },
                values = series.map { it.second },
                yMax = if (metric == 0) 100.0 else 5.0,
                yStep = if (metric == 0) 20.0 else 1.0,
                valueDecimals = if (metric == 0) 1 else 2,
                colorFn = if (metric == 0) ::colorForGrade else ::colorForPoint
            )
        }
    }
}

@Composable
private fun LineChart(
    labels: List<String>,
    values: List<Double>,
    yMax: Double,
    yStep: Double,
    valueDecimals: Int,
    colorFn: (Double) -> Color
) {
    val grid = AppTheme.colors.outlineLowContrastColor.copy(alpha = .35f)
    val axisText = AppTheme.colors.secondaryTextColor
    val pointRing = AppTheme.colors.bgCardColor
    Canvas(Modifier.fillMaxWidth().height(230.dp)) {
        val leftPad = 6.dp.toPx()
        val rightPad = 30.dp.toPx()
        val topPad = 26.dp.toPx()
        val bottomPad = 22.dp.toPx()
        val chartW = size.width - leftPad - rightPad
        val chartH = size.height - topPad - bottomPad
        val steps = (yMax / yStep).toInt().coerceAtLeast(1)

        // 网格线 + 右侧 Y 轴刻度
        for (i in 0..steps) {
            val y = topPad + chartH * (1f - i.toFloat() / steps)
            drawLine(grid, Offset(leftPad, y), Offset(leftPad + chartW, y), strokeWidth = 1.dp.toPx())
            val v = yStep * i
            drawChartText(
                if (v % 1.0 == 0.0) v.toInt().toString() else fmt(v, 1),
                leftPad + chartW + 4.dp.toPx(), y + 3.5.dp.toPx(), axisText, 10f, android.graphics.Paint.Align.LEFT
            )
        }
        if (values.isEmpty()) return@Canvas

        val points = values.mapIndexed { index, value ->
            val x = if (values.size == 1) leftPad + chartW / 2f
            else leftPad + chartW * index / (values.size - 1f)
            val y = topPad + chartH * (1f - (value / yMax).coerceIn(0.0, 1.0).toFloat())
            Offset(x, y)
        }
        // 折线（相邻两点按各自颜色渐变）
        for (i in 1 until points.size) {
            drawLine(
                brush = Brush.linearGradient(
                    listOf(colorFn(values[i - 1]), colorFn(values[i])),
                    start = points[i - 1], end = points[i]
                ),
                start = points[i - 1], end = points[i],
                strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round
            )
        }
        // 数据点 + 数值标签 + X 轴学期
        points.forEachIndexed { index, p ->
            val c = colorFn(values[index])
            drawCircle(pointRing, 6.dp.toPx(), p)
            drawCircle(c, 4.dp.toPx(), p)

            val label = fmt(values[index], valueDecimals)
            val chipW = (label.length * 6.2f + 12f).dp.toPx()
            val chipH = 16.dp.toPx()
            val chipX = (p.x - chipW / 2).coerceIn(leftPad, leftPad + chartW - chipW)
            val chipY = (p.y - 9.dp.toPx() - chipH).coerceAtLeast(0f)
            drawRoundRect(
                color = c.copy(alpha = 0.2f),
                topLeft = Offset(chipX, chipY),
                size = Size(chipW, chipH),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )
            drawChartText(label, chipX + chipW / 2, chipY + chipH - 4.5.dp.toPx(), c, 10f, android.graphics.Paint.Align.CENTER, bold = true)

            drawChartText(labels.getOrElse(index) { "" }, p.x, size.height - 5.dp.toPx(), axisText, 9f, android.graphics.Paint.Align.CENTER)
        }
    }
}

// ---------------------------------------------------------------------------
// 绩点 / 成绩 分布柱状图
// ---------------------------------------------------------------------------

@Composable
private fun DistributionSection(data: GradeAnalytics) {
    val colors = AppTheme.colors
    var dist by rememberSaveable { mutableIntStateOf(0) } // 0 绩点, 1 成绩
    HyperSurface(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("绩点/成绩分布", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = colors.primaryTextColor, modifier = Modifier.weight(1f))
                Segmented(listOf("绩点", "成绩"), dist) { dist = it }
            }
            Spacer(Modifier.height(16.dp))
            val bars = data.gradePointDistribution.map { (point, count) ->
                val label = if (dist == 0) fmt(point, 1) else (gradePointToRange[point] ?: fmt(point, 1))
                Triple(label, count, colorForPoint(point))
            }
            BarChart(bars = bars, labelSizeDp = if (dist == 1) 9f else 10f)
        }
    }
}

@Composable
private fun BarChart(bars: List<Triple<String, Int, Color>>, labelSizeDp: Float) {
    val grid = AppTheme.colors.outlineLowContrastColor.copy(alpha = .35f)
    val axisText = AppTheme.colors.secondaryTextColor
    val maxCount = bars.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1
    val (yMax, yStep) = niceAxis(maxCount.toDouble())
    Canvas(Modifier.fillMaxWidth().height(220.dp)) {
        val leftPad = 6.dp.toPx()
        val rightPad = 26.dp.toPx()
        val topPad = 22.dp.toPx()
        val bottomPad = 24.dp.toPx()
        val chartW = size.width - leftPad - rightPad
        val chartH = size.height - topPad - bottomPad
        val steps = (yMax / yStep).toInt().coerceAtLeast(1)

        for (i in 0..steps) {
            val y = topPad + chartH * (1f - i.toFloat() / steps)
            drawLine(grid, Offset(leftPad, y), Offset(leftPad + chartW, y), strokeWidth = 1.dp.toPx())
            val v = yStep * i
            drawChartText(
                if (v % 1.0 == 0.0) v.toInt().toString() else fmt(v, 1),
                leftPad + chartW + 4.dp.toPx(), y + 3.5.dp.toPx(), axisText, 10f, android.graphics.Paint.Align.LEFT
            )
        }
        if (bars.isEmpty()) return@Canvas

        val slot = chartW / bars.size
        val barW = (slot * 0.52f).coerceAtMost(46.dp.toPx())
        bars.forEachIndexed { index, (label, count, color) ->
            val cx = leftPad + slot * (index + 0.5f)
            val barH = chartH * (count.toFloat() / yMax.toFloat())
            val top = topPad + chartH - barH
            drawRoundRect(
                color = color,
                topLeft = Offset(cx - barW / 2, top),
                size = Size(barW, barH),
                cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
            )
            drawChartText(count.toString(), cx, top - 5.dp.toPx(), color, 11f, android.graphics.Paint.Align.CENTER, bold = true)
            drawChartText(label, cx, size.height - 7.dp.toPx(), axisText, labelSizeDp, android.graphics.Paint.Align.CENTER)
        }
    }
}

// ---------------------------------------------------------------------------
// 分段切换控件
// ---------------------------------------------------------------------------

@Composable
private fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    val colors = AppTheme.colors
    Row(Modifier.hyperConcave(12.dp).padding(3.dp)) {
        options.forEachIndexed { index, text ->
            Text(
                text,
                Modifier
                    .then(if (selected == index) Modifier.background(colors.bgCardColor, RoundedCornerShape(9.dp)) else Modifier)
                    .hyperTap { onSelect(index) }
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                fontSize = 12.sp,
                fontWeight = if (selected == index) FontWeight.Bold else FontWeight.Normal,
                color = if (selected == index) colors.functionalTextColor else colors.secondaryTextColor
            )
        }
    }
}

@Composable
private fun EmptyGrades() {
    val colors = AppTheme.colors
    Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.TopCenter) {
        HyperSurface(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(vertical = 34.dp, horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("暂无成绩数据", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = colors.primaryTextColor)
                Spacer(Modifier.height(8.dp))
                Text("请先进入“成绩查询”刷新数据，分析结果会自动生成。", fontSize = 13.sp, color = colors.secondaryTextColor)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 计算 & 工具
// ---------------------------------------------------------------------------

private fun buildAnalytics(grades: List<Grade>): GradeAnalytics {
    val totalCourses = grades.size
    val totalHours = grades.sumOf { it.hoursValue }
    val totalCredits = grades.sumOf { it.creditValue }

    val scored = grades.filter { it.gradeValue != null }
    val overallAverage = if (scored.isNotEmpty()) scored.sumOf { it.gradeValue!! } / scored.size else 0.0
    val scoredCredits = scored.sumOf { it.creditValue }
    val weightedAverage = if (scoredCredits > 0.0) scored.sumOf { it.gradeValue!! * it.creditValue } / scoredCredits else overallAverage
    val overallGpa = if (totalCredits > 0.0) grades.sumOf { it.pointValue * it.creditValue } / totalCredits else 0.0

    val distribution = grades.groupBy { it.pointValue }
        .map { (point, list) -> point to list.size }
        .sortedByDescending { it.first }

    val bySemester = grades.filter { it.item.isNotBlank() }.groupBy { it.item }
    val semesterAverages = bySemester.map { (term, list) ->
        val s = list.filter { it.gradeValue != null }
        term to if (s.isNotEmpty()) s.sumOf { it.gradeValue!! } / s.size else 0.0
    }.sortedBy { it.first }
    val semesterGpas = bySemester.map { (term, list) ->
        val credits = list.sumOf { it.creditValue }
        term to if (credits > 0.0) list.sumOf { it.pointValue * it.creditValue } / credits else 0.0
    }.sortedBy { it.first }

    return GradeAnalytics(
        totalCourses, totalHours, totalCredits,
        overallAverage, overallGpa, weightedAverage,
        distribution, semesterAverages, semesterGpas
    )
}

/** 生成“好看”的坐标轴上界与步长（约 5 格），并保证最高柱不顶到轴顶。 */
private fun niceAxis(maxValue: Double, ticks: Int = 5): Pair<Double, Double> {
    if (maxValue <= 0.0) return 1.0 to 1.0
    val rawStep = maxValue / ticks
    val mag = Math.pow(10.0, Math.floor(Math.log10(rawStep)))
    val norm = rawStep / mag
    val niceNorm = when {
        norm <= 1.0 -> 1.0
        norm <= 2.0 -> 2.0
        norm <= 5.0 -> 5.0
        else -> 10.0
    }
    val step = niceNorm * mag
    var niceMax = ceil(maxValue / step) * step
    if (niceMax <= maxValue) niceMax += step // 顶部留白
    return niceMax to step
}

private fun DrawScope.drawChartText(
    text: String,
    x: Float,
    y: Float,
    color: Color,
    sizeDp: Float,
    align: android.graphics.Paint.Align,
    bold: Boolean = false
) {
    val paint = android.graphics.Paint().apply {
        isAntiAlias = true
        this.color = color.toArgb()
        textSize = sizeDp.dp.toPx()
        textAlign = align
        if (bold) typeface = android.graphics.Typeface.DEFAULT_BOLD
    }
    drawContext.canvas.nativeCanvas.drawText(text, x, y, paint)
}

private val Grade.creditValue: Double get() = score.toDoubleOrNull() ?: 0.0
private val Grade.hoursValue: Double get() = timeR.toDoubleOrNull() ?: 0.0
private val Grade.pointValue: Double get() = point.toDoubleOrNull() ?: 0.0
private val Grade.gradeValue: Double? get() = gradeValueOrNull(grade)

private fun fmt(value: Double, decimals: Int): String =
    String.format(Locale.CHINA, "%.${decimals}f", value)

private fun shortTerm(term: String): String {
    val match = Regex("(\\d{4})-(\\d{4})-([12])").find(term)
    return match?.value ?: term
}
