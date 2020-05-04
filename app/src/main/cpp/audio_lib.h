#include <jni.h>
#include <string>
#include <android/log.h>

#ifndef MVVM_JKPLAYER_AUDIOLIB_H
#define MVVM_JKPLAYER_AUDIOLIB_H
#define LOG_TAG "WuQuan"
#define AUDIO_SAMPLE_RATE 44100
#define SLOG(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

extern "C" {
#include "libavutil/avutil.h"
#include "libavformat/avformat.h"
#include "libavdevice/avdevice.h"
#include "libavcodec/avcodec.h"
#include "libswscale/swscale.h"
#include "libswresample/swresample.h"
#include "libavutil/avutil.h"
}

jobject audioTrack;
jmethodID jAudioTrackWriteMid;

jobject initAudioTrack(JNIEnv *env) {
    jclass jAudioTrackClass = env->FindClass("android/media/AudioTrack");
    jmethodID jAudioTrackCMid = env->GetMethodID(jAudioTrackClass, "<init>", "(IIIIII)V"); //构造
    int streamType = 3;
    int sampleRateInHz = 44100;
    int channelConfig = (0x4 | 0x8);
    int audioFormat = 2;
    jmethodID jGetMinBufferSizeMid = env->GetStaticMethodID(jAudioTrackClass, "getMinBufferSize",
                                                            "(III)I");
    int bufferSizeInBytes = env->CallStaticIntMethod(jAudioTrackClass, jGetMinBufferSizeMid,
                                                     sampleRateInHz, channelConfig, audioFormat);
    int mode = 1;

    //创建了AudioTrack
    jobject jAudioTrack = env->NewObject(jAudioTrackClass, jAudioTrackCMid, streamType,
                                         sampleRateInHz, channelConfig, audioFormat,
                                         bufferSizeInBytes, mode);

    //play方法
    jmethodID jPlayMid = env->GetMethodID(jAudioTrackClass, "play", "()V");
    env->CallVoidMethod(jAudioTrack, jPlayMid);

    // write method
    jAudioTrackWriteMid = env->GetMethodID(jAudioTrackClass, "write", "([BII)I");
    return jAudioTrack;
}


void freeContext(AVFormatContext *pFormatContext) {
    if (pFormatContext != NULL) {
        avformat_close_input(&pFormatContext);
        avformat_free_context(pFormatContext);
        pFormatContext = NULL;
    }
}

AVFormatContext *initFormatCnontext(const char *url) {
    AVFormatContext *pFormatContext = NULL;
    //1、初始化所有组件，只有调用了该函数，才能使用复用器和编解码器
    av_register_all();
    //2、打开文件
    int open_input_result = avformat_open_input(&pFormatContext, url, NULL, NULL);
    if (open_input_result != 0) {
        SLOG("format open input error: %s", av_err2str(open_input_result));
        freeContext(pFormatContext);
        return NULL;
    }

    //3.填充流信息到 pFormatContext
    int formatFindStreamInfoRes = avformat_find_stream_info(pFormatContext, NULL);
    if (formatFindStreamInfoRes < 0) {
        SLOG("format find stream info error: %s", av_err2str(formatFindStreamInfoRes));
        freeContext(pFormatContext);
        return NULL;
    }
    return pFormatContext;
}

AVCodecContext *openDecoder(AVFormatContext *pFormatContext, AVCodecParameters *pCodecParameters) {
    //1.查找编码器
    AVCodec *pCodec = avcodec_find_decoder(pCodecParameters->codec_id);

    if (pCodec == NULL) {
        SLOG("查找編碼器 error");
        freeContext(pFormatContext);
        return NULL;
    }

    //2、创建编码器上下文
    AVCodecContext *pCodecContext = avcodec_alloc_context3(pCodec);
    if (pCodecContext == NULL) {
        SLOG("创建编码器上下文 error");
        freeContext(pFormatContext);
        return NULL;
    }

    //3 打开编码器
    int codecOpenRes = avcodec_open2(pCodecContext, pCodec, NULL);
    if (codecOpenRes != 0) {
        SLOG("打開編碼器 error");
        freeContext(pFormatContext);
        return NULL;
    }
    return pCodecContext;
}

SwrContext *initSample(AVFormatContext *pFormatContext, AVCodecContext *pCodecContext) {
    SwrContext *swrContext = swr_alloc_set_opts(NULL,
                                                AV_CH_LAYOUT_STEREO,
                                                AVSampleFormat::AV_SAMPLE_FMT_S16,
                                                AUDIO_SAMPLE_RATE,
                                                AV_CH_LAYOUT_STEREO,
                                                AV_SAMPLE_FMT_FLT,
                                                AUDIO_SAMPLE_RATE, 0, NULL);
    if (swrContext == NULL) {
        SLOG("swr_alloc_set_opts error");
        freeContext(pFormatContext);
        return NULL;
    }

    int swrInitRes = swr_init(swrContext);
    if (swrInitRes < 0) {
        SLOG("swr_init error");
        freeContext(pFormatContext);
        return NULL;
    }
    return swrContext;
}


#endif
