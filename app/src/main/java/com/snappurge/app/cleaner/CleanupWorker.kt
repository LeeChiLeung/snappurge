package com.snappurge.app.cleaner

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.snappurge.app.R
import com.snappurge.app.data.CleanerPreferences
import java.util.concurrent.TimeUnit

class CleanupWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "CleanupWorker"
        const val WORK_NAME = "daily_screenshot_cleanup"
        private const val CHANNEL_ID = "cleaner_notification_channel"
        private const val NOTIFICATION_ID = 1001

        fun schedule(context: Context, requireCharging: Boolean = false) {
            val constraintsBuilder = Constraints.Builder()
                .setRequiresBatteryNotLow(true)

            if (requireCharging) {
                constraintsBuilder.setRequiresCharging(true)
            }

            // 每天运行一次 (24 小时)
            val workRequest = PeriodicWorkRequestBuilder<CleanupWorker>(24, TimeUnit.HOURS)
                .setConstraints(constraintsBuilder.build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )
            Log.i(TAG, "Scheduled daily screenshot cleanup worker (requireCharging=$requireCharging)")
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            Log.i(TAG, "Cancelled daily screenshot cleanup worker")
        }
    }

    override suspend fun doWork(): Result {
        Log.i(TAG, "CleanupWorker started executing...")
        val prefs = CleanerPreferences(context)
        val settings = prefs.getSettings()

        if (!settings.autoCleanupEnabled) {
            Log.i(TAG, "Auto cleanup is disabled in settings, skipping.")
            return Result.success()
        }

        return try {
            val items = ScreenshotCleaner.scan(
                context = context,
                prefixes = settings.prefixes,
                olderThanHours = settings.retentionHours
            )

            if (items.isNotEmpty()) {
                val result = ScreenshotCleaner.clean(context, items)
                prefs.recordCleanup(result.deletedCount, result.freedBytes)
                Log.i(TAG, "Auto cleanup completed: deleted ${result.deletedCount} files, freed ${result.freedBytes} bytes")
                
                if (result.deletedCount > 0) {
                    showCompletionNotification(result.deletedCount, result.freedBytes)
                }
            } else {
                Log.i(TAG, "No expired screenshots matched.")
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Cleanup worker failed", e)
            Result.retry()
        }
    }

    private fun showCompletionNotification(deletedCount: Int, freedBytes: Long) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "截图清理提醒",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "后台定时清理结果汇报"
            }
            manager.createNotificationChannel(channel)
        }

        val mb = freedBytes / (1024.0 * 1024.0)
        val content = "已自动清理 $deletedCount 张过期截图，释放 ${"%.1f".format(mb)} MB 存储空间"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("截净 · 定时清理完成")
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .build()

        try {
            manager.notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            Log.w(TAG, "Post notification failed: no permission")
        }
    }
}
