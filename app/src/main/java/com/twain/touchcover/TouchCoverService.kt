package com.twain.touchcover

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat

/**
 * @author tuwentai
 * @email  wentaitu@gmail.com
 * @date   2020-08-26
 * @description: 通过Service后台不断检测当前屏幕状态，用于横屏状态下添加左右两侧矩形遮盖区域
 */
class TouchCoverService : Service() {

    private var NOTIFICATION_ID = android.os.Process.myPid()
    private var CLOSE_BROADCASTRECEIVER = "com.twain.close"

    private lateinit var mWindowManager: WindowManager
    private lateinit var startView: View
    private lateinit var endView: View
    private lateinit var mHandler: Handler

    private var broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                CLOSE_BROADCASTRECEIVER -> {
                    stopForeground(NOTIFICATION_ID)
                    stopSelf()
                    mWindowManager.removeView(startView)
                    mWindowManager.removeView(endView)
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        mHandler = Handler()
        mWindowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        startView = LayoutInflater.from(this).inflate(R.layout.main_cover_layout, null)
        endView = LayoutInflater.from(this).inflate(R.layout.main_cover_layout, null)

        val filter = IntentFilter()
        filter.addAction(CLOSE_BROADCASTRECEIVER)
        registerReceiver(broadcastReceiver, filter)

        // 获取服务通知
        val notification = createForegroundNotification()
        // 将服务置于启动状态 ,NOTIFICATION_ID指的是创建的通知的ID
        startForeground(NOTIFICATION_ID, notification)

        checkPermission()

        var layoutParams = WindowManager.LayoutParams(
            165,
            WindowManager.LayoutParams.MATCH_PARENT,
            0,
            WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM,
            PixelFormat.TRANSPARENT)

        layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            layoutParams.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            layoutParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
        }
        layoutParams.gravity = Gravity.START


        var layoutParams2 = WindowManager.LayoutParams(
            165,
            WindowManager.LayoutParams.MATCH_PARENT,
            0,
            WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM,
            PixelFormat.TRANSPARENT)

        layoutParams2.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            layoutParams2.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutParams2.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            layoutParams2.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
        }
        layoutParams2.gravity = Gravity.END

        var controlCoverViewShow = false // 控制是否显示CoverView，竖屏状态下remove view
        val configuration: Configuration = this.resources.configuration //获取设置的配置信息
        Thread(Runnable {
            while (true) {
                val ori: Int = configuration.orientation //获取屏幕方向
                if (ori == Configuration.ORIENTATION_LANDSCAPE) {
                    if (!controlCoverViewShow) {
                        mHandler.post {
                            mWindowManager.addView(startView, layoutParams)
                            mWindowManager.addView(endView, layoutParams2)
                            controlCoverViewShow = true
                        }
                    }
                } else {
                    if (controlCoverViewShow) {
                        mHandler.post {
                            mWindowManager.removeView(startView)
                            mWindowManager.removeView(endView)
                            controlCoverViewShow = false
                        }
                    }

                }
                Thread.sleep(1000)
            }
        }).start()

    }


    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    private fun createForegroundNotification(): Notification? {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // 唯一的通知通道的id.
        val notificationChannelId = "notification_channel_id_01"

        // Android8.0以上的系统，新建消息通道
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //用户可见的通道名称
            val channelName = "Foreground Service Notification"
            //通道的重要程度
            val importance = NotificationManager.IMPORTANCE_HIGH
            val notificationChannel = NotificationChannel(notificationChannelId, channelName, importance)
            notificationChannel.description = "Channel description"
            //LED灯
            //notificationChannel.enableLights(true)
            //notificationChannel.lightColor = Color.RED
            //震动
            //notificationChannel.vibrationPattern = longArrayOf(0, 1000, 500, 1000)
            //notificationChannel.enableVibration(true)
            notificationManager.createNotificationChannel(notificationChannel)
        }
        val builder = NotificationCompat.Builder(this, notificationChannelId)
        //通知小图标
        builder.setSmallIcon(R.mipmap.ic_launcher)
        //通知标题
        builder.setContentTitle("TouchCover")
        //通知内容
        builder.setContentText("运行中")
        //设定通知显示的时间
        builder.setWhen(System.currentTimeMillis())
        //设定启动的内容
        val broadcastIntent = Intent(CLOSE_BROADCASTRECEIVER)
        val broadcastPendingIntent = PendingIntent.getBroadcast(this, 0, broadcastIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        //builder.setContentIntent(broadcastPendingIntent)

        var remoteViews = RemoteViews(packageName, R.layout.remote_views)
        remoteViews.setOnClickPendingIntent(R.id.btnCloseService, broadcastPendingIntent)
        builder.setCustomContentView(remoteViews)
        //创建通知并返回
        return builder.build()
    }

    private fun checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                this.startActivity(intent)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(broadcastReceiver)
    }

}
