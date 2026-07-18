package com.creamaker.changli_planet_app.common.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ReleaseNotesFormatterTest {
    @Test
    fun formatsEscapedMarkdownAsReadableBullets() {
        val raw = "本次更新：\\n\\n- **修复更新弹窗**\\n- 优化[检查入口](https://example.com)\\n\\nAPK SHA-256：ABC"

        val result = ReleaseNotesFormatter.format(raw)

        assertEquals("• 修复更新弹窗\n• 优化检查入口", result)
        assertFalse(result.contains("\\n"))
        assertFalse(result.contains("SHA-256"))
    }
}
