package com.example.lammy.ffmpegdemo.view

import android.app.Activity
import android.graphics.ImageFormat
import android.hardware.Camera
import android.hardware.Camera.PreviewCallback
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import android.os.Bundle
import android.os.Environment
import android.view.SurfaceHolder
import android.view.View
import android.widget.Toast
import com.example.ffmpeg_lib.device.CameraController
import com.example.ffmpeg_lib.flv.FlvPacker
import com.example.ffmpeg_lib.flv.Packer
import com.example.ffmpeg_lib.utils.LogUtils
import com.example.lammy.ffmpegdemo.R
import com.example.lammy.ffmpegdemo.ffmpeg.FFmpegHandle
import com.example.lammy.ffmpegdemo.rtmp.RtmpHandle
import com.example.lammy.ffmpegdemo.widget.UiSurfaceView
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.util.*
import java.util.concurrent.Executors

/**
 * Desc :
 * Modified :
 */
class CameraMediaCodecRtmpActivity : Activity(), SurfaceHolder.Callback {
    private var sv: UiSurfaceView? = null
    private val WIDTH = 480
    private val HEIGHT = 320
    private var mHolder: SurfaceHolder? = null
    private val url = "rtmp://192.168.31.127/live/test"

    //采集到每帧数据时间
    var previewTime: Long = 0

    //每帧开始编码时间
    var encodeTime: Long = 0

    //采集数量
    var count = 0

    //编码数量
    var encodeCount = 0

