package com.example.lammy.ffmpegdemo.view.audio

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.databinding.ViewDataBinding
import com.aleyn.mvvm.base.BaseActivity
import com.aleyn.mvvm.base.NoViewModel
import com.blankj.utilcode.util.*
import com.example.lammy.ffmpegdemo.R
import com.example.lammy.ffmpegdemo.util.FFmpegUtil
import kotlinx.android.synthetic.main.activity_audio.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class AudioActivity : BaseActivity<NoViewModel, ViewDataBinding>() {
    val file=FileUtils.getFileByPath("/sdcard/test.mp3")

    override fun layoutId(): Int {
        return R.layout.activity_audio
    }

    override fun initView(savedInstanceState: Bundle?) {
    }

    override fun initData() {
        btn_audio_info.setOnClickListener(View.OnClickListener {
             CoroutineScope(Dispatchers.IO).launch {
                 FFmpegUtil.getAudioMp3Info(file.absolutePath)
            }
        })
        btn_resampling.setOnClickListener(View.OnClickListener {
             CoroutineScope(Dispatchers.IO).launch {
                 FFmpegUtil.nativeAudioPlay(file.absolutePath)
            }
        })
        btn_audio_change_by_MediaCodec.setOnClickListener(View.OnClickListener {
            startActivity(Intent().setClass(this, AudioFormatChangeMediaCodecActivity::class.java))
        })
        btn_aac_MediaCodec.setOnClickListener(View.OnClickListener {
            startActivity(Intent().setClass(this, AudioRecordMediaCodecActivity::class.java))
        })
        btn_aac_ffmpeg.setOnClickListener(View.OnClickListener {
            startActivity(Intent().setClass(this, AudioRecordFFmpegActivity::class.java))
        })
    }
}