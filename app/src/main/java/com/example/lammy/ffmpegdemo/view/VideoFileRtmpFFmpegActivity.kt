package com.example.lammy.ffmpegdemo.view

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.TextView
import com.example.ffmpeg_lib.ffmpeg.PushCallback
import com.example.ffmpeg_lib.utils.LogUtils
import com.example.lammy.ffmpegdemo.R
import com.example.lammy.ffmpegdemo.ffmpeg.FFmpegHandle
import java.io.File

/**
 * Modified :
 */
class VideoFileRtmpFFmpegActivity : Activity() {
    private var tvPushInfo: TextView? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_push_file_rtmp)
        initView()
        initData()
    }

    private fun initView() {
        tvPushInfo = findViewById(R.id.tv_push_info)
    }

    private fun initData() {
        val res: Int = FFmpegHandle.setCallback(PushCallback { pts, dts, duration, index ->
            val sb = StringBuilder()
            sb.append("pts: ").append(pts).append("\n")
            sb.append("dts: ").append(dts).append("\n")
            sb.append("duration: ").append(duration).append("\n")
            sb.append("index: ").append(index).append("\n")
            tvPushInfo!!.text = sb.toString()
        }

        )
        LogUtils.d("result $res")
    }

    fun btnPush(view: View?) {
        val path = "/sdcard/input_test.flv"
        val file = File(path)
        LogUtils.d(path + "  " + file.exists())
        object : Thread() {
            override fun run() {
                super.run()
                val result: Int = FFmpegHandle.pushRtmpFile(path)
                LogUtils.d("result $result")
            }
        }.start()
    }
}