    //采集数据回调
    private var mStreamIt: StreamIt? = null
    private var mMediaCodec: MediaCodec? = null
    private val DATA_DIR = Environment.getExternalStorageDirectory().toString() + File.separator + "AndroidVideo"
    private var mFlvPacker: FlvPacker? = null
    private val FRAME_RATE = 15
    private val mOutStream: OutputStream? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)
        init()
    }

    var pushExecutor = Executors.newSingleThreadExecutor()
    private fun init() {
        FFmpegHandle.initVideo(url, WIDTH, HEIGHT)
        sv = findViewById(R.id.sv)
        initMediaCodec()
        mFlvPacker = FlvPacker()
        mFlvPacker!!.initVideoParams(WIDTH, HEIGHT, FRAME_RATE)
        mFlvPacker!!.setPacketListener(Packer.OnPacketListener { data, packetType ->
            pushExecutor.execute {
                val ret: Int = RtmpHandle.push(data, data.size)
                LogUtils.w("type：" + packetType + "  length:" + data.size + "  推流结果:" + ret)
            }
        })
        mStreamIt = StreamIt()
        CameraController.getInstance().open(1)
        val params: Camera.Parameters = CameraController.getInstance().getParams()
        params.pictureFormat = ImageFormat.YV12
        params.previewFormat = ImageFormat.YV12
        params.setPictureSize(WIDTH, HEIGHT)
        params.setPreviewSize(WIDTH, HEIGHT)
        params.setPreviewFpsRange(15000, 20000)
        val focusModes = params.supportedFocusModes
        if (focusModes.contains("continuous-video")) {
            params.focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO
        }
        CameraController.getInstance().resetParams(params)
        mHolder = sv!!.getHolder()
        mHolder!!.addCallback(this)
    }

    private fun initMediaCodec() {
        val bitrate = 2 * WIDTH * HEIGHT * FRAME_RATE / 20
        try {
            val mediaCodecInfo = selectCodec(VCODEC_MIME)
            if (mediaCodecInfo == null) {
                Toast.makeText(this, "mMediaCodec null", Toast.LENGTH_LONG).show()
                throw RuntimeException("mediaCodecInfo is Empty")
            }
            LogUtils.w("MediaCodecInfo " + mediaCodecInfo.name)
            mMediaCodec = MediaCodec.createByCodecName(mediaCodecInfo.name)
            val mediaFormat = MediaFormat.createVideoFormat(VCODEC_MIME, WIDTH, HEIGHT)
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
            mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE)
            mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                    MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar)
            mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            mMediaCodec!!.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            mMediaCodec!!.start()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun selectCodec(mimeType: String): MediaCodecInfo? {
        val numCodecs = MediaCodecList.getCodecCount()
        for (i in 0 until numCodecs) {
            val codecInfo = MediaCodecList.getCodecInfoAt(i)
            //是否是编码器
            if (!codecInfo.isEncoder) {
                continue
            }
            val types = codecInfo.supportedTypes
            LogUtils.w(Arrays.toString(types))
            for (type in types) {
                LogUtils.e("equal " + mimeType.equals(type, ignoreCase = true))
                if (mimeType.equals(type, ignoreCase = true)) {
                    LogUtils.e("codecInfo " + codecInfo.name)
                    return codecInfo
                }
            }
        }
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        FFmpegHandle.close()
        CameraController.getInstance().close()
    }

    override fun onResume() {
        super.onResume()
        if (mHolder != null) {
            CameraController.getInstance().startPreview(mHolder, mStreamIt)
        }
    }

    override fun onPause() {
        super.onPause()
        CameraController.getInstance().stopPreview()
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        mFlvPacker!!.start()
        //        mOutStream = IOUtils.open(DATA_DIR + File.separator + "/easy.flv", true);
        CameraController.getInstance().startPreview(mHolder, mStreamIt)
        pushExecutor.execute {
            val ret: Int = RtmpHandle.connect("rtmp://192.168.31.127/live")
            LogUtils.w("打开RTMP连接: $ret")
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}
    override fun surfaceDestroyed(holder: SurfaceHolder) {
        mFlvPacker!!.stop()
        CameraController.getInstance().stopPreview()
        CameraController.getInstance().close()
        val ret: Int = RtmpHandle.close()
        LogUtils.w("关闭RTMP连接：$ret")
        //        IOUtils.close(mOutStream);
    }

    var executor = Executors.newSingleThreadExecutor()
    fun btnStart(view: View?) {}
    inner class StreamIt : PreviewCallback {
        override fun onPreviewFrame(data: ByteArray, camera: Camera) {
            val endTime = System.currentTimeMillis()
            executor.execute {
                encodeTime = System.currentTimeMillis()
                flvPackage(data)
                LogUtils.w("编码第:" + encodeCount++ + "帧，耗时:" + (System.currentTimeMillis() - encodeTime))
            }
            LogUtils.d("采集第:" + ++count + "帧，距上一帧间隔时间:"
                    + (endTime - previewTime) + "  " + Thread.currentThread().name)
            previewTime = endTime
        }
    }

    private fun flvPackage(buf: ByteArray) {
        val LENGTH = HEIGHT * WIDTH
        //YV12数据转化成COLOR_FormatYUV420Planar
        LogUtils.d(LENGTH.toString() + "  " + (buf.size - LENGTH))
        for (i in LENGTH until LENGTH + LENGTH / 4) {
            val temp = buf[i]
            buf[i] = buf[i + LENGTH / 4]
            buf[i + LENGTH / 4] = temp
        }
        val inputBuffers = mMediaCodec!!.inputBuffers
        val outputBuffers = mMediaCodec!!.outputBuffers
        try {
            //查找可用的的input buffer用来填充有效数据
            val bufferIndex = mMediaCodec!!.dequeueInputBuffer(-1)
            if (bufferIndex >= 0) {
                //数据放入到inputBuffer中
                val inputBuffer = inputBuffers[bufferIndex]
                inputBuffer.clear()
                inputBuffer.put(buf, 0, buf.size)
                //把数据传给编码器并进行编码
                mMediaCodec!!.queueInputBuffer(bufferIndex, 0,
                        inputBuffers[bufferIndex].position(),
                        System.nanoTime() / 1000, 0)
                val bufferInfo = MediaCodec.BufferInfo()

                //输出buffer出队，返回成功的buffer索引。
                var outputBufferIndex = mMediaCodec!!.dequeueOutputBuffer(bufferInfo, 0)
                while (outputBufferIndex >= 0) {
                    val outputBuffer = outputBuffers[outputBufferIndex]
                    //进行flv封装
                    mFlvPacker!!.onVideoData(outputBuffer, bufferInfo)
                    mMediaCodec!!.releaseOutputBuffer(outputBufferIndex, false)
                    outputBufferIndex = mMediaCodec!!.dequeueOutputBuffer(bufferInfo, 0)
                }
            } else {
                LogUtils.w("No buffer available !")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        private const val VCODEC_MIME = "video/avc"
    }
}