package xyz.monkeytong.hongbao.activities

import android.annotation.TargetApi
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.webkit.CookieSyncManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
import android.widget.Toast
import xyz.monkeytong.hongbao.R
import xyz.monkeytong.hongbao.utils.DownloadUtil

/**
 * Created by Zhongyi on 1/19/16.
 * Settings page.
 */
class WebViewActivity : Activity() {
    private var webView: WebView? = null
    private var webViewUrl: String? = null
    private var webViewTitle: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        loadUI()

        val bundle = intent.extras
        if (bundle != null && !bundle.isEmpty) {
            webViewTitle = bundle.getString("title")
            webViewUrl = bundle.getString("url")

            val webViewBar = findViewById(R.id.webview_bar) as TextView
            webViewBar.text = webViewTitle

            webView = findViewById(R.id.webView) as WebView
            webView!!.settings.builtInZoomControls = false
            webView!!.settings.javaScriptEnabled = true
            webView!!.settings.domStorageEnabled = true
            webView!!.settings.cacheMode = WebSettings.LOAD_DEFAULT
            webView!!.setWebViewClient(object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                    if (url.contains("apk")) {
                        Toast.makeText(applicationContext, getString(R.string.download_backend), Toast.LENGTH_SHORT).show()
                        DownloadUtil().enqueue(url, applicationContext)
                        return true
                    } else if (!url.contains("http")) {
                        Toast.makeText(applicationContext, getString(R.string.download_redirect), Toast.LENGTH_LONG).show()
                        webViewBar.text = getString(R.string.download_hint)
                        return false
                    } else {
                        view.loadUrl(url)
                        return false
                    }
                }

                override fun onPageFinished(view: WebView, url: String) {
                    CookieSyncManager.getInstance().sync()
                }
            })
            webView!!.loadUrl(webViewUrl)
        }
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private fun loadUI() {
        setContentView(R.layout.activity_webview)

        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return

        val window = this.window

        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)

        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)

        window.statusBarColor = 0xffE46C62.toInt()
    }

    fun performBack(view: View) {
        super.onBackPressed()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            when (keyCode) {
                KeyEvent.KEYCODE_BACK -> {
                    if (webView!!.canGoBack()) {
                        webView!!.goBack()
                    } else {
                        finish()
                    }
                    return true
                }
            }

        }
        return super.onKeyDown(keyCode, event)
    }

    fun openLink(view: View) {
        val intent = Intent(Intent.ACTION_VIEW,
                Uri.parse(this.webViewUrl))
        startActivity(intent)
    }
}
