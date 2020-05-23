package com.example.lammy.ffmpegdemo.view

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.databinding.ViewDataBinding
import com.aleyn.mvvm.base.BaseActivity
import com.aleyn.mvvm.base.NoViewModel
import com.example.lammy.ffmpegdemo.R
import com.example.lammy.ffmpegdemo.view.audio.AudioActivity
import com.example.lammy.ffmpegdemo.view.video.VideoActivity
import com.hjq.permissions.OnPermission
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import kotlinx.android.synthetic.main.activity_splash.*


class MainActivity : BaseActivity<NoViewModel, ViewDataBinding>() {
    override fun layoutId(): Int {
        return R.layout.activity_splash
    }

    override fun initView(savedInstanceState: Bundle?) {
        applyPermissions()
    }

    private fun applyPermissions() {
        XXPermissions.with(this)
                .constantRequest()
                .permission(Permission.READ_EXTERNAL_STORAGE,Permission.WRITE_EXTERNAL_STORAGE,Permission.CAMERA,Permission.RECORD_AUDIO)
                .request(object : OnPermission {
                    override fun hasPermission(granted: List<String>, all: Boolean) {}
                    override fun noPermission(denied: List<String>, quick: Boolean) {}
                })
    }

    override fun initData() {
        btn_audio_about.setOnClickListener(View.OnClickListener {
            startActivity(Intent().setClass(this, AudioActivity::class.java))
        })

        btn_medio_about.setOnClickListener(View.OnClickListener {
            startActivity(Intent().setClass(this, VideoActivity::class.java))
        })
    }
}