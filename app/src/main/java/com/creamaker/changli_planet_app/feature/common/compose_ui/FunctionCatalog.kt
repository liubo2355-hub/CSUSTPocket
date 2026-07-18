package com.creamaker.changli_planet_app.feature.common.compose_ui

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.creamaker.changli_planet_app.R
import com.creamaker.changli_planet_app.core.Route
import com.creamaker.changli_planet_app.widget.view.CustomToast

@Immutable
data class FunctionShortcut(
    val id: String,
    val title: String,
    @DrawableRes val iconRes: Int,
    val tintColor: Color,
    val destination: FunctionDestination
)

@Immutable
data class FunctionGroup(
    val title: String,
    val items: List<FunctionShortcut>
)

enum class FunctionDestination {
    Timetable,
    ScoreInquiry,
    CampusMap,
    Classroom,
    Homework,
    Electronic,
    ExamArrangement,
    Calendar,
    Cet,
    LostFound,
    AccountBook,
    Contract,
    Mandarin,
    GradeAnalysis,
    MoocCourses,
    MoocHomework,
    ElectricityRecharge,
    WebVpn,
    Evaluation,
    PhysicsSchedule,
    PhysicsGrade,
    Ncre
}

fun portalServiceGroups(): List<FunctionGroup> = listOf(
    FunctionGroup(
        "教务系统",
        listOf(
            FunctionShortcut("schedule", "我的课表", R.drawable.ic_timetable, FunctionColors.Schedule, FunctionDestination.Timetable),
            FunctionShortcut("grade", "成绩查询", R.drawable.ic_exam, FunctionColors.Grade, FunctionDestination.ScoreInquiry),
            FunctionShortcut("exam", "考试安排", R.drawable.ic_schedule, FunctionColors.Exam, FunctionDestination.ExamArrangement),
            FunctionShortcut("grade_analysis", "成绩分析", R.drawable.ic_rank, FunctionColors.Document, FunctionDestination.GradeAnalysis)
        )
    ),
    FunctionGroup(
        "网络课程中心",
        listOf(
            FunctionShortcut("mooc_courses", "所有课程", R.drawable.ic_document, FunctionColors.Schedule, FunctionDestination.MoocCourses),
            FunctionShortcut("mooc_homework", "待提交作业", R.drawable.ic_homework, FunctionColors.Homework, FunctionDestination.MoocHomework)
        )
    ),
    FunctionGroup(
        "校园工具",
        listOf(
            FunctionShortcut("electric", "电量查询", R.drawable.ic_bill, FunctionColors.Electric, FunctionDestination.Electronic),
            FunctionShortcut("classroom", "空教室查询", R.drawable.ic_classroom, FunctionColors.Classroom, FunctionDestination.Classroom),
            FunctionShortcut("map", "校园地图", R.drawable.ic_map, FunctionColors.Map, FunctionDestination.CampusMap),
            FunctionShortcut("calendar", "校历", R.drawable.ic_calendar, FunctionColors.Calendar, FunctionDestination.Calendar),
            FunctionShortcut("electric_recharge", "电费充值", R.drawable.ic_bill, FunctionColors.Exam, FunctionDestination.ElectricityRecharge),
            FunctionShortcut("webvpn", "WebVPN", R.drawable.ic_document, FunctionColors.Schedule, FunctionDestination.WebVpn),
            FunctionShortcut("evaluation", "评教系统", R.drawable.ic_exam, FunctionColors.Grade, FunctionDestination.Evaluation)
        )
    ),
    FunctionGroup(
        "大学物理实验",
        listOf(
            FunctionShortcut("physics_schedule", "实验安排", R.drawable.ic_schedule, FunctionColors.Calendar, FunctionDestination.PhysicsSchedule),
            FunctionShortcut("physics_grade", "实验成绩", R.drawable.ic_rank, FunctionColors.Grade, FunctionDestination.PhysicsGrade)
        )
    ),
    FunctionGroup(
        "其他考试查询",
        listOf(
            FunctionShortcut("cet", "四六级查询", R.drawable.ic_essay, FunctionColors.CET, FunctionDestination.Cet),
            FunctionShortcut("mandarin", "普通话查询", R.drawable.ic_talking, FunctionColors.Mandarin, FunctionDestination.Mandarin),
            FunctionShortcut("ncre", "计算机等级查询", R.drawable.ic_game_computer, FunctionColors.Account, FunctionDestination.Ncre)
        )
    )
)

