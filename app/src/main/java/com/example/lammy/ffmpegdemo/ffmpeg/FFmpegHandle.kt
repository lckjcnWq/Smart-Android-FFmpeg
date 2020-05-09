package com.example.lammy.ffmpegdemo.ffmpeg

import com.example.ffmpeg_lib.ffmpeg.PushCallback

/**
 * Desc :
 * Modified :
 */
object FFmpegHandle {
    init {
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
    external fun setCallback(pushCallback: PushCallback?): Int

    @JvmStatic
    external fun getAvcodecConfiguration(): String?

    @JvmStatic
    external fun pushRtmpFile(path: String?): Int

    @JvmStatic
    external fun initVideo(url: String?, jwidth: Int, jheight: Int): Int

    @JvmStatic
    external fun onFrameCallback(buffer: ByteArray?): Int

    @JvmStatic
    external fun sendH264(buffer: ByteArray?, len: Int): Int

    @JvmStatic
    external fun initVideo2(url: String?): Int

    @JvmStatic
    external fun close(): Int
}