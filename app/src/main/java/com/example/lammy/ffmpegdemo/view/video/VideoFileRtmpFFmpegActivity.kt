package com.example.lammy.ffmpegdemo.view.video

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.databinding.ViewDataBinding
import com.aleyn.mvvm.base.BaseActivity
import com.aleyn.mvvm.base.NoViewModel
import com.example.ffmpeg_lib.ffmpeg.PushCallback
import com.example.ffmpeg_lib.utils.LogUtils
import com.example.lammy.ffmpegdemo.R
import com.example.lammy.ffmpegdemo.ffmpeg.FFmpegHandle
import kotlinx.android.synthetic.main.activity_push_file_rtmp.*
import java.io.File

/**
 * Modified :推送文件RTMP流(ffmpeg)
 */
class VideoFileRtmpFFmpegActivity : BaseActivity<NoViewModel, ViewDataBinding>() {
    override fun layoutId(): Int {
        return R.layout.activity_push_file_rtmp
    }

    override fun initView(savedInstanceState: Bundle?) {
        btn_push.setOnClickListener(View.OnClickListener {
            btnPush(it)
        })
    }

    override fun initData() {
        val res: Int = FFmpegHandle.setCallback(PushCallback { pts, dts, duration, index ->
            val sb = StringBuilder()
            sb.append("pts: ").append(pts).append("\n")
            sb.append("dts: ").append(dts).append("\n")
            sb.append("duration: ").append(duration).append("\n")
            sb.append("index: ").append(index).append("\n")
            tv_push_info.text = sb.toString()
        }

        )
        LogUtils.d("result $res")
    }

    fun btnPush(view: View?) {
        object : Thread() {
            override fun run() {
                super.run()
                val result: Int = FFmpegHandle.pushRtmpFile("/sdcard/input_test.flv")
                LogUtils.d("result $result")
            }
        }.start()
    }

}