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
    val mActivities: ArrayList<WeakReference<Activity>> = arrayListOf()

    fun setActivity(activity: Activity) {
        mActivities.add(WeakReference<Activity>(activity))
        println("setActivity:$mActivities")
    }

    fun finishActivity() {
        println("finishActivity:$mActivities")
        mActivities.forEach {
            println("关闭:$it-->${it.get()}")
            it.get()?.finish()
        }
        mActivities.clear()
    }
}

