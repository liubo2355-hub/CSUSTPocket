package com.creamaker.changli_planet_app.overview.data

import com.creamaker.changli_planet_app.overview.ui.model.ExamHighlightType
import com.creamaker.changli_planet_app.overview.ui.model.OverviewExamUiModel
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class ExamHighlightSelectorTest {
    private val now = LocalDateTime.of(2026, 7, 18, 10, 0)

    @Test
    fun currentExamTakesPriorityOverLaterExam() {
        val result = ExamHighlightSelector.select(
            listOf(
                exam("下午考试", now.withHour(14), now.withHour(16)),
                exam("正在考试", now.withHour(9), now.withHour(11))
            ),
            now
        )

        assertEquals(ExamHighlightType.TODAY_CURRENT, result.type)
        assertEquals("正在考试", result.courseName)
    }

    @Test
    fun nextTodayExamIsSelectedWhenNothingIsRunning() {
        val result = ExamHighlightSelector.select(
            listOf(exam("下午考试", now.withHour(14), now.withHour(16))),
            now
        )

        assertEquals(ExamHighlightType.TODAY_UPCOMING, result.type)
        assertEquals("下午考试", result.courseName)
    }

    @Test
    fun firstTomorrowExamIncludesRemainingCount() {
        val tomorrow = now.plusDays(1)
        val result = ExamHighlightSelector.select(
            listOf(
                exam("明天下午", tomorrow.withHour(14), tomorrow.withHour(16)),
                exam("明天上午", tomorrow.withHour(9), tomorrow.withHour(11))
            ),
            now
        )

        assertEquals(ExamHighlightType.TOMORROW, result.type)
        assertEquals("明天上午", result.courseName)
        assertEquals(1, result.moreCount)
    }

    @Test
    fun emptyStateIsReturnedWithoutTodayOrTomorrowExam() {
        val result = ExamHighlightSelector.select(emptyList(), now)

        assertEquals(ExamHighlightType.EMPTY, result.type)
        assertEquals("今天没有考试哦", result.courseName)
    }

    private fun exam(
        name: String,
        start: LocalDateTime,
        end: LocalDateTime
    ) = OverviewExamUiModel(
        id = name,
        courseName = name,
        examTime = "${start.toLocalTime()}~${end.toLocalTime()}",
        location = "测试教室",
        startTime = start,
        endTime = end
    )
}
