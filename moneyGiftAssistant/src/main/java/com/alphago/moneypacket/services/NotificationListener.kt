package com.alphago.moneypacket.services

import android.app.ActivityManager
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.preference.PreferenceManager
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.view.WindowManager
import com.alphago.extensions.support
import com.alphago.moneypacket.R

/**
 * @author Chris
 * @Desc
 * @Date 2017/2/16 016
 */
class NotificationListener : NotificationListenerService() {
    val az by lazy { getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager }
    val sp by lazy { PreferenceManager.getDefaultSharedPreferences(this) }
    val handler by lazy { Handler(this.mainLooper) }

    override fun onListenerConnected() {
        super.onListenerConnected()
        processServiceState()

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
        if (checkServiceState().not() && sp.getBoolean(getString(R.string.dont_show_again), true)) {
            handler.post {
                val dialog = AlertDialog.Builder(this, support(24,
                        { android.R.style.Theme_Material_Light_Dialog_NoActionBar },
                        { android.R.style.Theme_Holo_Light_Dialog_NoActionBar }))
                        .setTitle(R.string.app_name)
                        .setMessage(R.string.service_not_running)
                        .setPositiveButton(R.string.go_right_now) { dialog, which ->
                            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                            dialog.dismiss()
                        }
                        .setNegativeButton(R.string.already_know) { dialog, which -> dialog.dismiss() }
                        .setNeutralButton(R.string.dont_show_again) { dialog, which ->
                            PreferenceManager.getDefaultSharedPreferences(this)
                                    .edit()
                                    .putBoolean(getString(R.string.dont_show_again), false)
                                    .apply()
                            dialog.dismiss()
                        }
                        .create()
                dialog.window.setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT)
                dialog.show()
            }
//            startActivity(Intent(this, DialogActivity::class.java)
//                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or
//                            Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS))
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