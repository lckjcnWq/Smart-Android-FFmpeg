package com.example.lammy.ffmpegdemo.view

import android.app.Activity
import android.os.Bundle
import android.view.View
import androidx.annotation.Nullable
import com.example.ffmpeg_lib.utils.FileUtil
import com.example.ffmpeg_lib.utils.LogUtils
import com.example.lammy.ffmpegdemo.R
import com.example.lammy.ffmpegdemo.rtmp.RtmpHandle
import java.io.File

/**
 * Modified :
 */
class VideoFileRtmpRtmpDumpActivity : Activity() {
    override fun onCreate(@Nullable savedInstanceState: Bundle) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rtmpdump_file)
    }

    fun btnStart(view: View?) {
        object : Thread() {
            override fun run() {
                super.run()
                RtmpHandle.pushFile("/sdcard/input_test.flv")
            }
        }.start()
    }
}