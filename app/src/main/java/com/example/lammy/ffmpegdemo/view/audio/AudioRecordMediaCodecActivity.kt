package com.example.lammy.ffmpegdemo.view.audio

import android.media.*
import android.os.Bundle
import android.view.View
import androidx.databinding.ViewDataBinding
import com.aleyn.mvvm.base.BaseActivity
import com.aleyn.mvvm.base.NoViewModel
import com.example.ffmpeg_lib.utils.ADTSUtils
import com.example.ffmpeg_lib.utils.FileUtil
import com.example.ffmpeg_lib.utils.LogUtils
import com.example.lammy.ffmpegdemo.R
import kotlinx.android.synthetic.main.activity_audio_record.*
import java.io.*
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.ArrayBlockingQueue

/**
 * Desc :录制AAC编码(MedioCodec)
 * Modified :
 */
class AudioRecordMediaCodecActivity : BaseActivity<NoViewModel, ViewDataBinding>() {
    private var mAudioRecord: AudioRecord? = null
    private var mAudioSampleRate = 0
    private var mAudioChanelCount = 0
    private lateinit var mAudioBuffer: ByteArray
    private var mAudioEncoder: MediaCodec? = null
    private var presentationTimeUs: Long = 0
    private var mRecordThread: Thread? = null
    private var mEncodeThread: Thread? = null
    private lateinit var encodeInputBuffers: Array<ByteBuffer>
    private lateinit var encodeOutputBuffers: Array<ByteBuffer>
    private var mAudioEncodeBufferInfo: MediaCodec.BufferInfo? = null
    private var mSampleRateType = 0
    private var mAudioBos: BufferedOutputStream? = null
    private var queue: ArrayBlockingQueue<ByteArray>? = null
    private var isRecord = false
    private val MAX_BUFFER_SIZE = 8192

    override fun layoutId(): Int {
        return R.layout.activity_audio_record
    }

    override fun initView(savedInstanceState: Bundle?) {
    }

    override fun initData() {
        btn_record_start.setOnClickListener(View.OnClickListener {
            btnStart(it)
        })

        btn_record_end.setOnClickListener(View.OnClickListener {
            btnStop(it)
        })
    }

    fun btnStart(view: View?) {
        initAudioDevice()
        mAudioEncoder = try {
            initAudioEncoder()
        } catch (e: IOException) {
            e.printStackTrace()
            throw RuntimeException("audio encoder init fail")
        }
        //开启录音
        mRecordThread = Thread(fetchAudioRunnable())
        try {
            mAudioBos = BufferedOutputStream(FileOutputStream(File(FileUtil.getMainDir(), "record.aac")), 200 * 1024)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }
        presentationTimeUs = Date().time * 1000
        mAudioRecord!!.startRecording()
        queue = ArrayBlockingQueue(10)
        isRecord = true
        if (mAudioEncoder != null) {
            mAudioEncoder!!.start()
            encodeInputBuffers = mAudioEncoder!!.inputBuffers
            encodeOutputBuffers = mAudioEncoder!!.outputBuffers
            mAudioEncodeBufferInfo = MediaCodec.BufferInfo()
            mEncodeThread = Thread(EncodeRunnable())
            mEncodeThread!!.start()
        }
        mRecordThread!!.start()
    }

    fun btnStop(view: View?) {
        isRecord = false
    }

    private fun fetchAudioRunnable(): Runnable {
        return Runnable { fetchPcmFromDevice() }
    }

