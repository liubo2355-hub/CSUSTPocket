package com.csust.pocket.overview.announcement

import androidx.compose.runtime.Immutable

enum class AnnouncementLevel {
    NORMAL,
    IMPORTANT,
    URGENT;

    companion object {
        fun fromWire(value: String?): AnnouncementLevel = when (value?.lowercase()) {
            "urgent" -> URGENT
            "important" -> IMPORTANT
            else -> NORMAL
        }
    }
}

@Immutable
data class AnnouncementUiModel(
    val id: String,
    val revision: Int,
    val level: AnnouncementLevel,
    val title: String,
    val summary: String,
    val content: List<String>,
    val publishAt: String,
    val displayTime: String,
    val expireAt: String,
    val actionText: String,
    val actionUrl: String,
    val isRead: Boolean
) {
    val readKey: String get() = "$id@$revision"
}

@Immutable
data class AnnouncementState(
    val announcements: List<AnnouncementUiModel> = emptyList(),
    val unreadCount: Int = 0,
    val isRefreshing: Boolean = false,
    val hasLoaded: Boolean = false,
    val lastUpdatedAtMillis: Long = 0L,
    val urgentToPresent: AnnouncementUiModel? = null
)

internal data class AnnouncementManifestDto(
    val revision: Int? = null,
    val announcements: List<AnnouncementDto>? = null
)

internal data class AnnouncementDto(
    val id: String? = null,
    val revision: Int? = null,
    val enabled: Boolean? = null,
    val level: String? = null,
    val title: String? = null,
    val summary: String? = null,
    val content: List<String>? = null,
    val publishAt: String? = null,
    val expireAt: String? = null,
    val minVersionCode: Int? = null,
    val maxVersionCode: Int? = null,
    val actionText: String? = null,
    val actionUrl: String? = null
)
