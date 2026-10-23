package com.snappurge.app.hardware

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

/**
 * 战术级高保真马达触感震动管理器 (适配 Android 8.0 ~ Android 16+)
 */
class HapticHelper(context: Context) {
    companion object {
        private const val TAG = "HapticHelper"
    }

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    val hasVibrator: Boolean
        get() = vibrator?.hasVibrator() == true

    init {
        Log.i(TAG, "HapticHelper initialized: hasVibrator=$hasVibrator, hasAmplitudeControl=${vibrator?.hasAmplitudeControl()}")
    }

    /**
     * 轻触微震 (开关切换、点击成果卡片等)
     */
    fun tap() {
        if (!hasVibrator) return
        Log.i(TAG, "tap() called")
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(25L, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(25L)
            }
        } catch (e: Exception) {
            Log.w(TAG, "tap vibration failed", e)
        }
    }

    /**
     * 机械吸入感震动 (扫描预览)
     * 卡扣咬合 -> 机械滑入 -> 锁定就位
     */
    fun preview() {
        if (!hasVibrator) return
        Log.i(TAG, "preview() called")
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && vibrator?.hasAmplitudeControl() == true) {
                val timings = longArrayOf(0L, 25L, 35L, 45L, 30L, 65L)
                val amplitudes = intArrayOf(0, 180, 0, 120, 0, 220)
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0L, 25L, 35L, 50L, 30L, 70L)
                vibrator?.vibrate(VibrationEffect.createWaveform(timings, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(0L, 25L, 35L, 50L, 30L, 70L), -1)
            }
        } catch (e: Exception) {
            Log.w(TAG, "preview vibration failed", e)
        }
    }

    /**
     * 战术级阶梯递增爆破震感 (立即清理)
     * 5段阶梯节奏 (伴随5连爆闪光效与引擎轰鸣/碎纸机音效) + 最终沉重强力收尾重击
     */
    fun clean() {
        if (!hasVibrator) return
        Log.i(TAG, "clean() called")
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && vibrator?.hasAmplitudeControl() == true) {
                // 5次阶梯递增脉冲 (45ms脉冲, 45ms休止, 力度 130 -> 160 -> 190 -> 225 -> 255) + 强力终结重击 (110ms, 力度 255)
                val timings = longArrayOf(
                    0L,
                    45L, 45L,
                    45L, 45L,
                    45L, 45L,
                    45L, 45L,
                    50L, 40L,
                    110L
                )
                val amplitudes = intArrayOf(
                    0,
                    130, 0,
                    160, 0,
                    190, 0,
                    225, 0,
                    255, 0,
                    255
                )
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0L, 45L, 45L, 45L, 45L, 45L, 45L, 45L, 45L, 50L, 40L, 110L)
                vibrator?.vibrate(VibrationEffect.createWaveform(timings, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(0L, 45L, 45L, 45L, 45L, 45L, 45L, 45L, 45L, 50L, 40L, 110L), -1)
            }
        } catch (e: Exception) {
            Log.w(TAG, "clean vibration failed", e)
        }
    }
}
