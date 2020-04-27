
#include <jni.h>
#include <string>
#include <android/log.h>
#include "common.h"

using namespace std;
//暂时用全局变量，后面再抽取优化
jmethodID jAudioTrackWriteMid;
jobject audioTrack;

jobject  initAudioTrack(JNIEnv *env){
    jclass jAudioTrackClass = env->FindClass("android/media/AudioTrack");
    jmethodID jAudioTrackCMid = env->GetMethodID(jAudioTrackClass,"<init>","(IIIIII)V"); //构造

    //  public static final int STREAM_MUSIC = 3;
    int streamType = 3;
    int sampleRateInHz = 44100;
    // public static final int CHANNEL_OUT_STEREO = (CHANNEL_OUT_FRONT_LEFT | CHANNEL_OUT_FRONT_RIGHT);
    int channelConfig = (0x4 | 0x8);
    // public static final int ENCODING_PCM_16BIT = 2;
    int audioFormat = 2;
    // getMinBufferSize(int sampleRateInHz, int channelConfig, int audioFormat)
    jmethodID jGetMinBufferSizeMid = env->GetStaticMethodID(jAudioTrackClass, "getMinBufferSize", "(III)I");
    int bufferSizeInBytes = env->CallStaticIntMethod(jAudioTrackClass, jGetMinBufferSizeMid, sampleRateInHz, channelConfig, audioFormat);
    // public static final int MODE_STREAM = 1;
    int mode = 1;

    //创建了AudioTrack
    jobject jAudioTrack = env->NewObject(jAudioTrackClass,jAudioTrackCMid, streamType, sampleRateInHz, channelConfig, audioFormat, bufferSizeInBytes, mode);

    //play方法
    jmethodID jPlayMid = env->GetMethodID(jAudioTrackClass,"play","()V");
    env->CallVoidMethod(jAudioTrack,jPlayMid);

    // write method
    jAudioTrackWriteMid = env->GetMethodID(jAudioTrackClass, "write", "([BII)I");

    return jAudioTrack;
}


