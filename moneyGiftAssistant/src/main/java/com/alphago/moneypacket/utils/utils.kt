package com.alphago.moneypacket.utils

import android.app.Activity
import java.lang.ref.WeakReference
import java.util.*

/**
 * @author Chris
 * @Desc
 * @Date 2017/2/23 023
 */
object ScreenManager {
    var mActivities: ArrayList<WeakReference<Activity>> = arrayListOf()

    fun setActivity(activity: Activity) {
        mActivities.add(WeakReference<Activity>(activity))
    }

    fun finishActivity() {
        mActivities.forEach {
            it.get()?.finish()
        }
        mActivities.clear()
    }
}

