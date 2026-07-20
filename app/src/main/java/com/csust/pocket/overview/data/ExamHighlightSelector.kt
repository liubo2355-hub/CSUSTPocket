package com.csust.pocket.overview.data

import com.csust.pocket.overview.ui.model.ExamHighlightType
import com.csust.pocket.overview.ui.model.ExamHighlightUiModel
import com.csust.pocket.overview.ui.model.OverviewExamUiModel
import java.time.LocalDateTime

internal object ExamHighlightSelector {
    fun select(
        exams: List<OverviewExamUiModel>,
        now: LocalDateTime = LocalDateTime.now()
    ): ExamHighlightUiModel {
        val today = now.toLocalDate()
        val tomorrow = today.plusDays(1)
        val todayExams = exams
            .filter { it.startTime?.toLocalDate() == today }
            .sortedBy { it.startTime }
        val tomorrowExams = exams
            .filter { it.startTime?.toLocalDate() == tomorrow }
            .sortedBy { it.startTime }

        val current = todayExams.firstOrNull { exam ->
            val start = exam.startTime
            val end = exam.endTime
            start != null && end != null && !now.isBefore(start) && now.isBefore(end)
        }
        val upcoming = todayExams.firstOrNull { exam ->
            exam.startTime?.let(now::isBefore) == true
        }

        return when {
            current != null -> current.toHighlight(ExamHighlightType.TODAY_CURRENT, "正在考试")
            upcoming != null -> upcoming.toHighlight(ExamHighlightType.TODAY_UPCOMING, "今日考试")
            tomorrowExams.isNotEmpty() -> tomorrowExams.first().toHighlight(
                type = ExamHighlightType.TOMORROW,
                title = "明日考试",
                moreCount = tomorrowExams.size - 1
            )
            else -> ExamHighlightUiModel(
                type = ExamHighlightType.EMPTY,
                title = "考试安排",
                courseName = "今天没有考试哦",
                timeText = "继续保持！"
            )
        }
    }

    private fun OverviewExamUiModel.toHighlight(
        type: ExamHighlightType,
        title: String,
        moreCount: Int = 0
    ) = ExamHighlightUiModel(
        type = type,
        title = title,
        courseName = courseName,
        timeText = examTime,
        location = location,
        moreCount = moreCount.coerceAtLeast(0)
    )
}
