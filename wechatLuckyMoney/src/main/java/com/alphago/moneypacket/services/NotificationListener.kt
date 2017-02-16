package com.alphago.moneypacket.services

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.preference.PreferenceManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.alphago.moneypacket.R
import com.alphago.moneypacket.activities.DialogActivity
import org.jetbrains.anko.newTask

/**
 * @author Chris
 * @Desc
 * @Date 2017/2/16 016
 */
class NotificationListener : NotificationListenerService() {
    val az by lazy { getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager }
    val sp by lazy { PreferenceManager.getDefaultSharedPreferences(this) }

    override fun onListenerConnected() {
        super.onListenerConnected()
        processServiceState()

    }

    override fun onNotificationRankingUpdate(rankingMap: RankingMap?) {
        super.onNotificationRankingUpdate(rankingMap)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        try {
            if (packageName == sbn?.packageName) {
                startForeground(0, sbn!!.notification)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        processServiceState()
    }

    private fun processServiceState() {
        if (checkServiceState().not() && sp.getBoolean(
                getString(R.string.dont_show_again), false).not()) {
            startActivity(Intent(this, DialogActivity::class.java).newTask())
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        processServiceState()
    }

    fun checkServiceState(): Boolean {
        val runningServices = az.getRunningServices(Int.MAX_VALUE)
        return runningServices.any {
            HongBaoService::class.java.name == it.service.className
        }
    }
}