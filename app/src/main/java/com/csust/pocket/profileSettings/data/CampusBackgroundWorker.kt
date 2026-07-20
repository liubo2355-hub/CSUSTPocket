package com.csust.pocket.profileSettings.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.csust.pocket.feature.common.data.repository.ElectricityRepository

class CampusBackgroundWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = runCatching {
        val prefs = applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getBoolean("task_electricity", true)) ElectricityRepository().query(force = true)
        Result.success()
    }.getOrElse { Result.retry() }

    companion object { const val PREFS = "ios_parity_settings"; const val UNIQUE_WORK = "campus_periodic_refresh" }
}
