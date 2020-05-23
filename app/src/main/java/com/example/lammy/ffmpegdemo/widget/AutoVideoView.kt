package com.example.lammy.ffmpegdemo.widget

import android.annotation.TargetApi
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.view.Surface
import android.view.SurfaceView
import com.example.lammy.ffmpegdemo.video.VideoHandler

//import com.example.lammy.ffmpegdemo.video.VideoHandler.playVideo

/**
 * Time: 09:35
 */
class AutoVideoView : SurfaceView {
    var mSurface: Surface? = null

    constructor(context: Context?) : super(context) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init()
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {
        init()
    }

    private fun init() {
        holder.setFormat(PixelFormat.RGBA_8888)
        mSurface = holder.surface
    }

    fun playVideo(videoPath: String?) {
        Thread(Runnable {
            Log.d("FFVideoView", "run: playVideo")
//            VideoHandler.playVideo(videoPath!!, mSurface!!)
        }).start()
    }
}