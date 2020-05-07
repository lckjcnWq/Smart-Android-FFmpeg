package com.example.lammy.ffmpegdemo.view

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
import java.io.File


class AudioActivity : BaseActivity<NoViewModel, ViewDataBinding>() {
    val file=FileUtils.getFileByPath("/sdcard/test.mp3")
    override fun layoutId(): Int {
        return R.layout.activity_audio
    }

    override fun initView(savedInstanceState: Bundle?) {
    }

    override fun initData() {
        btn_get_mp3.setOnClickListener(View.OnClickListener {
             LogUtils.i("打印路径:"+file.absolutePath)
             CoroutineScope(Dispatchers.IO).launch {
                 FFmpegUtil.getAudioMp3Info(file.absolutePath)
            }
        })
        btn_play_mp3.setOnClickListener(View.OnClickListener {
             LogUtils.i("打印路径:"+file.absolutePath)
             CoroutineScope(Dispatchers.IO).launch {
                 FFmpegUtil.nativeAudioPlay(file.absolutePath)
            }
        })

        btn_audio_code.setOnClickListener(View.OnClickListener {

        })

        btn_audio_decode.setOnClickListener(View.OnClickListener {

        })
        btn_sign.setOnClickListener(View.OnClickListener {
        })

    }
}