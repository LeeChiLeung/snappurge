package com.snappurge.app.cleaner

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.snappurge.app.data.CleanerPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            Log.i("BootReceiver", "Device boot or package replaced, checking cleaner schedule...")
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val prefs = CleanerPreferences(context)
                    val settings = prefs.getSettings()
                    if (settings.autoCleanupEnabled) {
                        CleanupWorker.schedule(context, settings.requireCharging)
                        Log.i("BootReceiver", "Restored cleanup schedule successfully.")
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
