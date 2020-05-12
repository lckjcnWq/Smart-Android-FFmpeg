package com.example.lammy.ffmpegdemo.view

import android.app.Activity
import android.graphics.ImageFormat
import android.hardware.Camera
import android.hardware.Camera.PreviewCallback
import android.os.Bundle
import android.view.SurfaceHolder
import android.view.View
import android.widget.FrameLayout
import com.example.ffmpeg_lib.device.CameraController
import com.example.ffmpeg_lib.utils.LogUtils
import com.example.ffmpeg_lib.utils.PhoneUtils
import com.example.lammy.ffmpegdemo.R
import com.example.lammy.ffmpegdemo.ffmpeg.FFmpegHandle
import com.example.lammy.ffmpegdemo.widget.UiSurfaceView
import java.util.concurrent.Executors

/**
 * Desc :
 * Modified :
 */
class CameraFFmpegPushRtmpActivity : Activity(), SurfaceHolder.Callback {
    private var sv: UiSurfaceView? = null
    private val WIDTH = 640
    private var HEIGHT = 480
    private var mHolder: SurfaceHolder? = null

    //    private String url = "rtmp://192.168.31.127/live/test";
    private val url = "/sdcard/input_test.flv"

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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)
        init()
    }

    private fun init() {
        sv = findViewById(R.id.sv)
        mStreamIt = StreamIt()
        CameraController.getInstance().open(1)
        val params: Camera.Parameters = CameraController.getInstance().getParams()
        params.pictureFormat = ImageFormat.NV21
        val list = params.supportedPictureSizes
        for (size in list) {
            LogUtils.d(size.width.toString() + " " + size.height)
            if (size.width == WIDTH) {
                HEIGHT = size.height
                break
            }
        }
        params.setPictureSize(WIDTH, HEIGHT)
        params.setPreviewSize(WIDTH, HEIGHT)
        params.setPreviewFpsRange(30000, 30000)
        val focusModes = params.supportedFocusModes
        if (focusModes.contains("continuous-video")) {
            params.focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO
        }
        CameraController.getInstance().adjustOrientation(this, CameraController.OnOrientationChangeListener {
            val lp = sv!!.getLayoutParams() as FrameLayout.LayoutParams
            LogUtils.d(PhoneUtils.getWidth().toString() + " " + PhoneUtils.getHeight())
            if (it == 90) {
                lp.height = PhoneUtils.getWidth() * WIDTH / HEIGHT
            } else {
                lp.height = PhoneUtils.getWidth() * HEIGHT / WIDTH
            }
            sv!!.setLayoutParams(lp)
        })
        CameraController.getInstance().resetParams(params)
        mHolder = sv!!.getHolder()
        mHolder!!.addCallback(this)
        FFmpegHandle.initVideo(url, WIDTH, HEIGHT)
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
        CameraController.getInstance().startPreview(mHolder, mStreamIt)
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}
    override fun surfaceDestroyed(holder: SurfaceHolder) {
        CameraController.getInstance().stopPreview()
        CameraController.getInstance().close()
    }

    var executor = Executors.newSingleThreadExecutor()
    fun btnStart(view: View?) {}
    inner class StreamIt : PreviewCallback {
        override fun onPreviewFrame(data: ByteArray, camera: Camera) {
            val endTime = System.currentTimeMillis()
            executor.execute {
                encodeTime = System.currentTimeMillis()
                FFmpegHandle.onFrameCallback(data)
                LogUtils.w("编码第:" + encodeCount++ + "帧，耗时:" + (System.currentTimeMillis() - encodeTime))
            }
            LogUtils.d("采集第:" + ++count + "帧，距上一帧间隔时间:"
                    + (endTime - previewTime) + "  " + Thread.currentThread().name)
            previewTime = endTime
        }
    }
}