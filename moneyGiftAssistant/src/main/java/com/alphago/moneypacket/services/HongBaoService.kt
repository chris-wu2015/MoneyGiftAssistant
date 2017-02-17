package com.alphago.moneypacket.services

import android.accessibilityservice.AccessibilityService
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.graphics.Color
import android.preference.PreferenceManager
import android.provider.Settings
import android.support.v4.app.NotificationCompat
import android.support.v4.content.LocalBroadcastManager
import android.view.accessibility.AccessibilityEvent
import com.alphago.moneypacket.R
import com.alphago.moneypacket.utils.PowerUtil

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
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        println("onStartCommand")
        return super.onStartCommand(intent, flags, startId)
    }

    private fun showNotification() {
        val notification = NotificationCompat.Builder(this)
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
                .build()
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(0, notification)
        startForeground(0, notification)
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
}