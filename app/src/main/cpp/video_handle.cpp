#include <jni.h>
#include <string>
#include<android/log.h>
#include <exception>
#include <iostream>

//定义日志宏变量
#define logw(content)   __android_log_write(ANDROID_LOG_WARN,"eric",content)
#define loge(content)   __android_log_write(ANDROID_LOG_ERROR,"eric",content)
#define logd(content)   __android_log_write(ANDROID_LOG_DEBUG,"eric",content)

extern "C" {
#include "libavutil/avutil.h"
#include "libavdevice/avdevice.h"
#include "libavcodec/avcodec.h"
#include "libswscale/swscale.h"
#include "libswresample/swresample.h"
#include "libavutil/avutil.h"
#include "libavutil/imgutils.h"
#include "libavformat/avformat.h"
#include "libavutil/timestamp.h"
}

using namespace std;

//extern "C"
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