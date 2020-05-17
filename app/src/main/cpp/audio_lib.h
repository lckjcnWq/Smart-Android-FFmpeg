#include <jni.h>
#include <string>
#include <android/log.h>

#ifndef MVVM_JKPLAYER_AUDIOLIB_H
#define MVVM_JKPLAYER_AUDIOLIB_H
#define LOG_TAG "WuQuan"
#define V_WIDTH 960
#define V_HEIGTH 540
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

//@brief
//return
static AVFormatContext* open_dev(){

    int ret = 0;
    char errors[1024] = {0, };

    //ctx
    AVFormatContext *fmt_ctx = NULL;
    AVDictionary *options = NULL;

    //[[video device]:[audio device]]
    //0: 机器的摄像头
    //1: 桌面
    char *devicename = "0";

    //register audio device
    avdevice_register_all();

    //get format
    AVInputFormat *iformat = av_find_input_format("dshow");

    av_dict_set(&options, "video_size", "960x540", 0);
    av_dict_set(&options, "framerate", "30", 0);
    av_dict_set(&options, "pixel_format", "nv21", 0);

    //open device
    if((ret = avformat_open_input(&fmt_ctx, devicename, iformat, &options)) < 0 ){
        av_strerror(ret, errors, 1024);
        fprintf(stderr, "Failed to open video device, [%d]%s\n", ret, errors);
        return NULL;
    }

    return fmt_ctx;
}

/**
 * @brief xxxx
 * @param[in] xxx
 * @param[in] xxx
 * @param[out] xxx
 */
static void open_encoder(int width,
                         int height,
                         AVCodecContext **enc_ctx){

    int ret = 0;
    AVCodec *codec = NULL;

    codec = avcodec_find_encoder_by_name("libx264");
    if(!codec){
        printf("Codec libx264 not found\n");
        exit(1);
    }

    *enc_ctx = avcodec_alloc_context3(codec);
    if(!enc_ctx){
        printf("Could not allocate video codec context!\n");
        exit(1);
    }

    //SPS/PPS
    (*enc_ctx)->profile = FF_PROFILE_H264_HIGH_444;
    (*enc_ctx)->level = 50; //表示LEVEL是5.0

    //设置分辫率
    (*enc_ctx)->width = width;   //640
    (*enc_ctx)->height = height; //480

    //GOP
    (*enc_ctx)->gop_size = 250;
    (*enc_ctx)->keyint_min = 25; //option

    //设置B帧数据
    (*enc_ctx)->max_b_frames = 3; //option
    (*enc_ctx)->has_b_frames = 1; //option

    //参考帧的数量
    (*enc_ctx)->refs = 3;         //option

    //设置输入YUV格式
    (*enc_ctx)->pix_fmt = AV_PIX_FMT_YUV420P;

    //设置码率
    (*enc_ctx)->bit_rate = 6000000; //2000kbps

    //设置帧率
    (*enc_ctx)->time_base = (AVRational){1, 25}; //帧与帧之间的间隔是time_base
    (*enc_ctx)->framerate = (AVRational){25, 1}; //帧率，每秒 25 帧

    ret = avcodec_open2((*enc_ctx), codec, NULL);
    if(ret<0){
        printf("Could not open codec: %s!\n", av_err2str(ret));
        exit(1);
    }
}

/**
 * @brief xxxx
 * @param[in] width,
 * @param[in] height
 * @return AVFrame*
 */
static AVFrame* create_frame(int width, int height){

    int ret = 0;
    AVFrame *frame = NULL;

    frame = av_frame_alloc();
    if(!frame){
        printf("Error, No Memory!\n");
        goto __ERROR;
    }

    //设置参数
    frame->width = width;
    frame->height = height;
    frame->format = AV_PIX_FMT_YUV420P;

    //alloc inner memory
    ret = av_frame_get_buffer(frame, 32); //按 32 位对齐
    if(ret < 0){
        printf("Error, Failed to alloc buffer for frame!\n");
        goto __ERROR;
    }

    return frame;

    __ERROR:

    if(frame){
        av_frame_free(&frame);
    }

    return NULL;
}

static void encode(AVCodecContext *enc_ctx,
                   AVFrame *frame,
                   AVPacket *newpkt,
                   FILE *outfile){

    int ret = 0;
    if(frame){
        printf("send frame to encoder, pts=%lld", frame->pts);
    }
    //送原始数据给编码器进行编码
    ret = avcodec_send_frame(enc_ctx, frame);
    if(ret < 0) {
        printf("Error, Failed to send a frame for enconding!\n");
        exit(1);
    }

    //从编码器获取编码好的数据
    while(ret >=0) {
        ret = avcodec_receive_packet(enc_ctx, newpkt);

        //如果编码器数据不足时会返回  EAGAIN,或者到数据尾时会返回 AVERROR_EOF
        if( ret == AVERROR(EAGAIN) || ret == AVERROR_EOF){
            return;
        }else if (ret <0){
            printf("Error, Failed to encode!\n");
            exit(1);
        }

        fwrite(newpkt->data, 1, newpkt->size, outfile);
        av_packet_unref(newpkt);
    }
}

static  void DirectoryOperations(){

    AVIODirContext *ctx=NULL;
    AVIODirEntry *entry=NULL;
    int ret = avio_open_dir(&ctx, "./", NULL);
    while (1){
        ret=avio_read_dir(ctx, &entry);
        if(!entry){
            break;
        }
    }

}



#endif
