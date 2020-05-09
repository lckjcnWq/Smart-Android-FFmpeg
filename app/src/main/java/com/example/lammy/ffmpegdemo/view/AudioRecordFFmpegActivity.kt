package com.example.lammy.ffmpegdemo.view

import android.app.Activity
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.view.View
import android.widget.TextView
import com.example.ffmpeg_lib.audio.AudioBuffer
import com.example.ffmpeg_lib.utils.ADTSUtils
import com.example.ffmpeg_lib.utils.FileUtil
import com.example.ffmpeg_lib.utils.IOUtils
import com.example.ffmpeg_lib.utils.LogUtils
import com.example.lammy.ffmpegdemo.R
import com.example.lammy.ffmpegdemo.audio.FFmpegAudioHandle
import java.io.*

/**
 * Desc :  如果需要把采集的原始数据输出到pcm文件，把out相关的注释打开即可
 * Modified :
 */
class AudioRecordFFmpegActivity : Activity() {
    private val tvInfo: TextView? = null
    private var mAudioRecord: AudioRecord? = null
    private var mAudioSampleRate = 0
    private var mAudioChanelCount = 0
    private lateinit var mAudioBuffer: ByteArray
    private var mRecordThread: Thread? = null
    private var mEncodeThread: Thread? = null
    private var mSampleRateType = 0
    private var isRecord = false
    private val MAX_BUFFER_SIZE = 10240
    private var ret = 0

    //采集数据并输入到pcm文件。
    private val out: OutputStream? = null
    private var audioBuffer: AudioBuffer? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_audio_record_ffmpeg)
    }

    fun btnStart(view: View?) {
        ret = FFmpegAudioHandle.initAudio(FileUtil.getMainDir().toString() + "/record_ffmpeg.aac")
        if (ret < 0) {
            LogUtils.e("initAudio error $ret")
            return
        }
        LogUtils.d("ReadSize $ret")
        audioBuffer = AudioBuffer(ret)
        if (!initAudioDevice()) {
            LogUtils.e("initAudioDevice failed")
            return
        }
        isRecord = true
        //开启录音
        mRecordThread = Thread(fetchAudioRunnable())
        mEncodeThread = Thread(EncodeRunnable())
        mAudioRecord!!.startRecording()
        mEncodeThread!!.start()
        mRecordThread!!.start()
    }

    fun btnStop(view: View?) {
        isRecord = false
    }

    /**
     * 编码tdjm.pcm文件为tdjm.aac
     *
     * @param view
     */
    fun btnEncodePcmFile(view: View?) {
        val pcmFileName = "tdjm.pcm"
        Thread(Runnable { FFmpegAudioHandle.encodePcmFile(FileUtil.getMainDir().toString() + "/" + pcmFileName, FileUtil.getMainDir().toString() + "/tdjm.aac") }).start()
    }

    fun btnTest(view: View?) {
        val pcmFileName = "tdjm.pcm"
        val filePcm = File(FileUtil.getMainDir(), pcmFileName)
        //        final File filePcm = new File(FileUtil.getMainDir(), "AudioRecordFFmpegActivity.pcm");
        if (!filePcm.exists()) {
            LogUtils.d("$filePcm is not exist")
            return
        }
        Thread(Runnable {
            var `in`: InputStream? = null
            val readSize: Int = FFmpegAudioHandle.initAudio(FileUtil.getMainDir().toString() + "/tdjm.aac")
            if (readSize <= 0) {
                LogUtils.e("init audio error ")
                return@Runnable
            }
            LogUtils.d("readSize$readSize")
            audioBuffer = AudioBuffer(readSize)
            try {
                val buff = ByteArray(readSize)
                `in` = FileInputStream(filePcm)
                while (`in`.read(buff) >= readSize) {
                    FFmpegAudioHandle.encodeAudio(buff)
                }
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            } finally {
                IOUtils.close(`in`)
                FFmpegAudioHandle.close()
            }
        }).start()
    }

    private fun fetchAudioRunnable(): Runnable {
        return Runnable { fetchPcmFromDevice() }
    }

    /**
     * 初始化AudioRecord  这里测试就直接用44100采样率 双声道  16bit。于FFmpeg编码器重保持一致
     * 当然大家可以根据自己的情况来修改，这里只是抛砖引玉。
     */
    private fun initAudioDevice(): Boolean {
        val sampleRate = 44100
        //编码制式
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        // stereo 立体声，
        val channelConfig = AudioFormat.CHANNEL_CONFIGURATION_STEREO
        val buffsize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        mAudioRecord = AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, channelConfig,
                audioFormat, buffsize)
        if (mAudioRecord!!.state == AudioRecord.STATE_INITIALIZED && buffsize <= MAX_BUFFER_SIZE) {
            mAudioSampleRate = sampleRate
            mAudioChanelCount = if (channelConfig == AudioFormat.CHANNEL_CONFIGURATION_STEREO) 2 else 1
            mAudioBuffer = ByteArray(buffsize)
            mSampleRateType = ADTSUtils.getSampleRateType(sampleRate)
            LogUtils.w("编码器参数:$mAudioSampleRate $mSampleRateType $mAudioChanelCount $buffsize")
            return true
        }
        return false
    }

    /**
     * 采集音频数据
     */
    private fun fetchPcmFromDevice() {
        LogUtils.w("录音线程开始")
        while (isRecord && mAudioRecord != null && !Thread.interrupted()) {
            val size = mAudioRecord!!.read(mAudioBuffer, 0, mAudioBuffer.size)
            if (size < 0) {
                LogUtils.w("audio ignore ,no data to read")
                break
            }
            if (isRecord) {
                val audio = ByteArray(size)
                System.arraycopy(mAudioBuffer, 0, audio, 0, size)
                LogUtils.v("采集到数据:" + audio.size)
                //                IOUtils.write(out, audio, 0, audio.length);
                audioBuffer?.put(audio, 0, audio.size)
            }
        }
    }

    private inner class EncodeRunnable : Runnable {
        override fun run() {
            LogUtils.w("编码线程开始")
            while (isRecord || audioBuffer?.isEmpty!!) encodePCM()
            release()
        }
    }

    private fun encodePCM() {
        val chunkPCM: ByteArray = audioBuffer?.getFrameBuf() ?: return //获取解码器所在线程输出的数据 代码后边会贴上
        FFmpegAudioHandle.encodeAudio(chunkPCM)
    }

    /**
     * 释放资源
     */
    fun release() {
        if (mAudioRecord != null) {
            mAudioRecord!!.stop()
            mAudioRecord!!.release()
        }
        LogUtils.w("release")
        //        IOUtils.close(out);
    }
}