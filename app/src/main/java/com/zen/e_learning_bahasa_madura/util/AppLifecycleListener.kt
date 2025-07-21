package com.zen.e_learning_bahasa_madura.util

import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

class AppLifecycleListener(private val context: Context) : DefaultLifecycleObserver {

    override fun onStart(owner: LifecycleOwner) {
        BacksoundManager.resume()
    }

    override fun onStop(owner: LifecycleOwner) {
        BacksoundManager.pause()
    }
}
