package com.creamaker.changli_planet_app.feature.common.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.creamaker.changli_planet_app.R
import com.creamaker.changli_planet_app.core.MainActivity
import com.creamaker.changli_planet_app.feature.common.data.repository.ElectricityRepository
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

/** 宿舍电量提醒设置（持久化到 MMKV）。 */
object ElectricityReminderPrefs {
    private val mmkv by lazy { MMKV.defaultMMKV() }

    var dailyEnabled: Boolean
        get() = mmkv.decodeBool("elec_reminder_daily_enabled", false)
        set(v) { mmkv.encode("elec_reminder_daily_enabled", v) }

    /** 每日提醒时间（24 小时制）。默认 20:00。 */
    var hour: Int
        get() = mmkv.decodeInt("elec_reminder_hour", 20)
        set(v) { mmkv.encode("elec_reminder_hour", v) }
    var minute: Int
        get() = mmkv.decodeInt("elec_reminder_minute", 0)
        set(v) { mmkv.encode("elec_reminder_minute", v) }

    var lowEnabled: Boolean
        get() = mmkv.decodeBool("elec_reminder_low_enabled", false)
        set(v) { mmkv.encode("elec_reminder_low_enabled", v) }

    /** 低电量预警阈值（度）。默认 10。 */
    var lowThreshold: Float
        get() = mmkv.decodeFloat("elec_reminder_low_threshold", 10f)
        set(v) { mmkv.encode("elec_reminder_low_threshold", v) }

    /** 低电量事件去重：低于阈值只提醒一次，回升到阈值以上后复位。 */
    var lowEpisodeActive: Boolean
        get() = mmkv.decodeBool("elec_reminder_low_episode", false)
        set(v) { mmkv.encode("elec_reminder_low_episode", v) }
}

object ElectricityReminder {
    const val CHANNEL_ID = "elec_reminder"
    const val MODE = "mode"
    const val MODE_DAILY = "daily"
    const val MODE_MONITOR = "monitor"

    private const val WORK_DAILY = "elec_reminder_daily"
    private const val WORK_MONITOR = "elec_reminder_monitor"
    private const val NOTIF_DAILY = 55001
    private const val NOTIF_LOW = 55002

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = context.getSystemService(NotificationManager::class.java) ?: return
            if (mgr.getNotificationChannel(CHANNEL_ID) == null) {
                mgr.createNotificationChannel(
                    NotificationChannel(CHANNEL_ID, "宿舍电量提醒", NotificationManager.IMPORTANCE_HIGH).apply {
                        description = "定时播报宿舍剩余电量与低电量预警"
                    }
                )
            }
        }
    }

    /** 依据当前设置重新编排后台任务；设置变更后调用。 */
    fun reschedule(context: Context) {
        val wm = WorkManager.getInstance(context.applicationContext)
        val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

        if (ElectricityReminderPrefs.dailyEnabled) {
            val req = PeriodicWorkRequestBuilder<ElectricityReminderWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(untilNextMillis(ElectricityReminderPrefs.hour, ElectricityReminderPrefs.minute), TimeUnit.MILLISECONDS)
                .setConstraints(constraints)
                .setInputData(workDataOf(MODE to MODE_DAILY))
                .build()
            wm.enqueueUniquePeriodicWork(WORK_DAILY, ExistingPeriodicWorkPolicy.REPLACE, req)
        } else {
            wm.cancelUniqueWork(WORK_DAILY)
        }

        if (ElectricityReminderPrefs.lowEnabled) {
            val req = PeriodicWorkRequestBuilder<ElectricityReminderWorker>(6, TimeUnit.HOURS)
                .setInitialDelay(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .setInputData(workDataOf(MODE to MODE_MONITOR))
                .build()
            wm.enqueueUniquePeriodicWork(WORK_MONITOR, ExistingPeriodicWorkPolicy.REPLACE, req)
        } else {
            wm.cancelUniqueWork(WORK_MONITOR)
        }
    }

    private fun untilNextMillis(hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        val next = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (!next.after(now)) next.add(Calendar.DAY_OF_YEAR, 1)
        return (next.timeInMillis - now.timeInMillis).coerceAtLeast(0L)
    }

    fun notifyDaily(context: Context, title: String, text: String) = post(context, NOTIF_DAILY, title, text)
    fun notifyLow(context: Context, title: String, text: String) = post(context, NOTIF_LOW, title, text)

    private fun post(context: Context, id: Int, title: String, text: String) {
        ensureChannel(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = PendingIntent.getActivity(
            context, id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.shuaxin)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .setContentIntent(pi)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        NotificationManagerCompat.from(context).notify(id, notif)
    }
}

/** 后台查询电量并推送提醒。mode=daily 每日播报；mode=monitor 低电量预警。 */
class ElectricityReminderWorker(
    private val appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val mode = inputData.getString(ElectricityReminder.MODE) ?: ElectricityReminder.MODE_DAILY
        val repo = ElectricityRepository()
        if (repo.getBinding() == null) return Result.success() // 未绑定宿舍，静默跳过

        val value = withContext(Dispatchers.IO) {
            runCatching { repo.query(force = true) }.getOrNull()?.numericValue
        }
        val threshold = ElectricityReminderPrefs.lowThreshold
        val low = value != null && ElectricityReminderPrefs.lowEnabled && value < threshold

        when (mode) {
            ElectricityReminder.MODE_DAILY -> {
                val text = when {
                    value == null -> "暂时查询不到电量，请打开 App 手动刷新"
                    low -> "当前剩余 ${fmt(value)} 度，电量偏低，请及时充值 ⚠️"
                    else -> "当前剩余 ${fmt(value)} 度"
                }
                ElectricityReminder.notifyDaily(appContext, "宿舍电量提醒", text)
                if (value != null) ElectricityReminderPrefs.lowEpisodeActive = low
            }

            ElectricityReminder.MODE_MONITOR -> {
                if (value == null) return Result.retry()
                if (low && !ElectricityReminderPrefs.lowEpisodeActive) {
                    ElectricityReminder.notifyLow(
                        appContext, "宿舍电量偏低 ⚠️",
                        "仅剩 ${fmt(value)} 度（低于 ${fmt(threshold)} 度），请及时充值"
                    )
                    ElectricityReminderPrefs.lowEpisodeActive = true
                } else if (!low) {
                    ElectricityReminderPrefs.lowEpisodeActive = false
                }
            }
        }
        return Result.success()
    }

    private fun fmt(v: Float): String = String.format(Locale.CHINA, "%.2f", v)
}
