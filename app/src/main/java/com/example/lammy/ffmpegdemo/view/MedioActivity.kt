package com.example.lammy.ffmpegdemo.view

import android.os.Bundle
import android.view.View
import androidx.databinding.ViewDataBinding
import com.aleyn.mvvm.base.BaseActivity
import com.aleyn.mvvm.base.NoViewModel
import com.blankj.utilcode.util.FileUtils
import com.blankj.utilcode.util.LogUtils
import com.example.lammy.ffmpegdemo.R
import com.example.lammy.ffmpegdemo.util.FFmpegUtil
import kotlinx.android.synthetic.main.activity_audio.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class MedioActivity : BaseActivity<NoViewModel, ViewDataBinding>() {
    override fun layoutId(): Int {
        return R.layout.activity_medio
    }

    override fun initView(savedInstanceState: Bundle?) {
    }

    override fun initData() {


    }
}