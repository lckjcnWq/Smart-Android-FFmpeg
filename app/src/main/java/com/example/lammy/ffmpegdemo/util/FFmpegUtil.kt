package com.example.lammy.ffmpegdemo.util

/**
 * Created by Lammy on 2018/9/1.
 */
object FFmpegUtil {

    init {
        System.loadLibrary("audio_lib")
        System.loadLibrary("avfilter-6")
        System.loadLibrary("avformat-57")
        System.loadLibrary("avcodec-57")
        System.loadLibrary("avdevice-57")
        System.loadLibrary("avutil-55")
        System.loadLibrary("swresample-2")
        System.loadLibrary("swscale-4")
    }
    //音频相关
    @JvmStatic
    external fun openAudioDevice():Int

    @JvmStatic
    external fun closeAudioDevice():Int

    @JvmStatic
    external fun recordAudioDevice():Int

    @JvmStatic
    external fun getAudioMp3Info(path: String)

    @JvmStatic
    external fun nativeAudioPlay(path: String)

    @JvmStatic
    external fun videoH264Encode()


}