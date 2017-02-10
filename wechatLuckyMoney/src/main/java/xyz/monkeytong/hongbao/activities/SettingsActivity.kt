package xyz.monkeytong.hongbao.activities

import android.annotation.TargetApi
import android.app.Activity
import android.app.Fragment
import android.app.FragmentManager
import android.app.FragmentTransaction
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.preference.Preference
import android.preference.PreferenceActivity
import android.preference.PreferenceManager
import android.provider.Settings
import android.support.v4.app.FragmentActivity
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import org.w3c.dom.Text
import xyz.monkeytong.hongbao.R
import xyz.monkeytong.hongbao.fragments.CommentSettingsFragment
import xyz.monkeytong.hongbao.fragments.GeneralSettingsFragment
import xyz.monkeytong.hongbao.utils.UpdateTask

/**
 * Created by Zhongyi on 1/19/16.
 * Settings page.
 */
class SettingsActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preferences)

        loadUI()
        prepareSettings()
    }

    private fun prepareSettings() {
        val title: String
        val fragId: String
        val bundle = intent.extras
        if (bundle != null) {
            title = bundle.getString("title")
            fragId = bundle.getString("frag_id")
        } else {
            title = getString(R.string.preference)
            fragId = "GeneralSettingsFragment"
        }

        val textView = findViewById(R.id.settings_bar) as TextView
        textView.text = title

        val fragmentManager = fragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()

        if ("GeneralSettingsFragment" == fragId) {
            fragmentTransaction.replace(R.id.preferences_fragment, GeneralSettingsFragment())
        } else if ("CommentSettingsFragment" == fragId) {
            fragmentTransaction.replace(R.id.preferences_fragment, CommentSettingsFragment())
        }
        fragmentTransaction.commit()
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private fun loadUI() {
        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return

        val window = this.window

        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)

        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)

        window.statusBarColor = 0xffE46C62.toInt()
    }

    override fun onResume() {
        super.onResume()
    }

    fun performBack(view: View) {
        super.onBackPressed()
    }

    fun enterAccessibilityPage(view: View) {
        Toast.makeText(this, getString(R.string.turn_on_toast), Toast.LENGTH_SHORT).show()
        val mAccessibleIntent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(mAccessibleIntent)
    }
}
