package com.creamaker.changli_planet_app.overview.data

import androidx.compose.ui.graphics.Color
import com.creamaker.changli_planet_app.overview.ui.model.CourseHighlightType
import com.creamaker.changli_planet_app.overview.ui.model.OverviewCourseUiModel
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class CourseHighlightSelectorTest {

    @Test
    fun currentCourseHasHighestPriority() {
        val courses = listOf(course("早课", 1), course("正在上的课", 3), course("下一节课", 5))

        val result = CourseHighlightSelector.select(
            todayCourses = courses,
            tomorrowCourses = listOf(course("明日课程", 1)),
            now = LocalDateTime.of(2026, 7, 20, 10, 30),
        )

        assertEquals(CourseHighlightType.CURRENT, result.type)
        assertEquals("正在上的课", result.courseName)
    }

    @Test
    fun nextCourseIsSelectedAfterEarlierCoursesEnd() {
        val result = CourseHighlightSelector.select(
            todayCourses = listOf(course("已结束", 1), course("下午课程", 5)),
            tomorrowCourses = emptyList(),
            now = LocalDateTime.of(2026, 7, 20, 12, 30),
        )

        assertEquals(CourseHighlightType.TODAY_UPCOMING, result.type)
        assertEquals("下午课程", result.courseName)
    }

    @Test
    fun tomorrowCourseIsUsedWhenTodayHasNoRemainingCourse() {
        val result = CourseHighlightSelector.select(
            todayCourses = listOf(course("已结束", 1)),
            tomorrowCourses = listOf(course("明早课程", 1), course("明天下午", 5)),
            now = LocalDateTime.of(2026, 7, 20, 22, 0),
        )

        assertEquals(CourseHighlightType.TOMORROW, result.type)
        assertEquals("明早课程", result.courseName)
        assertEquals(1, result.moreCount)
    }

    private fun course(name: String, startSection: Int) = OverviewCourseUiModel(
        id = name,
        courseName = name,
        classroom = "A101",
        teacher = "教师",
        timeText = "测试时间",
        accentLabel = "校园课表",
        accentColor = Color.Blue,
        startSection = startSection,
        sectionSpan = 2,
    )
}
