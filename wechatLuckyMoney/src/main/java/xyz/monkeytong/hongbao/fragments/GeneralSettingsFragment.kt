package xyz.monkeytong.hongbao.fragments

import android.content.Intent
import android.os.Bundle
import android.preference.Preference
import android.preference.PreferenceFragment
import android.preference.PreferenceManager
import xyz.monkeytong.hongbao.R
import xyz.monkeytong.hongbao.activities.WebViewActivity
import xyz.monkeytong.hongbao.utils.UpdateTask

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
        // Check for updates
        val updatePref = findPreference("pref_etc_check_update")
        updatePref.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            UpdateTask(activity.applicationContext, true).update()
            false
        }

        // Open issue
        val issuePref = findPreference("pref_etc_issue")
        issuePref.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            val webViewIntent = Intent(activity, WebViewActivity::class.java)
            webViewIntent.putExtra("title", "GitHub Issues")
            webViewIntent.putExtra("url", getString(R.string.url_github_issues))
            webViewIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(webViewIntent)
            false
        }

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
