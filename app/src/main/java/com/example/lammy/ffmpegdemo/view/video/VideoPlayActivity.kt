package com.example.lammy.ffmpegdemo.view.video

import android.os.Bundle
import android.view.View
import androidx.databinding.ViewDataBinding
import com.aleyn.mvvm.base.BaseActivity
import com.aleyn.mvvm.base.NoViewModel
import com.blankj.utilcode.util.FileUtils
import com.example.lammy.ffmpegdemo.R
import com.example.lammy.ffmpegdemo.video.VideoHandler
import kotlinx.android.synthetic.main.activity_video_play.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class VideoPlayActivity  : BaseActivity<NoViewModel, ViewDataBinding>() {

    val inputFile= FileUtils.getFileByPath("/sdcard/input_test.mp4")
    val outputFile= FileUtils.getFileByPath("/sdcard/input00_test.mp4")
    override fun layoutId(): Int {
        return R.layout.activity_video_play
    }

    override fun initView(savedInstanceState: Bundle?) {
//        btn_video_cut.setOnClickListener(View.OnClickListener {
//           Thread(Runnable {
//               VideoHandler.videoCut(1.00,3.00,inputFile.absolutePath,outputFile.absolutePath)
//           })
//        })
        btn_video_play.setOnClickListener(View.OnClickListener {
            val videoPath = "/sdcard/input_test.mp4"
//            autoVideoView.playVideo(videoPath)
        })
    }

    override fun initData() {
    }
}