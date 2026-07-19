package com.creamaker.changli_planet_app.feature.timetable.ui.compose

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimetableGridRulesTest {

    @Test
    fun everyGridCellKeepsItsOwnWeekDayAndSection() {
        var checkedCells = 0
        for (week in 1..TIMETABLE_TOTAL_WEEKS) {
            for (day in 1..TIMETABLE_TOTAL_DAYS) {
                for (section in 1..TIMETABLE_TOTAL_SECTIONS) {
                    val slot = timetableGridSlot(week, day, section)
                    assertEquals(week, slot.week)
                    assertEquals(day, slot.dayOfWeek)
                    assertEquals(section, slot.startSection)
                    checkedCells++
                }
            }
        }
        assertEquals(1_400, checkedCells)
    }

    @Test
    fun lastSectionNeverExtendsPastTheGrid() {
        for (section in 1..TIMETABLE_TOTAL_SECTIONS) {
            val span = customCourseSpan(section)
            assertTrue(section + span - 1 <= TIMETABLE_TOTAL_SECTIONS)
        }
        assertEquals(2, customCourseSpan(9))
        assertEquals(1, customCourseSpan(10))
    }

    @Test
    fun courseAppearsOnlyInItsSelectedWeekAndKeepsSunday() {
        val sundayCourse = TimeTableCourseUi(
            id = 1,
            title = "周日课程",
            teacher = "教师",
            room = "A101",
            dayOfWeek = 7,
            startSection = 9,
            sectionSpan = 2,
            weeks = setOf(8),
            isCustom = true,
        )

        assertFalse(sundayCourse.isVisibleInWeek(7))
        assertTrue(sundayCourse.isVisibleInWeek(8))
        assertFalse(sundayCourse.isVisibleInWeek(9))
        assertEquals(7, sundayCourse.dayOfWeek)
        assertEquals(9..10, sundayCourse.occupiedSections())
    }

    @Test
    fun oversizedCourseCardIsClippedToTheFinalSection() {
        val course = TimeTableCourseUi(
            id = 2,
            title = "边界课程",
            teacher = "教师",
            room = "A102",
            dayOfWeek = 1,
            startSection = 10,
            sectionSpan = 4,
            weeks = setOf(1),
            isCustom = false,
        )

        assertEquals(10..10, course.occupiedSections())
    }
}
