package com.snappurge.app.hardware

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class FlashlightHelper(context: Context) {
    companion object {
        private const val TAG = "FlashlightHelper"
    }

    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager

    private val torchCameraId: String? by lazy {
        try {
            val list = cameraManager?.cameraIdList ?: emptyArray()
            val id = list.firstOrNull { id ->
                val characteristics = cameraManager?.getCameraCharacteristics(id) ?: return@firstOrNull false
                val hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                hasFlash && facing == CameraCharacteristics.LENS_FACING_BACK
            } ?: list.firstOrNull { id ->
                val characteristics = cameraManager?.getCameraCharacteristics(id) ?: return@firstOrNull false
                characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
            Log.i(TAG, "Resolved torch cameraId: $id (candidates: ${list.joinToString()})")
            id
        } catch (e: Exception) {
            Log.w(TAG, "Failed resolving camera with flash unit", e)
            null
        }
    }

    /**
     * 极速爆闪指定次数 (亮 onMs, 灭 offMs)
     */
    suspend fun strobe(times: Int = 4, onMs: Long = 60L, offMs: Long = 60L) {
        val cid = torchCameraId
        Log.i(TAG, "strobe requested: times=$times, torchCameraId=$cid")
        if (cid == null) return
        withContext(Dispatchers.Default) {
            try {
                repeat(times) {
                    cameraManager?.setTorchMode(cid, true)
                    delay(onMs)
                    cameraManager?.setTorchMode(cid, false)
                    delay(offMs)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Flashlight strobe failed", e)
            } finally {
                try {
                    cameraManager?.setTorchMode(cid, false)
                } catch (_: Exception) {}
            }
        }
    }
}
