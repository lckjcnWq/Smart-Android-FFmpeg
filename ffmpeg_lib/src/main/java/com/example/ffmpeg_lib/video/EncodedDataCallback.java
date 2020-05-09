package com.example.ffmpeg_lib.video;

import android.media.MediaCodec;

import java.nio.ByteBuffer;

/**
 * @description 音视频编码数据回调
 */
public interface EncodedDataCallback {
    void onAudioEncodedCallback(ByteBuffer byteBuffer, MediaCodec.BufferInfo bufferInfo);
    void onVideoEncodedCallback(ByteBuffer byteBuffer, MediaCodec.BufferInfo bufferInfo);
}
