package com.snappurge.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "cleaner_prefs")

data class CleanerSettings(
    val prefixes: Set<String>,
    val retentionHours: Int,
    val autoCleanupEnabled: Boolean,
    val requireCharging: Boolean,
    val totalDeletedCount: Int,
    val totalFreedBytes: Long,
    val lastCleanTimestamp: Long,
    val language: String = "zh",
    val soundEffect: String = "shredder",
    val flashEffectEnabled: Boolean = true,
    val hapticEnabled: Boolean = true
)

class CleanerPreferences(private val context: Context) {

    companion object {
        val KEY_PREFIXES = stringSetPreferencesKey("prefixes")
        val KEY_RETENTION_HOURS = intPreferencesKey("retention_hours")
        val KEY_AUTO_CLEAN_ENABLED = booleanPreferencesKey("auto_clean_enabled")
        val KEY_REQUIRE_CHARGING = booleanPreferencesKey("require_charging")
        val KEY_TOTAL_DELETED = intPreferencesKey("total_deleted")
        val KEY_TOTAL_FREED = longPreferencesKey("total_freed")
        val KEY_LAST_CLEAN_TIME = longPreferencesKey("last_clean_time")
        val KEY_LANGUAGE = stringPreferencesKey("app_language")
        val KEY_SOUND_EFFECT = stringPreferencesKey("sound_effect")
        val KEY_FLASH_EFFECT_ENABLED = booleanPreferencesKey("flash_effect_enabled")
        val KEY_HAPTIC_ENABLED = booleanPreferencesKey("haptic_enabled")

        val DEFAULT_PREFIXES = setOf(
            "Screenshot_",
            "Screenshot",
            "honor_screenshot_",
            "Screen_Capture_",
            "wechat_",
            "SVID_",
            "Screenrecorder"
        )
    }

    val settingsFlow: Flow<CleanerSettings> = context.dataStore.data.map { prefs ->
        val saved = prefs[KEY_PREFIXES]
        val prefixes = if (saved != null) {
            var updated = saved
            if ("SVID_" !in updated) updated = updated + "SVID_"
            if ("Screenrecorder" !in updated) updated = updated + "Screenrecorder"
            updated
        } else {
            DEFAULT_PREFIXES
        }
        CleanerSettings(
            prefixes = prefixes,
            retentionHours = prefs[KEY_RETENTION_HOURS] ?: 72, // 默认保留 3 天
            autoCleanupEnabled = prefs[KEY_AUTO_CLEAN_ENABLED] ?: true,
            requireCharging = prefs[KEY_REQUIRE_CHARGING] ?: false,
            totalDeletedCount = prefs[KEY_TOTAL_DELETED] ?: 0,
            totalFreedBytes = prefs[KEY_TOTAL_FREED] ?: 0L,
            lastCleanTimestamp = prefs[KEY_LAST_CLEAN_TIME] ?: 0L,
            language = prefs[KEY_LANGUAGE] ?: "zh",
            soundEffect = prefs[KEY_SOUND_EFFECT] ?: "shredder",
            flashEffectEnabled = prefs[KEY_FLASH_EFFECT_ENABLED] ?: true,
            hapticEnabled = prefs[KEY_HAPTIC_ENABLED] ?: true
        )
    }

    suspend fun getSettings(): CleanerSettings = settingsFlow.first()

    suspend fun setLanguage(language: String) {
        context.dataStore.edit { it[KEY_LANGUAGE] = language }
    }

    suspend fun setSoundEffect(soundEffect: String) {
        context.dataStore.edit { it[KEY_SOUND_EFFECT] = soundEffect }
    }

    suspend fun setFlashEffectEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_FLASH_EFFECT_ENABLED] = enabled }
    }

    suspend fun setHapticEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_HAPTIC_ENABLED] = enabled }
    }

    suspend fun setPrefixes(prefixes: Set<String>) {
        context.dataStore.edit { it[KEY_PREFIXES] = prefixes }
    }

    suspend fun addPrefix(prefix: String) {
        val trimmed = prefix.trim()
        if (trimmed.isNotEmpty()) {
            context.dataStore.edit { prefs ->
                val current = prefs[KEY_PREFIXES] ?: DEFAULT_PREFIXES
                prefs[KEY_PREFIXES] = current + trimmed
            }
        }
    }

    suspend fun removePrefix(prefix: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_PREFIXES] ?: DEFAULT_PREFIXES
            prefs[KEY_PREFIXES] = current - prefix
        }
    }

    suspend fun setRetentionHours(hours: Int) {
        context.dataStore.edit { it[KEY_RETENTION_HOURS] = hours }
    }

    suspend fun setAutoCleanupEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_AUTO_CLEAN_ENABLED] = enabled }
    }

    suspend fun setRequireCharging(required: Boolean) {
        context.dataStore.edit { it[KEY_REQUIRE_CHARGING] = required }
    }

    suspend fun recordCleanup(deletedCount: Int, freedBytes: Long) {
        context.dataStore.edit { prefs ->
            val currentDeleted = prefs[KEY_TOTAL_DELETED] ?: 0
            val currentFreed = prefs[KEY_TOTAL_FREED] ?: 0L
            prefs[KEY_TOTAL_DELETED] = currentDeleted + deletedCount
            prefs[KEY_TOTAL_FREED] = currentFreed + freedBytes
            prefs[KEY_LAST_CLEAN_TIME] = System.currentTimeMillis()
        }
    }
}