extern "C" JNIEXPORT jint JNICALL
Java_com_example_lammy_ffmpegdemo_util_FFmpegUtil_openAudioDevice(JNIEnv *env, jclass clazz) {
    AVFormatContext *fmt_ctx=NULL;
    const char *deviceName=":0";
    AVPacket pkt;
    int count=0;
    //open device
    avdevice_register_all();
    AVInputFormat *iformat = av_find_input_format("dshow");
    int ret = avformat_open_input(&fmt_ctx, deviceName, iformat, NULL);
    if(ret<0){
        av_log(NULL,AV_LOG_INFO,"i am error");
        return -1;
    }
    av_log(NULL,AV_LOG_INFO,"i am success");

    //create file
    FILE *outfile = fopen("G://audio.pcm", "wb+");
    //read data from device
    av_init_packet(&pkt);
    int readCode = av_read_frame(fmt_ctx, &pkt);
    while (readCode==0 && count++<500){
        //write file
        fwrite(pkt.data,pkt.size,1,outfile);
        fflush(outfile);
        printf("pkt size is %d \n",pkt.size);
        av_packet_unref(&pkt);
    }
    //关闭设备:防止内存泄漏
    av_packet_unref(&pkt);
    avformat_close_input(&fmt_ctx);
    fclose(outfile);
    return ret;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_example_lammy_ffmpegdemo_util_FFmpegUtil_recordAudioDevice(JNIEnv *env, jclass clazz) {

}

extern "C"
JNIEXPORT jint JNICALL
Java_com_example_lammy_ffmpegdemo_util_FFmpegUtil_closeAudioDevice(JNIEnv *env, jclass clazz) {
}
extern "C"
JNIEXPORT void JNICALL
Java_com_example_lammy_ffmpegdemo_util_FFmpegUtil_getAudioMp3Info(JNIEnv *env, jclass clazz,jstring url_) {
    AVFormatContext *avFormatContext = NULL;
    int audio_stream_idx;
    AVStream *audio_stream;
    const char *url = env->GetStringUTFChars(url_, 0);

    //初始化组件
    av_register_all();
    int open_res = avformat_open_input(&avFormatContext, url, NULL, NULL);
    if(open_res!=0){
        SLOG("lxb->Can't open file: %s", av_err2str(open_res));
        return ;
    }

    //获取文件信息
    int find_steam_info_res = avformat_find_stream_info(avFormatContext, NULL);
    if(find_steam_info_res<0){
        SLOG("lxb->Find stream info error: %s", av_err2str(find_steam_info_res));
        goto __avformat_close;
    }

    //获取采样率和通道
    audio_stream_idx =av_find_best_stream(avFormatContext,AVMediaType::AVMEDIA_TYPE_AUDIO, -1, -1, NULL, 0);
    if (audio_stream_idx < 0) {
        printf("lxb->Find audio stream info error: %s",av_err2str(find_steam_info_res));
        goto __avformat_close;
    }
    audio_stream = avFormatContext->streams[audio_stream_idx];
    SLOG("采样率：%d \n",audio_stream->codecpar->sample_rate);
    SLOG("通道数: %d \n",audio_stream->codecpar->channels);
    SLOG("format：%d \n",audio_stream->codecpar->format);
    SLOG("extradata_size：%d \n",audio_stream->codecpar->extradata_size);

    __avformat_close:
    avformat_close_input(&avFormatContext);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_lammy_ffmpegdemo_util_FFmpegUtil_nativeAudioPlay(JNIEnv *env, jclass clazz,
                                                                  jstring url_) {
    const char *url = env->GetStringUTFChars(url_, 0);
    AVFormatContext *pFormatContext = NULL;
    AVCodecParameters *pCodecParameters = NULL;
    AVCodec *pCodec = NULL;
    int formatFindStreamInfoRes = 0;
    int audioStramIndex = 0;
    AVCodecContext *pCodecContext = NULL;
    int codecParametersToContextRes = -1;
    int codecOpenRes = -1;
    SwrContext *swrContext = NULL;
    AVPacket *pPacket = NULL;
    AVFrame *pFrame = NULL;
    int index = 0;

    int outChannels;
    int dataSize;

    uint8_t *resampleOutBuffer;
    jbyte *jPcmData;
    int64_t out_ch_layout;
    int out_sample_rate;
    enum AVSampleFormat out_sample_fmt;
    int64_t in_ch_layout;
    enum AVSampleFormat in_sample_fmt;
    int in_sample_rate;
    int swrInitRes;

    //1、初始化所有组件，只有调用了该函数，才能使用复用器和编解码器（源码）
    av_register_all();
    //2、打开文件
    int open_input_result = avformat_open_input(&pFormatContext,url,NULL,NULL);
    if (open_input_result != 0){
        SLOG("format open input error: %s", av_err2str(open_input_result));
        goto _av_resource_destry;
    }

    //3.填充流信息到 pFormatContext
    formatFindStreamInfoRes = avformat_find_stream_info(pFormatContext, NULL);
    if (formatFindStreamInfoRes < 0) {
        SLOG("format find stream info error: %s", av_err2str(formatFindStreamInfoRes));
        goto _av_resource_destry;
    }

    //4.、查找音频流的 index，后面根据这个index处理音频
    audioStramIndex = av_find_best_stream(pFormatContext, AVMediaType::AVMEDIA_TYPE_AUDIO, -1, -1,NULL, 0);
    if (audioStramIndex < 0) {
        SLOG("format audio stream error:");
        goto _av_resource_destry;
    }

    //4、查找解码器
    //audioStramIndex 上一步已经获取了，通过音频流的index，可以从pFormatContext中拿到音频解码器的一些参数
    pCodecParameters = pFormatContext->streams[audioStramIndex]->codecpar;
    pCodec = avcodec_find_decoder(pCodecParameters->codec_id);

    SLOG("采样率：%d", pCodecParameters->sample_rate);
    SLOG("通道数: %d", pCodecParameters->channels);
    SLOG("format: %d", pCodecParameters->format);
    if (pCodec == NULL) {
        SLOG("codec find audio decoder error");
        goto _av_resource_destry;
    }

    //5、打开解码器
    SLOG("pCodecContext00");
    pCodecContext=avcodec_alloc_context3(pCodec);
    if (pCodecContext == NULL){
        SLOG("avcodec_alloc_context3 error");
        goto _av_resource_destry;
    }

    //pCodecParameters 转 context
    SLOG("pCodecContext11");
    codecParametersToContextRes = avcodec_parameters_to_context(pCodecContext,pCodecParameters);
    if(codecParametersToContextRes <0){
        SLOG("avcodec_parameters_to_context error");
        goto _av_resource_destry;
    }

    SLOG("pCodecContext22");
    codecOpenRes = avcodec_open2(pCodecContext,pCodec,NULL);
    if (codecOpenRes != 0) {
        SLOG("codec audio open error: %s", av_err2str(codecOpenRes));
        goto _av_resource_destry;
    }


    //到此，pCodecContext 已经初始化完毕，下面可以用来获取每一帧数据
    SLOG("pCodecContext33");
    pPacket=av_packet_alloc();
    pFrame=av_frame_alloc();

    //创建java的AudioTrack
    SLOG("pCodecContext44");
    audioTrack=initAudioTrack(env);

    // ---------- 重采样 构造 swrContext 参数 start----------
    SLOG("pCodecContext55");
    out_ch_layout = AV_CH_LAYOUT_STEREO;
    out_sample_fmt = AVSampleFormat::AV_SAMPLE_FMT_S16;
    out_sample_rate = AUDIO_SAMPLE_RATE;
    in_ch_layout = pCodecContext->channel_layout;
    in_sample_fmt = pCodecContext->sample_fmt;
    in_sample_rate = pCodecContext->sample_rate;
    swrContext = swr_alloc_set_opts(NULL, out_ch_layout, out_sample_fmt,
                                    out_sample_rate, in_ch_layout, in_sample_fmt, in_sample_rate, 0, NULL);
    if (swrContext == NULL) {
        // 提示错误
        SLOG("swr_alloc_set_opts error");
        goto _av_resource_destry;
    }

    SLOG("pCodecContext66");
    swrInitRes = swr_init(swrContext);
    if (swrInitRes < 0) {
        SLOG("swr_init error");
        goto _av_resource_destry;
    }
    // ---------- 重采样 构造 swrContext 参数 end----------

    // size 是播放指定的大小，是最终输出的大小
    SLOG("pCodecContext77");
    outChannels = av_get_channel_layout_nb_channels(out_ch_layout); //通道数
    dataSize = av_samples_get_buffer_size(NULL, outChannels, pCodecParameters->frame_size,out_sample_fmt, 0);
    resampleOutBuffer = (uint8_t *) malloc(dataSize);

    //一帧一帧播放，wile循环
    while (av_read_frame(pFormatContext,pPacket) >=0){
        // Packet 包，压缩的数据，解码成 pcm 数据
        //判断是音频帧
        SLOG("av_read_frame00");
        if (pPacket->stream_index != audioStramIndex) {
            continue;
        }

        SLOG("av_read_frame11");
        //输入原数据到解码器
        int codecSendPacketRes = avcodec_send_packet(pCodecContext,pPacket);
        if (codecSendPacketRes == 0){
            //解码器输出解码后的数据 pFrame
            SLOG("av_read_frame22");
            int codecReceiveFrameRes = avcodec_receive_frame(pCodecContext,pFrame);
            if(codecReceiveFrameRes == 0){
                index++;

                SLOG("av_read_frame %d:",index);
                //数据转换成Buffer,需要导入 libswresample/swresample.h
                swr_convert(swrContext, &resampleOutBuffer, pFrame->nb_samples,
                            (const uint8_t **) pFrame->data, pFrame->nb_samples);

                //内存拷贝
                memcpy(jPcmData, resampleOutBuffer, dataSize);

                jbyteArray jPcmDataArray = env->NewByteArray(dataSize);
                // native 创建 c 数组
                jPcmData = env->GetByteArrayElements(jPcmDataArray, NULL);
                // 同步刷新到 jbyteArray ，并释放 C/C++ 数组
                env->ReleaseByteArrayElements(jPcmDataArray, jPcmData, 0);

                //public int write(@NonNull byte[] audioData, int offsetInBytes, int sizeInBytes) {}
                env->CallIntMethod(audioTrack, jAudioTrackWriteMid, jPcmDataArray, 0, dataSize);

                SLOG("解码第 %d 帧dataSize =%d ", index , dataSize);

                // 解除 jPcmDataArray 的持有，让 javaGC 回收
                env->DeleteLocalRef(jPcmDataArray);
            }
        }

        //解引用
        av_packet_unref(pPacket);
        av_frame_unref(pFrame);
    }

    SLOG("av_read_frame44");
    // 解引用数据 data ， 2. 销毁 pPacket 结构体内存  3. pPacket = NULL
    av_frame_free(&pFrame);
    av_packet_free(&pPacket);

    //错误跳转释放资源
    _av_resource_destry:
    if (pFormatContext != NULL){
        avformat_close_input(&pFormatContext);
        avformat_free_context(pFormatContext);
        pFormatContext = NULL;
    }

    env->ReleaseStringUTFChars(url_, url);
}
