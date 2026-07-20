package com.csust.pocket.overview.announcement

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

internal object AnnouncementPolicy {
    private val displayFormatter = DateTimeFormatter.ofPattern("M月d日 HH:mm", Locale.CHINA)

    fun visibleAnnouncements(
        manifest: AnnouncementManifestDto,
        versionCode: Int,
        now: OffsetDateTime,
        readKeys: Set<String>,
        shownUrgentKeys: Set<String>
    ): AnnouncementState {
        val items = manifest.announcements.orEmpty()
            .mapNotNull { dto -> dto.toUiModelOrNull(versionCode, now, readKeys) }
            .sortedWith(
                compareByDescending<AnnouncementUiModel> { it.level.priority }
                    .thenByDescending { parseTime(it.publishAt)?.toInstant() }
            )
        val urgent = items.firstOrNull {
            it.level == AnnouncementLevel.URGENT && !it.isRead && it.readKey !in shownUrgentKeys
        }
        return AnnouncementState(
            announcements = items,
            unreadCount = items.count { !it.isRead },
            hasLoaded = true,
            urgentToPresent = urgent
        )
    }

    private fun AnnouncementDto.toUiModelOrNull(
        versionCode: Int,
        now: OffsetDateTime,
        readKeys: Set<String>
    ): AnnouncementUiModel? {
        val safeId = id?.trim().orEmpty()
        val safeTitle = title?.trim().orEmpty()
        if (safeId.isBlank() || safeTitle.isBlank() || enabled == false) return null
        if (versionCode < (minVersionCode ?: 1) || versionCode > (maxVersionCode ?: Int.MAX_VALUE)) return null

        val publishTime = parseTime(publishAt)
        val expireTime = parseTime(expireAt)
        if (publishTime != null && now.isBefore(publishTime)) return null
        if (expireTime != null && !now.isBefore(expireTime)) return null

        val safeRevision = (revision ?: 1).coerceAtLeast(1)
        val readKey = "$safeId@$safeRevision"
        return AnnouncementUiModel(
            id = safeId,
            revision = safeRevision,
            level = AnnouncementLevel.fromWire(level),
            title = safeTitle,
            summary = summary?.trim().orEmpty(),
            content = content.orEmpty().map(String::trim).filter(String::isNotBlank),
            publishAt = publishAt.orEmpty(),
            displayTime = publishTime?.format(displayFormatter).orEmpty(),
            expireAt = expireAt.orEmpty(),
            actionText = actionText?.trim().orEmpty(),
            actionUrl = actionUrl?.trim().orEmpty(),
            isRead = readKey in readKeys
        )
    }

    private fun parseTime(value: String?): OffsetDateTime? =
        value?.takeIf(String::isNotBlank)?.let { runCatching { OffsetDateTime.parse(it) }.getOrNull() }

    private val AnnouncementLevel.priority: Int
        get() = when (this) {
            AnnouncementLevel.URGENT -> 3
            AnnouncementLevel.IMPORTANT -> 2
            AnnouncementLevel.NORMAL -> 1
        }
}
