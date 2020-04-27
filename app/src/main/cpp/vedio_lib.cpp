//
// Created by Administrator on 2020/4/27 0027.
//

#include "vedio_lib.h"
#include "common.h"
#include <jni.h>
#include <string>
#include <android/log.h>

extern "C" {
#include "libavutil/avutil.h"
#include "libavformat/avformat.h"
#include "libavdevice/avdevice.h"
#include "libavcodec/avcodec.h"
#include "libswscale/swscale.h"
#include "libswresample/swresample.h"
}