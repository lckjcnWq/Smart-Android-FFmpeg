package com.example.lammy.ffmpegdemo.view

import android.os.Bundle
import android.view.View
import androidx.databinding.ViewDataBinding
import com.aleyn.mvvm.base.BaseActivity
import com.aleyn.mvvm.base.NoViewModel
import com.example.lammy.ffmpegdemo.R
import com.example.lammy.ffmpegdemo.rtmp.RtmpHandle
import kotlinx.android.synthetic.main.activity_rtmpdump_file.*

/**
 * dec      :推送文件RTMP流(librmpt)
 * Modified :
 */
class VideoFileRtmpRtmpDumpActivity : BaseActivity<NoViewModel, ViewDataBinding>() {
    override fun layoutId(): Int {
        return R.layout.activity_rtmpdump_file
    }

    override fun initView(savedInstanceState: Bundle?) {
        btn_start.setOnClickListener(View.OnClickListener {
            btnStart(it)
        })
    }

    override fun initData() {
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