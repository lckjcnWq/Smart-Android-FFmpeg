package com.example.lammy.ffmpegdemo.view

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.TextView
import com.example.ffmpeg_lib.audio.AudioCodec
import com.example.ffmpeg_lib.utils.FileUtil
import com.example.lammy.ffmpegdemo.R
import java.text.DecimalFormat
import java.text.NumberFormat

/**
 * Modified :
 */
class AudioFormatChangeFFmpegActivity : Activity() {
    private var tvInfo: TextView? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_audio_codec)
        initView()
    }

    private fun initView() {
        tvInfo = findViewById(R.id.tv_info)
    }

    fun btnStart(view: View?) {
        startRecord()
    }

    private fun startRecord() {
        val audioCodec: AudioCodec = AudioCodec.newInstance()
        audioCodec.setIOPath(FileUtil.getMainDir().absolutePath.toString() + "/dongfengpo.mp3", FileUtil.getMainDir().absolutePath.toString() + "/dongfengpo.aac")
        audioCodec.prepare()
        audioCodec.startAsync()
        audioCodec.setOnCompleteListener(AudioCodec.OnCompleteListener {
            audioCodec.release()
            runOnUiThread { tvInfo!!.text = "100%" }
        })
        val df = NumberFormat.getInstance() as DecimalFormat
        df.applyPattern("##.##%")
        audioCodec.setOnProgressListener(AudioCodec.OnProgressListener { current, total ->
            runOnUiThread { tvInfo!!.text = current.toString() + "/" + total + "  " + df.format(current.toDouble() / total) }
        })
    }
}