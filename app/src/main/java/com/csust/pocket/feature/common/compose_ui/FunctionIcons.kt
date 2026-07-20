package com.csust.pocket.feature.common.compose_ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AssignmentTurnedIn
import androidx.compose.material.icons.outlined.Biotech
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.automirrored.outlined.EventNote
import androidx.compose.material.icons.automirrored.outlined.FactCheck
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.automirrored.outlined.LibraryBooks
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.MeetingRoom
import androidx.compose.material.icons.outlined.RateReview
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.outlined.VpnLock
import androidx.compose.ui.graphics.vector.ImageVector

/** 服务入口统一使用同一套 Material Outlined 线性图标。 */
fun functionIcon(destination: FunctionDestination): ImageVector = when (destination) {
    FunctionDestination.Timetable -> Icons.Outlined.CalendarMonth
    FunctionDestination.ScoreInquiry -> Icons.AutoMirrored.Outlined.FactCheck
    FunctionDestination.ExamArrangement -> Icons.AutoMirrored.Outlined.EventNote
    FunctionDestination.GradeAnalysis -> Icons.Outlined.Insights
    FunctionDestination.MoocCourses -> Icons.AutoMirrored.Outlined.LibraryBooks
    FunctionDestination.MoocHomework,
    FunctionDestination.Homework -> Icons.Outlined.AssignmentTurnedIn
    FunctionDestination.Electronic -> Icons.Outlined.Bolt
    FunctionDestination.Classroom -> Icons.Outlined.MeetingRoom
    FunctionDestination.CampusMap -> Icons.Outlined.Map
    FunctionDestination.Calendar -> Icons.Outlined.DateRange
    FunctionDestination.ElectricityRecharge,
    FunctionDestination.AccountBook -> Icons.Outlined.CreditCard
    FunctionDestination.WebVpn -> Icons.Outlined.VpnLock
    FunctionDestination.Evaluation -> Icons.Outlined.RateReview
    FunctionDestination.PhysicsSchedule -> Icons.Outlined.Science
    FunctionDestination.PhysicsGrade -> Icons.Outlined.Biotech
    FunctionDestination.Cet -> Icons.Outlined.Translate
    FunctionDestination.Mandarin -> Icons.Outlined.RecordVoiceOver
    FunctionDestination.Ncre -> Icons.Outlined.Computer
    FunctionDestination.LostFound,
    FunctionDestination.Contract -> Icons.AutoMirrored.Outlined.FactCheck
}

fun functionIconForId(id: String): ImageVector = when (id) {
    "schedule" -> Icons.Outlined.CalendarMonth
    "grade" -> Icons.AutoMirrored.Outlined.FactCheck
    "exam" -> Icons.AutoMirrored.Outlined.EventNote
    "grade_analysis" -> Icons.Outlined.Insights
    "mooc_courses", "document" -> Icons.AutoMirrored.Outlined.LibraryBooks
    "mooc_homework", "homework" -> Icons.Outlined.AssignmentTurnedIn
    "electric" -> Icons.Outlined.Bolt
    "classroom" -> Icons.Outlined.MeetingRoom
    "map" -> Icons.Outlined.Map
    "calendar" -> Icons.Outlined.DateRange
    "electric_recharge", "account" -> Icons.Outlined.CreditCard
    "webvpn" -> Icons.Outlined.VpnLock
    "evaluation" -> Icons.Outlined.RateReview
    "physics_schedule" -> Icons.Outlined.Science
    "physics_grade" -> Icons.Outlined.Biotech
    "cet" -> Icons.Outlined.Translate
    "mandarin" -> Icons.Outlined.RecordVoiceOver
    "ncre" -> Icons.Outlined.Computer
    else -> Icons.AutoMirrored.Outlined.FactCheck
}
