package com.csust.pocket.feature.mooc.data.model

import com.csust.pocket.core.network.ApiResponse
import com.dcelysia.csust_spider.mooc.data.remote.dto.MoocHomework
import com.dcelysia.csust_spider.mooc.data.remote.dto.MoocTest
import com.dcelysia.csust_spider.mooc.data.remote.dto.PendingAssignmentCourse

data class HomeworkItem(
    val homework: MoocHomework,
    val isDueSoon: Boolean = false
)

data class CourseItem(
    val course: PendingAssignmentCourse,
    val courseNumber: String = "",
    val department: String = "",
    val teacher: String = "",
    val homeworks: List<HomeworkItem> = emptyList(),
    val tests: List<MoocTest> = emptyList(),
    val isExpanded: Boolean = false,
)

data class MoocUiState(
    val loginState: ApiResponse<Boolean> = ApiResponse.Loading(),
    val courses: ApiResponse<List<CourseItem>> = ApiResponse.Loading(),
    val isRefreshing: Boolean = false,
    val dialogMessage: String? = null,
    val showForceRefreshPrompt: Boolean = false,
    val loadingCourseIds: Set<String> = emptySet(),
)

fun String.cleanCourseId(): String =
    substringBefore("&").replace(Regex("[^0-9]"), "")
