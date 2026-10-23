package com.snappurge.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import com.snappurge.app.cleaner.CleanupWorker
import com.snappurge.app.data.CleanerPreferences
import com.snappurge.app.ui.MainScreen
import com.snappurge.app.ui.theme.SnapPurgeTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var prefs: CleanerPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        prefs = CleanerPreferences(applicationContext)

        // 默认初始化后台调度计划
        lifecycleScope.launch {
            val settings = prefs.getSettings()
            if (settings.autoCleanupEnabled) {
                CleanupWorker.schedule(applicationContext, settings.requireCharging)
            }
        }

        setContent {
            SnapPurgeTheme {
                val settings by prefs.settingsFlow.collectAsState(
                    initial = com.snappurge.app.data.CleanerSettings(
                        prefixes = CleanerPreferences.DEFAULT_PREFIXES,
                        retentionHours = 72,
                        autoCleanupEnabled = true,
                        requireCharging = false,
                        totalDeletedCount = 0,
                        totalFreedBytes = 0L,
                        lastCleanTimestamp = 0L,
                        language = "zh",
                        soundEffect = "shredder",
                        flashEffectEnabled = true,
                        hapticEnabled = true
                    )
                )
                MainScreen(prefs = prefs, settings = settings)
            }
        }
    }
}
