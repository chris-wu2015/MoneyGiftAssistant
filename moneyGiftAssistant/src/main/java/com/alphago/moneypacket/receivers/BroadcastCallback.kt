package com.alphago.moneypacket.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter

/**
 * @author Chris
 * @Desc
 * @Date 2017/2/23 023
 */
class BroadcastCallback(vararg actions: String,
                        val autoClose: Boolean = false,
                        val run: (Context, Intent) -> Unit) : BroadcastReceiver() {
    val filter: IntentFilter = IntentFilter()
    private var registered: Boolean = false

    init {
        if (actions.isEmpty()) throw IllegalArgumentException("BroadcastCallback.actions are not " +
                "allow empty here!")
        actions.forEach {
            filter.addAction(it)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (filter.matchAction(intent.action)) {
            run(context, intent)
            if (autoClose) unregister(context)
        }
    }

    fun register(context: Context): BroadcastCallback {
        context.registerReceiver(this, filter)
        registered = true
        return this
    }

    fun unregister(context: Context) {
        try {
            if (registered) {
                context.unregisterReceiver(this)
                registered = false
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}