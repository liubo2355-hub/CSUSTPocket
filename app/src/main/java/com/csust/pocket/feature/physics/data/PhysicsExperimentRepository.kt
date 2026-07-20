package com.csust.pocket.feature.physics.data

import android.content.Context
import android.util.Base64
import com.csust.pocket.feature.mooc.cookie.PersistentCookieJar
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

data class PhysicsCourse(
    val id: Int,
    val name: String,
    val batch: String,
    val teacher: String,
    val location: String,
    val startTime: Long,
    val endTime: Long,
    val classHours: Int,
    val week: Int,
    val dayOfWeek: String
)

data class PhysicsGrade(
    val courseCode: String,
    val courseName: String,
    val itemName: String,
    val previewGrade: Int?,
    val operationGrade: Int?,
    val reportGrade: Int?,
    val totalGrade: Int
)

/** Android port of CSUSTKit's PhysicsExperimentHelper. */
class PhysicsExperimentRepository(private val context: Context) {
    private val gson = Gson()
    private val preferences = context.getSharedPreferences("physics_experiment", Context.MODE_PRIVATE)
    private val client = OkHttpClient.Builder()
        .cookieJar(PersistentCookieJar())
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    val savedUsername: String get() = preferences.getString("username", "").orEmpty()

    suspend fun login(username: String, password: String) = withContext(Dispatchers.IO) {
        val url = HttpUrl.Builder()
            .scheme("http").host(DIRECT_HOST)
            .addPathSegment("login.aspx")
            .addQueryParameter("UserType", "0")
            .addQueryParameter("txtUserName", username.toBase64())
            .addQueryParameter("txtPass", password.toBase64())
            .build()
        val response = execute(url, post = true)
        require(response.contains("true", ignoreCase = true)) { "用户名或密码错误" }
        preferences.edit().putString("username", username).apply()
    }

    suspend fun getCourses(): List<PhysicsCourse> = withContext(Dispatchers.IO) {
        val url = HttpUrl.Builder().scheme("http").host(DIRECT_HOST)
            .addPathSegment("Student").addPathSegment("myalltasklist.aspx")
            .addQueryParameter("generalCourseId", "2")
            .addQueryParameter("generalCourseName", "大学物理实验")
            .build()
        val html = execute(url)
        ensureLoggedIn(html)
        val table = Jsoup.parse(html).getElementsByClass("msgtable").first()
            ?: error("未找到实验安排表")
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)
        table.select("tr").drop(1).mapNotNull { row ->
            val cols = row.select("td")
            if (cols.size < 8) return@mapNotNull null
            val rawTime = cols[5].text().trim().split(" - ")
            if (rawTime.size != 2) return@mapNotNull null
            val date = rawTime[0].substringBeforeLast(" ")
            val weekInfo = cols[7].text().trim()
            PhysicsCourse(
                id = cols[0].text().trim().toIntOrNull() ?: return@mapNotNull null,
                name = cols[1].text().trim(), batch = cols[2].text().trim(),
                teacher = cols[3].text().trim(), location = cols[4].text().trim(),
                startTime = formatter.parse(rawTime[0])?.time ?: return@mapNotNull null,
                endTime = formatter.parse("$date ${rawTime[1]}")?.time ?: return@mapNotNull null,
                classHours = cols[6].text().trim().toIntOrNull() ?: 0,
                week = Regex("\\d+").find(weekInfo)?.value?.toIntOrNull() ?: 0,
                dayOfWeek = weekInfo.substringAfter(" ", "")
            )
        }.also { saveCache(COURSE_CACHE, it) }
    }

    suspend fun getGrades(): List<PhysicsGrade> = withContext(Dispatchers.IO) {
        val url = HttpUrl.Builder().scheme("http").host(DIRECT_HOST)
            .addPathSegment("Student").addPathSegment("GeneralCourseScore.aspx").build()
        val html = execute(url)
        ensureLoggedIn(html)
        val table = Jsoup.parse(html).getElementById("gvList") ?: error("未找到实验成绩表")
        table.select("tr").drop(1).mapNotNull { row ->
            val cols = row.select("td")
            if (cols.size < 7) return@mapNotNull null
            PhysicsGrade(
                cols[0].text().trim(), cols[1].text().trim(), cols[2].text().trim(),
                cols[3].text().trim().toIntOrNull(), cols[4].text().trim().toIntOrNull(),
                cols[5].text().trim().toIntOrNull(),
                cols[6].text().trim().toIntOrNull() ?: return@mapNotNull null
            )
        }.also { saveCache(GRADE_CACHE, it) }
    }

    fun cachedCourses(): List<PhysicsCourse> = readCache(COURSE_CACHE)
    fun cachedGrades(): List<PhysicsGrade> = readCache(GRADE_CACHE)

    private fun execute(url: HttpUrl, post: Boolean = false): String {
        val builder = Request.Builder().url(url)
        if (post) builder.post(ByteArray(0).toRequestBodyCompat())
        client.newCall(builder.build()).execute().use { response ->
            if (!response.isSuccessful) error("服务器返回 ${response.code}")
            return response.body?.string().orEmpty()
        }
    }

    private fun ensureLoggedIn(html: String) {
        if (html.contains("name=\"txtUserName\"", ignoreCase = true)) error("请先登录大学物理实验系统")
    }

    private inline fun <reified T> readCache(key: String): List<T> {
        val json = preferences.getString(key, null) ?: return emptyList()
        return runCatching { gson.fromJson<List<T>>(json, object : TypeToken<List<T>>() {}.type) }.getOrDefault(emptyList())
    }

    private fun saveCache(key: String, value: Any) {
        preferences.edit().putString(key, gson.toJson(value)).putLong("${key}_updated", System.currentTimeMillis()).apply()
    }

    private fun String.toBase64() = Base64.encodeToString(toByteArray(), Base64.NO_WRAP)

    companion object {
        private const val DIRECT_HOST = "10.255.65.52"
        private const val COURSE_CACHE = "course_cache"
        private const val GRADE_CACHE = "grade_cache"
    }
}

private fun ByteArray.toRequestBodyCompat() = okhttp3.RequestBody.create(null, this)
