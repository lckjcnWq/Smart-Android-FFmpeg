package com.example.ffmpeg_lib.video;

/**
 * @description
 */
public interface SourceDataCallback {
    void onAudioSourceDataCallback(byte[] data, int index);
    void onVideoSourceDataCallback(byte[] data, int index);
}
