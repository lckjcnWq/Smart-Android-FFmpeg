package com.example.lammy.ffmpegdemo.view

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.databinding.ViewDataBinding
import com.aleyn.mvvm.base.BaseActivity
import com.aleyn.mvvm.base.NoViewModel
import com.blankj.utilcode.util.FileUtils
import com.blankj.utilcode.util.LogUtils
import com.example.lammy.ffmpegdemo.R
import com.example.lammy.ffmpegdemo.util.FFmpegUtil
import kotlinx.android.synthetic.main.activity_audio.*
import kotlinx.android.synthetic.main.activity_medio.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class VideoActivity : BaseActivity<NoViewModel, ViewDataBinding>() {
    override fun layoutId(): Int {
        return R.layout.activity_medio
    }

    override fun initView(savedInstanceState: Bundle?) {
    }

    override fun initData() {
        btn_h264_encode.setOnClickListener(View.OnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                FFmpegUtil.videoH264Encode()   //适用于ios平台NV12转YUV420P
            }
        })
        btn_rtmp_ffmpeg.setOnClickListener(View.OnClickListener {
            startActivity(Intent().setClass(this,VideoFileRtmpFFmpegActivity::class.java))
        })
        btn_rtmp_librmpt.setOnClickListener(View.OnClickListener {
            startActivity(Intent().setClass(this,VideoFileRtmpRtmpDumpActivity::class.java))
        })
        btn_ffmpeg_rmpt.setOnClickListener(View.OnClickListener {
            startActivity(Intent().setClass(this,CameraFFmpegPushRtmpActivity::class.java))
        })
        btn_MediaCodec_flv.setOnClickListener(View.OnClickListener {
            startActivity(Intent().setClass(this,CameraMediaCodecFileActivity::class.java))
        })
        btn_ffmpeg_rmpt_io.setOnClickListener(View.OnClickListener {
            startActivity(Intent().setClass(this,CameraMediaCodecRtmpActivity::class.java))
        })
        btn_merge.setOnClickListener(View.OnClickListener {
            startActivity(Intent().setClass(this,VideoCompoundFileActivity::class.java))
        })
        btn_video_cut.setOnClickListener(View.OnClickListener {
            startActivity(Intent().setClass(this,VideoPlayActivity::class.java))
        })
    }
}