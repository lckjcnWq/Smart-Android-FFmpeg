package com.example.lammy.ffmpegdemo.view

import android.app.Activity
import android.graphics.ImageFormat
import android.hardware.Camera
import android.hardware.Camera.PreviewCallback
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.os.Bundle
import android.view.SurfaceHolder
import android.widget.FrameLayout
import com.example.ffmpeg_lib.device.CameraController
import com.example.ffmpeg_lib.flv.FlvPacker
import com.example.ffmpeg_lib.flv.Packer
import com.example.ffmpeg_lib.utils.FileUtil
import com.example.ffmpeg_lib.utils.IOUtils
import com.example.ffmpeg_lib.utils.LogUtils
import com.example.ffmpeg_lib.utils.PhoneUtils
import com.example.ffmpeg_lib.video.VideoComponent
import com.example.lammy.ffmpegdemo.R
import com.example.lammy.ffmpegdemo.widget.UiSurfaceView
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.util.concurrent.Executors

/**
 * Desc :将摄像头采集的数据data--->视频编码MediaCodec(H264硬编码)--->生成flv文件
 * Modified :
 */
class CameraMediaCodecFileActivity : Activity(), SurfaceHolder.Callback {
    var executor = Executors.newSingleThreadExecutor()
    private var sv: UiSurfaceView? = null

    //建议的视频宽度，不超过这个宽度，自动寻找4：3的尺寸
    private val SUGGEST_PREVIEW_WIDTH = 640
    private var videoWidth = 0
    private var videoHeight = 0
    private var mHolder: SurfaceHolder? = null

    //采集到每帧数据时间
    var previewTime: Long = 0

    //每帧开始编码时间
    var encodeTime: Long = 0

    //采集数量
    var count = 0

    //编码数量
    var encodeCount = 0

    //采集数据回调
    private var mPreviewFrameCallback: PreviewFrameCallback? = null
    private var mMediaCodec: MediaCodec? = null
    private var mFlvPacker: FlvPacker? = null
    private val FRAME_RATE = 15
    private var mOutStream: OutputStream? = null

    //视频编码组件封装
    private var mVideoComponent: VideoComponent? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)
        init()
    }

    private fun init() {
        sv = findViewById(R.id.sv)
        mVideoComponent = VideoComponent()
        mPreviewFrameCallback = PreviewFrameCallback()
        initCamera()
        initMediaCodec()
        mFlvPacker = FlvPacker()
        mFlvPacker!!.initVideoParams(videoWidth, videoHeight, FRAME_RATE)
            mFlvPacker!!.setPacketListener(Packer.OnPacketListener { data, packetType ->
                IOUtils.write(mOutStream, data, 0, data.size)
                LogUtils.w(data.size.toString() + " " + packetType)
            })
    }

    private fun initMediaCodec() {
        val bitrate = 2 * videoWidth * videoHeight * FRAME_RATE / 20
        try {
            val mediaCodecInfo: MediaCodecInfo = mVideoComponent!!.getSupportMediaCodecInfo(VCODEC_MIME)
                    ?: throw RuntimeException("mediaCodecInfo is Empty")
            mMediaCodec = MediaCodec.createByCodecName(mediaCodecInfo.name)
            val mediaFormat = MediaFormat.createVideoFormat(VCODEC_MIME, videoWidth, videoHeight)
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
            mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE)
            mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                    mVideoComponent!!.getSupportMediaCodecColorFormat(mediaCodecInfo))
            mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            mMediaCodec!!.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            mMediaCodec!!.start()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun initCamera() {
        CameraController.getInstance().open(0)
        val params: Camera.Parameters = CameraController.getInstance().getParams()
        //查找合适的预览尺寸
        val size: Camera.Size = mVideoComponent!!.getSupportPreviewSize(params, SUGGEST_PREVIEW_WIDTH)
                ?: throw RuntimeException("not found support preview size")
        videoWidth = size.width
        videoHeight = size.height
        params.pictureFormat = ImageFormat.JPEG
        params.previewFormat = mVideoComponent!!.getSupportPreviewColorFormat(params)
        //      params.setPictureSize(videoWidth, videoHeight);
        params.setPreviewSize(videoWidth, videoHeight)
        params.setPreviewFpsRange(15000, 20000)
        val focusModes = params.supportedFocusModes
        if (focusModes.contains("continuous-video")) {
            params.focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO
        }
        CameraController.getInstance().adjustOrientation(this, CameraController.OnOrientationChangeListener {
            val lp = sv!!.getLayoutParams() as FrameLayout.LayoutParams
            LogUtils.d(PhoneUtils.getWidth().toString() + " " + PhoneUtils.getHeight())
            if (it == 90) {
                lp.height = PhoneUtils.getWidth() * videoWidth / videoHeight
            } else {
                lp.height = PhoneUtils.getWidth() * videoHeight / videoWidth
            }
            sv!!.setLayoutParams(lp)
        })
        CameraController.getInstance().resetParams(params)
        mHolder = sv!!.getHolder()
        mHolder!!.addCallback(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        CameraController.getInstance().close()
    }

    override fun onResume() {
        super.onResume()
        if (mHolder != null) {
            CameraController.getInstance().startPreview(mHolder, mPreviewFrameCallback)
        }
    }

    override fun onPause() {
        super.onPause()
        CameraController.getInstance().stopPreview()
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        mFlvPacker!!.start()
        mOutStream = IOUtils.open(FileUtil.getMainDir().toString() + File.separator + "/CameraMediaCodecFileActivity.flv", true)
        CameraController.getInstance().startPreview(mHolder, mPreviewFrameCallback)
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}
    override fun surfaceDestroyed(holder: SurfaceHolder) {
        mFlvPacker!!.stop()
        CameraController.getInstance().stopPreview()
        CameraController.getInstance().close()
        IOUtils.close(mOutStream)
    }

    inner class PreviewFrameCallback : PreviewCallback {
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

    private fun flvPackage(bufSou: ByteArray) {
        //编码格式转换
        val buf: ByteArray = mVideoComponent!!.convert(bufSou)
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