package com.alphago.moneypacket.activities

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.annotation.TargetApi
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.provider.Settings
import android.support.v4.app.ActivityCompat
import android.support.v4.app.NotificationManagerCompat
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityManager
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.alphago.extensions.dialog
import com.alphago.extensions.support
import com.alphago.moneypacket.BuildConfig
import com.alphago.moneypacket.R
import com.alphago.moneypacket.receivers.Receivers
import com.tencent.bugly.Bugly

class MainActivity : AppCompatActivity(), AccessibilityManager.AccessibilityStateChangeListener {

    //开关切换按钮
    private var pluginStatusText: TextView? = null
    private var pluginStatusIcon: ImageView? = null
    //AccessibilityService 管理
    private var accessibilityManager: AccessibilityManager? = null
//    private val receiver by lazy { Receivers() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //CrashReport.initCrashReport(getApplicationContext(), "900019352", false);
        Bugly.init(applicationContext, "cbaedd81ff", !BuildConfig.DEBUG)
        setContentView(R.layout.activity_main)
        pluginStatusText = findViewById(R.id.layout_control_accessibility_text) as TextView
        pluginStatusIcon = findViewById(R.id.layout_control_accessibility_icon) as ImageView
        (findViewById(R.id.textView5) as TextView).text = BuildConfig.VERSION_NAME

        handleMaterialStatusBar()

        explicitlyLoadPreferences()

        //监听AccessibilityService 变化
        accessibilityManager = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        accessibilityManager!!.addAccessibilityStateChangeListener(this)
        updateServiceStatus()
//        val filter = IntentFilter()
//        filter.support(24, { addAction(Intent.ACTION_USER_UNLOCKED) },
//                { addAction("android.intent.action.USER_UNLOCKED") })
//        filter.addAction(Intent.ACTION_SCREEN_ON)
//        registerReceiver(receiver, filter)

        println(ActivityCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_BOOT_COMPLETED) == PackageManager.PERMISSION_GRANTED)
    }

    override fun onBackPressed() {
        startActivity(Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_HOME))
    }

    private fun explicitlyLoadPreferences() {
        PreferenceManager.setDefaultValues(this, R.xml.general_preferences, false)
    }

    /**
     * 适配MIUI沉浸状态栏
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private fun handleMaterialStatusBar() {
        // Not supported in APK level lower than 21
        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return

        val window = this.window

        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)

        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)

        window.statusBarColor = 0xffE46C62.toInt()

    }

    override fun onPause() {
        super.onPause()
    }

    override fun onResume() {
        super.onResume()

        updateServiceStatus()
    }

    override fun onDestroy() {
        //移除监听服务
        accessibilityManager!!.removeAccessibilityStateChangeListener(this)
//        unregisterReceiver(receiver)
        super.onDestroy()
    }

    fun openAccessibility(view: View) {
        if (queryNotificationListenerState().not()) {
            dialog {
                title(R.string.app_name)
                message(R.string.register_notification_listener)
                positiveButton(R.string.go_right_now) {
                    try {
                        startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    } catch (e: Exception) {
                        startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
                    }
                    dismiss()
                }
                negativeButton(R.string.dont_need) {
                    openSysAccessibilitySetting()
                    dismiss()
                }
            }
                    ?.show()
            return
        }
        openSysAccessibilitySetting()
    }

    fun showAppDetail(v: View) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(Uri.parse("package:" + packageName))
        startActivity(intent)
    }

    private fun queryNotificationListenerState(): Boolean {
        try {
            val packages = NotificationManagerCompat.getEnabledListenerPackages(this)
            return packages.any { packageName == it }
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    private fun openSysAccessibilitySetting() {
        try {
            Toast.makeText(this, getString(R.string.turn_on_toast) + pluginStatusText!!.text, Toast.LENGTH_SHORT).show()
            val accessibleIntent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(accessibleIntent)
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.turn_on_error_toast), Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    fun openSettings(view: View) {
        val settingsIntent = Intent(this, SettingsActivity::class.java)
        settingsIntent.putExtra("title", getString(R.string.preference))
        settingsIntent.putExtra("frag_id", "GeneralSettingsFragment")
        startActivity(settingsIntent)
    }

    fun checkNotificationEnable(v: View) {
        try {
            var tag = (v.tag as? Int) ?: 0
            val uri = Uri.parse("package:" +
                    if (tag == 0) {
                        tag++
                        getString(R.string.wechat_package)
                    } else {
                        tag--
                        getString(R.string.qq_package)
                    })
            v.tag = tag
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    .setData(uri))


        } catch (e: Exception) {
            e.printStackTrace()
            v.tag = 0
        }
    }

    override fun onAccessibilityStateChanged(enabled: Boolean) {
        updateServiceStatus()
    }

    /**
     * 更新当前 HongbaoService 显示状态
     */
    private fun updateServiceStatus() {
        if (isServiceEnabled) {
            pluginStatusText!!.setText(R.string.service_off)
            pluginStatusIcon!!.setBackgroundResource(R.mipmap.ic_stop)
        } else {
            pluginStatusText!!.setText(R.string.service_on)
            pluginStatusIcon!!.setBackgroundResource(R.mipmap.ic_start)
        }
    }

    /**
     * 获取 HongbaoService 是否启用状态

     * @return
     */
    private val isServiceEnabled: Boolean
        get() {
            try {
                val accessibilityEnable = Settings.Secure.getInt(contentResolver,
                        Settings.Secure.ACCESSIBILITY_ENABLED)
                return accessibilityEnable == 1
            } catch (e: Exception) {
                val accessibilityServices = accessibilityManager!!.
                        getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC)
                return accessibilityServices.any { it.id == packageName + "/.services.HongBaoService" }
            }
        }
}