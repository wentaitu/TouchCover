package com.twain.touchcover

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/**
 * @author tuwentai
 * @email  wentaitu@gmail.com
 * @date   2020-08-26
 * @description: 主Activity用于服务启动
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(Intent(this, TouchCoverService::class.java))
        } else {
            startService(Intent(this, TouchCoverService::class.java))
        }

        finish()
    }

}
