package com.creamaker.changli_planet_app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews
import com.creamaker.changli_planet_app.common.data.local.mmkv.StudentInfoManager
import com.creamaker.changli_planet_app.feature.common.data.local.entity.Grade
import com.creamaker.changli_planet_app.feature.common.data.local.mmkv.ScoreCache
import com.creamaker.changli_planet_app.feature.common.ui.ScoreInquiryActivity
import com.dcelysia.csust_spider.education.data.remote.EducationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.Locale

/** 桌面成绩概览：与首页一致显示 GPA 和加权平均分。 */
class GradeAppWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { render(context, manager, it, refreshing = false) }
        if (ids.isNotEmpty()) {
            val pending = goAsync()
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                refreshCache()
                ids.forEach { render(context, manager, it, refreshing = false) }
                pending.finish()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_REFRESH) {
            super.onReceive(context, intent)
            return
        }
        val widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return
        val manager = AppWidgetManager.getInstance(context)
        render(context, manager, widgetId, refreshing = true)
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            refreshCache()
            render(context, manager, widgetId, refreshing = false)
            pending.finish()
        }
    }

    companion object {
        const val ACTION_REFRESH = "com.creamaker.pocket_csust.widget.REFRESH_GRADE"

        private suspend fun refreshCache() {
            runCatching {
                if (StudentInfoManager.studentId.isBlank() || StudentInfoManager.studentPassword.isBlank()) return
                val response = EducationHelper.getCourseGrades()
                if (response?.code != "200") return
                val grades = response.data.orEmpty().map {
                    Grade(
                        id = it.courseID, item = it.semester, name = it.courseName,
                        grade = it.grade.toString(), flag = it.gradeIdentifier,
                        score = it.credit.toString(), timeR = it.totalHours.toString(),
                        point = it.gradePoint.toString(), upperReItem = it.retakeSemester,
                        method = it.assessmentMethod, property = it.examNature,
                        attribute = it.courseAttribute, reItem = it.groupName,
                        studyMode = it.studyMode, courseNature = it.courseNature.chineseName,
                        courseCategory = it.courseCategory, pscjUrl = it.gradeDetailUrl
                    )
                }
                ScoreCache.saveGrades(grades)
            }
        }

        internal fun render(context: Context, manager: AppWidgetManager, widgetId: Int, refreshing: Boolean) {
            val appContext = context.applicationContext
            val views = RemoteViews(appContext.packageName, R.layout.widget_grade)
            val openIntent = Intent(appContext, ScoreInquiryActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            views.setOnClickPendingIntent(
                R.id.grade_widget_root,
                PendingIntent.getActivity(appContext, widgetId, openIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            )
            val refreshIntent = Intent(appContext, GradeAppWidget::class.java).apply {
                action = ACTION_REFRESH
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            }
            views.setOnClickPendingIntent(
                R.id.grade_widget_refresh,
                PendingIntent.getBroadcast(appContext, widgetId, refreshIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            )

            val grades = ScoreCache.getGrades().orEmpty()
                .groupBy { it.name }
                .mapNotNull { (_, attempts) ->
                    attempts.find {
                        it.upperReItem.isNotBlank() ||
                            it.property.contains("重修", ignoreCase = true) ||
                            it.property.contains("补考", ignoreCase = true)
                    } ?: attempts.maxByOrNull { it.grade.toDoubleOrNull() ?: 0.0 }
                }
            val totalCredits = grades.sumOf { it.score.toDoubleOrNull() ?: 0.0 }
            if (grades.isEmpty() || totalCredits <= 0.0) {
                views.setTextViewText(R.id.grade_widget_course, if (StudentInfoManager.studentId.isBlank()) "请先登录" else "暂无成绩数据")
                views.setTextViewText(R.id.grade_widget_score, "--")
                views.setTextViewText(R.id.grade_widget_meta, if (refreshing) "正在刷新" else "点击刷新查询")
            } else {
                val gpa = grades.sumOf {
                    (it.score.toDoubleOrNull() ?: 0.0) * (it.point.toDoubleOrNull() ?: 0.0)
                } / totalCredits
                val average = grades.sumOf {
                    (it.score.toDoubleOrNull() ?: 0.0) * (it.grade.toDoubleOrNull() ?: 0.0)
                } / totalCredits
                views.setTextViewText(R.id.grade_widget_course, "GPA")
                views.setTextViewText(R.id.grade_widget_score, String.format(Locale.CHINA, "%.2f", gpa))
                views.setTextViewText(
                    R.id.grade_widget_meta,
                    "平均分：${String.format(Locale.CHINA, "%.1f", average)}${if (refreshing) " · 刷新中" else ""}"
                )
                views.setTextColor(R.id.grade_widget_score, Color.rgb(18, 185, 105))
            }
            manager.updateAppWidget(widgetId, views)
        }
    }
}
