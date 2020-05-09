package com.example.lammy.ffmpegdemo.widget

import android.content.Context
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView

/**
 * Author : eric
 * CreateDate : 2017/10/9  16:36
 * Email : ericli_wang@163.com
 * Version : 2.0
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