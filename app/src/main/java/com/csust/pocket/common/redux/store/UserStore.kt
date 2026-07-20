package com.csust.pocket.common.redux.store

import android.app.Activity
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.csust.pocket.common.data.local.mmkv.StudentInfoManager
import com.csust.pocket.common.data.local.mmkv.UserInfoManager
import com.csust.pocket.common.data.remote.dto.DomesticUpdateManifest
import com.csust.pocket.common.data.remote.dto.GitHubRelease
import com.csust.pocket.common.redux.action.UserAction
import com.csust.pocket.common.redux.state.UserState
import com.csust.pocket.core.PlanetApplication
import com.csust.pocket.core.Store
import com.csust.pocket.utils.event.AppEventBus
import com.csust.pocket.utils.event.FinishEvent
import com.csust.pocket.widget.dialog.BindingFromWebDialog
import com.csust.pocket.widget.dialog.NormalResponseDialog
import com.csust.pocket.widget.dialog.UpdateDialog
import com.csust.pocket.widget.view.CustomToast
import com.google.gson.Gson
import com.dcelysia.csust_spider.core.RetrofitUtils as CsustRetrofitUtils
import com.dcelysia.csust_spider.education.data.remote.EducationData
import com.dcelysia.csust_spider.education.data.remote.services.AuthService
import com.dcelysia.csust_spider.mooc.data.remote.repository.MoocRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

class UserStore : Store<UserState, UserAction>() {
    companion object {
        private const val TAG = "UserStore"
        private var currentState = UserState()
        private const val GITEE_MANIFEST_URL =
            "https://gitee.com/liubo2355-hub/csustpocket/raw/master/update/latest.json"
        private const val GITHUB_API = "https://api.github.com/repos/liubo2355-hub/CSUSTPocket/releases/latest"
        private const val AUTO_CHECK_INTERVAL_MS = 30 * 60 * 1000L
        private const val MAX_UPDATE_ATTEMPTS = 3
        private val updateCheckInProgress = AtomicBoolean(false)
        private val bindingInProgress = AtomicBoolean(false)
        private val lastAutomaticCheckAt = AtomicLong(0L)
        private val updateDialogShowing = AtomicBoolean(false)
        @Volatile private var lastPromptedVersion: String? = null
        @Volatile private var automaticFailureNotified = false
    }

