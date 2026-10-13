package com.seagull.clip

import android.app.Application
import com.seagull.clip.data.ClipRepo
import com.seagull.clip.data.Prefs

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Prefs.init(this)
        ClipRepo.init(this)
    }
}
