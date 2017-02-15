package com.alphago.moneypacket.utils

import android.app.KeyguardManager
import android.content.Context
import android.os.PowerManager

/**
 * Created by Zhongyi on 1/29/16.
 */
class PowerUtil(context: Context) {
    private val wakeLock: PowerManager.WakeLock
    private val keyguardLock: KeyguardManager.KeyguardLock

    init {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "HongbaoWakelock")
        val km = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        keyguardLock = km.newKeyguardLock("HongbaoKeyguardLock")
    }

    private fun acquire() {
        wakeLock.acquire(1800000)
        keyguardLock.disableKeyguard()
    }

    private fun release() {
        if (wakeLock.isHeld) {
            wakeLock.release()
            keyguardLock.reenableKeyguard()
        }
    }

    fun handleWakeLock(isWake: Boolean) {
        if (isWake) {
            this.acquire()
        } else {
            this.release()
        }
    }
}
