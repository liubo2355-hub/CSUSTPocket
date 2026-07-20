package com.csust.pocket.overview.data

import android.content.Context
import androidx.compose.ui.graphics.Color
import com.csust.pocket.R
import com.csust.pocket.common.data.local.mmkv.StudentInfoManager
import com.csust.pocket.common.data.local.mmkv.UserInfoManager
import com.csust.pocket.common.data.local.room.database.UserDataBase
import com.csust.pocket.feature.common.compose_ui.FunctionDestination
import com.csust.pocket.feature.common.data.local.entity.Grade
import com.csust.pocket.feature.common.data.local.entity.TimeTableMySubject
import com.csust.pocket.feature.common.data.local.mmkv.ExamArrangementCache
import com.csust.pocket.feature.common.data.local.mmkv.ScoreCache
import com.csust.pocket.feature.common.data.local.room.database.CoursesDataBase
import com.csust.pocket.feature.common.data.repository.ElectricityRepository
import com.csust.pocket.feature.calendar.data.repository.SemesterCalendarRepository
import com.csust.pocket.overview.data.local.OverviewLocalCache
import com.csust.pocket.overview.data.local.OverviewLocalCache.ElectricityHistoryEntry
import com.csust.pocket.overview.data.local.OverviewLocalCache.ElectricitySnapshot
import com.csust.pocket.overview.ui.model.OverviewCourseUiModel
import com.csust.pocket.overview.ui.model.CourseHighlightType
import com.csust.pocket.overview.ui.model.OverviewExamUiModel
import com.csust.pocket.overview.ui.model.OverviewMetricUiModel
import com.csust.pocket.overview.ui.model.OverviewUiState
import com.csust.pocket.utils.NetworkUtil
import com.dcelysia.csust_spider.core.Resource
import com.dcelysia.csust_spider.education.data.remote.EducationHelper
import com.dcelysia.csust_spider.education.data.remote.model.ExamArrange
import com.dcelysia.csust_spider.education.data.remote.services.ExamArrangeService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import kotlin.math.abs

