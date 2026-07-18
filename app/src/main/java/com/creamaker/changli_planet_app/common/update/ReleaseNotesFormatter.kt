package com.creamaker.changli_planet_app.common.update

/** 将 GitHub Release 的 Markdown 文本转换成适合更新弹窗显示的纯文本条目。 */
object ReleaseNotesFormatter {
    private val bulletPattern = Regex("^[-*+]\\s+")
    private val orderedPattern = Regex("^\\d+[.)]\\s+")
    private val markdownLinkPattern = Regex("\\[([^]]+)]\\([^)]+\\)")

    fun format(raw: String): String {
        val normalized = raw
            .replace("\\r\\n", "\n")
            .replace("\\n", "\n")
            .replace("\r\n", "\n")
            .replace('\r', '\n')

        val result = mutableListOf<String>()
        normalized.lineSequence().forEach { sourceLine ->
            var line = sourceLine.trim()
            if (line.isBlank()) return@forEach
            if (line.contains("SHA-256", ignoreCase = true) ||
                line.contains("SHA256", ignoreCase = true)
            ) return@forEach

            line = line.trimStart('#').trim()
            val heading = line.trimEnd(':', '：').trim()
            if (heading.equals("本次更新", ignoreCase = true) ||
                heading.equals("更新内容", ignoreCase = true) ||
                heading.equals("更新说明", ignoreCase = true)
            ) return@forEach

            line = markdownLinkPattern.replace(line, "$1")
                .replace("**", "")
                .replace("__", "")
                .replace("`", "")

            line = when {
                bulletPattern.containsMatchIn(line) -> bulletPattern.replace(line, "• ")
                orderedPattern.containsMatchIn(line) -> orderedPattern.replace(line, "• ")
                else -> line
            }
            if (line.isNotBlank()) result += line
        }

        return result.joinToString("\n").take(1_200)
            .ifBlank { "修复已知问题并提升使用体验" }
    }
}
