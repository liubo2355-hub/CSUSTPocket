package com.csust.pocket.widget.internal

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.abs

/**
 * 小组件渲染时使用的纯文本工具。
 */
object WidgetText {

    private val HHMM = ThreadLocal.withInitial { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    private val MDHM = ThreadLocal.withInitial { SimpleDateFormat("M-d HH:mm", Locale.getDefault()) }
    private val MD = ThreadLocal.withInitial { SimpleDateFormat("M/d", Locale.getDefault()) }

    fun hhmm(time: Long): String = HHMM.get()!!.format(Date(time))
    fun mdhm(time: Long): String = MDHM.get()!!.format(Date(time))
    fun md(time: Long): String = MD.get()!!.format(Date(time))

    /**
     * 把 timestamp 渲染为"X 秒前 / X 分钟前 / X 小时前 / 昨天 HH:mm / M-d HH:mm"等。
     */
    fun relativeTime(time: Long, now: Long = System.currentTimeMillis()): String {
        val diff = now - time
        if (diff < 0L) return hhmm(time)
        return when {
            diff < TimeUnit.MINUTES.toMillis(1) -> "刚刚"
            diff < TimeUnit.HOURS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toMinutes(diff)} 分钟前"
            diff < TimeUnit.DAYS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toHours(diff)} 小时前"
            diff < TimeUnit.DAYS.toMillis(2) -> "昨天 ${hhmm(time)}"
            diff < TimeUnit.DAYS.toMillis(7) -> "${TimeUnit.MILLISECONDS.toDays(diff)} 天前"
            else -> mdhm(time)
        }
    }

    /**
     * 把"距截止时间"渲染为人类可读倒计时（iOS 风格）：
     * - 已截止：已截止
     * - 1 小时内：X 分钟后
     * - 24 小时内：X 小时后
     * - 3 天内：明天 / 后天 / 周X
     * - 其它：M-d
     */
    fun countdown(toTime: Long, now: Long = System.currentTimeMillis()): String {
        val diff = toTime - now
        if (diff <= 0L) return "已截止"
        return when {
            diff < TimeUnit.MINUTES.toMillis(1) -> "1 分钟内"
            diff < TimeUnit.HOURS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toMinutes(diff)} 分钟后"
            diff < TimeUnit.DAYS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toHours(diff)} 小时后"
            diff < TimeUnit.DAYS.toMillis(2) -> "明天 ${hhmm(toTime)}"
            diff < TimeUnit.DAYS.toMillis(3) -> "后天 ${hhmm(toTime)}"
            diff < TimeUnit.DAYS.toMillis(7) -> {
                val weekday = (Calendar.getInstance().apply { timeInMillis = toTime }).get(Calendar.DAY_OF_WEEK)
                val name = arrayOf("周日", "周一", "周二", "周三", "周四", "周五", "周六")[weekday - 1]
                "$name ${hhmm(toTime)}"
            }
            else -> mdhm(toTime)
        }
    }

    /**
     * 倒计时档位（红/橙/绿）。
     */
    enum class CountdownTier { RED, ORANGE, GREEN, GREY }
    fun tierForDeadline(toTime: Long, now: Long = System.currentTimeMillis()): CountdownTier {
        val diff = toTime - now
        return when {
            diff <= 0L -> CountdownTier.GREY
            diff < TimeUnit.HOURS.toMillis(6) -> CountdownTier.RED
            diff < TimeUnit.HOURS.toMillis(24) -> CountdownTier.ORANGE
            diff < TimeUnit.DAYS.toMillis(3) -> CountdownTier.GREEN
            else -> CountdownTier.GREY
        }
    }

    fun weekdayShort(calendar: Calendar): String =
        arrayOf("周日", "周一", "周二", "周三", "周四", "周五", "周六")[calendar.get(Calendar.DAY_OF_WEEK) - 1]

    /**
     * "7月19日 周日 · 第19周"
     */
    fun todayLabel(calendar: Calendar = Calendar.getInstance(), weekNumber: Int? = null): String {
        val weekday = weekdayShort(calendar)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        return if (weekNumber != null) {
            "${month}月${day}日 $weekday · 第${weekNumber}周"
        } else {
            "${month}月${day}日 $weekday"
        }
    }

    /**
     * 拼接"08:00-09:40"格式。
     */
    fun timeRange(startMin: Int, endMin: Int): String = "${minText(startMin)}-${minText(endMin)}"

    private fun minText(value: Int): String = "%02d:%02d".format(Locale.getDefault(), value / 60, value % 60)

    @Suppress("unused")
    fun minToText(value: Int): String = minText(value)

    /**
     * 截断字符串到指定长度（字符数），超长时加省略号。
     */
    fun truncate(value: String, max: Int): String {
        if (value.length <= max) return value
        if (max <= 1) return value.substring(0, max)
        return value.substring(0, max - 1) + "…"
    }

    /**
     * 计算两个时间戳之间相差多少天（向下取整，按本地日历日）。
     */
    fun daysBetween(from: Long, to: Long): Int {
        if (from <= 0L || to <= 0L) return 0
        val a = Calendar.getInstance().apply { timeInMillis = from; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
        val b = Calendar.getInstance().apply { timeInMillis = to; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
        val diffMs = b.timeInMillis - a.timeInMillis
        return TimeUnit.MILLISECONDS.toDays(diffMs).toInt()
    }

    @Suppress("unused")
    fun sameDay(a: Long, b: Long): Boolean {
        val ca = Calendar.getInstance().apply { timeInMillis = a }
        val cb = Calendar.getInstance().apply { timeInMillis = b }
        return ca.get(Calendar.YEAR) == cb.get(Calendar.YEAR) &&
            ca.get(Calendar.DAY_OF_YEAR) == cb.get(Calendar.DAY_OF_YEAR)
    }

    fun safeAbs(value: Float): Float = if (value.isNaN() || value.isInfinite()) 0f else abs(value)

    @Suppress("unused")
    fun contextHint(context: Context): String = context.packageName
}
