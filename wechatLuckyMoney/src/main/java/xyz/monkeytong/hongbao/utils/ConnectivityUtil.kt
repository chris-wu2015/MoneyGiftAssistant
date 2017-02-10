package xyz.monkeytong.hongbao.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo

/**
 * Created by Zhongyi on 1/29/16.
 */
object ConnectivityUtil {
    fun isWifi(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val activeNetwork = cm.activeNetworkInfo
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting
                && activeNetwork.type == ConnectivityManager.TYPE_WIFI
    }
}
