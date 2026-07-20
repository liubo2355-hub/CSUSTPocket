package com.csust.pocket.feature.mooc.ui

import android.os.Bundle
import com.csust.pocket.R
import com.csust.pocket.base.ComposeActivity
import com.csust.pocket.common.data.local.mmkv.StudentInfoManager.studentId
import com.csust.pocket.common.data.local.mmkv.StudentInfoManager.studentPassword
import com.csust.pocket.core.PlanetApplication
import com.csust.pocket.core.Route
import com.csust.pocket.widget.view.CustomToast

class MoocActivity : ComposeActivity() {
    companion object {
        const val EXTRA_PAGE = "mooc_page"
        const val PAGE_COURSES = "courses"
        const val PAGE_HOMEWORK = "homework"
    }

    private val moocViewModel by lazy { (application as PlanetApplication).moocViewModel }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (studentId.isEmpty() || studentPassword.isEmpty()) {
            CustomToast.showMessage(this, getString(R.string.bind_notification))
            Route.goBindingUser(this)
            finish()
            return
        }

        setComposeContent {
            MoocScreen(
                moocViewModel = moocViewModel,
                pageMode = if (intent.getStringExtra(EXTRA_PAGE) == PAGE_COURSES) {
                    MoocPageMode.Courses
                } else {
                    MoocPageMode.Homework
                },
                onBack = ::finish,
                onOpenCoursePage = { courseId ->
                    Route.goMoocCoursePage(this, courseId)
                }
            )
        }
    }

    override fun onResume() {
        super.onResume()
        if (studentId.isNotEmpty() && studentPassword.isNotEmpty() && moocViewModel.shouldAutoRefreshOnEnter()) {
            moocViewModel.refreshCourses()
        }
    }
}
