package com.creamaker.changli_planet_app.overview.announcement

import java.time.OffsetDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AnnouncementPolicyTest {
    private val now = OffsetDateTime.parse("2026-07-18T17:00:00+08:00")

    @Test
    fun filtersByTimeAndVersion() {
        val state = AnnouncementPolicy.visibleAnnouncements(
            manifest = AnnouncementManifestDto(
                announcements = listOf(
                    dto(id = "visible", minVersionCode = 1),
                    dto(id = "future", publishAt = "2026-07-19T00:00:00+08:00"),
                    dto(id = "expired", expireAt = "2026-07-18T16:00:00+08:00"),
                    dto(id = "newer-app", minVersionCode = 99)
                )
            ),
            versionCode = 53,
            now = now,
            readKeys = emptySet(),
            shownUrgentKeys = emptySet()
        )

        assertEquals(listOf("visible"), state.announcements.map { it.id })
    }

    @Test
    fun readRevisionRemovesUnreadDot() {
        val state = AnnouncementPolicy.visibleAnnouncements(
            manifest = AnnouncementManifestDto(announcements = listOf(dto(id = "notice", revision = 2))),
            versionCode = 53,
            now = now,
            readKeys = setOf("notice@2"),
            shownUrgentKeys = emptySet()
        )

        assertEquals(0, state.unreadCount)
        assertEquals(true, state.announcements.single().isRead)
    }

    @Test
    fun urgentAnnouncementOnlyAutoPresentsOnce() {
        val manifest = AnnouncementManifestDto(
            announcements = listOf(dto(id = "urgent", level = "urgent"))
        )
        val first = AnnouncementPolicy.visibleAnnouncements(
            manifest, 53, now, emptySet(), emptySet()
        )
        val later = AnnouncementPolicy.visibleAnnouncements(
            manifest, 53, now, emptySet(), setOf("urgent@1")
        )

        assertEquals("urgent", first.urgentToPresent?.id)
        assertNull(later.urgentToPresent)
        assertEquals(1, later.unreadCount)
    }

    private fun dto(
        id: String,
        revision: Int = 1,
        level: String = "normal",
        publishAt: String = "2026-07-18T16:00:00+08:00",
        expireAt: String = "2026-08-18T16:00:00+08:00",
        minVersionCode: Int = 1
    ) = AnnouncementDto(
        id = id,
        revision = revision,
        enabled = true,
        level = level,
        title = "测试公告",
        summary = "摘要",
        content = listOf("内容"),
        publishAt = publishAt,
        expireAt = expireAt,
        minVersionCode = minVersionCode
    )
}
