package com.alphago.moneypacket.activities

import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.provider.Settings
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityManager
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.tencent.bugly.Bugly
import com.tencent.bugly.crashreport.CrashReport
import com.alphago.moneypacket.BuildConfig
import com.alphago.moneypacket.R

//import com.alphago.moneypacket.utils.UpdateTask


class MainActivity : Activity(), AccessibilityManager.AccessibilityStateChangeListener {

    //开关切换按钮
    private var pluginStatusText: TextView? = null
    private var pluginStatusIcon: ImageView? = null
    //AccessibilityService 管理
    private var accessibilityManager: AccessibilityManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //CrashReport.initCrashReport(getApplicationContext(), "900019352", false);
        Bugly.init(applicationContext, "cbaedd81ff", !BuildConfig.DEBUG)
        setContentView(R.layout.activity_main)
        pluginStatusText = findViewById(R.id.layout_control_accessibility_text) as TextView
        pluginStatusIcon = findViewById(R.id.layout_control_accessibility_icon) as ImageView

        handleMaterialStatusBar()

        explicitlyLoadPreferences()

        //监听AccessibilityService 变化
        accessibilityManager = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        accessibilityManager!!.addAccessibilityStateChangeListener(this)
        updateServiceStatus()
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
        // Check for update when WIFI is connected or on first time.
//        if (ConnectivityUtil.isWifi(this) || UpdateTask.count == 0)
//            UpdateTask(this, false).update()
    }

    override fun onDestroy() {
        //移除监听服务
        accessibilityManager!!.removeAccessibilityStateChangeListener(this)
        super.onDestroy()
    }

    fun openAccessibility(view: View) {
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
//            val accessibilityServices = accessibilityManager!!.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC)
//            return accessibilityServices.any { it.id == packageName + "/.services.HongBaoService" }
            val accessibilityEnable = Settings.Secure.getInt(contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED)
            return accessibilityEnable == 1
        }
}
