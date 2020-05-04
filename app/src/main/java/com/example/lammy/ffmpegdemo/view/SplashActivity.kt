package com.example.lammy.ffmpegdemo.view

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.databinding.ViewDataBinding
import com.aleyn.mvvm.base.BaseActivity
import com.aleyn.mvvm.base.NoViewModel
import com.example.lammy.ffmpegdemo.R
import com.hjq.permissions.OnPermission
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import kotlinx.android.synthetic.main.activity_splash.*


class SplashActivity : BaseActivity<NoViewModel, ViewDataBinding>() {
    override fun layoutId(): Int {
        return R.layout.activity_splash
    }

    override fun initView(savedInstanceState: Bundle?) {
        applyPermissions()
    }

    private fun applyPermissions() {
        XXPermissions.with(this)
                .constantRequest()
                .permission(Permission.Group.STORAGE)
                .request(object : OnPermission {
                    override fun hasPermission(granted: List<String>, all: Boolean) {}
                    override fun noPermission(denied: List<String>, quick: Boolean) {}
                })
    }

    override fun initData() {
        btn_audio_about.setOnClickListener(View.OnClickListener {
            val inte=Intent()
            inte.setClass(this,AudioActivity::class.java)
            startActivity(inte)
        })

        btn_medio_about.setOnClickListener(View.OnClickListener {
            val inte=Intent()
            inte.setClass(this,MedioActivity::class.java)
            startActivity(inte)
        })
    }
}