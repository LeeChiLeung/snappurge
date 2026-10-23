package com.snappurge.app.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.Log
import com.snappurge.app.R

class SoundEffectsHelper(context: Context) {
    companion object {
        private const val TAG = "SoundEffectsHelper"
    }

    private val soundPool: SoundPool
    private var shredderSoundId: Int = 0
    private var engineSoundId: Int = 0
    private var discInsertSoundId: Int = 0
    private val loadedSoundIds = mutableSetOf<Int>()

    init {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(3)
            .setAudioAttributes(audioAttributes)
            .build()

        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) {
                loadedSoundIds.add(sampleId)
            } else {
                Log.w(TAG, "SoundPool load failed for id $sampleId, status $status")
            }
        }

        try {
            shredderSoundId = soundPool.load(context, R.raw.shredder, 1)
            engineSoundId = soundPool.load(context, R.raw.engine, 1)
            discInsertSoundId = soundPool.load(context, R.raw.disc_insert, 1)
        } catch (e: Exception) {
            Log.e(TAG, "Failed loading raw sound resources", e)
        }
    }

    fun play(soundType: String) {
        val soundId = when (soundType) {
            "shredder" -> shredderSoundId
            "engine" -> engineSoundId
            "disc_insert" -> discInsertSoundId
            else -> 0
        }
        if (soundId != 0) {
            soundPool.play(soundId, 1f, 1f, 1, 0, 1f)
        }
    }

    fun playDiscInsert() {
        if (discInsertSoundId != 0) {
            soundPool.play(discInsertSoundId, 1f, 1f, 1, 0, 1f)
        }
    }

    fun release() {
        soundPool.release()
    }
}
