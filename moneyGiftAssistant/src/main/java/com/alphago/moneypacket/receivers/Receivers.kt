package com.alphago.moneypacket.receivers

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.support.v4.app.NotificationManagerCompat
import com.alphago.moneypacket.services.NotificationListener
import org.jetbrains.anko.newTask

/**
 * @author Chris
 * @Desc
 * @Date 2017/2/17 017
 */
class Receivers : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        println("接收到广播：${intent.action}")
        println("listener=" + NotificationManagerCompat.getEnabledListenerPackages(context))
        context.packageManager
                .setComponentEnabledSetting(
                        ComponentName(context, NotificationListener::class.java),
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
        context.packageManager
                .setComponentEnabledSetting(
                        ComponentName(context, NotificationListener::class.java),
                        PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)

        if (context.packageName == intent.action || Intent.ACTION_BOOT_COMPLETED == intent.action) {
            context.startActivity(
                    context.packageManager
                            .getLaunchIntentForPackage(context.packageName)
                            .newTask())
        }
    }
}