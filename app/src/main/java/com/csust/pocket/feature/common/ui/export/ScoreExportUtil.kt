package com.csust.pocket.feature.common.ui.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.csust.pocket.feature.common.data.local.entity.Grade
import java.io.File

/**
 * 成绩单 CSV 导出 + 系统分享。
 *
 * - CSV 带 UTF-8 BOM，保证 Excel / WPS / Numbers 正确识别中文；
 * - 文件写入 cacheDir/exports（已在 file_paths.xml 的 cache-path 覆盖范围内），经 FileProvider 分享。
 */
object ScoreExportUtil {

    /** UTF-8 BOM（U+FEFF）；用码点构造，避免源码中出现不可见字符。 */
    private val BOM: String = 0xFEFF.toChar().toString()

    private val HEADERS = listOf(
        "学期", "课程名称", "成绩", "绩点", "学分", "学时",
        "课程性质", "课程属性", "考核方式", "考试性质"
    )

    fun buildCsv(grades: List<Grade>): String {
        val sb = StringBuilder()
        sb.append(BOM) // UTF-8 BOM
        sb.append(HEADERS.joinToString(",") { csvCell(it) }).append("\r\n")
        grades.forEach { g ->
            val row = listOf(
                g.item, g.name, g.grade, g.point, g.score, g.timeR,
                g.courseNature.orEmpty(), g.attribute, g.method, g.property
            )
            sb.append(row.joinToString(",") { csvCell(it) }).append("\r\n")
        }
        return sb.toString()
    }

    /** 按 CSV 规则转义：含逗号/引号/换行的单元格用双引号包裹，内部引号翻倍。 */
    private fun csvCell(value: String): String {
        val v = value.replace("\"", "\"\"")
        return if (v.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"$v\"" else v
    }

    /** 写入 cacheDir/exports/<fileName>.csv 并拉起系统分享。返回是否成功。 */
    fun exportAndShare(context: Context, grades: List<Grade>, fileName: String): Boolean {
        if (grades.isEmpty()) return false
        return try {
            val dir = File(context.cacheDir, "exports").apply { mkdirs() }
            val file = File(dir, "$fileName.csv")
            file.writeText(buildCsv(grades), Charsets.UTF_8)

            val uri = FileProvider.getUriForFile(
                context, "${context.packageName}.fileprovider", file
            )
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, fileName)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(send, "导出成绩单"))
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
