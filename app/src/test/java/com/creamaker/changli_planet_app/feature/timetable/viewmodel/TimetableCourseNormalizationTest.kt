package com.creamaker.changli_planet_app.feature.timetable.viewmodel

import com.creamaker.changli_planet_app.feature.common.data.local.entity.TimeTableMySubject
import org.junit.Assert.assertEquals
import org.junit.Test

class TimetableCourseNormalizationTest {

    @Test
    fun sundayCourseKeepsTheSelectedWeek() {
        val sundayCourse = TimeTableMySubject(
            courseName = "自定义课程",
            classroom = "测试教室",
            teacher = "测试教师",
            weeks = listOf(19),
            start = 3,
            step = 2,
            weekday = 7,
            isCustom = true,
            term = "2025-2026-2",
        )

        val normalized = normalizeTimetableCourses(listOf(sundayCourse))

        assertEquals(listOf(19), normalized.single().weeks)
        assertEquals(7, normalized.single().weekday)
    }

    @Test
    fun exactDuplicatesAreRemovedWithoutChangingCoursePlacement() {
        val course = TimeTableMySubject(
            courseName = "测试课程",
            classroom = "A101",
            teacher = "教师",
            weeks = listOf(2, 4, 6),
            start = 1,
            step = 2,
            weekday = 2,
            term = "2025-2026-2",
        )

        val normalized = normalizeTimetableCourses(listOf(course, course.copy(id = 99)))

        assertEquals(1, normalized.size)
        assertEquals(listOf(2, 4, 6), normalized.single().weeks)
        assertEquals(2, normalized.single().weekday)
    }
}
