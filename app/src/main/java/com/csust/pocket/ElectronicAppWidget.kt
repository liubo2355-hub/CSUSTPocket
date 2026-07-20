package com.csust.pocket

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.csust.pocket.feature.common.data.repository.ElectricityRepository
import com.csust.pocket.feature.common.ui.ElectronicActivity
import com.csust.pocket.overview.data.local.OverviewLocalCache
import com.csust.pocket.widget.internal.WidgetChart
import com.csust.pocket.widget.internal.WidgetSize
import com.csust.pocket.widget.internal.WidgetText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 宿舍电量桌面小组件。
 *
 * 支持 3 种尺寸（由 [WidgetSize.tier] 决定）：
 * - small : 当前剩余电量 + 状态徽章 + 更新时间
 * - medium: + 近 7 日折线图
 * - large : + 近 30 日折线图 + 平均日耗
 *
 * 数据流：
 * - 第一次绑定：先显示本地缓存（避免空白），后台异步联网，再渲染
 * - 显式刷新按钮：先显示刷新中，再真实联网，再渲染
 * - 退出登录：本地缓存被清空，下次刷新降级到"未绑定宿舍"提示
 */
class ElectronicAppWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { render(context, manager, it, RefreshState.CACHED) }
        if (ids.isNotEmpty()) refreshFromNetwork(context, manager, ids, fromBackgroundUpdate = true)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        manager: AppWidgetManager,
        widgetId: Int,
        newOptions: android.os.Bundle
    ) {
        render(context, manager, widgetId, RefreshState.CACHED)
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_REFRESH) {
            val widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return
            val manager = AppWidgetManager.getInstance(context)
            render(context, manager, widgetId, RefreshState.REFRESHING)
            val pendingResult = goAsync()
            refreshFromNetwork(context, manager, intArrayOf(widgetId), fromBackgroundUpdate = false, pendingResult)
            return
        }
        super.onReceive(context, intent)
    }

    companion object {
        const val ACTION_REFRESH = "com.csust.pocket.widget.REFRESH_ELECTRICITY"

        private enum class RefreshState { CACHED, REFRESHING, ONLINE, FAILED }

        private fun refreshFromNetwork(
            context: Context,
            manager: AppWidgetManager,
            ids: IntArray,
            fromBackgroundUpdate: Boolean,
            pendingResult: android.content.BroadcastReceiver.PendingResult? = null
        ) {
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                val repository = ElectricityRepository()
                val result = runCatching { repository.query(force = true) }.getOrNull()
                val state = if (result?.numericValue != null) RefreshState.ONLINE else RefreshState.FAILED
                ids.forEach { render(context, manager, it, state) }
                pendingResult?.finish()
            }
        }

        private fun render(context: Context, manager: AppWidgetManager, widgetId: Int, state: RefreshState) {
            val appContext = context.applicationContext
            val tier = WidgetSize.tier(appContext, manager, widgetId)
            val layoutRes = when (tier) {
                WidgetSize.SMALL -> R.layout.widget_electricity_small
                WidgetSize.MEDIUM -> R.layout.widget_electricity_medium
                else -> R.layout.widget_electricity_large
            }
            val views = RemoteViews(appContext.packageName, layoutRes)

            // 打开应用
            val openIntent = Intent(appContext, ElectronicActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            views.setOnClickPendingIntent(
                R.id.ele_widget_root_layout,
                PendingIntent.getActivity(appContext, 10_000 + widgetId, openIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            )
            // 刷新按钮
            val refreshIntent = Intent(appContext, ElectronicAppWidget::class.java).apply {
                action = ACTION_REFRESH
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            }
            views.setOnClickPendingIntent(
                R.id.ele_widget_refresh,
                PendingIntent.getBroadcast(appContext, 10_000 + widgetId, refreshIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            )

            val repository = ElectricityRepository()
            val binding = repository.getBinding()
            if (binding == null) {
                renderUnbound(appContext, views, tier)
                manager.updateAppWidget(widgetId, views)
                return
            }
            views.setTextViewText(R.id.ele_widget_binding, formatBinding(binding))

            val snapshot = OverviewLocalCache.getElectricitySnapshot()
            val value = snapshot?.lastValue
            val now = System.currentTimeMillis()

            when (tier) {
                WidgetSize.SMALL -> {
                    if (value == null) {
                        views.setTextViewText(R.id.ele_widget_num, "--")
                        views.setViewVisibility(R.id.ele_widget_unit, View.GONE)
                        views.setTextViewText(R.id.ele_widget_status, "暂无数据")
                        views.setTextViewText(R.id.ele_widget_updated, "点击刷新查询")
                    } else {
                        views.setTextViewText(R.id.ele_widget_num, String.format(java.util.Locale.getDefault(), "%.2f", value))
                        views.setViewVisibility(R.id.ele_widget_unit, View.VISIBLE)
                        views.setTextViewText(R.id.ele_widget_status, electricityStatus(value))
                        views.setTextViewText(R.id.ele_widget_updated, statusPrefix(state, snapshot.lastTime))
                    }
                }
                WidgetSize.MEDIUM -> {
                    if (value == null) {
                        views.setTextViewText(R.id.ele_widget_num, "--")
                        views.setViewVisibility(R.id.ele_widget_unit, View.GONE)
                        views.setTextViewText(R.id.ele_widget_status, "暂无数据")
                        views.setTextViewText(R.id.ele_widget_updated, "点击刷新查询")
                    } else {
                        views.setTextViewText(R.id.ele_widget_num, String.format(java.util.Locale.getDefault(), "%.2f", value))
                        views.setViewVisibility(R.id.ele_widget_unit, View.VISIBLE)
                        views.setTextViewText(R.id.ele_widget_status, electricityStatus(value))
                        views.setTextViewText(R.id.ele_widget_updated, statusPrefix(state, snapshot.lastTime))
                    }
                    manager.updateAppWidget(widgetId, views)
                    val history = snapshot?.history.orEmpty()
                    val points = pickRecentPoints(history, days = 7)
                    val bmp = WidgetChart.drawElectricityLineChart(
                        context = appContext,
                        widthDp = 160f,
                        heightDp = 70f,
                        points = points
                    )
                    views.setImageViewBitmap(R.id.ele_widget_chart, bmp)
                    manager.updateAppWidget(widgetId, views)
                    return
                }
                WidgetSize.LARGE -> {
                    if (value == null) {
                        views.setTextViewText(R.id.ele_widget_num, "--")
                        views.setViewVisibility(R.id.ele_widget_unit, View.GONE)
                        views.setTextViewText(R.id.ele_widget_status, "暂无数据")
                        views.setTextViewText(R.id.ele_widget_updated, "点击刷新查询")
                    } else {
                        views.setTextViewText(R.id.ele_widget_num, String.format(java.util.Locale.getDefault(), "%.2f", value))
                        views.setViewVisibility(R.id.ele_widget_unit, View.VISIBLE)
                        views.setTextViewText(R.id.ele_widget_status, electricityStatus(value))
                        views.setTextViewText(R.id.ele_widget_updated, statusPrefix(state, snapshot.lastTime))
                    }
                    manager.updateAppWidget(widgetId, views)
                    val history = snapshot?.history.orEmpty()
                    val points = pickRecentPoints(history, days = 30)
                    val bmp = WidgetChart.drawElectricityLineChart(
                        context = appContext,
                        widthDp = 320f,
                        heightDp = 90f,
                        points = points
                    )
                    views.setImageViewBitmap(R.id.ele_widget_chart, bmp)
                    // 平均日耗（基于折线数据点）
                    val avgText = computeAverageUsage(history)
                    views.setTextViewText(R.id.ele_widget_meta_left, avgText)
                    manager.updateAppWidget(widgetId, views)
                    return
                }
            }
            manager.updateAppWidget(widgetId, views)
        }

        private fun renderUnbound(context: Context, views: RemoteViews, tier: String) {
            when (tier) {
                WidgetSize.SMALL -> {
                    views.setTextViewText(R.id.ele_widget_binding, "尚未绑定宿舍")
                    views.setTextViewText(R.id.ele_widget_num, "--")
                    views.setViewVisibility(R.id.ele_widget_unit, View.GONE)
                    views.setTextViewText(R.id.ele_widget_status, "请先绑定")
                    views.setTextViewText(R.id.ele_widget_updated, "打开掌上长理设置宿舍信息")
                }
                WidgetSize.MEDIUM -> {
                    views.setTextViewText(R.id.ele_widget_binding, "尚未绑定宿舍")
                    views.setTextViewText(R.id.ele_widget_num, "--")
                    views.setViewVisibility(R.id.ele_widget_unit, View.GONE)
                    views.setTextViewText(R.id.ele_widget_status, "请先绑定")
                    views.setTextViewText(R.id.ele_widget_updated, "打开掌上长理设置宿舍信息")
                    val bmp = WidgetChart.drawElectricityLineChart(context, 160f, 70f, emptyList())
                    views.setImageViewBitmap(R.id.ele_widget_chart, bmp)
                }
                WidgetSize.LARGE -> {
                    views.setTextViewText(R.id.ele_widget_binding, "尚未绑定宿舍")
                    views.setTextViewText(R.id.ele_widget_num, "--")
                    views.setViewVisibility(R.id.ele_widget_unit, View.GONE)
                    views.setTextViewText(R.id.ele_widget_status, "请先绑定")
                    views.setTextViewText(R.id.ele_widget_updated, "打开掌上长理设置宿舍信息")
                    val bmp = WidgetChart.drawElectricityLineChart(context, 320f, 90f, emptyList())
                    views.setImageViewBitmap(R.id.ele_widget_chart, bmp)
                    views.setTextViewText(R.id.ele_widget_meta_left, "")
                }
            }
        }

        private fun formatBinding(binding: ElectricityRepository.ElectricityBinding): String {
            val school = binding.school.replace("校区", "").trim()
            return buildString {
                if (school.isNotEmpty()) append(school).append(" · ")
                append(binding.dorm).append(' ')
                append(binding.room)
            }
        }

        private fun electricityStatus(value: Float): String = when {
            value < 5f -> "电量不足"
            value < 10f -> "电量偏低"
            value < 30f -> "电量正常"
            else -> "电量充足"
        }

        private fun statusPrefix(state: RefreshState, lastTime: Long): String {
            val prefix = when (state) {
                RefreshState.REFRESHING -> "正在联网"
                RefreshState.FAILED -> "联网失败 · 缓存"
                RefreshState.ONLINE -> "已更新"
                RefreshState.CACHED -> "本地缓存"
            }
            return "$prefix · ${WidgetText.relativeTime(lastTime)}"
        }

        /**
         * 从 history 中取最近 [days] 天的剩余电量点；如果少于 2 个点则按整体返回（不补点）。
         */
        private fun pickRecentPoints(
            history: List<OverviewLocalCache.ElectricityHistoryEntry>,
            days: Int
        ): List<Pair<Long, Float>> {
            if (history.isEmpty()) return emptyList()
            val threshold = System.currentTimeMillis() - days * 24L * 60L * 60L * 1000L
            return history
                .filter { it.timestamp >= threshold }
                .sortedBy { it.timestamp }
                .map { it.timestamp to it.value }
        }

        private fun computeAverageUsage(history: List<OverviewLocalCache.ElectricityHistoryEntry>): String {
            if (history.size < 2) return "近 30 日 · 数据收集中"
            val points = pickRecentPoints(history, days = 30)
            if (points.size < 2) return "近 30 日 · 数据收集中"
            // 计算"已记录总时长"内的总消耗，除以天数（按 elapsed/DAY_MILLIS），并对充值进行修正
            var totalConsumed = 0f
            var totalElapsedDays = 0f
            for (i in 1 until points.size) {
                val prev = points[i - 1]
                val cur = points[i]
                val diff = prev.second - cur.second
                if (diff > 0f) {
                    val elapsedMs = (cur.first - prev.first).toFloat()
                    totalConsumed += diff
                    totalElapsedDays += elapsedMs / (24f * 60f * 60f * 1000f)
                }
            }
            return if (totalElapsedDays > 0f) {
                val avg = totalConsumed / totalElapsedDays
                String.format(java.util.Locale.getDefault(), "近 30 日 · 平均 %.2f 度/日", avg)
            } else "近 30 日 · 数据收集中"
        }
    }
}
