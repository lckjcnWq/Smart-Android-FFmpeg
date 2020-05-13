package com.example.lammy.ffmpegdemo

import android.app.Application
import com.aleyn.mvvm.base.BaseApplication
import com.blankj.utilcode.util.CrashUtils

class AvideoApp : BaseApplication() {
    override fun onCreate() {
        super.onCreate()
        CrashUtils.init()
    }
}