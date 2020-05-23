package com.example.lammy.ffmpegdemo.view.video

import android.app.Activity
import android.graphics.ImageFormat
import android.hardware.Camera
import android.media.MediaCodec
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.view.SurfaceHolder
import android.widget.FrameLayout
import com.example.ffmpeg_lib.device.AudioRecordController
import com.example.ffmpeg_lib.device.CameraController
import com.example.ffmpeg_lib.flv.FlvPacker
import com.example.ffmpeg_lib.flv.Packer
import com.example.ffmpeg_lib.utils.FileUtil
import com.example.ffmpeg_lib.utils.IOUtils
import com.example.ffmpeg_lib.utils.LogUtils
import com.example.ffmpeg_lib.utils.PhoneUtils
import com.example.ffmpeg_lib.video.*
import com.example.lammy.ffmpegdemo.R
import com.example.lammy.ffmpegdemo.widget.UiSurfaceView
import java.io.File
import java.io.OutputStream
import java.nio.ByteBuffer
import java.util.concurrent.Executors

/**
 * Desc :  音视频合成。采用Android硬编码方式，编码后合成到FLV文件
 * Modified :
 */
class VideoCompoundFileActivity : Activity(), SurfaceHolder.Callback, EncodedDataCallback, SourceDataCallback {
    var executor = Executors.newSingleThreadExecutor()
    private var sv: UiSurfaceView? = null

    //建议的视频宽度，不超过这个宽度，自动寻找4：3的尺寸
    private val SUGGEST_PREVIEW_WIDTH = 640
    private var mHolder: SurfaceHolder? = null

    //每帧开始编码时间
    var mVideoencodeTime: Long = 0
    var mAudioencodeTime: Long = 0
    private var mFlvPacker: FlvPacker? = null
    private var mOutStream: OutputStream? = null

