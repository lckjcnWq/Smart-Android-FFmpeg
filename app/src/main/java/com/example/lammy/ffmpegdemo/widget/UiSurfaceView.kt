package com.example.lammy.ffmpegdemo.widget

import android.content.Context
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView

/**
 * Desc :
 * Modified :
 */
class UiSurfaceView(context: Context?, attrs: AttributeSet?) : SurfaceView(context, attrs) {
    private var mHolder: SurfaceHolder? = null
    private fun init() {
        mHolder = holder
    }

    init {
        init()
    }
}