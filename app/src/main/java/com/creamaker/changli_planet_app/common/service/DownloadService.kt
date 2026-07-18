package com.creamaker.changli_planet_app.common.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import com.creamaker.changli_planet_app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

class DownloadService : Service() {

    companion object {
        const val INSTALL_NOTIFICATION_ID = 1002
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "Download_Channel"
        const val EXTRA_APK_URL = "extra_apk_url"
        const val APK_FILE_NAME = "update.apk"

        const val ACTION_DOWNLOAD = "action_Download"

        fun startDownload(context: Context, apkUrl: String) {
            val intent = Intent(context, DownloadService::class.java).apply {
                action = ACTION_DOWNLOAD
                putExtra(EXTRA_APK_URL, apkUrl)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    private lateinit var notificationManager: NotificationManager
    private var downloadJob: Job? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())


    override fun onBind(p0: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_DOWNLOAD -> {
                val apkUrl = intent.getStringExtra(EXTRA_APK_URL).orEmpty()
                if (apkUrl.isBlank()) {
                    stopSelfResult(startId)
                    return START_NOT_STICKY
                }
                // 避免连续点击更新时并发写入同一个 APK 文件。
                if (downloadJob?.isActive == true) return START_NOT_STICKY
                deleteLocalApkIfExists()
                DownloadProgressStore.markStarted()
                showNotification(0, "开始更新", true)
                startDownload(apkUrl, startId)

            }
        }
        return START_NOT_STICKY

    }

    private fun startDownload(appUrl: String, startId: Int) {
        downloadJob = coroutineScope.launch {
            try {
                val file = withContext(Dispatchers.IO) {
                    downloadApk(appUrl)
                }
                val intent = installApk(file)
                DownloadProgressStore.markSuccess()
                showInstallNotification(intent, "下载完成点击安装")
                kotlin.runCatching {
                    startActivity(intent)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                e.printStackTrace()
                deleteLocalApkIfExists()
                DownloadProgressStore.markFailed(DownloadProgressStore.state.value.progress)
                showNotification(0, "下载失败，请重试", false)
            } finally {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelfResult(startId)
            }
        }
    }

    private fun showNotification(progress: Int, content: String, isIndetermiante: Boolean) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("应用更新")
            .setContentText(content)
            .setSmallIcon(R.drawable.notification2)
            .setProgress(100, progress, isIndetermiante)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .build()
        if (content == "开始更新") {
            startForeground(NOTIFICATION_ID, notification)
        } else {
            notificationManager.notify(NOTIFICATION_ID, notification)
        }

    }


    private suspend fun downloadApk(apkurl: String): File {
        val file = getLocalApkFile()
        val client = OkHttpClient.Builder().build()

        val request = Request.Builder()
            .url(apkurl)
            .build()

        client.newCall(request).execute().use { response ->
            check(response.isSuccessful) { "下载失败，HTTP ${response.code}" }
            val body = response.body
            val length = body.contentLength()
            body.byteStream().use { input ->
                FileOutputStream(file).use { output ->
                    var byteCopied: Long = 0
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var lastProgress = -1
                    while (true) {
                        val bytes = input.read(buffer)
                        if (bytes < 0) break
                        output.write(buffer, 0, bytes)
                        byteCopied += bytes

                        if (length > 0L) {
                            val progress = ((byteCopied * 100L) / length).toInt().coerceIn(0, 100)
                            // 每个百分点最多刷新一次，避免每 8KB 都切主线程并更新通知。
                            if (progress != lastProgress) {
                                lastProgress = progress
                                withContext(Dispatchers.Main.immediate) {
                                    DownloadProgressStore.markProgress(progress)
                                    showNotification(progress, "下载进度：${progress}%", false)
                                }
                            }
                        }
                    }
                    output.flush()
                }
            }
        }
        check(file.length() > 0L) { "下载文件为空" }
        return file
    }

    private fun getLocalApkFile(): File {
        return File(externalCacheDir, APK_FILE_NAME)
    }

    private fun deleteLocalApkIfExists() {
        kotlin.runCatching {
            val apkFile = getLocalApkFile()
            if (apkFile.exists()) {
                apkFile.delete()
            }
        }
    }


    private fun installApk(file: File): Intent {
        val intent = Intent(Intent.ACTION_VIEW)
        val uri =
            FileProvider.getUriForFile(
                this,
                "${this.packageName}.fileprovider",
                file
            )
        intent.setDataAndType(uri, "application/vnd.android.package-archive")
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        return intent
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "应用更新",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

    }

    private fun showInstallNotification(intent: Intent, msg: String) {
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("应用更新")
            .setContentText(msg)
            .setSmallIcon(R.drawable.notification2)
            .setContentIntent(pendingIntent)
            .build()
        notificationManager.notify(INSTALL_NOTIFICATION_ID, notification)
    }


    override fun onDestroy() {
        downloadJob?.cancel()
        coroutineScope.cancel()
        super.onDestroy()
    }
}
