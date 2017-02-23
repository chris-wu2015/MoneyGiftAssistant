package com.alphago.moneypacket.services

import android.accessibilityservice.AccessibilityService
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.preference.PreferenceManager
import android.provider.Settings
import android.support.v4.app.NotificationCompat
import android.support.v4.content.LocalBroadcastManager
import android.view.accessibility.AccessibilityEvent
import com.alphago.extensions.support
import com.alphago.moneypacket.R
import com.alphago.moneypacket.activities.KeepAliveActivity
import com.alphago.moneypacket.config.Config
import com.alphago.moneypacket.receivers.BroadcastCallback
import com.alphago.moneypacket.utils.PowerUtil
import com.alphago.moneypacket.utils.ScreenManager

/**
 * @author Chris
 * @Desc
 * @Date 2017/2/13 013
 */
class HongBaoService : AccessibilityService(), SharedPreferences.OnSharedPreferenceChangeListener {
    val WECHAT_PACKAGE_NAME = lazy { getString(R.string.wechat_package) }
    val QQ_PACKAGE_NAME = lazy { getString(R.string.qq_package) }

    private val sharedPreferences: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(this)
    }
    private val powerUtil: PowerUtil by lazy {
        PowerUtil(this)
    }
    private val weChatProcessor: HongBaoProcessor by lazy { WeChatProcessor(this) }
    private val qqProcessor: HongBaoProcessor by lazy { QQProcessor(this) }

    override fun onServiceConnected() {
        super.onServiceConnected()
        watchFlagsFromPreference()
        showNotification()

        val broadcast = BroadcastCallback(Intent.ACTION_SCREEN_ON, Intent.ACTION_SCREEN_OFF) {
            context, intent ->
            println("HongBaoService.接收到广播:${intent.action}")
            when (intent.action) {
                Intent.ACTION_SCREEN_ON -> ScreenManager.finishActivity()
                Intent.ACTION_SCREEN_OFF -> KeepAliveActivity.open(this)
            }
        }.register(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        println("onStartCommand")
        return Service.START_REDELIVER_INTENT
    }

    private fun showNotification() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        support(Build.VERSION_CODES.JELLY_BEAN_MR2, {
            val notification = buildNotification().build()
            nm.notify(Config.NOTIFICATION_ID, notification)
            startForeground(Config.NOTIFICATION_ID, notification)
            startService(Intent(this, InnerService::class.java))
        }, {
            nm.notify(Config.NOTIFICATION_ID, Notification())
            startForeground(Config.NOTIFICATION_ID, Notification())
        })
    }

    private fun buildNotification(): NotificationCompat.Builder {
        return NotificationCompat.Builder(this)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.app_description))
                .setColor(Color.RED)
                .setContentIntent(PendingIntent.getActivity(this, 0,
                        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS),
                        PendingIntent.FLAG_UPDATE_CURRENT))
                .setAutoCancel(false)
                .setOngoing(true)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher))
                .setShowWhen(true)
                .setLocalOnly(true)
                .setPriority(NotificationCompat.PRIORITY_MAX)
    }

    private fun cancelNotification() {
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(0)
        stopForeground(true)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        println("onUnbind")
        cancelNotification()
        return super.onUnbind(intent)
    }

    override fun onTrimMemory(level: Int) {
        println("onTrimMemory")
        cancelNotification()
        super.onTrimMemory(level)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (WECHAT_PACKAGE_NAME.value == event.packageName) {
            weChatProcessor.processAccessibilityEvent(this, rootInActiveWindow, event)
        }
        if (QQ_PACKAGE_NAME.value == event.packageName) {
            qqProcessor.processAccessibilityEvent(this, rootInActiveWindow, event)
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (IHongBaoProcessor.WATCH_ON_LOCK == key) {
            powerUtil.handleWakeLock(sharedPreferences?.getBoolean(key, false) ?: false)
        }
    }

    override fun onInterrupt() {
    }

    override fun onDestroy() {
        this.powerUtil.handleWakeLock(false)
        cancelNotification()
        LocalBroadcastManager.getInstance(this).sendBroadcast(Intent(packageName))
        super.onDestroy()
    }

    private fun watchFlagsFromPreference() {
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)
        val watchOnLockFlag = sharedPreferences.getBoolean(IHongBaoProcessor.WATCH_ON_LOCK, false)
        this.powerUtil.handleWakeLock(watchOnLockFlag)
    }

    inner class InnerService : Service() {
        override fun onBind(intent: Intent?): IBinder? {
            return null
        }

        override fun onCreate() {
            super.onCreate()
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val builder = buildNotification().build()
            nm.notify(Config.NOTIFICATION_ID, builder)
            startForeground(Config.NOTIFICATION_ID, builder)
            Handler(mainLooper).postDelayed({

                stopForeground(true)
                nm.cancel(Config.NOTIFICATION_ID)
                stopSelf()
            }, 100)
        }
    }
}