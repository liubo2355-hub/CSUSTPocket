package com.csust.pocket.common.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * GitHub Release API 返回的资产文件。
 * https://api.github.com/repos/{owner}/{repo}/releases/latest
 */
data class GitHubReleaseAsset(
    val name: String = "",
    @SerializedName("browser_download_url") val downloadUrl: String = ""
)

data class GitHubRelease(
    @SerializedName("tag_name") val tagName: String = "",
    @SerializedName("published_at") val publishedAt: String = "",
    val body: String = "",
    val assets: List<GitHubReleaseAsset> = emptyList()
)
