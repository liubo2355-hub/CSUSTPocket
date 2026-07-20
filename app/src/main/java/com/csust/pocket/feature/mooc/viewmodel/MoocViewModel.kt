package com.csust.pocket.feature.mooc.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.csust.pocket.R
import com.csust.pocket.common.data.local.mmkv.StudentInfoManager
import com.csust.pocket.core.network.ApiResponse
import com.csust.pocket.feature.mooc.data.MoocDataHelper
import com.csust.pocket.feature.mooc.data.local.MoocLocalCache
import com.csust.pocket.feature.mooc.data.model.MoocUiState
import com.csust.pocket.feature.mooc.data.model.HomeworkItem
import com.csust.pocket.utils.ResourceUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MoocViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        private const val TAG = "MoocViewModel"
    }

    private val _uiState = MutableStateFlow(MoocUiState())
    val uiState = _uiState.asStateFlow()

    init {
        restoreFromCache()
    }

    fun toggleCourseExpanded(courseId: String) {
        _uiState.value = _uiState.value.copy(
            courses = when (val c = _uiState.value.courses) {
                is ApiResponse.Success -> ApiResponse.Success(
                    c.data.map { item ->
                        if (item.course.id == courseId) item.copy(isExpanded = !item.isExpanded)
                        else item
                    }
                )
                else -> c
            }
        )
    }

    fun handleCourseClick(courseId: String) {
        toggleCourseExpanded(courseId)
    }

    fun dismissDialog() {
        _uiState.value = _uiState.value.copy(dialogMessage = null)
    }

    /** Drops the application-scoped MOOC state so a logout cannot leave old courses visible. */
    fun clearForLogout() {
        _uiState.value = MoocUiState()
    }

    fun requestForceRefresh() {
        _uiState.value = _uiState.value.copy(showForceRefreshPrompt = true)
    }

    fun dismissForceRefreshPrompt() {
        _uiState.value = _uiState.value.copy(showForceRefreshPrompt = false)
    }

    fun confirmForceRefresh() {
        _uiState.value = _uiState.value.copy(showForceRefreshPrompt = false)
        refreshCourses(forceRefresh = true)
    }

    fun refreshCourseHomeworks(courseId: String) {
        if (courseId.isBlank() || courseId in _uiState.value.loadingCourseIds) return
        viewModelScope.launch {
            val account = StudentInfoManager.studentId
            val password = StudentInfoManager.studentPassword
            if (account.isBlank() || password.isBlank()) return@launch
            _uiState.value = _uiState.value.copy(
                loadingCourseIds = _uiState.value.loadingCourseIds + courseId
            )
            try {
                val homeworks = MoocDataHelper.fetchCourseHomeworks(account, password, courseId)
                val current = _uiState.value.courses
                if (current is ApiResponse.Success) {
                    val updated = current.data.map { course ->
                        if (course.course.id == courseId) {
                            course.copy(
                                homeworks = homeworks.map { homework ->
                                    HomeworkItem(
                                        homework = homework,
                                        isDueSoon = MoocDataHelper.isHomeworkDueWithinOneDay(homework.deadline)
                                    )
                                }
                            )
                        } else course
                    }
                    MoocLocalCache.saveCourseItems(updated)
                    _uiState.value = _uiState.value.copy(courses = ApiResponse.Success(updated))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Fetch course homeworks failed: $courseId", e)
                showDialog("作业列表刷新失败：${e.message ?: "请稍后重试"}")
            } finally {
                _uiState.value = _uiState.value.copy(
                    loadingCourseIds = _uiState.value.loadingCourseIds - courseId
                )
            }
        }
    }

    private fun showDialog(message: String) {
        if (message.isBlank()) return
        _uiState.value = _uiState.value.copy(dialogMessage = message)
    }

    private fun hasCachedCourses(): Boolean {
        return (_uiState.value.courses as? ApiResponse.Success)?.data?.isNotEmpty() == true
    }

    fun shouldAutoRefreshOnEnter(): Boolean {
        val cachedCourses = (_uiState.value.courses as? ApiResponse.Success)?.data.orEmpty()
        val metadataNeedsMigration = cachedCourses.isNotEmpty() && cachedCourses.all { course ->
            course.courseNumber.orEmpty().isBlank() &&
                course.department.orEmpty().isBlank() &&
                course.teacher.orEmpty().isBlank()
        }
        return metadataNeedsMigration || MoocDataHelper.shouldAutoRefresh()
    }

    /**
     * 刷新慕课数据，委托给 MoocDataHelper。
     * 不需要单独 login，executeWithRetry 内部会自动处理登录重试。
     */
    fun refreshCourses(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            val account = StudentInfoManager.studentId
            val password = StudentInfoManager.studentPassword

            if (account.isEmpty() || password.isEmpty()) {
                val message = ResourceUtil.getStringRes(R.string.school_account_not_bound_cannot_query)
                _uiState.value = _uiState.value.copy(
                    loginState = ApiResponse.Error(message),
                    dialogMessage = message
                )
                if (!hasCachedCourses()) {
                    _uiState.value = _uiState.value.copy(courses = ApiResponse.Error(message))
                }
                return@launch
            }

            _uiState.value = _uiState.value.copy(
                isRefreshing = true,
                loginState = ApiResponse.Loading()
            )
            if (_uiState.value.courses !is ApiResponse.Success) {
                _uiState.value = _uiState.value.copy(courses = ApiResponse.Loading())
            }

            try {
                val courseItems = withContext(Dispatchers.IO) {
                    MoocDataHelper.fetchMoocCourses(account, password, forceRefresh)
                }
                _uiState.value = _uiState.value.copy(
                    loginState = ApiResponse.Success(true),
                    courses = ApiResponse.Success(courseItems)
                )
            } catch (e: Exception) {
                Log.e(TAG, "Fetch mooc courses failed", e)
                val message = e.message ?: ResourceUtil.getStringRes(R.string.error_unknown)
                _uiState.value = _uiState.value.copy(loginState = ApiResponse.Error(message))
                showDialog("慕课数据刷新失败：$message")
                if (!hasCachedCourses()) {
                    _uiState.value = _uiState.value.copy(courses = ApiResponse.Error(message))
                }
            } finally {
                _uiState.value = _uiState.value.copy(isRefreshing = false)
            }
        }
    }

    private fun restoreFromCache() {
        val cachedItems = MoocLocalCache.getCourseItems()
        if (cachedItems.isNotEmpty()) {
            val restored = cachedItems.map { item ->
                item.copy(
                    courseNumber = item.courseNumber.orEmpty(),
                    department = item.department.orEmpty(),
                    teacher = item.teacher.orEmpty(),
                    homeworks = item.homeworks.map { hwItem ->
                        hwItem.copy(isDueSoon = MoocDataHelper.isHomeworkDueWithinOneDay(hwItem.homework.deadline))
                    }
                )
            }
            _uiState.value = _uiState.value.copy(courses = ApiResponse.Success(restored))
        }
    }

    // 暴露给 Overview 使用的 extract 方法，委托给 MoocDataHelper
    fun extractPendingHomeworks(state: MoocUiState = _uiState.value): List<com.csust.pocket.overview.ui.model.OverviewHomeworkUiModel> {
        val courses = (state.courses as? ApiResponse.Success)?.data.orEmpty()
        return MoocDataHelper.extractPendingHomeworks(courses)
    }

    fun extractPendingTests(state: MoocUiState = _uiState.value): List<com.csust.pocket.overview.ui.model.OverviewTestUiModel> {
        val courses = (state.courses as? ApiResponse.Success)?.data.orEmpty()
        return MoocDataHelper.extractPendingTests(courses)
    }
}
