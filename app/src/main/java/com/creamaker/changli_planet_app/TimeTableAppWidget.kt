package com.creamaker.changli_planet_app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.creamaker.changli_planet_app.common.cache.CommonInfo
import com.creamaker.changli_planet_app.common.data.local.mmkv.StudentInfoManager
import com.creamaker.changli_planet_app.feature.common.data.local.entity.TimeTableMySubject
import com.creamaker.changli_planet_app.feature.common.data.local.room.database.CoursesDataBase
import com.creamaker.changli_planet_app.feature.timetable.ui.TimeTableActivity
import com.creamaker.changli_planet_app.overview.data.OverviewRepository
import com.creamaker.changli_planet_app.widget.internal.WidgetChart
import com.creamaker.changli_planet_app.widget.internal.WidgetSize
import com.creamaker.changli_planet_app.widget.internal.WidgetText
import com.creamaker.changli_planet_app.widget.internal.WidgetTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * 今日课表桌面小组件。
 *
 * 支持 3 种尺寸（由 [WidgetSize.tier] 决定）：
 * - small : 单节课程 + 状态徽章
 * - medium: 当天最多 3 节课列表
 * - large : 本周课程网格（Canvas 自绘）
 *
 * 数据流：
 * - 第一次绑定/系统刷新：渲染本地缓存 → 后台异步联网 → 重新渲染
 * - 显式刷新按钮：先显示"刷新中"，再联网，再重新渲染
 * - 退出登录：本地缓存被清空，下次刷新自动降级到"未登录"占位
 */
class TimeTableAppWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { render(context, manager, it, refreshing = false) }
        if (ids.isNotEmpty()) refreshAll(context, manager, ids)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        manager: AppWidgetManager,
        widgetId: Int,
        newOptions: android.os.Bundle
    ) {
        // 用户拖动改变尺寸时立即重渲染，让布局切换
        render(context, manager, widgetId, refreshing = false)
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_REFRESH) {
            val widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return
            val manager = AppWidgetManager.getInstance(context)
            render(context, manager, widgetId, refreshing = true)
            val pendingResult = goAsync()
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                runCatching { OverviewRepository(context).refreshCoursesOnly() }
                render(context, manager, widgetId, refreshing = false)
                pendingResult.finish()
            }
            return
        }
        super.onReceive(context, intent)
    }

    companion object {
        const val ACTION_REFRESH = "com.creamaker.pocket_csust.widget.REFRESH_TIMETABLE"

        private val LESSON_STARTS = intArrayOf(480, 535, 610, 665, 840, 895, 970, 1025, 1170, 1225)
        private val LESSON_ENDS = intArrayOf(525, 580, 655, 710, 885, 940, 1015, 1070, 1215, 1270)

        // 节次映射到 periodIndex (0..N-1)，给周视图网格用
        private fun periodIndex(start: Int): Int = ((start - 1) / 2).coerceAtLeast(0)

        private fun startMinute(course: TimeTableMySubject) = LESSON_STARTS.getOrElse((course.start - 1).coerceAtLeast(0)) { 0 }
        private fun endMinute(course: TimeTableMySubject): Int {
            val lastIndex = (course.start + course.step - 2).coerceIn(0, LESSON_ENDS.lastIndex)
            return LESSON_ENDS[lastIndex]
        }
        private fun minuteText(value: Int) = "%02d:%02d".format(Locale.getDefault(), value / 60, value % 60)

        private fun schoolWeek(term: String, calendar: Calendar): Int {
            val startText = CommonInfo.getTermStartDate(term) ?: return CommonInfo.getCurrentWeekInt(term)
            val start = runCatching { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(startText) }.getOrNull()
                ?: return CommonInfo.getCurrentWeekInt(term)
            return (((calendar.timeInMillis - start.time) / 86_400_000L) / 7L + 1L).toInt().coerceAtLeast(1)
        }

        private fun weekday(calendar: Calendar): Int = when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SUNDAY -> 7
            else -> calendar.get(Calendar.DAY_OF_WEEK) - 1
        }

        private fun refreshAll(context: Context, manager: AppWidgetManager, ids: IntArray) {
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                runCatching { OverviewRepository(context).refreshCoursesOnly() }
                ids.forEach { render(context, manager, it, refreshing = false) }
            }
        }

        /**
         * 主渲染入口。同步完成本地数据绑定；图表型布局会在 IO 协程里异步完成。
         */
        internal fun render(context: Context, manager: AppWidgetManager, widgetId: Int, refreshing: Boolean) {
            val appContext = context.applicationContext
            val tier = WidgetSize.tier(appContext, manager, widgetId)
            val layoutRes = when (tier) {
                WidgetSize.SMALL -> R.layout.widget_timetable_small
                WidgetSize.MEDIUM -> R.layout.widget_timetable_medium
                else -> R.layout.widget_timetable_large
            }
            val views = RemoteViews(appContext.packageName, layoutRes)

            // 打开应用 + 刷新按钮
            val openIntent = Intent(appContext, TimeTableActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("from_timeTable_widget", true)
            }
            views.setOnClickPendingIntent(
                R.id.widget_root_layout,
                PendingIntent.getActivity(appContext, widgetId, openIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            )
            val refreshIntent = Intent(appContext, TimeTableAppWidget::class.java).apply {
                action = ACTION_REFRESH
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            }
            views.setOnClickPendingIntent(
                R.id.widget_refresh,
                PendingIntent.getBroadcast(appContext, widgetId, refreshIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            )

            val now = Calendar.getInstance()
            val studentId = StudentInfoManager.studentId
            val password = StudentInfoManager.studentPassword

            // 未登录态：渲染提示
            if (studentId.isBlank() || password.isBlank()) {
                renderUnauthenticated(appContext, views, tier, refreshing)
                manager.updateAppWidget(widgetId, views)
                return
            }

            val term = CommonInfo.getCurrentTerm()
            val today = weekday(now)
            val week = schoolWeek(term, now)
            views.setTextViewText(R.id.widget_date, WidgetText.todayLabel(now, week))

            when (tier) {
                WidgetSize.SMALL -> {
                    manager.updateAppWidget(widgetId, views)
                    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                        val courses = readCoursesSync(appContext, term, studentId, password)
                        renderSmall(appContext, views, courses.forDay(today, week), now, refreshing)
                        manager.updateAppWidget(widgetId, views)
                    }
                    return
                }
                WidgetSize.MEDIUM -> {
                    manager.updateAppWidget(widgetId, views)
                    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                        val courses = readCoursesSync(appContext, term, studentId, password)
                        renderMedium(appContext, views, courses.forDay(today, week), now, refreshing)
                        manager.updateAppWidget(widgetId, views)
                    }
                    return
                }
                WidgetSize.LARGE -> {
                    views.setTextViewText(R.id.widget_term, term)
                    views.setTextViewText(R.id.widget_weekday, WidgetText.weekdayShort(now))
                    manager.updateAppWidget(widgetId, views)
                    val refreshSnapshot = refreshing
                    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                        val courses = readCoursesSync(appContext, term, studentId, password)
                        renderLarge(appContext, views, courses, week, now, refreshSnapshot)
                        manager.updateAppWidget(widgetId, views)
                    }
                    return
                }
            }
            manager.updateAppWidget(widgetId, views)
        }

        private fun renderUnauthenticated(context: Context, views: RemoteViews, tier: String, refreshing: Boolean) {
            val now = Calendar.getInstance()
            views.setTextViewText(R.id.widget_date, WidgetText.todayLabel(now))
            when (tier) {
                WidgetSize.SMALL -> {
                    views.setViewVisibility(R.id.widget_primary_block, View.GONE)
                    views.setViewVisibility(R.id.widget_empty_hint, View.VISIBLE)
                    views.setTextViewText(R.id.widget_empty_hint, "请先在掌上长理绑定学号")
                }
                WidgetSize.MEDIUM -> {
                    for (i in 1..3) {
                        views.setViewVisibility(rowId(i), View.GONE)
                    }
                    views.setViewVisibility(R.id.widget_updated, View.VISIBLE)
                    views.setTextViewText(R.id.widget_updated, if (refreshing) "正在联网同步…" else "请先在掌上长理绑定学号")
                }
                WidgetSize.LARGE -> {
                    views.setTextViewText(R.id.widget_term, "")
                    views.setTextViewText(R.id.widget_weekday, "")
                    views.setImageViewBitmap(
                        R.id.widget_chart,
                        WidgetChart.drawWeekScheduleGrid(context, widthDp = 320f, heightDp = 140f, cells = emptyList())
                    )
                    views.setTextViewText(R.id.widget_updated, "请先绑定学号")
                }
            }
        }

        private fun rowId(index: Int): Int = when (index) {
            1 -> R.id.widget_row_1
            2 -> R.id.widget_row_2
            3 -> R.id.widget_row_3
            else -> R.id.widget_row_1
        }
        private fun rowNameId(index: Int): Int = when (index) {
            1 -> R.id.widget_name_1
            2 -> R.id.widget_name_2
            3 -> R.id.widget_name_3
            else -> R.id.widget_name_1
        }
        private fun rowMetaId(index: Int): Int = when (index) {
            1 -> R.id.widget_meta_1
            2 -> R.id.widget_meta_2
            3 -> R.id.widget_meta_3
            else -> R.id.widget_meta_1
        }
        private fun rowTimeId(index: Int): Int = when (index) {
            1 -> R.id.widget_time_1
            2 -> R.id.widget_time_2
            3 -> R.id.widget_time_3
            else -> R.id.widget_time_1
        }
        private fun rowStripId(index: Int): Int = when (index) {
            1 -> R.id.widget_strip_1
            2 -> R.id.widget_strip_2
            3 -> R.id.widget_strip_3
            else -> R.id.widget_strip_1
        }

        private fun renderSmall(context: Context, views: RemoteViews, todayCourses: List<TimeTableMySubject>, now: Calendar, refreshing: Boolean) {
            val minute = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
            val running = todayCourses.firstOrNull { minute in startMinute(it)..endMinute(it) }
            val next = todayCourses.firstOrNull { startMinute(it) > minute }
            val primary = running ?: next
            if (primary == null) {
                views.setViewVisibility(R.id.widget_primary_block, View.GONE)
                views.setViewVisibility(R.id.widget_empty_hint, View.VISIBLE)
                views.setTextViewText(R.id.widget_empty_hint, "今天没有课 · 好好休息")
                return
            }
            views.setViewVisibility(R.id.widget_primary_block, View.VISIBLE)
            views.setViewVisibility(R.id.widget_empty_hint, View.GONE)
            val statusText = if (running != null) "正在上课" else "下一节"
            views.setTextViewText(R.id.widget_status, statusText)
            views.setTextViewText(R.id.widget_course_name, WidgetText.truncate(primary.courseName, 8))
            val room = primary.classroom.orEmpty().ifBlank { "教室待定" }
            views.setTextViewText(R.id.widget_course_meta, "${minuteText(startMinute(primary))}-${minuteText(endMinute(primary))} · $room")
            val color = WidgetTheme.courseStripColor(context, primary.courseName)
            views.setInt(R.id.widget_strip, "setBackgroundColor", color)
        }

        private fun renderMedium(context: Context, views: RemoteViews, todayCourses: List<TimeTableMySubject>, now: Calendar, refreshing: Boolean) {
            val picked = todayCourses.take(3)
            for (i in 1..3) {
                if (i <= picked.size) {
                    val c = picked[i - 1]
                    views.setViewVisibility(rowId(i), View.VISIBLE)
                    views.setTextViewText(rowNameId(i), WidgetText.truncate(c.courseName, 10))
                    val room = c.classroom.orEmpty().ifBlank { "" }
                    val teacher = c.teacher.orEmpty().ifBlank { "" }
                    val meta = buildString {
                        append("${minuteText(startMinute(c))}-${minuteText(endMinute(c))}")
                        if (room.isNotEmpty()) append(" · ").append(WidgetText.truncate(room, 8))
                        if (teacher.isNotEmpty()) append(" · ").append(WidgetText.truncate(teacher, 6))
                    }
                    views.setTextViewText(rowMetaId(i), meta)
                    views.setTextViewText(rowTimeId(i), minuteText(startMinute(c)))
                    val color = WidgetTheme.courseStripColor(context, c.courseName)
                    views.setInt(rowStripId(i), "setBackgroundColor", color)
                } else {
                    views.setViewVisibility(rowId(i), View.GONE)
                }
            }
            views.setTextViewText(R.id.widget_updated, if (refreshing) "正在联网同步…" else "本地课表 · ${WidgetText.hhmm(System.currentTimeMillis())} 更新")
        }

        private fun renderLarge(
            context: Context,
            views: RemoteViews,
            courses: List<TimeTableMySubject>,
            week: Int,
            now: Calendar,
            refreshing: Boolean
        ) {
            val weekCourses = courses.filter { it.weeks.isNullOrEmpty() || week in it.weeks.orEmpty() }
            val cells = weekCourses.map { c ->
                WidgetChart.ScheduleCell(
                    weekday = c.weekday,
                    periodIndex = periodIndex(c.start),
                    name = c.courseName.orEmpty(),
                    room = c.classroom.orEmpty(),
                    color = WidgetTheme.courseStripColor(context, c.courseName.orEmpty())
                )
            }
            val bitmap = WidgetChart.drawWeekScheduleGrid(
                context = context,
                widthDp = 320f,
                heightDp = 140f,
                cells = cells
            )
            views.setImageViewBitmap(R.id.widget_chart, bitmap)
            views.setTextViewText(R.id.widget_updated, if (refreshing) "正在联网同步…" else "本周课程 · 第${week}周 · ${WidgetText.hhmm(System.currentTimeMillis())} 更新")
        }

        private suspend fun readCoursesSync(context: Context, term: String, studentId: String, password: String): List<TimeTableMySubject> {
            return runCatching {
                CoursesDataBase.getDatabase(context.applicationContext).courseDao()
                    .getCoursesByTerm(term, studentId, password)
                    .distinctBy { "${it.courseName}${it.classroom}${it.teacher}${it.start}${it.step}${it.weekday}${it.weeks}" }
            }.getOrDefault(emptyList())
        }

        private fun List<TimeTableMySubject>.forDay(day: Int, week: Int) =
            filter { it.weekday == day && (it.weeks.isNullOrEmpty() || week in it.weeks.orEmpty()) }
                .sortedBy { it.start }
    }
}
