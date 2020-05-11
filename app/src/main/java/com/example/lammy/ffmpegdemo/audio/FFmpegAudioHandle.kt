package com.example.lammy.ffmpegdemo.audio

/**
 * Desc :
 * Modified :
 */
object FFmpegAudioHandle  {
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

    @JvmStatic
    external fun encodePcmFile(souPath: String?, tarPath: String?): Int

    @JvmStatic
    external fun initAudio(url: String?): Int

    @JvmStatic
    external fun encodeAudio(buffer: ByteArray?): Int

    @JvmStatic
    external fun close(): Int
}