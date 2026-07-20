package com.csust.pocket.common.data.remote.dto

/**
 * Gitee 国内更新源的稳定清单格式。
 *
 * 清单与 APK 都放在公开仓库中，应用无需登录即可检查和下载更新。
 */
data class DomesticUpdateManifest(
    val versionCode: Long = 0,
    val versionName: String = "",
    val releaseNotes: String = "",
    val apkUrl: String = "",
    val sha256: String = "",
    val publishedAt: String = ""
)
