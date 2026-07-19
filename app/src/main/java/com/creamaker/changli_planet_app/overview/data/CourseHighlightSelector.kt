package com.creamaker.changli_planet_app.overview.data

import com.creamaker.changli_planet_app.overview.ui.model.CourseHighlightType
import com.creamaker.changli_planet_app.overview.ui.model.CourseHighlightUiModel
import com.creamaker.changli_planet_app.overview.ui.model.OverviewCourseUiModel
import java.time.LocalDateTime
import java.time.LocalTime

internal object CourseHighlightSelector {
    private val sectionTimes = listOf(
        LocalTime.of(8, 0) to LocalTime.of(8, 45),
        LocalTime.of(8, 55) to LocalTime.of(9, 40),
        LocalTime.of(10, 10) to LocalTime.of(10, 55),
        LocalTime.of(11, 5) to LocalTime.of(11, 50),
        LocalTime.of(14, 0) to LocalTime.of(14, 45),
        LocalTime.of(14, 55) to LocalTime.of(15, 40),
        LocalTime.of(16, 10) to LocalTime.of(16, 55),
        LocalTime.of(17, 5) to LocalTime.of(17, 50),
        LocalTime.of(19, 30) to LocalTime.of(20, 15),
        LocalTime.of(20, 25) to LocalTime.of(21, 10),
    )

    fun select(
        todayCourses: List<OverviewCourseUiModel>,
        tomorrowCourses: List<OverviewCourseUiModel>,
        now: LocalDateTime = LocalDateTime.now(),
    ): CourseHighlightUiModel {
        val sortedToday = todayCourses.sortedBy { it.startSection }
        val sortedTomorrow = tomorrowCourses.sortedBy { it.startSection }
        val currentTime = now.toLocalTime()

        val current = sortedToday.firstOrNull { course ->
            val range = course.timeRange() ?: return@firstOrNull false
            !currentTime.isBefore(range.first) && currentTime.isBefore(range.second)
        }
        val upcoming = sortedToday.firstOrNull { course ->
            course.timeRange()?.first?.let(currentTime::isBefore) == true
        }

        return when {
            current != null -> current.toHighlight(CourseHighlightType.CURRENT, "正在上课")
            upcoming != null -> upcoming.toHighlight(CourseHighlightType.TODAY_UPCOMING, "下一节课")
            sortedTomorrow.isNotEmpty() -> sortedTomorrow.first().toHighlight(
                type = CourseHighlightType.TOMORROW,
                title = "明日课程",
                moreCount = sortedTomorrow.size - 1,
            )
            else -> CourseHighlightUiModel(
                type = CourseHighlightType.EMPTY,
                title = if (sortedToday.isEmpty()) "今日无课" else "今日课程已结束",
                courseName = if (sortedToday.isEmpty()) "今天没有课程，好好休息吧" else "今天的课程已经全部结束",
                timeText = "暂无后续课程安排",
            )
        }
    }

    private fun OverviewCourseUiModel.timeRange(): Pair<LocalTime, LocalTime>? {
        if (startSection !in 1..sectionTimes.size || sectionSpan <= 0) return null
        val endSection = (startSection + sectionSpan - 1).coerceAtMost(sectionTimes.size)
        return sectionTimes[startSection - 1].first to sectionTimes[endSection - 1].second
    }

    private fun OverviewCourseUiModel.toHighlight(
        type: CourseHighlightType,
        title: String,
        moreCount: Int = 0,
    ) = CourseHighlightUiModel(
        type = type,
        title = title,
        courseName = courseName,
        timeText = timeText,
        location = classroom,
        moreCount = moreCount.coerceAtLeast(0),
    )
}
