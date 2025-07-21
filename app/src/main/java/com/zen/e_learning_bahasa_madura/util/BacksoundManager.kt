package com.zen.e_learning_bahasa_madura.util

import android.content.Context
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import kotlin.math.max
import kotlin.math.min

object BacksoundManager {

    private var mediaPlayer: MediaPlayer? = null
    private var currentResId: Int? = null
    private var isPaused = false
    private var handler = Handler(Looper.getMainLooper())
    private var volume = 0.1f // 🔉 volume default (0.0f - 1.0f)

    fun start(context: Context, resId: Int) {
        if (mediaPlayer == null || currentResId != resId) {
            stop()
            mediaPlayer = MediaPlayer.create(context.applicationContext, resId).apply {
                isLooping = true
                setVolume(0f, 0f)
                start()
                fadeIn()
            }
            currentResId = resId
        } else if (isPaused) {
            fadeIn()
            mediaPlayer?.start()
            isPaused = false
        }
    }

    fun pause() {
        if (mediaPlayer?.isPlaying == true) {
            fadeOut {
                mediaPlayer?.pause()
                isPaused = true
            }
        }
    }

    fun resume() {
        if (isPaused) {
            mediaPlayer?.start()
            fadeIn()
            isPaused = false
        }
    }

    fun stop() {
        handler.removeCallbacksAndMessages(null)
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        currentResId = null
        isPaused = false
    }

    fun pauseImmediately() {
        mediaPlayer?.pause()
        isPaused = true
        handler.removeCallbacksAndMessages(null) // Stop fade thread
    }

    fun isPlaying(): Boolean = mediaPlayer?.isPlaying == true

    private fun fadeIn(duration: Long = 1000L) {
        handler.removeCallbacksAndMessages(null)
        var vol = 0f
        val step = volume / 10f
        repeat(10) { i ->
            handler.postDelayed({
                vol = min(volume, vol + step)
                mediaPlayer?.setVolume(vol, vol)
            }, i * (duration / 10))
        }
    }

    private fun fadeOut(duration: Long = 1000L, onEnd: () -> Unit) {
        handler.removeCallbacksAndMessages(null)
        var vol = volume
        val step = volume / 10f
        repeat(10) { i ->
            handler.postDelayed({
                vol = max(0f, vol - step)
                mediaPlayer?.setVolume(vol, vol)
                if (i == 9) onEnd()
            }, i * (duration / 10))
        }
    }
}
