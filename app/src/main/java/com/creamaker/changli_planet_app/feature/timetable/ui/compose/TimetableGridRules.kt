package com.creamaker.changli_planet_app.feature.timetable.ui.compose

internal const val TIMETABLE_TOTAL_WEEKS = 20
internal const val TIMETABLE_TOTAL_DAYS = 7
internal const val TIMETABLE_TOTAL_SECTIONS = 10

internal data class TimetableGridSlot(
    val week: Int,
    val dayOfWeek: Int,
    val startSection: Int,
)

internal fun timetableGridSlot(week: Int, dayOfWeek: Int, startSection: Int): TimetableGridSlot {
    require(week in 1..TIMETABLE_TOTAL_WEEKS) { "week out of range: $week" }
    require(dayOfWeek in 1..TIMETABLE_TOTAL_DAYS) { "day out of range: $dayOfWeek" }
    require(startSection in 1..TIMETABLE_TOTAL_SECTIONS) { "section out of range: $startSection" }
    return TimetableGridSlot(week, dayOfWeek, startSection)
}

internal fun customCourseSpan(startSection: Int): Int {
    require(startSection in 1..TIMETABLE_TOTAL_SECTIONS) { "section out of range: $startSection" }
    return minOf(2, TIMETABLE_TOTAL_SECTIONS - startSection + 1)
}

internal fun TimeTableCourseUi.isVisibleInWeek(week: Int): Boolean {
    return week in weeks &&
        dayOfWeek in 1..TIMETABLE_TOTAL_DAYS &&
        startSection in 1..TIMETABLE_TOTAL_SECTIONS &&
        sectionSpan > 0
}

internal fun TimeTableCourseUi.occupiedSections(): IntRange {
    if (startSection !in 1..TIMETABLE_TOTAL_SECTIONS || sectionSpan <= 0) return IntRange.EMPTY
    val endSection = (startSection + sectionSpan - 1).coerceAtMost(TIMETABLE_TOTAL_SECTIONS)
    return startSection..endSection
}
