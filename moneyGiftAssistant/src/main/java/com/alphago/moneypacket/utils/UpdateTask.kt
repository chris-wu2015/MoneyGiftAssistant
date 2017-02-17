//package com.alphago.moneypacket.utils
//
//import android.content.Context
//import android.content.Intent
//import android.content.pm.PackageInfo
//import android.net.Uri
//import android.os.AsyncTask
//import android.widget.Toast
//import org.apache.http.HttpResponse
//import org.apache.http.StatusLine
//import org.apache.http.client.HttpClient
//import org.apache.http.client.methods.HttpGet
//import org.apache.http.impl.client.DefaultHttpClient
//import org.json.JSONObject
//import com.alphago.moneypacket.R
//import com.alphago.moneypacket.activities.SettingsActivity
//import com.alphago.moneypacket.activities.WebViewActivity
//
//import java.io.ByteArrayOutputStream
//import java.io.IOException
//
///**
// * Created by Zhongyi on 1/20/16.
// * Util for app update task.
// */
//class UpdateTask(private val context: Context, private val isUpdateOnRelease: Boolean) : AsyncTask<String, String, String>() {
//
//    init {
//        if (this.isUpdateOnRelease) Toast.makeText(context, context.getString(R.string.checking_new_version), Toast.LENGTH_SHORT).show()
//    }
//
//    override fun doInBackground(vararg uri: String): String? {
//        val httpclient = DefaultHttpClient()
//        val response: HttpResponse
//        var responseString: String? = null
//        try {
//            response = httpclient.execute(HttpGet(uri[0]))
//            val statusLine = response.getStatusLine()
//            if (statusLine.getStatusCode() === 200) {
//                val out = ByteArrayOutputStream()
//                response.getEntity().writeTo(out)
//                responseString = out.toString()
//                out.close()
//            } else {
//                // Close the connection.
//                response.getEntity().getContent().close()
//                throw IOException(statusLine.getReasonPhrase())
//            }
//        } catch (e: Exception) {
//            return null
//        }
//
//        return responseString
//    }
//
//    override fun onPostExecute(result: String) {
//        super.onPostExecute(result)
//        try {
//            count += 1
//            val release = JSONObject(result)
//
//            // Get current version
//            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
//            val version = pInfo.versionName
//
//            val latestVersion = release.getString("tag_name")
//            val isPreRelease = release.getBoolean("prerelease")
//            if (!isPreRelease && version.compareTo(latestVersion, ignoreCase = true) >= 0) {
//                // Your version is ahead of or same as the latest.
//                if (this.isUpdateOnRelease)
//                    Toast.makeText(context, R.string.update_already_latest, Toast.LENGTH_SHORT).show()
//            } else {
//                if (!isUpdateOnRelease) {
//                    Toast.makeText(context, context.getString(R.string.update_new_seg1) + latestVersion + context.getString(R.string.update_new_seg3), Toast.LENGTH_LONG).show()
//                    return
//                }
//                // Need update.
//                val downloadUrl = release.getJSONArray("assets").getJSONObject(0).getString("browser_download_url")
//
//                // Give up on the fucking DownloadManager. The downloaded apk got renamed and unable to install. Fuck.
//                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl))
//                browserIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
//                context.startActivity(browserIntent)
//                Toast.makeText(context, context.getString(R.string.update_new_seg1) + latestVersion + context.getString(R.string.update_new_seg2), Toast.LENGTH_LONG).show()
//            }
//        } catch (e: Exception) {
//            e.printStackTrace()
//            if (this.isUpdateOnRelease) Toast.makeText(context, R.string.update_error, Toast.LENGTH_LONG).show()
//        }
//
//    }
//
//    fun update() {
//        super.execute(updateUrl)
//    }
//
//    companion object {
//        var count = 0
//        val updateUrl = "https://api.github.com/repos/geeeeeeeeek/WeChatLuckyMoney/releases/latest"
//    }
//}