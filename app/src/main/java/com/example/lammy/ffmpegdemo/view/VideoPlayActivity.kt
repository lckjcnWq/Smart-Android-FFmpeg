package com.example.lammy.ffmpegdemo.view

import android.os.Bundle
import androidx.databinding.ViewDataBinding
import com.aleyn.mvvm.base.BaseActivity
import com.aleyn.mvvm.base.NoViewModel

class VideoPlayActivity  : BaseActivity<NoViewModel, ViewDataBinding>() {
    override fun layoutId(): Int {
        return 0
    }

    override fun initView(savedInstanceState: Bundle?) {
    }

    override fun initData() {
    }
}