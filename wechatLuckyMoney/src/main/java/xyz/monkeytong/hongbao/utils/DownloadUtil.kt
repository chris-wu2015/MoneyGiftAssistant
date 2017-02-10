package xyz.monkeytong.hongbao.utils

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment

import java.net.URI

import android.content.Context.DOWNLOAD_SERVICE

/**
 * Created by Zhongyi on 8/1/16.
 */
class DownloadUtil {
    fun enqueue(url: String, context: Context) {
        val r = DownloadManager.Request(Uri.parse(url))
        r.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "Uber.apk")
        r.allowScanningByMediaScanner()
        r.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        val dm = context.getSystemService(DOWNLOAD_SERVICE) as DownloadManager
        dm.enqueue(r)
    }
}
