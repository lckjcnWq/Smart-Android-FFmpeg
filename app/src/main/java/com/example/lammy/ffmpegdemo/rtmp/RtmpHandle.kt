package com.example.lammy.ffmpegdemo.rtmp

/**
 * Desc :  RTMPDump调用类
 * Modified :
 */
object RtmpHandle  {
    init {
        System.loadLibrary("rtmp")
    }

    @JvmStatic
    external fun pushFile(path: String?)

    @JvmStatic
    external fun connect(url: String?): Int

    @JvmStatic
    external fun push(buf: ByteArray?, length: Int): Int

    @JvmStatic
    external fun close(): Int
}