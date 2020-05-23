#include <jni.h>
#include <android/log.h>
#include <android/native_window.h>
#include <android/native_window_jni.h>
#include <unistd.h>
#include <string>

#define LOG_TAG "FFNative"
#define ALOGV(...) ((void)__android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, __VA_ARGS__))
#define ALOGD(...) ((void)__android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__))
#define ALOGI(...) ((void)__android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__))
#define ALOGW(...) ((void)__android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__))
#define ALOGE(...) ((void)__android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__))

extern "C" {

#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>
#include <libavfilter/avfilter.h>
#include <libswscale/swscale.h>
#include "libswresample/swresample.h"
#include "libavutil/opt.h"
#include <libavutil/imgutils.h>

//JNIEXPORT jint JNICALL
//Java_com_example_lammy_ffmpegdemo_video_VideoHandler_videoCut(JNIEnv *env, jclass clazz,
//                                                              jdouble from_seconds, jdouble end_seconds,
//                                                              jstring in_filename, jstring out_filename) {
//    const char *  _in_filename = env->GetStringUTFChars(in_filename, 0);
//    const char *  _out_filename = env->GetStringUTFChars(out_filename, 0);
//    AVOutputFormat *ofmt = NULL;
//    AVFormatContext *ifmt_ctx = NULL, *ofmt_ctx = NULL;
//    AVPacket pkt;
//    int ret, i;
//
//    av_register_all();
//
//
//    //input context
//
//    if ((ret = avformat_open_input(&ifmt_ctx, _in_filename, 0, 0)) < 0) {
//        fprintf(stderr, "Could not open input file '%s'", in_filename);
//        goto end;
//    }
//
//    if ((ret = avformat_find_stream_info(ifmt_ctx, 0)) < 0) {
//        fprintf(stderr, "Failed to retrieve input stream information");
//        goto end;
//    }
//
//    av_dump_format(ifmt_ctx, 0, _in_filename, 0);
//
//    //output context
//    avformat_alloc_output_context2(&ofmt_ctx, NULL, NULL,
//                                   _out_filename);
//    if (!ofmt_ctx) {
//        fprintf(stderr, "Could not create output context\n");
//        ret = AVERROR_UNKNOWN;
//        goto end;
//    }
//
//    ofmt = ofmt_ctx->oformat;
//
//    for (i = 0; i < ifmt_ctx->nb_streams; i++) {
//        AVStream *in_stream = ifmt_ctx->streams[i];
//        AVStream *out_stream = avformat_new_stream(ofmt_ctx, in_stream->codec->codec);
//        if (!out_stream) {
//            fprintf(stderr, "Failed allocating output stream\n");
//            ret = AVERROR_UNKNOWN;
//            goto end;
//        }
//
//        ret = avcodec_copy_context(out_stream->codec, in_stream->codec);
//        if (ret < 0) {
//            fprintf(stderr, "Failed to copy context from input to output stream codec context\n");
//            goto end;
//        }
//        out_stream->codec->codec_tag = 0;
//        if (ofmt_ctx->oformat->flags & AVFMT_GLOBALHEADER)
//            out_stream->codec->flags |= AV_CODEC_FLAG_GLOBAL_HEADER;
//    }
//    av_dump_format(ofmt_ctx, 0, _out_filename, 1);
//
//    if (!(ofmt->flags & AVFMT_NOFILE)) {
//        ret = avio_open(&ofmt_ctx->pb, _out_filename, AVIO_FLAG_WRITE);
//        if (ret < 0) {
//            fprintf(stderr, "Could not open output file '%s'", out_filename);
//            goto end;
//        }
//    }
//
//    ret = avformat_write_header(ofmt_ctx, NULL);
//    if (ret < 0) {
//        fprintf(stderr, "Error occurred when opening output file\n");
//        goto end;
//    }
//
//    //    int64_t start_from = 8*AV_TIME_BASE;
//    //    进行裁剪的核心代码
//    ret = av_seek_frame(ifmt_ctx, -1, from_seconds*AV_TIME_BASE, AVSEEK_FLAG_ANY);
//    if (ret < 0) {
//        fprintf(stderr, "Error seek\n");
//        goto end;
//    }
//
//    int64_t *dts_start_from = static_cast<int64_t *>(malloc(
//            sizeof(int64_t) * ifmt_ctx->nb_streams));
//    memset(dts_start_from, 0, sizeof(int64_t) * ifmt_ctx->nb_streams);
//
//
//    int64_t *pts_start_from = static_cast<int64_t *>(malloc(
//            sizeof(int64_t) * ifmt_ctx->nb_streams));
//    memset(pts_start_from, 0, sizeof(int64_t) * ifmt_ctx->nb_streams);
//
//    while (1) {
//        AVStream *in_stream, *out_stream;
//
//        ret = av_read_frame(ifmt_ctx, &pkt);
//        if (ret < 0)
//            break;
//
//        in_stream  = ifmt_ctx->streams[pkt.stream_index];
//        out_stream = ofmt_ctx->streams[pkt.stream_index];
//
////        log_packet(ifmt_ctx, &pkt, "in");
//
//        if (av_q2d(in_stream->time_base) * pkt.pts > end_seconds) {
//            av_free_packet(&pkt);
//            break;
//        }
//
//        if (dts_start_from[pkt.stream_index] == 0) {
//            dts_start_from[pkt.stream_index] = pkt.dts;
////            printf("dts_start_from: %s\n", av_ts2str(dts_start_from[pkt.stream_index]));
//        }
//        if (pts_start_from[pkt.stream_index] == 0) {
//            pts_start_from[pkt.stream_index] = pkt.pts;
////            printf("pts_start_from: %s\n", av_ts2str(pts_start_from[pkt.stream_index]));
//        }
//
//        /* copy packet */
//        pkt.pts = av_rescale_q_rnd(pkt.pts - pts_start_from[pkt.stream_index], in_stream->time_base, out_stream->time_base, AV_ROUND_NEAR_INF|AV_ROUND_PASS_MINMAX);
//        pkt.dts = av_rescale_q_rnd(pkt.dts - dts_start_from[pkt.stream_index], in_stream->time_base, out_stream->time_base, AV_ROUND_NEAR_INF|AV_ROUND_PASS_MINMAX);
//        if (pkt.pts < 0) {
//            pkt.pts = 0;
//        }
//        if (pkt.dts < 0) {
//            pkt.dts = 0;
//        }
//        pkt.duration = (int)av_rescale_q((int64_t)pkt.duration, in_stream->time_base, out_stream->time_base);
//        pkt.pos = -1;
////        log_packet(ofmt_ctx, &pkt, "out");
//        printf("\n");
//
//        ret = av_interleaved_write_frame(ofmt_ctx, &pkt);
//        if (ret < 0) {
//            fprintf(stderr, "Error muxing packet\n");
//            break;
//        }
//        av_free_packet(&pkt);
//    }
//    free(dts_start_from);
//    free(pts_start_from);
//
//    av_write_trailer(ofmt_ctx);
//    end:
//
//    avformat_close_input(&ifmt_ctx);
//
//    /* close output */
//    if (ofmt_ctx && !(ofmt->flags & AVFMT_NOFILE))
//        avio_closep(&ofmt_ctx->pb);
//    avformat_free_context(ofmt_ctx);
//
//    if (ret < 0 && ret != AVERROR_EOF) {
//        fprintf(stderr, "Error occurred: %s\n", av_err2str(ret));
//        return 1;
//    }
//
//    return 0;
//}

//JNIEXPORT void JNICALL
//Java_com_example_lammy_ffmpegdemo_video_VideoHandler_playVideo(JNIEnv *env, jclass type, jstring videoPath_,
//                                                               jobject surface) {
//    const char *videoPath = env->GetStringUTFChars(videoPath_, 0);
//    ALOGI("PlayVideo: %s", videoPath);
//
//    if (videoPath == NULL) {
//        ALOGE("videoPath is null");
//        return;
//    }
//
//    AVFormatContext *formatContext = avformat_alloc_context();
//
//    // open video file
//    ALOGI("Open video file");
//    if (avformat_open_input(&formatContext, videoPath, NULL, NULL) != 0) {
//        ALOGE("Cannot open video file: %s\n", videoPath);
//        return;
//    }
//
//    // Retrieve stream information
//    ALOGI("Retrieve stream information");
//    if (avformat_find_stream_info(formatContext, NULL) < 0) {
//        ALOGE("Cannot find stream information.");
//        return;
//    }
//
//    // Find the first video stream
//    ALOGI("Find video stream");
//    int video_stream_index = -1;
//    for (int i = 0; i < formatContext->nb_streams; i++) {
//        if (formatContext->streams[i]->codecpar->codec_type == AVMEDIA_TYPE_VIDEO) {
//            video_stream_index = i;
//        }
//    }
//
//    if (video_stream_index == -1) {
//        ALOGE("No video stream found.");
//        return; // no video stream found.
//    }
//
//    // Get a pointer to the codec context for the video stream
//    ALOGI("Get a pointer to the codec context for the video stream");
//    AVCodecParameters *codecParameters = formatContext->streams[video_stream_index]->codecpar;
//
//    // Find the decoder for the video stream
//    ALOGI("Find the decoder for the video stream");
//    AVCodec *codec = avcodec_find_decoder(codecParameters->codec_id);
//    if (codec == NULL) {
//        ALOGE("Codec not found.");
//        return; // Codec not found
//    }
//
//    AVCodecContext *codecContext = avcodec_alloc_context3(codec);
//
//    if (codecContext == NULL) {
//        ALOGE("CodecContext not found.");
//        return; // CodecContext not found
//    }
//
//    // fill CodecContext according to CodecParameters
//    if (avcodec_parameters_to_context(codecContext, codecParameters) < 0) {
//        ALOGD("Fill CodecContext failed.");
//        return;
//    }
//
//    // init codex context
//    ALOGI("open Codec");
//    if (avcodec_open2(codecContext, codec, NULL)) {
//        ALOGE("Init CodecContext failed.");
//        return;
//    }
//
//    AVPixelFormat dstFormat = AV_PIX_FMT_RGBA;
//
//    // Allocate av packet
//    AVPacket *packet = av_packet_alloc();
//    if (packet == NULL) {
//        ALOGD("Could not allocate av packet.");
//        return;
//    }
//
//    // Allocate video frame
//    ALOGI("Allocate video frame");
//    AVFrame *frame = av_frame_alloc();
//    // Allocate render frame
//    ALOGI("Allocate render frame");
//    AVFrame *renderFrame = av_frame_alloc();
//
//    if (frame == NULL || renderFrame == NULL) {
//        ALOGD("Could not allocate video frame.");
//        return;
//    }
//
//    // Determine required buffer size and allocate buffer
//    ALOGI("Determine required buffer size and allocate buffer");
//    int size = av_image_get_buffer_size(dstFormat, codecContext->width, codecContext->height, 1);
//    uint8_t *buffer = (uint8_t *) av_malloc(size * sizeof(uint8_t));
//    av_image_fill_arrays(renderFrame->data, renderFrame->linesize, buffer, dstFormat,
//                         codecContext->width, codecContext->height, 1);
//
//    // init SwsContext
//    ALOGI("init SwsContext");
//    struct SwsContext *swsContext = sws_getContext(codecContext->width,
//                                                   codecContext->height,
//                                                   codecContext->pix_fmt,
//                                                   codecContext->width,
//                                                   codecContext->height,
//                                                   dstFormat,
//                                                   SWS_BILINEAR,
//                                                   NULL,
//                                                   NULL,
//                                                   NULL);
//    if (swsContext == NULL) {
//        ALOGE("Init SwsContext failed.");
//        return;
//    }
//
//    // native window
//    ALOGI("native window");
//    ANativeWindow *nativeWindow = ANativeWindow_fromSurface(env, surface);
//    ANativeWindow_Buffer windowBuffer;
//
//    // get video width , height
//    ALOGI("get video width , height");
//    int videoWidth = codecContext->width;
//    int videoHeight = codecContext->height;
//    ALOGI("VideoSize: [%d,%d]", videoWidth, videoHeight);
//
//    // 设置native window的buffer大小,可自动拉伸
//    ALOGI("set native window");
//    ANativeWindow_setBuffersGeometry(nativeWindow, videoWidth, videoHeight,
//                                     WINDOW_FORMAT_RGBA_8888);
//
//
//    ALOGI("read frame");
//    while (av_read_frame(formatContext, packet) == 0) {
//        // Is this a packet from the video stream?
//        if (packet->stream_index == video_stream_index) {
//
//            // Send origin data to decoder
//            int sendPacketState = avcodec_send_packet(codecContext, packet);
//            if (sendPacketState == 0) {
//                ALOGD("向解码器-发送数据");
//
//                int receiveFrameState = avcodec_receive_frame(codecContext, frame);
//                if (receiveFrameState == 0) {
//                    ALOGD("从解码器-接收数据");
//                    // lock native window buffer
//                    ANativeWindow_lock(nativeWindow, &windowBuffer, NULL);
//
//                    // 格式转换
//                    sws_scale(swsContext, (uint8_t const *const *) frame->data,
//                              frame->linesize, 0, codecContext->height,
//                              renderFrame->data, renderFrame->linesize);
//
//                    // 获取stride
//                    uint8_t *dst = (uint8_t *) windowBuffer.bits;
//                    uint8_t *src = (renderFrame->data[0]);
//                    int dstStride = windowBuffer.stride * 4;
//                    int srcStride = renderFrame->linesize[0];
//
//                    // 由于window的stride和帧的stride不同,因此需要逐行复制
//                    for (int i = 0; i < videoHeight; i++) {
//                        memcpy(dst + i * dstStride, src + i * srcStride, srcStride);
//                    }
//
//                    ANativeWindow_unlockAndPost(nativeWindow);
//                } else if (receiveFrameState == AVERROR(EAGAIN)) {
//                    ALOGD("从解码器-接收-数据失败：AVERROR(EAGAIN)");
//                } else if (receiveFrameState == AVERROR_EOF) {
//                    ALOGD("从解码器-接收-数据失败：AVERROR_EOF");
//                } else if (receiveFrameState == AVERROR(EINVAL)) {
//                    ALOGD("从解码器-接收-数据失败：AVERROR(EINVAL)");
//                } else {
//                    ALOGD("从解码器-接收-数据失败：未知");
//                }
//            } else if (sendPacketState == AVERROR(EAGAIN)) {//发送数据被拒绝，必须尝试先读取数据
//                ALOGD("向解码器-发送-数据包失败：AVERROR(EAGAIN)");//解码器已经刷新数据但是没有新的数据包能发送给解码器
//            } else if (sendPacketState == AVERROR_EOF) {
//                ALOGD("向解码器-发送-数据失败：AVERROR_EOF");
//            } else if (sendPacketState == AVERROR(EINVAL)) {//遍解码器没有打开，或者当前是编码器，也或者需要刷新数据
//                ALOGD("向解码器-发送-数据失败：AVERROR(EINVAL)");
//            } else if (sendPacketState == AVERROR(ENOMEM)) {//数据包无法压如解码器队列，也可能是解码器解码错误
//                ALOGD("向解码器-发送-数据失败：AVERROR(ENOMEM)");
//            } else {
//                ALOGD("向解码器-发送-数据失败：未知");
//            }
//
//        }
//        av_packet_unref(packet);
//    }
//
//
//    //内存释放
//    ALOGI("release memory");
//    ANativeWindow_release(nativeWindow);
//    av_frame_free(&frame);
//    av_frame_free(&renderFrame);
//    av_packet_free(&packet);
//    avcodec_close(codecContext);
//    avcodec_free_context(&codecContext);
//    avformat_close_input(&formatContext);
//    avformat_free_context(formatContext);
//    env->ReleaseStringUTFChars(videoPath_, videoPath);
//}
}
