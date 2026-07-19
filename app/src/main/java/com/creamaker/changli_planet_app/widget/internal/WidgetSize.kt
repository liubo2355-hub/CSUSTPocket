package com.creamaker.changli_planet_app.widget.internal

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.res.Configuration
import androidx.core.content.ContextCompat
import com.creamaker.changli_planet_app.R

/**
 * 小组件尺寸档位判定。
 *
 * Android 桌面小组件的实际像素尺寸由 launcher 决定，[AppWidgetManager.getAppWidgetOptions]
 * 会以 dp 形式告知；我们按"较窄的一边 dp 值"分档：
 *
 * - SMALL  : 较窄边 < 180dp（约 2x1 桌面格子），单节课程
 * - MEDIUM : 较窄边 < 240dp（约 2x2 桌面格子），列表 2–3 行
 * - LARGE  : 较窄边 >= 240dp（约 4x2 桌面格子），完整列表或网格
 *
 * 如果 launcher 没有传 option（罕见），则按 [AppWidgetManager.getAppWidgetInfo] 的
 * minWidth/minHeight 估算。
 */
object WidgetSize {

    const val SMALL = "small"
    const val MEDIUM = "medium"
    const val LARGE = "large"

    /**
     * 解析当前 widget 实例的尺寸档位。
     *
     * @param context 任意 context，将取 applicationContext 读取资源
     * @param manager [AppWidgetManager]
     * @param widgetId 小组件实例 id
     */
    fun tier(context: Context, manager: AppWidgetManager, widgetId: Int): String {
        // Resizing is currently disabled. Some launchers report the full-screen
        // maximum size even for a fixed 2×2 widget, which previously selected
        // a mismatched large RemoteViews layout.
        return SMALL

        val options = manager.getAppWidgetOptions(widgetId)
        val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
        val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)
        val maxWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 0)
        val maxHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0)

        // 任一有效尺寸参数缺失时回退到 provider 描述的 minWidth/minHeight
        val effectiveMinWidth = if (minWidth > 0) minWidth else providerMinWidth(context, manager, widgetId)
        val effectiveMinHeight = if (minHeight > 0) minHeight else providerMinHeight(context, manager, widgetId)
        val effectiveMaxWidth = if (maxWidth > 0) maxWidth else effectiveMinWidth
        val effectiveMaxHeight = if (maxHeight > 0) maxHeight else effectiveMinHeight

        val smaller = minOf(effectiveMinWidth, effectiveMinHeight)
        val larger = minOf(effectiveMaxWidth, effectiveMaxHeight)
        return when {
            larger >= 320 || smaller >= 240 -> LARGE
            smaller >= 180 || larger >= 240 -> MEDIUM
            else -> SMALL
        }
    }

    private fun providerMinWidth(context: Context, manager: AppWidgetManager, widgetId: Int): Int {
        val info = runCatching { manager.getAppWidgetInfo(widgetId) }.getOrNull() ?: return 180
        val ctx = context.applicationContext
        val density = ctx.resources.displayMetrics.density
        return (info.minWidth / density).toInt().coerceAtLeast(110)
    }

    private fun providerMinHeight(context: Context, manager: AppWidgetManager, widgetId: Int): Int {
        val info = runCatching { manager.getAppWidgetInfo(widgetId) }.getOrNull() ?: return 110
        val ctx = context.applicationContext
        val density = ctx.resources.displayMetrics.density
        return (info.minHeight / density).toInt().coerceAtLeast(110)
    }
}

/**
 * 小组件主题色。按当前系统夜间模式加载 day/night 资源。
 * 渲染 Bitmap 时使用这套色（无法通过 RemoteViews 引用资源）。
 */
object WidgetTheme {

    fun isDark(context: Context): Boolean {
        val mode = context.applicationContext.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return mode == Configuration.UI_MODE_NIGHT_YES
    }

