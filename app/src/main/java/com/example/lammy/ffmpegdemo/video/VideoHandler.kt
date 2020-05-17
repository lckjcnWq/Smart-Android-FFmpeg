package com.example.lammy.ffmpegdemo.video

object VideoHandler {
    init {
        System.loadLibrary("audio_lib")
        System.loadLibrary("avfilter-6")
        System.loadLibrary("avformat-57")
        System.loadLibrary("avcodec-57")
        System.loadLibrary("avdevice-57")
        System.loadLibrary("avutil-55")
        System.loadLibrary("swresample-2")
        System.loadLibrary("swscale-4")
        System.loadLibrary("rtmp")
    }

//    @JvmStatic
//    external fun videoCut(start: Double, end:Double, input: String,outPut:String):Int
}