package com.zen.e_learning_bahasa_madura.view

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import com.zen.e_learning_bahasa_madura.R
import com.zen.e_learning_bahasa_madura.util.AppLifecycleListener
import com.zen.e_learning_bahasa_madura.util.BacksoundManager

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        ProcessLifecycleOwner.get().lifecycle.addObserver(AppLifecycleListener(this))
    }

    override fun onTerminate() {
        super.onTerminate()
        BacksoundManager.stop()
    }
}
