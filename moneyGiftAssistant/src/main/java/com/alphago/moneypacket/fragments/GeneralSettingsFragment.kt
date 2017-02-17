package com.alphago.moneypacket.fragments

import android.os.Bundle
import android.preference.Preference
import android.preference.PreferenceFragment
import android.preference.PreferenceManager
import com.alphago.moneypacket.R


/**
 * Created by Zhongyi on 2/4/16.
 */
class GeneralSettingsFragment : PreferenceFragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.general_preferences)
        setPrefListeners()
    }

    private fun setPrefListeners() {

        val excludeWordsPref = findPreference("pref_watch_exclude_words")
        val summary = resources.getString(R.string.pref_watch_exclude_words_summary)
        val value = PreferenceManager.getDefaultSharedPreferences(activity).getString("pref_watch_exclude_words", "")
        if (value!!.length > 0) excludeWordsPref.summary = summary + ":" + value

        excludeWordsPref.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { preference, o ->
            val summary = resources.getString(R.string.pref_watch_exclude_words_summary)
            if (o != null && o.toString().length > 0) {
                preference.summary = summary + ":" + o.toString()
            } else {
                preference.summary = summary
            }
            true
        }
    }
}