    //视频编码组件封装
    private var mVideoComponent: VideoComponent? = null
    private var mAudioComponent: AudioComponent? = null
    private var mPackageThread: HandlerThread? = null
    private var mPackageHandler: Handler? = null
    private val TAG = "VideoCompoundFileActivity"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_compound_file)
        LogUtils.d(TAG, "onCreate")
        init()
    }

    private fun init() {
        sv = findViewById(R.id.sv)
        mPackageThread = HandlerThread("Package-Thread")
        mPackageThread!!.start()
        mPackageHandler = Handler(mPackageThread!!.looper)
        mVideoComponent = VideoComponent()
        mAudioComponent = AudioComponent(AudioRecordController.MAX_BUFFER_SIZE)
        //初始化摄像头
        val cameraConfig: VideoConfig? = initCamera()
        //初始化录音设备
        val audioConfig: AudioConfig = initAudioRecord()
        if (cameraConfig == null) {
            LogUtils.e(TAG, "VideoConfig is null")
            return
        }
        if (audioConfig == null) {
            LogUtils.e(TAG, "AudioConfig is null")
            return
        }
        //配置视频组件
        mVideoComponent!!.config(cameraConfig)
        //配置音频组件
        mAudioComponent!!.config(audioConfig)
        //设置编码回调
        mVideoComponent!!.setEncodedDataCallback(this)
        mAudioComponent!!.setEncodedDataCallback(this)
        //初始化并设置flv打包参数
        mFlvPacker = FlvPacker()
        //设置FLV打包器参数
        mFlvPacker!!.initVideoParams(mVideoComponent!!.getWidth(), mVideoComponent!!.getHeight(), mVideoComponent!!.getFrameRate())
        mFlvPacker!!.initAudioParams(mAudioComponent!!.getAudioSampleRate(),
                mAudioComponent!!.getAudioChanelCount() * 8, mAudioComponent!!.getAudioChanelCount() === 2)
        //FLV封装数据回调
        mFlvPacker!!.setPacketListener(Packer.OnPacketListener { data, packetType ->
            IOUtils.write(mOutStream, data, 0, data.size)
            LogUtils.w("flv输出 type:" + packetType + ",length:" + data.size)
        })
    }

    //===================编码后的数据回调=======================
    override fun onAudioEncodedCallback(byteBuffer: ByteBuffer?, bufferInfo: MediaCodec.BufferInfo?) {
        mFlvPacker!!.onAudioData(byteBuffer, bufferInfo)
    }

    override fun onVideoEncodedCallback(byteBuffer: ByteBuffer?, bufferInfo: MediaCodec.BufferInfo?) {
        mFlvPacker!!.onVideoData(byteBuffer, bufferInfo)
    }

    // ===================采集原始数据回调=======================
    override fun onAudioSourceDataCallback(data: ByteArray, index: Int) {
        executor.execute {
            mAudioencodeTime = System.currentTimeMillis()
            mAudioComponent!!.putData(data)
            LogUtils.w("编码第:" + index + "帧，size:" + data.size + "耗时:" + (System.currentTimeMillis() - mAudioencodeTime))
        }
    }

    override fun onVideoSourceDataCallback(data: ByteArray, index: Int) {
        executor.execute {
            mVideoencodeTime = System.currentTimeMillis()
            mVideoComponent!!.encode(data)
            LogUtils.w("编码第:" + index + "帧，size:" + data.size + "耗时:" + (System.currentTimeMillis() - mVideoencodeTime))
        }
    }

    /**
     * 初始胡摄像头
     *
     * @return
     */
    private fun initCamera(): VideoConfig? {
        val cameraConfig = VideoConfig()
        CameraController.getInstance().open(0)
        val params: Camera.Parameters = CameraController.getInstance().getParams()
        //查找合适的预览尺寸
        val size: Camera.Size = CameraController.getInstance().getSupportPreviewSize(params, SUGGEST_PREVIEW_WIDTH)
        if (size == null) {
            LogUtils.e("getSupportPreviewSize failed")
            return null
        }
        val width = size.width
        val height = size.height
        //查找合适的预览图像格式
        val previewColorFormat: Int = CameraController.getInstance().getSupportPreviewColorFormat(params)
        if (previewColorFormat <= 0) {
            LogUtils.e("getSupportPreviewColorFormat failed")
            return null
        }
        params.pictureFormat = ImageFormat.JPEG
        params.previewFormat = previewColorFormat
        //        params.setPictureSize(videoWidth, videoHeight);
        params.setPreviewSize(width, height)
        params.setPreviewFpsRange(15000, 20000)
        val focusModes = params.supportedFocusModes
        if (focusModes.contains("continuous-video")) {
            params.focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO
        }
        //调整布局尺寸
        CameraController.getInstance().adjustOrientation(this, CameraController.OnOrientationChangeListener {
            val lp = sv!!.getLayoutParams() as FrameLayout.LayoutParams
            LogUtils.d(PhoneUtils.getWidth().toString() + " " + PhoneUtils.getHeight())
            if (it == 90) {
                lp.height = PhoneUtils.getWidth() * width / height
            } else {
                lp.height = PhoneUtils.getWidth() * width / height
            }
            sv!!.setLayoutParams(lp)
        })
        CameraController.getInstance().resetParams(params)
        CameraController.getInstance().setCallback(this)
        mHolder = sv!!.getHolder()
        mHolder!!.addCallback(this)
        cameraConfig.setPreviewWidth(width)
        cameraConfig.setPreviewHeight(height)
        cameraConfig.setPreviewColorFormat(previewColorFormat)
        cameraConfig.setFrameRate(15)
        return cameraConfig
    }

    /**
     * 初始话录音设备
     *
     * @return
     */
    private fun initAudioRecord(): AudioConfig {
        AudioRecordController.getInstance().init()
        AudioRecordController.getInstance().setCallback(this)
        return AudioRecordController.getInstance().getAudioConfig()
    }

    override fun onDestroy() {
        super.onDestroy()
        LogUtils.d(TAG, "onDestroy")
        CameraController.getInstance().close()
        CameraController.getInstance().setCallback(null)
        AudioRecordController.getInstance().setCallback(null)
        AudioRecordController.getInstance().release()
        mAudioComponent!!.setEncodedDataCallback(null)
        mVideoComponent!!.setEncodedDataCallback(null)
    }

    override fun onResume() {
        super.onResume()
        LogUtils.d(TAG, "onResume")
        if (mHolder != null) {
            CameraController.getInstance().startPreview(mHolder)
        }
        AudioRecordController.getInstance().start()
        mVideoComponent!!.start()
        mAudioComponent!!.start()
    }

    override fun onPause() {
        super.onPause()
        LogUtils.d(TAG, "onPause")
        CameraController.getInstance().stopPreview()
        AudioRecordController.getInstance().stop()
        mVideoComponent!!.stop()
        mAudioComponent!!.stop()
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        LogUtils.d(TAG, "surfaceCreated")
        mFlvPacker!!.start()
        mOutStream = IOUtils.open(FileUtil.getMainDir().toString() + File.separator + "/VideoCompound.flv", false)
        CameraController.getInstance().startPreview(mHolder)
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        LogUtils.d(TAG, "surfaceChanged")
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        LogUtils.d(TAG, "surfaceDestroyed")
        mFlvPacker!!.stop()
        CameraController.getInstance().stopPreview()
        CameraController.getInstance().close()
        IOUtils.close(mOutStream)
    }
}