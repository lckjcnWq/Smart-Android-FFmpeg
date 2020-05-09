package com.example.ffmpeg_lib.ffmpeg;

/**
 * Modified :
 */

public interface PushCallback {
    void videoCallback(long pts, long dts, long duration, long index);
}
