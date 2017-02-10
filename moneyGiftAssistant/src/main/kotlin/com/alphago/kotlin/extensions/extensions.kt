package com.alphago.kotlin.extensions

import android.os.Build
import android.util.Log
import com.alphago.apps.BuildConfig


/**
 * @author Chris
 * @Desc
 * @Date 2017/2/7 007
 */
@Suppress("UNCHECKED_CAST")
fun <S, R> S.support(version: Int, support: S.(S) -> R, nonsupport: S.(S) -> R = { Unit as R })
        = if (Build.VERSION.SDK_INT >= version) support(this) else nonsupport(this)

fun String.log(tag: String? = null) {
    if (BuildConfig.DEBUG) {
        if (tag != null) Log.i(tag, this)
        else println(this)
    }
}
