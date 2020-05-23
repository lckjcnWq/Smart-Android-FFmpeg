package com.example.lammy.ffmpegdemo.video

import android.view.Surface

object VideoHandler {
    init {
        System.loadLibrary("video_lib")
        System.loadLibrary("avfilter-6")
        System.loadLibrary("avformat-57")
        System.loadLibrary("avcodec-57")
        System.loadLibrary("avdevice-57")
        System.loadLibrary("avutil-55")
        System.loadLibrary("swresample-2")
        System.loadLibrary("swscale-4")
    }

//    @JvmStatic
//    external fun videoCut(start: Double, end:Double, input: String,outPut:String):Int

//    @JvmStatic
//    external fun playVideo(videoPath: String, surface: Surface)
}