    /**
     * 初始化AudioRecord
     */
    private fun initAudioDevice() {
        val sampleRates = intArrayOf(44100, 22050, 16000, 11025)
        for (sampleRate in sampleRates) {
            //编码制式
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT
            // stereo 立体声，
            val channelConfig = AudioFormat.CHANNEL_CONFIGURATION_STEREO
            val buffsize = 2 * AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
            mAudioRecord = AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, channelConfig,
                    audioFormat, buffsize)
            if (mAudioRecord!!.state == AudioRecord.STATE_INITIALIZED && buffsize <= MAX_BUFFER_SIZE) {
                mAudioSampleRate = sampleRate
                mAudioChanelCount = if (channelConfig == AudioFormat.CHANNEL_CONFIGURATION_STEREO) 2 else 1
                mAudioBuffer = ByteArray(buffsize)
                mSampleRateType = ADTSUtils.getSampleRateType(sampleRate)
                LogUtils.w("编码器参数:$mAudioSampleRate $mSampleRateType $mAudioChanelCount $buffsize")
                break
            }
        }
    }

    /**
     * 初始化编码器
     * @return
     * @throws IOException
     */
    @Throws(IOException::class)
    private fun initAudioEncoder(): MediaCodec {
        val encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC,
                mAudioSampleRate, mAudioChanelCount)
        format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, MAX_BUFFER_SIZE)
        format.setInteger(MediaFormat.KEY_BIT_RATE, 1000 * 30)
        encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        return encoder
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
                putPCMData(audio)
            }
        }
    }

    /**
     * 将PCM数据存入队列
     *
     * @param pcmChunk PCM数据块
     */
    private fun putPCMData(pcmChunk: ByteArray) {
        try {
            queue!!.put(pcmChunk)
        } catch (e: InterruptedException) {
            e.printStackTrace()
            LogUtils.e("queue put error")
        }
    }

    /**
     * 在Container中队列取出PCM数据
     *
     * @return PCM数据块
     */
    private val pCMData: ByteArray?
        private get() {
            try {
                return if (queue!!.isEmpty()) {
                    null
                } else queue!!.take()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
            return null
        }

    private inner class EncodeRunnable : Runnable {
        override fun run() {
            LogUtils.w("编码线程开始")
            while (isRecord || !queue!!.isEmpty()) encodePCM()
            release()
        }
    }

    /**
     * 编码PCM数据 得到MediaFormat.MIMETYPE_AUDIO_AAC格式的音频文件，并保存到
     */
    private fun encodePCM() {
        val inputIndex: Int
        val inputBuffer: ByteBuffer
        var outputIndex: Int
        var outputBuffer: ByteBuffer
        var chunkAudio: ByteArray
        var outBitSize: Int
        var outPacketSize: Int
        val chunkPCM: ByteArray?
        chunkPCM = pCMData //获取解码器所在线程输出的数据 代码后边会贴上
        if (chunkPCM == null) {
            return
        }
        inputIndex = mAudioEncoder!!.dequeueInputBuffer(-1) //同解码器
        if (inputIndex >= 0) {
            inputBuffer = encodeInputBuffers[inputIndex] //同解码器
            inputBuffer.clear() //同解码器
            inputBuffer.limit(chunkPCM.size)
            inputBuffer.put(chunkPCM) //PCM数据填充给inputBuffer
            val pts = Date().time * 1000 - presentationTimeUs
            LogUtils.d("开始编码: ")
            mAudioEncoder!!.queueInputBuffer(inputIndex, 0, chunkPCM.size, pts, 0) //通知编码器 编码
        }
        outputIndex = mAudioEncoder!!.dequeueOutputBuffer(mAudioEncodeBufferInfo, 10000) //同解码器
        while (outputIndex >= 0) { //同解码器
            outBitSize = mAudioEncodeBufferInfo!!.size
            outPacketSize = outBitSize + 7 //7为ADTS头部的大小
            outputBuffer = encodeOutputBuffers[outputIndex] //拿到输出Buffer
            outputBuffer.position(mAudioEncodeBufferInfo!!.offset)
            outputBuffer.limit(mAudioEncodeBufferInfo!!.offset + outBitSize)
            chunkAudio = ByteArray(outPacketSize)
            ADTSUtils.addADTStoPacket(mSampleRateType, chunkAudio, outPacketSize) //添加ADTS 代码后面会贴上
            outputBuffer[chunkAudio, 7, outBitSize] //将编码得到的AAC数据 取出到byte[]中 偏移量offset=7 你懂得
            outputBuffer.position(mAudioEncodeBufferInfo!!.offset)
            try {
                LogUtils.d("接受编码后数据 " + chunkAudio.size)
                mAudioBos!!.write(chunkAudio, 0, chunkAudio.size) //BufferOutputStream 将文件保存到内存卡中 *.aac
            } catch (e: IOException) {
                e.printStackTrace()
            }
            mAudioEncoder!!.releaseOutputBuffer(outputIndex, false)
            outputIndex = mAudioEncoder!!.dequeueOutputBuffer(mAudioEncodeBufferInfo, 10000)
        }
    }

    /**
     * 释放资源
     */
    fun release() {
        try {
            if (mAudioBos != null) {
                mAudioBos!!.flush()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            if (mAudioBos != null) {
                try {
                    mAudioBos!!.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                } finally {
                    mAudioBos = null
                }
            }
        }
        if (mAudioRecord != null) {
            mAudioRecord!!.stop()
            mAudioRecord!!.release()
        }
        if (mAudioEncoder != null) {
            mAudioEncoder!!.stop()
            mAudioEncoder!!.release()
            mAudioEncoder = null
        }
        LogUtils.w("release")
    }

}