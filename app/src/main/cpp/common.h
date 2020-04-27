#ifndef MVVM_IJK_AUDIO_LIB_H
#define MVVM_IJK_AUDIO_LIB_H
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
#endif
