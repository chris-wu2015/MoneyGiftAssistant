package com.alphago.moneypacket.fragments

import android.os.Build
import android.os.Bundle
import android.preference.Preference
import android.preference.PreferenceFragment
import android.preference.PreferenceManager
import android.widget.Toast
import com.alphago.moneypacket.R

/**
 * Created by Zhongyi on 2/4/16.
 */
class CommentSettingsFragment : PreferenceFragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.comment_preferences)
        setPrefListeners()
    }

    private fun setPrefListeners() {
        val updatePref = findPreference("pref_comment_switch")
        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            updatePref.isEnabled = false
        }
        Toast.makeText(activity, "该功能尚处于实验中,只能自动填充感谢语,无法直接发送.", Toast.LENGTH_LONG).show()

        val commentWordsPref = findPreference("pref_comment_words")
        val summary = resources.getString(R.string.pref_comment_words_summary)
        val value = PreferenceManager.getDefaultSharedPreferences(activity).getString("pref_comment_words", "")
        if (value!!.length > 0) commentWordsPref.summary = summary + ":" + value

        commentWordsPref.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { preference, o ->
            val summary = resources.getString(R.string.pref_comment_words_summary)
            if (o != null && o.toString().length > 0) {
                preference.summary = summary + ":" + o.toString()
            } else {
                preference.summary = summary
            }
            true
        }
    }
}
