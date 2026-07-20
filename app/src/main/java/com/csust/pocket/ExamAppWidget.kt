package com.csust.pocket

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.csust.pocket.common.cache.CommonInfo
import com.csust.pocket.common.data.local.mmkv.StudentInfoManager
import com.csust.pocket.feature.common.data.local.mmkv.ExamArrangementCache
import com.csust.pocket.feature.common.ui.ExamArrangementActivity
import com.dcelysia.csust_spider.core.Resource
import com.dcelysia.csust_spider.education.data.remote.services.ExamArrangeService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime

/** 桌面考试安排：与首页一致优先显示今天，其次明天。 */
class ExamAppWidget : AppWidgetProvider() {
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
        const val ACTION_REFRESH = "com.csust.pocket.widget.REFRESH_EXAM"

        private suspend fun refreshCache() {
            runCatching {
                if (StudentInfoManager.studentId.isBlank() || StudentInfoManager.studentPassword.isBlank()) return
                val result = ExamArrangeService.getExamArrange(CommonInfo.getCurrentTerm())
                if (result is Resource.Success) {
                    ExamArrangementCache().saveExamArrangement(result.data)
                }
            }
        }

        internal fun render(context: Context, manager: AppWidgetManager, widgetId: Int, refreshing: Boolean) {
            val appContext = context.applicationContext
            val views = RemoteViews(appContext.packageName, R.layout.widget_exam)
            val openIntent = Intent(appContext, ExamArrangementActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            views.setOnClickPendingIntent(
                R.id.exam_widget_root,
                PendingIntent.getActivity(appContext, widgetId, openIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            )
            val refreshIntent = Intent(appContext, ExamAppWidget::class.java).apply {
                action = ACTION_REFRESH
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            }
            views.setOnClickPendingIntent(
                R.id.exam_widget_refresh,
                PendingIntent.getBroadcast(appContext, widgetId, refreshIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            )

            val now = LocalDateTime.now()
            val today = LocalDate.now()
            val tomorrow = today.plusDays(1)
            val exams = ExamArrangementCache().getExamArrangement().orEmpty()
            val currentExam = exams
                .filter { exam ->
                    val start = exam.examStartTimeval
                    val end = exam.examEndTimeval
                    start != null && end != null && !now.isBefore(start) && !now.isAfter(end)
                }
                .minByOrNull { it.examStartTimeval ?: LocalDateTime.MAX }
            val todayExam = exams
                .filter { it.examStartTimeval?.toLocalDate() == today && it.examStartTimeval?.isAfter(now) == true }
                .minByOrNull { it.examStartTimeval ?: LocalDateTime.MAX }
            val tomorrowExam = exams
                .filter { it.examStartTimeval?.toLocalDate() == tomorrow }
                .minByOrNull { it.examStartTimeval ?: LocalDateTime.MAX }
            val exam = currentExam ?: todayExam ?: tomorrowExam

            if (exam == null) {
                views.setTextViewText(R.id.exam_widget_badge, "✓")
                views.setTextViewText(R.id.exam_widget_name, if (StudentInfoManager.studentId.isBlank()) "请先登录" else "今天没有考试哦")
                views.setTextViewText(R.id.exam_widget_time, if (StudentInfoManager.studentId.isBlank()) "打开掌上长理完成登录" else "继续保持！")
                views.setTextViewText(R.id.exam_widget_place, "")
            } else {
                val status = when {
                    exam === currentExam -> "正在考试"
                    exam.examStartTimeval?.toLocalDate() == today -> "今日考试"
                    else -> "明日考试"
                }
                views.setTextViewText(R.id.exam_widget_badge, "考")
                views.setTextViewText(R.id.exam_widget_name, exam.courseNameval)
                views.setTextViewText(R.id.exam_widget_time, "$status · ${exam.examTime}")
                views.setTextViewText(R.id.exam_widget_place, listOf(exam.campus, exam.examRoomval).filter { it.isNotBlank() }.joinToString(" · "))
            }
            manager.updateAppWidget(widgetId, views)
        }
    }
}
