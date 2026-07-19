package com.creamaker.changli_planet_app.widget.internal

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.TextPaint
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * 小组件图表绘制工具。
 *
 * RemoteViews 不支持任意 View，因此小组件内的折线图、柱状图必须先用 Canvas
 * 渲染到 Bitmap，再通过 [android.widget.RemoteViews.setImageViewBitmap] 注入。
 *
 * 图表配色使用 [WidgetTheme]，日间/夜间模式自动适配。
 */
object WidgetChart {

    private const val DEFAULT_DENSITY_FACTOR = 2f

    private fun dp(context: Context, value: Float): Float = value * context.resources.displayMetrics.density

    private fun resolveTypeface(context: Context, medium: Boolean): Typeface =
        Typeface.create(Typeface.DEFAULT, if (medium) Typeface.BOLD else Typeface.NORMAL)

    /**
     * 绘制剩余电量折线图（iOS 小组件风格）。
     *
     * @param context 上下文
     * @param widthDp 输出位图宽
     * @param heightDp 输出位图高
     * @param points 数据点（时间戳 + 剩余电量），按时间升序
     */
    fun drawElectricityLineChart(
        context: Context,
        widthDp: Float,
        heightDp: Float,
        points: List<Pair<Long, Float>>,
    ): Bitmap {
        val scale = DEFAULT_DENSITY_FACTOR
        val w = (dp(context, widthDp) * scale).toInt().coerceAtLeast(64)
        val h = (dp(context, heightDp) * scale).toInt().coerceAtLeast(64)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val bg = WidgetTheme.surface(context)
        canvas.drawColor(bg)

        if (points.size < 1) return bmp
        val lineColor = WidgetTheme.chartLine(context)
        val gridColor = WidgetTheme.chartGrid(context)
        val axisColor = WidgetTheme.chartAxisText(context)
        val regular = resolveTypeface(context, medium = false)
        val mediumTf = resolveTypeface(context, medium = true)

        val padding = dp(context, 6f) * scale
        val labelHeight = dp(context, 11f) * scale
        val chartLeft = padding
        val chartRight = w - padding
        val chartTop = padding
        val chartBottom = h - padding - labelHeight

        val gridPaint = Paint().apply {
            isAntiAlias = true
            color = gridColor
            strokeWidth = 1f * scale
            style = Paint.Style.STROKE
        }

        // 三条横向网格
        val rows = 3
        for (i in 0..rows) {
            val y = chartTop + (chartBottom - chartTop) * (i / rows.toFloat())
            canvas.drawLine(chartLeft, y, chartRight, y, gridPaint)
        }

        val values = points.map { it.second }
        val minValue = values.min()
        val maxValue = values.max()
        val range = (maxValue - minValue).takeIf { it > 0.001f } ?: 1f
        val valuePad = range * 0.18f
        val yMin = (minValue - valuePad).coerceAtLeast(0f)
        val yMax = maxValue + valuePad
        val yRange = (yMax - yMin).takeIf { it > 0.001f } ?: 1f

        fun toX(index: Int): Float {
            if (points.size == 1) return (chartLeft + chartRight) / 2f
            return chartLeft + (chartRight - chartLeft) * (index / (points.size - 1).toFloat())
        }

        fun toY(value: Float): Float =
            chartBottom - (chartBottom - chartTop) * ((value - yMin) / yRange)

        val fillColorStart = (lineColor and 0x00FFFFFF) or 0x55000000
        val fillColorEnd = (lineColor and 0x00FFFFFF) or 0x00000000

        // 渐变填充
        val path = Path()
        points.forEachIndexed { i, pair ->
            val x = toX(i)
            val y = toY(pair.second)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        val fillPath = Path(path)
        fillPath.lineTo(toX(points.lastIndex), chartBottom)
        fillPath.lineTo(toX(0), chartBottom)
        fillPath.close()
        val fillPaint = Paint().apply {
            isAntiAlias = true
            shader = LinearGradient(
                0f, chartTop, 0f, chartBottom,
                fillColorStart, fillColorEnd, Shader.TileMode.CLAMP
            )
            style = Paint.Style.FILL
        }
        canvas.drawPath(fillPath, fillPaint)

        // 折线
        val linePaint = Paint().apply {
            isAntiAlias = true
            color = lineColor
            strokeWidth = 2.4f * scale
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        canvas.drawPath(path, linePaint)

        // 数据点
        val dotPaint = Paint().apply {
            isAntiAlias = true
            color = lineColor
            style = Paint.Style.FILL
        }
        val dotHalo = Paint().apply {
            isAntiAlias = true
            color = bg
            style = Paint.Style.FILL
        }
        points.forEachIndexed { i, pair ->
            val x = toX(i)
            val y = toY(pair.second)
            canvas.drawCircle(x, y, 4.2f * scale, dotHalo)
            canvas.drawCircle(x, y, 2.6f * scale, dotPaint)
        }

        // y 轴标签（最大/最小）
        val axisPaint = TextPaint().apply {
            isAntiAlias = true
            color = axisColor
            textSize = dp(context, 9f) * scale
            typeface = regular ?: Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val maxLabel = String.format(Locale.getDefault(), "%.0f", maxValue)
        val minLabel = String.format(Locale.getDefault(), "%.0f", yMin.coerceAtLeast(0f))
        canvas.drawText(maxLabel, chartLeft, chartTop + axisPaint.textSize, axisPaint)
        canvas.drawText(minLabel, chartLeft, chartBottom, axisPaint)

        // x 轴日期标签：起点 + 终点
        if (points.isNotEmpty()) {
            val first = points.first().first
            val last = points.last().first
            val dateFmt = SimpleDateFormat("M/d", Locale.getDefault())
            val firstLabel = dateFmt.format(Date(first))
            val lastLabel = dateFmt.format(Date(last))
            val xAxisPaint = TextPaint(axisPaint).apply {
                typeface = mediumTf ?: Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }
            canvas.drawText(firstLabel, chartLeft, h - padding * 0.4f, xAxisPaint)
            val lastWidth = xAxisPaint.measureText(lastLabel)
            canvas.drawText(lastLabel, chartRight - lastWidth, h - padding * 0.4f, xAxisPaint)
        }

        return bmp
    }

    /**
     * 绘制 GPA 分布柱状图（iOS 成绩分析风格）。
     *
     * @param buckets GPA 桶数据：key（如 4.0/3.7/3.3...）+ count
     */
    fun drawGpaDistribution(
        context: Context,
        widthDp: Float,
        heightDp: Float,
        buckets: List<GpaBucket>,
    ): Bitmap {
        val scale = DEFAULT_DENSITY_FACTOR
        val w = (dp(context, widthDp) * scale).toInt().coerceAtLeast(64)
        val h = (dp(context, heightDp) * scale).toInt().coerceAtLeast(64)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(WidgetTheme.surface(context))

        val axisColor = WidgetTheme.chartAxisText(context)
        val gridColor = WidgetTheme.chartGrid(context)
        val regular = resolveTypeface(context, medium = false)
        val mediumTf = resolveTypeface(context, medium = true)

        val padding = dp(context, 6f) * scale
        val labelHeight = dp(context, 10f) * scale
        val valueHeight = dp(context, 9f) * scale
        val chartLeft = padding
        val chartRight = w - padding
        val chartTop = padding + valueHeight
        val chartBottom = h - padding - labelHeight

        if (buckets.isEmpty()) return bmp
        val maxCount = buckets.maxOf { it.count }.coerceAtLeast(1)

        // 横向网格 + 右侧 y 轴刻度（最大/一半/0）
        val gridPaint = Paint().apply {
            isAntiAlias = true
            color = gridColor
            strokeWidth = 1f * scale
            style = Paint.Style.STROKE
        }
        for (i in 0..3) {
            val y = chartTop + (chartBottom - chartTop) * (i / 3f)
            canvas.drawLine(chartLeft, y, chartRight, y, gridPaint)
        }
        val axisPaint = TextPaint().apply {
            isAntiAlias = true
            color = axisColor
            textSize = dp(context, 8f) * scale
            typeface = regular ?: Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        canvas.drawText(maxCount.toString(), chartRight - axisPaint.measureText(maxCount.toString()), chartTop + axisPaint.textSize, axisPaint)
        canvas.drawText("0", chartRight - axisPaint.measureText("0"), chartBottom, axisPaint)

        val gap = dp(context, 1.5f) * scale
        val totalGap = gap * (buckets.size + 1)
        val barWidth = ((chartRight - chartLeft) - totalGap) / buckets.size
        val cornerRadius = dp(context, 1.5f) * scale

        val valuePaint = TextPaint().apply {
            isAntiAlias = true
            color = axisColor
            textSize = dp(context, 8f) * scale
            typeface = mediumTf ?: Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val barPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
        }
        val labelPaint = TextPaint().apply {
            isAntiAlias = true
            color = axisColor
            textSize = dp(context, 8.5f) * scale
            typeface = regular ?: Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }

        buckets.forEachIndexed { i, bucket ->
            val x = chartLeft + gap + i * (barWidth + gap)
            val ratio = (bucket.count / maxCount.toFloat()).coerceAtLeast(0.02f)
            val barHeight = (chartBottom - chartTop) * ratio
            val top = chartBottom - barHeight
            barPaint.color = bucket.color(context)
            canvas.drawRoundRect(
                RectF(x, top, x + barWidth, chartBottom),
                cornerRadius, cornerRadius, barPaint
            )
            // 顶部数字
            if (bucket.count > 0) {
                val text = bucket.count.toString()
                val tw = valuePaint.measureText(text)
                canvas.drawText(text, x + (barWidth - tw) / 2f, top - dp(context, 1f) * scale, valuePaint)
            }
            // 底部 GPA 标签
            val label = String.format(Locale.getDefault(), "%.1f", bucket.gpa)
            canvas.drawText(label, x + barWidth / 2f, h - padding * 0.3f, labelPaint)
        }
        return bmp
    }

    /**
     * 绘制"本周课程"网格（iOS 周视图小组件风格）：
     * - 行：节次
     * - 列：周一至周日
     * - 每个课程块：左侧色条 + 课程名 + 教室
     */
    fun drawWeekScheduleGrid(
        context: Context,
        widthDp: Float,
        heightDp: Float,
        cells: List<ScheduleCell>,
        weekDays: List<String> = listOf("一", "二", "三", "四", "五", "六", "日"),
        periods: List<String> = listOf("1-2", "3-4", "5-6", "7-8", "9-10"),
    ): Bitmap {
        val scale = DEFAULT_DENSITY_FACTOR
        val w = (dp(context, widthDp) * scale).toInt().coerceAtLeast(120)
        val h = (dp(context, heightDp) * scale).toInt().coerceAtLeast(120)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(WidgetTheme.surface(context))

        val axisColor = WidgetTheme.chartAxisText(context)
        val gridColor = WidgetTheme.dividerSubtle(context)
        val bodyColor = WidgetTheme.body(context)
        val metaColor = WidgetTheme.meta(context)
        val regular = resolveTypeface(context, medium = false)
        val mediumTf = resolveTypeface(context, medium = true)

        val padTop = dp(context, 14f) * scale
        val padLeft = dp(context, 4f) * scale
        val padRight = dp(context, 4f) * scale
        val padBottom = dp(context, 2f) * scale
        val headerHeight = dp(context, 12f) * scale
        val labelWidth = dp(context, 22f) * scale
        val cellGap = dp(context, 1.5f) * scale

        val gridLeft = padLeft + labelWidth
        val gridRight = w - padRight
        val gridTop = padTop + headerHeight
        val gridBottom = h - padBottom
        val cellWidth = ((gridRight - gridLeft) - cellGap * (weekDays.size - 1)) / weekDays.size
        val cellHeight = ((gridBottom - gridTop) - cellGap * (periods.size - 1)) / periods.size

        // 表头
        val headerPaint = TextPaint().apply {
            isAntiAlias = true
            color = axisColor
            textSize = dp(context, 8.5f) * scale
            typeface = mediumTf ?: Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        weekDays.forEachIndexed { i, day ->
            val cx = gridLeft + i * (cellWidth + cellGap) + cellWidth / 2f
            canvas.drawText(day, cx, padTop + headerPaint.textSize, headerPaint)
        }

        // 左侧节次标签
        val rowLabelPaint = TextPaint().apply {
            isAntiAlias = true
            color = axisColor
            textSize = dp(context, 7.5f) * scale
            typeface = regular ?: Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        periods.forEachIndexed { i, period ->
            val cy = gridTop + i * (cellHeight + cellGap) + cellHeight / 2f + rowLabelPaint.textSize / 3f
            canvas.drawText(period, padLeft + labelWidth / 2f, cy, rowLabelPaint)
        }

        // 网格
        val gridPaint = Paint().apply {
            isAntiAlias = true
            color = gridColor
            strokeWidth = 1f * scale
            style = Paint.Style.STROKE
        }
        // 列分隔线
        for (i in 0..weekDays.size) {
            val x = gridLeft + i * (cellWidth + cellGap) - (if (i == weekDays.size) 0f else cellGap / 2f)
            canvas.drawLine(x, gridTop, x, gridBottom, gridPaint)
        }
        // 行分隔线
        for (i in 0..periods.size) {
            val y = gridTop + i * (cellHeight + cellGap) - (if (i == periods.size) 0f else cellGap / 2f)
            canvas.drawLine(gridLeft, y, gridRight, y, gridPaint)
        }

        // 课程块
        val stripPaint = Paint().apply { isAntiAlias = true; style = Paint.Style.FILL }
        val titlePaint = TextPaint().apply {
            isAntiAlias = true
            color = bodyColor
            textSize = dp(context, 8f) * scale
            typeface = mediumTf ?: Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val roomPaint = TextPaint().apply {
            isAntiAlias = true
            color = metaColor
            textSize = dp(context, 7f) * scale
            typeface = regular ?: Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val fillPaint = Paint().apply {
            isAntiAlias = true
            color = WidgetTheme.pillGreyBg(context)
            style = Paint.Style.FILL
        }

        cells.forEach { cell ->
            val dayIndex = (cell.weekday - 1).coerceIn(0, weekDays.lastIndex)
            val periodIndex = (cell.periodIndex).coerceIn(0, periods.lastIndex)
            val x = gridLeft + dayIndex * (cellWidth + cellGap)
            val y = gridTop + periodIndex * (cellHeight + cellGap)
            val rect = RectF(x, y, x + cellWidth, y + cellHeight)
            canvas.drawRoundRect(rect, dp(context, 3f) * scale, dp(context, 3f) * scale, fillPaint)
            // 色条
            val stripWidth = dp(context, 2f) * scale
            val stripRect = RectF(x + dp(context, 2f) * scale, y + dp(context, 2f) * scale,
                x + stripWidth + dp(context, 2f) * scale, y + cellHeight - dp(context, 2f) * scale)
            stripPaint.color = cell.color
            canvas.drawRoundRect(stripRect, dp(context, 1f) * scale, dp(context, 1f) * scale, stripPaint)
            // 文字
            val textX = stripRect.right + dp(context, 2f) * scale
            val textWidth = (x + cellWidth) - textX - dp(context, 2f) * scale
            val name = clipText(cell.name, titlePaint, textWidth, max = 6)
            canvas.drawText(name, textX, y + titlePaint.textSize + dp(context, 2f) * scale, titlePaint)
            if (cell.room.isNotEmpty()) {
                val room = clipText(cell.room, roomPaint, textWidth, max = 6)
                canvas.drawText(room, textX, y + titlePaint.textSize + roomPaint.textSize + dp(context, 4f) * scale, roomPaint)
            }
        }
        return bmp
    }

    private fun clipText(text: String, paint: TextPaint, maxWidth: Float, max: Int): String {
        if (text.length <= max) return text
        if (paint.measureText(text) <= maxWidth) return text
        var end = text.length
        while (end > 1 && paint.measureText(text.substring(0, end - 1) + "…") > maxWidth) end--
        return text.substring(0, (end - 1).coerceAtLeast(1)) + "…"
    }

    /** GPA 桶数据。 */
    data class GpaBucket(val gpa: Double, val count: Int) {
        fun color(context: Context): Int = when {
            gpa >= 3.7 -> WidgetTheme.barExcellent(context)
            gpa >= 3.3 -> WidgetTheme.barGood(context)
            gpa >= 3.0 -> WidgetTheme.barAverage(context)
            gpa >= 2.0 -> WidgetTheme.barPass(context)
            else -> WidgetTheme.barFail(context)
        }
    }

    /** 课程格子数据。 */
    data class ScheduleCell(
        val weekday: Int, // 1..7
        val periodIndex: Int, // 0..N-1
        val name: String,
        val room: String,
        val color: Int,
    )

    @Suppress("unused")
    fun truncatedDateLabel(calendar: Calendar): String =
        SimpleDateFormat("M/d", Locale.getDefault()).format(calendar.time)
}