fun primaryFunctionShortcuts(): List<FunctionShortcut> =
    listOf(
        FunctionShortcut("schedule", "课表", R.drawable.ic_timetable, FunctionColors.Schedule, FunctionDestination.Timetable),
        FunctionShortcut("grade", "成绩查询", R.drawable.ic_exam, FunctionColors.Grade, FunctionDestination.ScoreInquiry),
        FunctionShortcut("map", "校园地图", R.drawable.ic_map, FunctionColors.Map, FunctionDestination.CampusMap),
        FunctionShortcut("classroom", "空教室", R.drawable.ic_classroom, FunctionColors.Classroom, FunctionDestination.Classroom),
        FunctionShortcut("homework", "作业查询", R.drawable.ic_homework, FunctionColors.Homework, FunctionDestination.Homework),
        FunctionShortcut("electric", "电费查询", R.drawable.ic_bill, FunctionColors.Electric, FunctionDestination.Electronic),
        FunctionShortcut("exam", "考试安排", R.drawable.ic_schedule, FunctionColors.Exam, FunctionDestination.ExamArrangement),
        FunctionShortcut("calendar", "校历", R.drawable.ic_calendar, FunctionColors.Calendar, FunctionDestination.Calendar),
    )

fun FunctionShortcut.toFunctionItemData(context: Context): FunctionItemData =
    FunctionItemData(
        id = id,
        title = title,
        iconRes = iconRes,
        tintColor = tintColor,
        onClick = { openFunctionShortcut(context, destination) }
    )

fun openFunctionShortcut(context: Context, destination: FunctionDestination) {
    when (destination) {
        FunctionDestination.Timetable -> Route.goTimetable(context)
        FunctionDestination.ScoreInquiry -> Route.goScoreInquiry(context)
        FunctionDestination.CampusMap -> Route.goCampusMap(context)
        FunctionDestination.Classroom -> Route.goClassInfo(context)
        FunctionDestination.Homework -> Route.goMoocHomework(context)
        FunctionDestination.Electronic -> Route.goElectronic(context)
        FunctionDestination.ExamArrangement -> Route.goExamArrangement(context)
        FunctionDestination.Calendar -> Route.goCalendar(context)
        FunctionDestination.Cet -> Route.goCet(context)
        FunctionDestination.LostFound -> CustomToast.showMessage(context, "正在全力开发中")
        FunctionDestination.AccountBook -> Route.goAccountBook(context)
        FunctionDestination.Contract -> CustomToast.showMessage(context, "正在全力开发中")
        FunctionDestination.Mandarin -> Route.goMande(context)
        FunctionDestination.GradeAnalysis -> Route.goGradeAnalysis(context)
        FunctionDestination.MoocCourses -> Route.goMoocCourses(context)
        FunctionDestination.MoocHomework -> Route.goMoocHomework(context)
        FunctionDestination.ElectricityRecharge -> Route.goElectricityRecharge(context)
        FunctionDestination.WebVpn -> Route.goWebVpnConverter(context)
        FunctionDestination.Evaluation -> Route.goEvaluation(context)
        FunctionDestination.PhysicsSchedule -> Route.goPhysicsSchedule(context)
        FunctionDestination.PhysicsGrade -> Route.goPhysicsGrade(context)
        FunctionDestination.Ncre -> Route.goNcre(context)
    }
}
