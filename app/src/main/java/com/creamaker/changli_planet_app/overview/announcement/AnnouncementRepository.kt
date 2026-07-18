package com.creamaker.changli_planet_app.overview.announcement

import android.util.Log
import com.creamaker.changli_planet_app.BuildConfig
import com.google.gson.Gson
import com.tencent.mmkv.MMKV
import java.io.IOException
import java.time.OffsetDateTime
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

class AnnouncementRepository {
    companion object {
        private const val TAG = "AnnouncementRepository"
        private const val MANIFEST_URL =
            "https://gitee.com/liubo2355-hub/csustpocket/raw/master/config/announcements.json"
        private const val CACHE_ID = "announcement_cache"
        private const val KEY_MANIFEST = "manifest"
        private const val KEY_ETAG = "etag"
        private const val KEY_LAST_MODIFIED = "last_modified"
        private const val KEY_LAST_CHECK = "last_check"
        private const val KEY_READ = "read_keys"
        private const val KEY_URGENT_SHOWN = "urgent_shown_keys"
        const val HOME_REFRESH_THROTTLE_MS = 60_000L
        const val FOREGROUND_REFRESH_INTERVAL_MS = 5 * 60_000L
    }

    private val mmkv by lazy { MMKV.mmkvWithID(CACHE_ID) }
    private val gson by lazy { Gson() }
    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .build()
    }

    fun loadCached(): AnnouncementState = stateFromCachedManifest()

    suspend fun refresh(force: Boolean = false): AnnouncementState = withContext(Dispatchers.IO) {
        val nowMillis = System.currentTimeMillis()
        val lastCheck = mmkv.decodeLong(KEY_LAST_CHECK, 0L)
        if (!force && nowMillis - lastCheck < HOME_REFRESH_THROTTLE_MS) {
            return@withContext stateFromCachedManifest()
        }

        try {
            val requestBuilder = Request.Builder()
                .url(MANIFEST_URL)
                .header("User-Agent", "CSUSTPocket-Android/${BuildConfig.VERSION_NAME}")
                .header("Cache-Control", "no-cache")
            mmkv.decodeString(KEY_ETAG, "")?.takeIf(String::isNotBlank)?.let {
                requestBuilder.header("If-None-Match", it)
            }
            mmkv.decodeString(KEY_LAST_MODIFIED, "")?.takeIf(String::isNotBlank)?.let {
                requestBuilder.header("If-Modified-Since", it)
            }

            client.newCall(requestBuilder.build()).execute().use { response ->
                when {
                    response.code == 304 -> Unit
                    !response.isSuccessful -> throw IOException("公告源 HTTP ${response.code}")
                    else -> {
                        val raw = response.body?.string().orEmpty()
                        val parsed = gson.fromJson(raw, AnnouncementManifestDto::class.java)
                            ?: throw IOException("公告清单格式无效")
                        AnnouncementPolicy.visibleAnnouncements(
                            parsed,
                            BuildConfig.VERSION_CODE,
                            OffsetDateTime.now(),
                            emptySet(),
                            emptySet()
                        )
                        mmkv.encode(KEY_MANIFEST, raw)
                        response.header("ETag")?.let { mmkv.encode(KEY_ETAG, it) }
                        response.header("Last-Modified")?.let { mmkv.encode(KEY_LAST_MODIFIED, it) }
                    }
                }
            }
        } catch (error: Throwable) {
            Log.w(TAG, "公告刷新失败，继续使用缓存", error)
        } finally {
            // A failed request is still a check. This prevents every quick return to
            // the home screen from starting another network request on poor networks.
            mmkv.encode(KEY_LAST_CHECK, nowMillis)
        }
        stateFromCachedManifest().copy(lastUpdatedAtMillis = mmkv.decodeLong(KEY_LAST_CHECK, 0L))
    }

    fun markRead(readKey: String): AnnouncementState {
        val keys = readKeys().toMutableSet().apply { add(readKey) }
        mmkv.encode(KEY_READ, keys)
        return stateFromCachedManifest()
    }

    fun markUrgentPresented(readKey: String): AnnouncementState {
        val keys = urgentShownKeys().toMutableSet().apply { add(readKey) }
        mmkv.encode(KEY_URGENT_SHOWN, keys)
        return stateFromCachedManifest()
    }

    private fun stateFromCachedManifest(): AnnouncementState {
        val raw = mmkv.decodeString(KEY_MANIFEST, "").orEmpty()
        if (raw.isBlank()) return AnnouncementState(hasLoaded = true)
        return runCatching {
            val manifest = gson.fromJson(raw, AnnouncementManifestDto::class.java)
            AnnouncementPolicy.visibleAnnouncements(
                manifest = manifest,
                versionCode = BuildConfig.VERSION_CODE,
                now = OffsetDateTime.now(),
                readKeys = readKeys(),
                shownUrgentKeys = urgentShownKeys()
            ).copy(lastUpdatedAtMillis = mmkv.decodeLong(KEY_LAST_CHECK, 0L))
        }.getOrElse {
            Log.w(TAG, "公告缓存损坏", it)
            AnnouncementState(hasLoaded = true)
        }
    }

    private fun readKeys(): Set<String> =
        mmkv.decodeStringSet(KEY_READ, emptySet())?.toSet().orEmpty()

    private fun urgentShownKeys(): Set<String> =
        mmkv.decodeStringSet(KEY_URGENT_SHOWN, emptySet())?.toSet().orEmpty()
}
