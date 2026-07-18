package com.creamaker.changli_planet_app.feature.common.ui.compose

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/**
 * 全 App 统一的成绩 / 绩点动态配色。
 *
 * 移植自 CSUSTPocket 的 ColorUtil：system red / yellow / green 三色线性插值。
 * - 成绩：<60 红，60→78 红黄，78→90 黄绿，≥90 绿
 * - 绩点：<1 红，1→3 红黄，3→4 黄绿，≥4 绿
 */

val GradeRed = Color(0xFFFF3B30)
val GradeYellow = Color(0xFFFFCC00)
val GradeGreen = Color(0xFF34C759)
val GradePurple = Color(0xFFAF52DE)
val GradeBlue = Color(0xFF007AFF)

fun colorForGrade(grade: Double): Color = when {
    grade < 60.0 -> GradeRed
    grade < 78.0 -> lerp(GradeRed, GradeYellow, ((grade - 60.0) / 18.0).toFloat().coerceIn(0f, 1f))
    grade < 90.0 -> lerp(GradeYellow, GradeGreen, ((grade - 78.0) / 12.0).toFloat().coerceIn(0f, 1f))
    else -> GradeGreen
}

fun colorForPoint(point: Double): Color = when {
    point < 1.0 -> GradeRed
    point < 3.0 -> lerp(GradeRed, GradeYellow, ((point - 1.0) / 2.0).toFloat().coerceIn(0f, 1f))
    point < 4.0 -> lerp(GradeYellow, GradeGreen, ((point - 3.0) / 1.0).toFloat().coerceIn(0f, 1f))
    else -> GradeGreen
}

/** 把成绩文本（数字，或“优秀/良好/…”等等级）解析为分数；无法识别返回 null。 */
fun gradeValueOrNull(text: String): Double? =
    text.trim().toDoubleOrNull() ?: when (text.trim()) {
        "优秀" -> 95.0
        "良好" -> 85.0
        "中等" -> 75.0
        "及格", "合格" -> 65.0
        "不及格", "不合格" -> 50.0
        else -> null
    }

/** 成绩文本 → 动态色；无法识别时用 fallback。 */
fun colorForGradeText(text: String, fallback: Color): Color =
    gradeValueOrNull(text)?.let { colorForGrade(it) } ?: fallback

/** 标准绩点档 → 成绩段字符串（移植自 ColorUtil.gradeRanges）。 */
val gradePointToRange: Map<Double, String> = mapOf(
    4.0 to "90-100", 3.7 to "85-89", 3.3 to "82-84", 3.0 to "78-81",
    2.7 to "75-77", 2.3 to "72-74", 2.0 to "68-71", 1.7 to "66-67",
    1.3 to "64-65", 1.0 to "60-63", 0.0 to "≤59"
)