    private val handler = Handler(Looper.getMainLooper())
    private val storeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun handleEvent(action: UserAction) {
        currentState = when (action) {
            is UserAction.initilaize -> {
                _state.onNext(currentState)
                currentState
            }

            is UserAction.QueryIsLastedApk -> {
                val now = System.currentTimeMillis()
                if (!action.manual && now - lastAutomaticCheckAt.get() < AUTO_CHECK_INTERVAL_MS) {
                    Log.d(TAG, "跳过更新检查：仍在自动检查冷却期内")
                    return
                }
                if (!updateCheckInProgress.compareAndSet(false, true)) {
                    if (action.manual) {
                        CustomToast.showMessage(action.context, "正在检查更新，请稍候")
                    }
                    return
                }
                if (action.manual) {
                    CustomToast.showMessage(action.context, "正在检查更新…")
                } else {
                    lastAutomaticCheckAt.set(now)
                }
                storeScope.launch {
                    Log.d(TAG, "QueryIsLastedApk versionName=${action.versionName} versionCode=${action.versionCode}")
                    try {
                        val update = fetchLatestUpdateWithFallback()
                        if (update.versionName.isNotBlank()) {
                            val hasUpdate = if (update.versionCode > 0) {
                                update.versionCode > action.versionCode
                            } else {
                                isNewerVersion(update.versionName, action.versionName)
                            }
                            Log.d(
                                TAG,
                                "${update.source} latest=${update.versionName}(${update.versionCode}) " +
                                    "local=${action.versionName}(${action.versionCode})"
                            )
                            if (hasUpdate) {
                                handler.post {
                                    showUpdateDialogIfPossible(
                                        action = action,
                                        latestVersion = update.versionName,
                                        updateContent = update.releaseNotes.ifBlank { "新版 ${update.versionName}" },
                                        downloadUrl = update.downloadUrl.ifBlank {
                                            "https://github.com/liubo2355-hub/CSUSTPocket/releases/latest"
                                        }
                                    )
                                }
                            } else {
                                if (action.manual) {
                                    handler.post {
                                        CustomToast.showMessage(
                                            action.context,
                                            "已是最新版本 v${action.versionName}"
                                        )
                                    }
                                }
                                Log.d(TAG, "远程最新版号与当前一致或更低，无需更新")
                            }
                        }
                    } catch (error: Throwable) {
                        Log.e(TAG, "Gitee 与 GitHub 更新源均查询失败", error)
                        if (action.manual || !automaticFailureNotified) {
                            automaticFailureNotified = true
                            handler.post {
                                CustomToast.showMessage(
                                    action.context,
                                    if (action.manual) {
                                        "检查更新失败，请检查网络后重试"
                                    } else {
                                        "自动检查更新失败，可在“我的”页面重试"
                                    }
                                )
                            }
                        }
                    } finally {
                        updateCheckInProgress.set(false)
                    }
                }
                currentState
            }

            is UserAction.BindingStudentNumber -> {
                if (!bindingInProgress.compareAndSet(false, true)) return
                currentState.uiForLoading = true
                storeScope.launch {
                    CsustRetrofitUtils.ClearClient("moocClient")
                    CsustRetrofitUtils.ClearClient("EducationClient")
                    try {
                        val ssoResult = MoocRepository.instance
                            .login(action.studentNumber, action.studentPassword)
                            .filter { it !is com.dcelysia.csust_spider.core.Resource.Loading }
                            .first()
                        when (ssoResult) {
                            is com.dcelysia.csust_spider.core.Resource.Success -> {
                                Log.d(TAG, "sso登陆成功")
                                // SSO 登录成功后抓取 MOOC 用户信息并更新头像 / 用户名，失败不影响主流程
                                runCatching {
                                    val userResource = MoocRepository.instance
                                        .getLoginUser()
                                        .filter { it !is com.dcelysia.csust_spider.core.Resource.Loading }
                                        .first()
                                    if (userResource is com.dcelysia.csust_spider.core.Resource.Success) {
                                        userResource.data?.let { sso ->
                                            val avatarUrl = sso.avatar
                                            if (avatarUrl.isNotBlank()) {
                                                UserInfoManager.userAvatar = avatarUrl
                                                currentState.avatarUri = avatarUrl
                                                currentState.userProfile.avatarUrl = avatarUrl
                                            }
                                            if (sso.userName.isNotBlank()) {
                                                UserInfoManager.account = sso.userName
                                            }
                                        }
                                    }
                                }.onFailure { Log.w(TAG, "获取 SSO 用户信息失败", it) }

                                val eduSuccess = AuthService.login(
                                    action.studentNumber,
                                    action.studentPassword
                                )
                                if (eduSuccess) {
                                    currentState.uiForLoading = false
                                    EducationData.studentId = action.studentNumber
                                    EducationData.studentPassword = action.studentPassword
                                    Log.d(TAG, "教务登录成功")
                                    StudentInfoManager.studentId = action.studentNumber
                                    StudentInfoManager.studentPassword = action.studentPassword
                                    handler.post { AppEventBus.finishEvent.tryEmit(FinishEvent("bindingUser")) }
                                    PlanetApplication.clearSchoolDataCacheAll()
                                    _state.onNext(currentState)
                                } else {
                                    currentState.userStats.studentNumber = action.studentNumber
                                    currentState.uiForLoading = false
                                    handler.post {
                                        NormalResponseDialog(
                                            action.context,
                                            "学号或密码错误，请重试",
                                            "绑定失败"
                                        ).show()
                                    }
                                }
                            }

                            is com.dcelysia.csust_spider.core.Resource.Error -> {
                                currentState.userStats.studentNumber = action.studentNumber
                                currentState.uiForLoading = false
                                Log.d(TAG, "ssoResult:${ssoResult}")
                                if (!(ssoResult.msg.contains("请在手机网页登录一次"))) {
                                    handler.post {
                                        NormalResponseDialog(
                                            action.context,
                                            ssoResult.msg,
                                            "绑定失败"
                                        ).show()
                                    }
                                } else {
                                    handler.post {
                                        BindingFromWebDialog(
                                            action.context,
                                            ssoResult.msg ?: "SSO 登录失败",
                                            "绑定失败",
                                            action.webLogin
                                        ).show()
                                    }
                                }
                                _state.onNext(currentState)
                            }

                            else -> {
                                currentState.userStats.studentNumber = action.studentNumber
                                currentState.uiForLoading = false
                                handler.post {
                                    NormalResponseDialog(
                                        action.context,
                                        "网络或未知错误，请重试",
                                        "绑定失败"
                                    ).show()
                                }
                                _state.onNext(currentState)
                            }
                        }
                    } catch (e: Exception) {
                        currentState.userStats.studentNumber = action.studentNumber
                        currentState.uiForLoading = false
                        e.printStackTrace()
                        handler.post {
                            NormalResponseDialog(
                                action.context,
                                "网络错误: ${e.message ?: "未知"}",
                                "绑定失败"
                            ).show()
                        }
                        _state.onNext(currentState)
                    } finally {
                        bindingInProgress.set(false)
                    }
                }
                _state.onNext(currentState)
                currentState
            }

            is UserAction.WebLoginSuccess -> {
                StudentInfoManager.studentId = action.account
                StudentInfoManager.studentPassword = action.password
                currentState.userStats =
                    currentState.userStats.copy(studentNumber = action.account)
                _state.onNext(currentState)
                handleEvent(
                    UserAction.BindingStudentNumber(
                        action.context,
                        action.account,
                        action.password
                    ) {}
                )
                currentState
            }

            is UserAction.RefreshMoocProfileSilently -> {
                val studentId = StudentInfoManager.studentId
                val studentPassword = StudentInfoManager.studentPassword
                val alreadyHasProfile = UserInfoManager.account.isNotBlank()
                val canRefresh = studentId.isNotBlank() &&
                    studentPassword.isNotBlank() &&
                    !alreadyHasProfile
                if (canRefresh) {
                    storeScope.launch {
                        runCatching {
                            val ssoResult = MoocRepository.instance
                                .login(studentId, studentPassword)
                                .filter { it !is com.dcelysia.csust_spider.core.Resource.Loading }
                                .first()
                            if (ssoResult !is com.dcelysia.csust_spider.core.Resource.Success) {
                                return@runCatching
                            }

                            val userResource = MoocRepository.instance
                                .getLoginUser()
                                .filter { it !is com.dcelysia.csust_spider.core.Resource.Loading }
                                .first()
                            if (userResource !is com.dcelysia.csust_spider.core.Resource.Success) {
                                return@runCatching
                            }

                            userResource.data?.let { sso ->
                                if (sso.avatar.isNotBlank()) {
                                    UserInfoManager.userAvatar = sso.avatar
                                }
                                if (sso.userName.isNotBlank()) {
                                    UserInfoManager.account = sso.userName
                                }
                            }
                        }.onFailure { Log.w(TAG, "静默刷新 MOOC 用户资料失败", it) }
                    }
                }
                currentState
            }
        }
    }

