package com.example.lammy.ffmpegdemo.view

import android.os.Bundle
import android.view.View
import androidx.databinding.ViewDataBinding
import com.aleyn.mvvm.base.BaseActivity
import com.aleyn.mvvm.base.NoViewModel
import com.example.ffmpeg_lib.audio.AudioCodec
import com.example.lammy.ffmpegdemo.R
import kotlinx.android.synthetic.main.activity_audio_codec.*
import java.text.DecimalFormat
import java.text.NumberFormat

/**
 * Modified :音视频格式转换
 */
class AudioFormatChangeMediaCodecActivity : BaseActivity<NoViewModel, ViewDataBinding>() {

    override fun layoutId(): Int {
        return R.layout.activity_audio_codec
    }

    override fun initView(savedInstanceState: Bundle?) {
    }

    override fun initData() {
        btn_start.setOnClickListener(View.OnClickListener {
            startRecord()
        })
    }

    private fun startRecord() {
        val audioCodec: AudioCodec = AudioCodec.newInstance()
        audioCodec.setIOPath("/sdcard/input_test.mp3", "/sdcard/aaa.aac")
        audioCodec.prepare()
        audioCodec.startAsync()
        audioCodec.setOnCompleteListener(AudioCodec.OnCompleteListener {
            audioCodec.release()
            runOnUiThread { tv_info.text = "100%" }
        })
        val df = NumberFormat.getInstance() as DecimalFormat
        df.applyPattern("##.##%")
        audioCodec.setOnProgressListener(AudioCodec.OnProgressListener { current, total ->
            runOnUiThread { tv_info.text = current.toString() + "/" + total + "  " + df.format(current.toDouble() / total) }
        })
    }

}