#include "audio_lib.h"

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
    AVFormatContext *pFormatContext = NULL;
    AVCodecParameters *pCodecParameters = NULL;
    AVCodecContext *pCodecContext = NULL;
    SwrContext *swrContext = NULL;
    AVPacket *pPacket = NULL;
    AVFrame *pFrame = NULL;
    uint8_t *resampleOutBuffer;
    jbyte *jPcmData;
    int audioStramIndex = 0;
    int index = 0;
    int outChannels;
    int dataSize;

    const char *url = env->GetStringUTFChars(url_, 0);
    //0.链接设备并且初始化上下文
    pFormatContext=initFormatCnontext(url);

    //1.查找音频流的 index，后面根据这个index处理音频
    audioStramIndex = av_find_best_stream(pFormatContext, AVMediaType::AVMEDIA_TYPE_AUDIO, -1, -1,NULL, 0);
    if (audioStramIndex < 0) {
        SLOG("format audio stream error:");
        goto _av_resource_destry;
    }

    //2、查找并且打开解码器
    pCodecParameters = pFormatContext->streams[audioStramIndex]->codecpar;
    pCodecContext = openDecoder(pFormatContext,pCodecParameters);


    //3.初始化數據包Packet
    pPacket=av_packet_alloc();
    pFrame=av_frame_alloc();

    //创建java的AudioTrack
    audioTrack=initAudioTrack(env);

    // 4.重采样
    SLOG("重採樣");
    swrContext = initSample(pFormatContext,pCodecContext);

    outChannels = av_get_channel_layout_nb_channels(AV_CH_LAYOUT_STEREO);
    dataSize = av_samples_get_buffer_size(NULL, outChannels, pCodecParameters->frame_size,AVSampleFormat::AV_SAMPLE_FMT_S16, 0);
    resampleOutBuffer = (uint8_t *) malloc(dataSize);

    //一帧一帧播放，wile循环
    while (av_read_frame(pFormatContext,pPacket) >=0){
        // Packet 包，压缩的数据，解码成 pcm 数据
        //判断是音频帧
        if (pPacket->stream_index != audioStramIndex) {
            continue;
        }

        //输入原数据到解码器
        int codecSendPacketRes = avcodec_send_packet(pCodecContext,pPacket);
        if (codecSendPacketRes == 0){
            //解码器输出解码后的数据 pFrame
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
    av_frame_free(&pFrame);
    av_packet_free(&pPacket);

    _av_resource_destry:
    if (pFormatContext != NULL){
        avformat_close_input(&pFormatContext);
        avformat_free_context(pFormatContext);
        pFormatContext = NULL;
    }

    env->ReleaseStringUTFChars(url_, url);
}