    fun surface(context: Context): Int = resolveColor(context, R.color.widget_surface)
    fun title(context: Context): Int = resolveColor(context, R.color.widget_title)
    fun subtitle(context: Context): Int = resolveColor(context, R.color.widget_subtitle)
    fun body(context: Context): Int = resolveColor(context, R.color.widget_body)
    fun meta(context: Context): Int = resolveColor(context, R.color.widget_meta)
    fun divider(context: Context): Int = resolveColor(context, R.color.widget_divider)
    fun dividerSubtle(context: Context): Int = resolveColor(context, R.color.widget_divider_subtle)
    fun indicator(context: Context): Int = resolveColor(context, R.color.widget_indicator)

    fun chartLine(context: Context): Int = resolveColor(context, R.color.widget_chart_line)
    fun chartGrid(context: Context): Int = resolveColor(context, R.color.widget_chart_grid)
    fun chartAxisText(context: Context): Int = resolveColor(context, R.color.widget_chart_axis_text)

    fun pillRedBg(context: Context): Int = resolveColor(context, R.color.widget_pill_red_bg)
    fun pillRedText(context: Context): Int = resolveColor(context, R.color.widget_pill_red_text)
    fun pillOrangeBg(context: Context): Int = resolveColor(context, R.color.widget_pill_orange_bg)
    fun pillOrangeText(context: Context): Int = resolveColor(context, R.color.widget_pill_orange_text)
    fun pillGreenBg(context: Context): Int = resolveColor(context, R.color.widget_pill_green_bg)
    fun pillGreenText(context: Context): Int = resolveColor(context, R.color.widget_pill_green_text)
    fun pillBlueBg(context: Context): Int = resolveColor(context, R.color.widget_pill_blue_bg)
    fun pillBlueText(context: Context): Int = resolveColor(context, R.color.widget_pill_blue_text)
    fun pillGreyBg(context: Context): Int = resolveColor(context, R.color.widget_pill_grey_bg)
    fun pillGreyText(context: Context): Int = resolveColor(context, R.color.widget_pill_grey_text)

    fun stripRed(context: Context): Int = resolveColor(context, R.color.widget_strip_red)
    fun stripOrange(context: Context): Int = resolveColor(context, R.color.widget_strip_orange)
    fun stripYellow(context: Context): Int = resolveColor(context, R.color.widget_strip_yellow)
    fun stripGreen(context: Context): Int = resolveColor(context, R.color.widget_strip_green)
    fun stripBlue(context: Context): Int = resolveColor(context, R.color.widget_strip_blue)
    fun stripPurple(context: Context): Int = resolveColor(context, R.color.widget_strip_purple)
    fun stripTeal(context: Context): Int = resolveColor(context, R.color.widget_strip_teal)
    fun stripPink(context: Context): Int = resolveColor(context, R.color.widget_strip_pink)

    fun barExcellent(context: Context): Int = resolveColor(context, R.color.widget_bar_excellent)
    fun barGood(context: Context): Int = resolveColor(context, R.color.widget_bar_good)
    fun barAverage(context: Context): Int = resolveColor(context, R.color.widget_bar_average)
    fun barPass(context: Context): Int = resolveColor(context, R.color.widget_bar_pass)
    fun barFail(context: Context): Int = resolveColor(context, R.color.widget_bar_fail)

    /** 根据课程名稳定分配一种色条颜色。 */
    fun courseStripColor(context: Context, courseName: String): Int {
        val palette = intArrayOf(
            stripBlue(context),
            stripOrange(context),
            stripGreen(context),
            stripPurple(context),
            stripRed(context),
            stripTeal(context),
            stripPink(context),
            stripYellow(context)
        )
        if (courseName.isEmpty()) return stripBlue(context)
        val idx = (courseName.hashCode() and Int.MAX_VALUE) % palette.size
        return palette[idx]
    }

    private fun resolveColor(context: Context, resId: Int): Int = ContextCompat.getColor(context.applicationContext, resId)
}
