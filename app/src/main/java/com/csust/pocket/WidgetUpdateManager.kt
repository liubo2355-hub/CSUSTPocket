package com.csust.pocket

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent

object WidgetUpdateManager {
    fun updateAll(context: Context) {
        updateProvider(context, TimeTableAppWidget::class.java)
        updateProvider(context, ElectronicAppWidget::class.java)
        updateProvider(context, ExamAppWidget::class.java)
        updateProvider(context, GradeAppWidget::class.java)
    }

    fun updateTimetable(context: Context) = updateProvider(context, TimeTableAppWidget::class.java)

    fun updateElectricity(context: Context) = updateProvider(context, ElectronicAppWidget::class.java)

    fun updateExam(context: Context) = updateProvider(context, ExamAppWidget::class.java)

    fun updateGrade(context: Context) = updateProvider(context, GradeAppWidget::class.java)

    private fun updateProvider(context: Context, provider: Class<*>) {
        val appContext = context.applicationContext
        val manager = AppWidgetManager.getInstance(appContext)
        val ids = manager.getAppWidgetIds(ComponentName(appContext, provider))
        if (ids.isEmpty()) return
        appContext.sendBroadcast(Intent(appContext, provider).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        })
    }
}
