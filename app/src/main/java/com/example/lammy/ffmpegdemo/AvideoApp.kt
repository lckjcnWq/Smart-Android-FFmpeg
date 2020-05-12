package com.example.lammy.ffmpegdemo

import android.app.Application
import com.blankj.utilcode.util.CrashUtils

class AvideoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashUtils.init()
    }
}