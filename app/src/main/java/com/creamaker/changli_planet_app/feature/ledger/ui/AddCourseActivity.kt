package com.creamaker.changli_planet_app.feature.ledger.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import com.creamaker.changli_planet_app.base.FullScreenActivity
import com.creamaker.changli_planet_app.common.data.local.mmkv.StudentInfoManager
import com.creamaker.changli_planet_app.core.PlanetApplication
import com.creamaker.changli_planet_app.core.theme.AppSkinTheme
import com.creamaker.changli_planet_app.databinding.ActivityAddCourseInTimetableBinding
import com.creamaker.changli_planet_app.feature.common.data.local.entity.TimeTableMySubject
import com.creamaker.changli_planet_app.feature.common.data.local.room.database.CoursesDataBase
import com.creamaker.changli_planet_app.feature.ledger.ui.compose.AddCourseScreen
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddCourseActivity : FullScreenActivity<ActivityAddCourseInTimetableBinding>() {
    private val gson by lazy { Gson() }
    private val database by lazy { CoursesDataBase.getDatabase(PlanetApplication.appContext) }
    private val studentId by lazy { StudentInfoManager.studentId }
    private val studentPassword by lazy { StudentInfoManager.studentPassword }

    override fun createViewBinding(): ActivityAddCourseInTimetableBinding =
        ActivityAddCourseInTimetableBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val start = intent.getIntExtra("start", 1).coerceIn(1, 10)
        val span = intent.getIntExtra("span", 1).coerceIn(1, 11 - start)
        val week = intent.getIntExtra("curWeek", 1).coerceIn(1, 20)
        val day = intent.getIntExtra("day", 1).coerceIn(1, 7)
        val term = intent.getStringExtra("curTerm").orEmpty()

        lifecycleScope.launch {
            val suggestions = withContext(Dispatchers.IO) {
                database.courseDao().getCoursesByTerm(term, studentId, studentPassword)
                    .map { it.courseName.trim() }
                    .filter { it.isNotBlank() }
                    .distinct()
                    .take(12)
            }
            setContent {
                AppSkinTheme {
                    AddCourseScreen(
                        initialWeek = week,
                        initialDay = day,
                        initialStartSection = start,
                        initialSpan = span,
                        courseSuggestions = suggestions,
                        onBack = { finish() },
                        onSave = { draft ->
                            if (draft.courseName.isBlank()) {
                                Toast.makeText(this@AddCourseActivity, "请输入课程名称", Toast.LENGTH_SHORT).show()
                                return@AddCourseScreen
                            }
                            val courses = draft.timeSlots.map { slot ->
                                TimeTableMySubject(
                                    courseName = draft.courseName.trim(),
                                    classroom = draft.room.trim(),
                                    teacher = draft.teacher.trim(),
                                    weeks = slot.weeks.sorted(),
                                    start = slot.startSection,
                                    step = slot.sectionSpan,
                                    weekday = slot.dayOfWeek,
                                    isCustom = true,
                                    term = term,
                                    studentId = studentId,
                                    studentPassword = studentPassword,
                                    credit = draft.credit.trim(),
                                    note = draft.note.trim(),
                                    customColor = draft.colorValue,
                                )
                            }
                            setResult(RESULT_OK, Intent().putExtra("newCourses", gson.toJson(courses)))
                            finish()
                        },
                    )
                }
            }
        }
    }
}