class OverviewRepository(
    context: Context
) {

    private val appContext = context.applicationContext
    private val courseDao by lazy { CoursesDataBase.getDatabase(appContext).courseDao() }
    private val userDao by lazy { UserDataBase.getInstance(appContext).itemDao() }
    private val examCache by lazy { ExamArrangementCache() }
    private val electricityRepository by lazy { ElectricityRepository() }

    suspend fun loadLocalState(): OverviewUiState = withContext(Dispatchers.IO) {
        buildState(
            courses = readLocalCourses(),
            grades = ScoreCache.getGrades().orEmpty(),
            exams = examCache.getExamArrangement().orEmpty().toUiExams()
        )
    }

    suspend fun refreshState(): OverviewUiState = withContext(Dispatchers.IO) {
        coroutineScope {
            val currentTerm = getCurrentTerm()
            // 刷新前触发一次校历预取（非阻塞 fire-and-forget，失败静默）
            SemesterCalendarRepository.prefetchDetailIfMissing(currentTerm)
            val courseDeferred = async(Dispatchers.IO) { fetchCourses(currentTerm) }
            val gradesDeferred = async(Dispatchers.IO) { fetchGrades() }
            val examsDeferred = async(Dispatchers.IO) { fetchExams(currentTerm) }
            val electricityDeferred = async(Dispatchers.IO) { refreshElectricityIfNeeded() }
            val courses = courseDeferred.await() ?: readLocalCourses()
            val grades = gradesDeferred.await() ?: ScoreCache.getGrades().orEmpty()
            val exams = examsDeferred.await() ?: examCache.getExamArrangement().orEmpty().toUiExams()
            electricityDeferred.await()

            buildState(
                courses = courses,
                grades = grades,
                exams = exams
            ).copy(isSilentSyncing = false)
        }
    }

    suspend fun refreshMetric(metricId: String): OverviewUiState = withContext(Dispatchers.IO) {
        when (metricId) {
            FunctionDestination.Electronic.name -> electricityRepository.query(force = true)
            FunctionDestination.ScoreInquiry.name -> fetchGrades()
        }
        loadLocalState()
    }

    suspend fun refreshExams(): OverviewUiState = withContext(Dispatchers.IO) {
        val term = getCurrentTerm()
        fetchExams(term)
        loadLocalState()
    }

    /** Refreshes only timetable data for lightweight desktop-widget updates. */
    suspend fun refreshCoursesOnly(): Boolean = withContext(Dispatchers.IO) {
        fetchCourses(getCurrentTerm()) != null
    }

    private suspend fun buildState(
        courses: List<TimeTableMySubject>,
        grades: List<Grade>,
        exams: List<OverviewExamUiModel>
    ): OverviewUiState = withContext(Dispatchers.IO) {
        val studentId = StudentInfoManager.studentId
        val studentPassword = StudentInfoManager.studentPassword
        val currentTerm = getCurrentTerm()
        val currentWeek = getCurrentWeek(currentTerm)
        val currentUser = UserInfoManager.userId.takeIf { it > 0 }?.let { userDao.getUserById(it) }

        val todayCalendar = Calendar.getInstance()
        val tomorrowCalendar = (todayCalendar.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 1) }
        val todayWeekday = todayCalendar.toCourseWeekday()
        val tomorrowWeekday = tomorrowCalendar.toCourseWeekday()
        val tomorrowWeek = if (tomorrowWeekday == 1) currentWeek + 1 else currentWeek
        val todayCourses = courses.toDayCourses(todayWeekday, currentWeek)
        val tomorrowCourses = courses.toDayCourses(tomorrowWeekday, tomorrowWeek)
        val courseHighlight = CourseHighlightSelector.select(todayCourses, tomorrowCourses)
        val isShowingTomorrow = courseHighlight.type == CourseHighlightType.TOMORROW
        val electricitySnapshot = OverviewLocalCache.getElectricitySnapshot()
        val isElectricityBound = hasElectricityBinding() || electricitySnapshot != null

        OverviewUiState(
            isRefreshing = false,
            isSilentSyncing = hasNetwork(),
            isBoundStudent = studentId.isNotBlank() && studentPassword.isNotBlank(),
            isOnline = hasNetwork(),
            isElectricityBound = isElectricityBound,
            accountName = currentUser?.account?.takeIf { it.isNotBlank() } ?: UserInfoManager.account.ifBlank { "掌上长理" },
            avatarUrl = currentUser?.avatarUrl?.takeIf { it.isNotBlank() } ?: UserInfoManager.userAvatar,
            studentId = studentId,
            dateText = buildDateText(currentTerm, currentWeek, todayCalendar),
            currentTerm = currentTerm,
            currentWeek = currentWeek,
            dataSourceLabel = if (courses.isEmpty() && grades.isEmpty()) "本地数据已上屏" else "已完成静默刷新",
            metrics = buildMetrics(grades, electricitySnapshot, isElectricityBound),
            todayCourses = todayCourses,
            todayCourseMessage = when {
                studentId.isBlank() || studentPassword.isBlank() -> "先绑定学号"
                todayCourses.isNotEmpty() -> ""
                else -> "没有数据"
            },
            isShowingTomorrow = isShowingTomorrow,
            courseHighlight = courseHighlight,
            pendingHomeworks = emptyList(),
            pendingHomeworkMessage = when {
                studentId.isBlank() || studentPassword.isBlank() -> "先绑定学号"
                else -> "没有数据"
            },
            pendingTests = emptyList(),
            pendingTestMessage = when {
                studentId.isBlank() || studentPassword.isBlank() -> "先绑定学号"
                else -> "没有数据"
            },
            upcomingExams = exams.take(3),
            examHighlight = ExamHighlightSelector.select(exams),
            examMessage = when {
                studentId.isBlank() || studentPassword.isBlank() -> "先绑定学号"
                exams.isNotEmpty() -> ""
                else -> "没有数据"
            }
        )
    }

    private suspend fun readLocalCourses(): List<TimeTableMySubject> {
        val studentId = StudentInfoManager.studentId
        val studentPassword = StudentInfoManager.studentPassword
        if (studentId.isBlank() || studentPassword.isBlank()) return emptyList()
        return withContext(Dispatchers.IO) {
            courseDao.getCoursesByTerm(getCurrentTerm(), studentId, studentPassword)
        }
    }

    private suspend fun fetchGrades(): List<Grade>? {
        val result = runCatching { EducationHelper.getCourseGrades() }.getOrNull() ?: return null
        if (result.code != "200") return null
        val grades = result.data?.map {
            Grade(
                id = it.courseID,
                item = it.semester,
                name = it.courseName,
                grade = it.grade.toString(),
                flag = it.gradeIdentifier,
                score = it.credit.toString(),
                timeR = it.totalHours.toString(),
                point = it.gradePoint.toString(),
                upperReItem = it.retakeSemester,
                method = it.assessmentMethod,
                property = it.examNature,
                attribute = it.courseAttribute,
                reItem = it.groupName,
                studyMode = it.studyMode,
                courseNature = it.courseNature.chineseName,
                courseCategory = it.courseCategory,
                pscjUrl = it.gradeDetailUrl
            )
        }.orEmpty()
        val processedGrades = preprocessGrades(grades)
        if (processedGrades.isNotEmpty()) ScoreCache.saveGrades(processedGrades)
        return processedGrades
    }

    private suspend fun fetchCourses(term: String): List<TimeTableMySubject>? {
        return when (val result = runCatching { EducationHelper.getCourseScheduleByTerm("", term) }.getOrNull()) {
            is Resource.Success -> {
                val subjects = result.data.courses.map {
                    val weekInfo = parseWeeks(it.weeks)
                    TimeTableMySubject(
                        courseName = it.courseName,
                        classroom = it.classroom,
                        teacher = it.teacher,
                        weeks = weekInfo.weeks,
                        start = weekInfo.start,
                        step = weekInfo.step,
                        weekday = it.weekday.toInt(),
                        term = term,
                        studentId = StudentInfoManager.studentId,
                        studentPassword = StudentInfoManager.studentPassword
                    )
                }.distinctBy {
                    "${it.courseName}${it.classroom}${it.teacher}${it.start}${it.step}${it.weekday}${it.term}"
                }
                val studentId = StudentInfoManager.studentId
                val studentPassword = StudentInfoManager.studentPassword
                if (subjects.isNotEmpty() && studentId.isNotBlank() && studentPassword.isNotBlank()) {
                    withContext(Dispatchers.IO) {
                        courseDao.deleteNetworkCoursesByTerm(term, studentId, studentPassword)
                        courseDao.insertCourses(subjects.toMutableList())
                    }
                }
                if (studentId.isBlank() || studentPassword.isBlank()) {
                    subjects
                } else {
                    courseDao.getCoursesByTerm(term, studentId, studentPassword)
                }
            }

            else -> null
        }
    }

    private suspend fun fetchExams(term: String): List<OverviewExamUiModel>? {
        val examResponse = runCatching { ExamArrangeService.getExamArrange(term) }.getOrNull() ?: return null
        if (examResponse !is Resource.Success) return null
        val exams = examResponse.data
        if (exams.isNotEmpty()) {
            examCache.saveExamArrangement(exams)
        }
        return exams.toUiExams()
    }

    private fun buildMetrics(
        grades: List<Grade>,
        electricitySnapshot: ElectricitySnapshot?,
        isElectricityBound: Boolean
    ): List<OverviewMetricUiModel> {
        val processedGrades = preprocessGrades(grades)
        val totalCredits = processedGrades.sumOf { it.score.toDoubleOrNull() ?: 0.0 }
        val gpa = if (totalCredits > 0) {
            processedGrades.sumOf { (it.score.toDoubleOrNull() ?: 0.0) * (it.point.toDoubleOrNull() ?: 0.0) } / totalCredits
        } else {
            0.0
        }
        val averageScore = if (totalCredits > 0) {
            processedGrades.sumOf { (it.grade.toDoubleOrNull() ?: 0.0) * (it.score.toDoubleOrNull() ?: 0.0) } / totalCredits
        } else {
            0.0
        }

        val electricityEstimate = if (isElectricityBound && electricitySnapshot != null) {
            estimateElectricityDays(electricitySnapshot.history, electricitySnapshot.lastValue.toDouble())
        } else null
        val electricitySubtitle = buildElectricitySubtitle(electricitySnapshot, isElectricityBound, electricityEstimate)
        val electricityUpdatedAt = buildElectricityUpdatedAt(electricitySnapshot)
        val semesterGpaTrend = grades
            .filter { it.item.isNotBlank() }
            .groupBy { it.item }
            .toSortedMap()
            .values
            .mapNotNull { semesterGrades ->
                val processed = preprocessGrades(semesterGrades)
                val credits = processed.sumOf { it.score.toDoubleOrNull() ?: 0.0 }
                if (credits <= 0.0) null else (
                    processed.sumOf { (it.score.toDoubleOrNull() ?: 0.0) * (it.point.toDoubleOrNull() ?: 0.0) } / credits
                ).toFloat()
            }
        val electricityTrend = electricitySnapshot?.history
            .orEmpty()
            .sortedBy { it.timestamp }
            .map { it.value }
            .takeLast(12)
        return listOf(
            OverviewMetricUiModel(
                id = FunctionDestination.ScoreInquiry.name,
                title = "成绩查询",
                value = if (totalCredits > 0) String.format(Locale.CHINA, "%.2f", gpa) else "--",
                subtitle = if (totalCredits > 0) "平均分: ${String.format(Locale.CHINA, "%.1f", averageScore)}" else "去成绩查询加载数据",
                iconRes = R.drawable.ic_rank,
                accentColor = Color(0xFFE3B92C),
                trendValues = semesterGpaTrend
            ),
            OverviewMetricUiModel(
                id = FunctionDestination.Electronic.name,
                title = "宿舍电量",
                value = electricitySnapshot?.lastValue?.let { formatMetricNumber(it.toDouble()) } ?: "--",
                unit = if (electricitySnapshot != null) "kWh" else "",
                subtitle = electricitySubtitle,
                secondarySubtitle = electricityUpdatedAt,
                iconRes = R.drawable.ic_bill,
                accentColor = Color(0xFF62C466),
                trendValues = electricityTrend,
                badgeText = electricityEstimate?.let { "置信" + it.confidence + "%" } ?: ""
            )
        )
    }

    private fun formatMetricNumber(value: Double): String {
        return String.format(Locale.CHINA, "%.2f", value)
            .trimEnd('0')
            .trimEnd('.')
    }

    private fun preprocessGrades(rawData: List<Grade>): List<Grade> {
        return rawData
            .groupBy { it.name }
            .map { (_, grades) ->
                val retakeGrade = grades.find { grade ->
                    grade.upperReItem.isNotBlank() ||
                        grade.property.contains("重修", ignoreCase = true) ||
                        grade.property.contains("补考", ignoreCase = true)
                }

                retakeGrade ?: grades.maxByOrNull { it.grade.toDoubleOrNull() ?: 0.0 }!!
            }
    }

    private fun buildElectricitySubtitle(
        snapshot: ElectricitySnapshot?,
        isBound: Boolean,
        estimate: ElectricityEstimate?
    ): String {
        if (!isBound) return "去绑定电费"
        if (snapshot == null) return "还没有电费缓存"
        if (estimate == null) return "暂无预测数据"
        val days = estimate.days
        val prefix = when {
            estimate.isStale -> "数据较旧"
            estimate.recentTrendUp -> "用电增加"
            else -> "预计"
        }
        return prefix + "约" + days + "天后耗尽"
    }

    private fun buildElectricityUpdatedAt(snapshot: ElectricitySnapshot?): String {
        if (snapshot == null) return ""
        return "更新于 " + SimpleDateFormat("MM-dd HH:mm", Locale.CHINA).format(Date(snapshot.lastTime))
    }    private data class ElectricityEstimate(
        val days: Int,
        val confidence: Int,
        val isStale: Boolean,
        val recentTrendUp: Boolean
    )

    private fun estimateElectricityDays(
        history: List<ElectricityHistoryEntry>,
        currentValue: Double
    ): ElectricityEstimate? {
        if (history.size < 2 || currentValue <= 0.0) return null
        val now = System.currentTimeMillis()
        val sortedHistory = history
            .sortedBy { it.timestamp }
            .filter { now - it.timestamp <= TimeUnit.DAYS.toMillis(30) }
            .takeLast(20)
        if (sortedHistory.size < 2) return null
        val allRates = sortedHistory.zipWithNext().mapNotNull { (prev, curr) ->
            val deltaHours = (curr.timestamp - prev.timestamp).toDouble() / TimeUnit.HOURS.toMillis(1)
            val deltaValue = prev.value - curr.value
            when {
                deltaHours < 0.5 -> null
                deltaHours > 168.0 -> null
                deltaValue <= 0.0f -> null
                deltaValue < 0.02f -> null
                else -> {
                    val dailyUsage = deltaValue / deltaHours * 24.0
                    val daysAgo = (now - prev.timestamp).toDouble() / TimeUnit.DAYS.toMillis(1)
                    val recencyWeight = Math.pow(0.5, daysAgo / 7.0)
                    val intervalWeight = (deltaHours.coerceAtMost(48.0) / 24.0).coerceIn(0.3, 2.0)
                    UsageRate(dailyUsage, recencyWeight * intervalWeight)
                }
            }
        }
        if (allRates.isEmpty()) return null
        val effectiveRates = when {
            allRates.size >= 8 -> {
                val sorted = allRates.map { it.dailyUsage }.sorted()
                val q1 = sorted[sorted.size / 4]
                val q3 = sorted[sorted.size * 3 / 4]
                val iqr = (q3 - q1).coerceAtLeast(0.1)
                val filtered = allRates.filter {
                    it.dailyUsage in (q1 - 1.5 * iqr)..(q3 + 1.5 * iqr)
                }
                if (filtered.isEmpty()) allRates else filtered
            }
            allRates.size >= 4 -> {
                val sorted = allRates.map { it.dailyUsage }.sorted()
                val q1 = sorted[sorted.size / 4]
                val q3 = sorted[sorted.size * 3 / 4]
                val iqr = (q3 - q1).coerceAtLeast(0.1)
                val filtered = allRates.filter {
                    it.dailyUsage in (q1 - 2.0 * iqr)..(q3 + 2.0 * iqr)
                }
                if (filtered.isEmpty()) allRates else filtered
            }
            else -> allRates
        }
        val totalWeight = effectiveRates.sumOf { it.weight }
        var finalUsage = effectiveRates.sumOf { it.dailyUsage * it.weight } / totalWeight
        val recentRates = effectiveRates.takeLast(3)
        var recentTrendUp = false
        if (recentRates.size >= 2) {
            val recentAvg = recentRates.map { it.dailyUsage }.average()
            if (recentAvg > finalUsage * 1.5 && recentAvg > 0.1) {
                recentTrendUp = true
                finalUsage = recentAvg
            }
        }
       if (finalUsage <= 0.0) return null
       val lastReadingTime = sortedHistory.lastOrNull()?.timestamp ?: 0L
       val isStale = now - lastReadingTime > TimeUnit.DAYS.toMillis(3)
       // Confidence: 4-factor continuous model (0-100, coerced to 10-95)
       // 1. Data quantity (0-35): each rate adds ~4.4 pts, capped at 8+ rates
       val dataScore = (effectiveRates.size * 4.4).coerceAtMost(35.0)
       // 2. Consistency (0-30): lower coefficient of variation = higher score
       val meanUsage = effectiveRates.map { it.dailyUsage }.average()
       val variance = effectiveRates.map { Math.pow(it.dailyUsage - meanUsage, 2.0) }.average()
       val cv = if (meanUsage > 0) Math.sqrt(variance) / meanUsage else 1.0
       val consistencyScore = (1.0 - cv.coerceIn(0.0, 1.0)) * 30.0
       // 3. Freshness (0-20): continuous decay, 0h=20, 24h~12, 48h~5, 67h=0
       val hoursSinceLast = (now - lastReadingTime) / 3_600_000.0
       val freshnessScore = (20.0 - hoursSinceLast * 0.3).coerceAtLeast(0.0)
       // 4. Time coverage (0-15): data spanning more days = higher score
       val dataSpanHours = if (sortedHistory.size >= 2) {
           (sortedHistory.last().timestamp - sortedHistory.first().timestamp) / 3_600_000.0
       } else 0.0
       val coverageScore = (dataSpanHours / 24.0 * 2.0).coerceAtMost(15.0)
       val confidence = (dataScore + consistencyScore + freshnessScore + coverageScore).toInt().coerceIn(10, 95)
       val days = kotlin.math.ceil(currentValue / finalUsage).toInt().coerceAtLeast(1)
       return ElectricityEstimate(
           days = days.coerceAtMost(365),
           confidence = confidence,
           isStale = isStale,
           recentTrendUp = recentTrendUp
       )
    }

    private fun hasElectricityBinding(): Boolean {
        return electricityRepository.hasBinding()
    }

    private suspend fun refreshElectricityIfNeeded() {
        if (!hasNetwork()) return
        electricityRepository.refreshIfNeeded()
    }

    private fun hasNetwork(): Boolean = NetworkUtil.getNetworkType(appContext) != NetworkUtil.NetworkType.None

    private fun buildDateText(term: String, week: Int, targetCalendar: Calendar = Calendar.getInstance(), isShowingTomorrow: Boolean = false): String {
        val weekday = listOf("", "周日", "周一", "周二", "周三", "周四", "周五", "周六")[targetCalendar.get(Calendar.DAY_OF_WEEK)]
        val prefix = if (isShowingTomorrow) "明日" else ""
        return "${prefix}${targetCalendar.get(Calendar.MONTH) + 1}月${targetCalendar.get(Calendar.DAY_OF_MONTH)}日 $weekday  ·  $term 第${week}周"
    }

    private fun getCurrentTerm(): String =
        com.csust.pocket.common.cache.CommonInfo.getCurrentTerm()

    private fun getCurrentWeek(term: String): Int =
        com.csust.pocket.common.cache.CommonInfo.getCurrentWeekInt(term)

    private fun getCurrentWeekForDate(term: String, calendar: Calendar): Int {
        val startTime = com.csust.pocket.common.cache.CommonInfo.getTermStartDate(term) ?: return 1
        return runCatching {
            val startDate = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).parse(startTime.take(10)) ?: return 1
            val targetDate = calendar.apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.time
            (((targetDate.time - startDate.time) / TimeUnit.DAYS.toMillis(1)) / 7 + 1).toInt().coerceAtLeast(1)
        }.getOrDefault(1)
    }

    private fun parseWeeks(weekJson: String): WeekJsonInfo {
        val pattern = Pattern.compile("(\\d+(?:-\\d+)?(?:,\\d+(?:-\\d+)?)*)\\((周|单周|双周)?\\)?\\[(\\d{2})(?:-(\\d{2}))?(?:-(\\d{2}))?(?:-(\\d{2}))?节\\]")
        val matcher = pattern.matcher(weekJson)
        if (!matcher.find()) return WeekJsonInfo(emptyList(), 0, 0)
        val weeksRange = matcher.group(1)
        val weekType = matcher.group(2)
        val startClass = matcher.group(3)?.toIntOrNull() ?: 0
        val endClass = listOfNotNull(
            matcher.group(4)?.toIntOrNull(),
            matcher.group(5)?.toIntOrNull(),
            matcher.group(6)?.toIntOrNull()
        ).lastOrNull() ?: startClass
        val weeks = weeksRange?.split(",")?.flatMap { part ->
            if (part.contains("-")) {
                val (start, end) = part.split("-").map { it.toInt() }
                when (weekType) {
                    "单周" -> (start..end).filter { it % 2 != 0 }
                    "双周" -> (start..end).filter { it % 2 == 0 }
                    else -> (start..end).toList()
                }
            } else {
                listOf(part.toInt())
            }
        }.orEmpty()
        return WeekJsonInfo(weeks, startClass, endClass - startClass + 1)
    }

    private fun List<ExamArrange>.toUiExams(): List<OverviewExamUiModel> {
        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)
        return mapNotNull { exam ->
            if (exam.courseNameval.isBlank() || exam.examTime.isBlank()) null else {
                val start = exam.examStartTimeval
                val end = exam.examEndTimeval
                val date = start?.toLocalDate()
                val badge = when (date) {
                    today -> "今天"
                    tomorrow -> "明天"
                    else -> if (date != null && date.isAfter(today)) "未来" else "已结束"
                }
                if (start == null || badge == "已结束") null else {
                    OverviewExamUiModel(
                        id = "${exam.courseNameval}_${exam.examTime}",
                        courseName = exam.courseNameval,
                        examTime = formatExamTimeRange(start, end),
                        location = listOf(exam.campus, exam.examRoomval).filter { it.isNotBlank() }.joinToString(" · "),
                        badge = badge,
                        startTime = start,
                        endTime = end
                    )
                }
            }
        }.sortedBy { it.startTime }
    }

    private fun formatExamTimeRange(start: LocalDateTime?, end: LocalDateTime?): String {
        if (start == null) return ""
        val formatter = DateTimeFormatter.ofPattern("HH:mm", Locale.CHINA)
        val startText = start.format(formatter)
        val endText = end?.format(formatter) ?: ""
        return if (endText.isNotEmpty()) "$startText - $endText" else startText
    }

    private fun List<TimeTableMySubject>.toDayCourses(targetWeekday: Int, currentWeek: Int): List<OverviewCourseUiModel> {
        val colors = listOf(Color(0xFF5E87F6), Color(0xFF45B979), Color(0xFFF08D3C), Color(0xFFB974F0))
        return filter { it.weekday == targetWeekday && (it.weeks.isNullOrEmpty() || currentWeek in it.weeks.orEmpty()) }
            .sortedBy { it.start }
            .mapIndexed { index, course ->
                OverviewCourseUiModel(
                    id = "${course.courseName}_${course.start}_${course.weekday}_$index",
                    courseName = course.courseName,
                    classroom = course.classroom.orEmpty().ifBlank { "教室待定" },
                    teacher = course.teacher.ifBlank { "教师待定" },
                    timeText = buildCourseTimeText(course.start, course.step),
                    accentLabel = if (course.isCustom) "自定义" else "校园课表",
                    accentColor = colors[index % colors.size],
                    startSection = course.start,
                    sectionSpan = course.step,
                )
            }
    }

    private fun Calendar.toCourseWeekday(): Int = when (get(Calendar.DAY_OF_WEEK)) {
        Calendar.SUNDAY -> 7
        else -> get(Calendar.DAY_OF_WEEK) - 1
    }

    private fun buildCourseTimeText(startSection: Int, sectionSpan: Int): String {
        val sectionTimes = listOf(
            "08:00" to "08:45",
            "08:55" to "09:40",
            "10:10" to "10:55",
            "11:05" to "11:50",
            "14:00" to "14:45",
            "14:55" to "15:40",
            "16:10" to "16:55",
            "17:05" to "17:50",
            "19:30" to "20:15",
            "20:25" to "21:10"
        )
        val safeStart = startSection.coerceIn(1, sectionTimes.size)
        val safeEnd = (startSection + sectionSpan - 1).coerceIn(safeStart, sectionTimes.size)
        val startTime = sectionTimes[safeStart - 1].first
        val endTime = sectionTimes[safeEnd - 1].second
        return "$startTime-$endTime · ${safeStart}-${safeEnd}节"
    }

    private data class WeekJsonInfo(
        val weeks: List<Int>,
        val start: Int,
        val step: Int
    )

    private data class UsageRate(
        val dailyUsage: Double,
        val weight: Double
    )
}