    fun getUserState(): UserState = currentState

    // ---------- 国内主更新源 + GitHub 备用源 ----------

    private data class LatestUpdateInfo(
        val versionCode: Long,
        val versionName: String,
        val releaseNotes: String,
        val downloadUrl: String,
        val source: String
    )

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .build()
    }
    private val gson by lazy { Gson() }

    private suspend fun fetchLatestUpdateWithFallback(): LatestUpdateInfo {
        try {
            val manifest = fetchDomesticManifestWithRetry()
            if (manifest.versionName.isBlank() || manifest.apkUrl.isBlank()) {
                throw IOException("Gitee 更新清单缺少版本号或 APK 地址")
            }
            automaticFailureNotified = false
            return LatestUpdateInfo(
                versionCode = manifest.versionCode,
                versionName = manifest.versionName.trimStart('v'),
                releaseNotes = manifest.releaseNotes,
                downloadUrl = manifest.apkUrl,
                source = "Gitee"
            )
        } catch (error: Throwable) {
            Log.w(TAG, "Gitee 国内更新源不可用，切换 GitHub 备用源", error)
        }

        val release = fetchGitHubLatestReleaseWithRetry()
        val apkAsset = release.assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }
        automaticFailureNotified = false
        return LatestUpdateInfo(
            versionCode = 0,
            versionName = release.tagName.trimStart('v'),
            releaseNotes = release.body,
            downloadUrl = apkAsset?.downloadUrl.orEmpty(),
            source = "GitHub"
        )
    }

    private suspend fun fetchDomesticManifestWithRetry(): DomesticUpdateManifest {
        var lastError: Throwable? = null
        repeat(MAX_UPDATE_ATTEMPTS) { attempt ->
            try {
                return fetchDomesticManifest()
            } catch (error: Throwable) {
                lastError = error
                Log.w(TAG, "Gitee 更新检查第 ${attempt + 1} 次请求失败", error)
                if (attempt < MAX_UPDATE_ATTEMPTS - 1) {
                    delay(800L * (attempt + 1))
                }
            }
        }
        throw lastError ?: IOException("Gitee 更新检查失败")
    }

    private fun fetchDomesticManifest(): DomesticUpdateManifest {
        val request = Request.Builder()
            // Gitee raw 会缓存同一路径；时间戳确保每次检查都读取最新发布清单。
            .url("$GITEE_MANIFEST_URL?check=${System.currentTimeMillis()}")
            .header("User-Agent", "CSUSTPocket-Android")
            .header("Cache-Control", "no-cache")
            .build()
        return httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Gitee raw HTTP ${response.code}")
            }
            val body = response.body ?: throw IOException("Gitee 更新清单为空")
            gson.fromJson(body.charStream(), DomesticUpdateManifest::class.java)
                ?: throw IOException("Gitee 更新清单无效")
        }
    }

    private suspend fun fetchGitHubLatestReleaseWithRetry(): GitHubRelease {
        var lastError: Throwable? = null
        repeat(MAX_UPDATE_ATTEMPTS) { attempt ->
            try {
                return fetchGitHubLatestRelease()
            } catch (error: Throwable) {
                lastError = error
                Log.w(TAG, "更新检查第 ${attempt + 1} 次请求失败", error)
                if (attempt < MAX_UPDATE_ATTEMPTS - 1) {
                    delay(1_200L * (attempt + 1))
                }
            }
        }
        throw lastError ?: IOException("更新检查失败")
    }

    private fun fetchGitHubLatestRelease(): GitHubRelease {
        val request = Request.Builder()
            .url(GITHUB_API)
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "CSUSTPocket-Android")
            .header("Cache-Control", "no-cache")
            .build()
        return httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("GitHub API HTTP ${response.code}")
            }
            val body = response.body ?: throw IOException("GitHub API 返回空响应")
            gson.fromJson(body.charStream(), GitHubRelease::class.java)
                ?: throw IOException("GitHub API 返回无效数据")
        }
    }

    private fun isNewerVersion(latestVersion: String, currentVersion: String): Boolean {
        if (latestVersion == currentVersion) return false
        val appParts = parseVersion(currentVersion)
        val releaseParts = parseVersion(latestVersion)
        return if (appParts != null && releaseParts != null) {
            compareVersions(releaseParts, appParts) > 0
        } else {
            latestVersion > currentVersion
        }
    }

    private fun showUpdateDialogIfPossible(
        action: UserAction.QueryIsLastedApk,
        latestVersion: String,
        updateContent: String,
        downloadUrl: String
    ) {
        val activity = action.context as? Activity ?: return
        val destroyed = Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && activity.isDestroyed
        if (activity.isFinishing || destroyed) return
        if (!action.manual && lastPromptedVersion == latestVersion) return
        if (!updateDialogShowing.compareAndSet(false, true)) return

        lastPromptedVersion = latestVersion
        UpdateDialog(activity, latestVersion, updateContent, downloadUrl).apply {
            setOnDismissListener { updateDialogShowing.set(false) }
            show()
        }
    }

    /** 将 "2.0.9" 解析为 [2,0,9]；无法解析返回 null。 */
    private fun parseVersion(v: String): List<Int>? {
        try {
            val parts = v.split(".").map { it.toInt() }
            if (parts.size < 2) return null
            return parts
        } catch (_: NumberFormatException) { return null }
    }

    /** 返回 >0 表示 a 比 b 新，==0 同版，<0 旧。 */
    private fun compareVersions(a: List<Int>, b: List<Int>): Int {
        for (i in 0 until maxOf(a.size, b.size)) {
            val av = a.getOrElse(i) { 0 }; val bv = b.getOrElse(i) { 0 }
            if (av != bv) return av - bv
        }
        return 0
    }
}
